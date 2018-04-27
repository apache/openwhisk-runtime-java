/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package openwhisk.java.action;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import com.google.gson.JsonParser;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ErrorHandler;

public class Proxy extends AbstractVerticle {

  private static final int HTTP_INTERNAL_SERVER_ERROR = 500;
  // Return 502 if the function is not found
  private static final int ERROR_FUNCTION_NOT_FOUND = 502;
  private static final int HTTP_OK = 200;

  private static final String[] OW_ENV_KEYS = { "api_key", "namespace", "action_name", "activation_id", "deadline" };
  private static final int PORT = 8080;

  private URLClassLoader urlClassLoader;
  private Method mainMethod;

  @Override
  public void start() throws InterruptedException {
    HttpServer httpServer = vertx.createHttpServer();
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().failureHandler(ErrorHandler.create(true));

    router.route("/init").handler(this::initHandler);
    router.route("/run").handler(this::runHandler);

    httpServer.requestHandler(router::accept).listen(PORT);
  }

  private void initHandler(RoutingContext rc) {
    if (urlClassLoader != null || mainMethod != null) {
      rc.response().setStatusCode(HTTP_INTERNAL_SERVER_ERROR).end("Cannot initialize the action more than once.");
      return;
    }

    final JsonObject request = rc.getBodyAsJson();
    final JsonObject value = request.getJsonObject("value");
    final byte[] jarBinary = value.getBinary("code");

    Proxy.writeCodeOnFileSystem(vertx, jarBinary, ar -> {
      if (ar.failed()) {
        rc.fail(ar.cause());
      } else {
        if (!ar.result().toFile().isFile()) {
          rc.response().setStatusCode(HTTP_INTERNAL_SERVER_ERROR).end("Error invoking function");
        }
        try {
          final URL url = ar.result().toUri().toURL();
          urlClassLoader = new URLClassLoader(new URL[] { url});
          String clazzName = value.getString("main");
          String methodName = "main";
          if(clazzName == null || clazzName.isEmpty()){
            clazzName = getClassNameFromManifest(url);
          }
          if(clazzName.indexOf('#')>-1){
            final String[] splitted = clazzName.split("#");
            clazzName = splitted[0];
            methodName= splitted[1];
          }
          if(clazzName == null ){
            rc.response().setStatusCode(ERROR_FUNCTION_NOT_FOUND).end("Main class is empty, can not determine the function to invoke");
            return;
          }
          final Class<?> mainClass = urlClassLoader.loadClass(clazzName);

          final Method m = mainClass.getMethod(methodName, new Class[] { com.google.gson.JsonObject.class });
          m.setAccessible(true);
          final int modifiers = m.getModifiers();
          if (m.getReturnType() != com.google.gson.JsonObject.class || !Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            throw new NoSuchMethodException(methodName);
          }
          this.mainMethod = m;

          rc.response().setStatusCode(HTTP_OK).end("OK");

        }catch(NoSuchMethodException e){
          rc.response().setStatusCode(ERROR_FUNCTION_NOT_FOUND).end(e);
        }
        catch (ClassNotFoundException | IOException e) {
          rc.response().setStatusCode(HTTP_INTERNAL_SERVER_ERROR).end("An error has occurred (see logs for details): " + e);
        }
      }
    });
  }

  private String getClassNameFromManifest(URL url ) throws IOException{
    final URL jarUrl = new URL("jar", "", url + "!/");
    final JarURLConnection connection = (JarURLConnection)jarUrl.openConnection();
    final Attributes attributes = connection.getMainAttributes();
    return attributes != null
                   ? attributes.getValue(Attributes.Name.MAIN_CLASS)
                   : null;
  }

  private void runHandler(RoutingContext rc) {
    final JsonObject request = rc.getBodyAsJson();
    final JsonObject value = request.getJsonObject("value");
    final JsonParser parser = new JsonParser();
    final com.google.gson.JsonObject req = parser.parse(value.toString()).getAsJsonObject();

    final HashMap<String, String> env = new HashMap<>();
    for (String envKey : OW_ENV_KEYS) {
      env.put(String.format("__OW_%s", envKey.toUpperCase()), value.getString(envKey));
    }
    augmentEnv(env);

    Thread.currentThread().setContextClassLoader(urlClassLoader);
    System.setSecurityManager(new WhiskSecurityManager());

    try {
      final com.google.gson.JsonObject out = (com.google.gson.JsonObject) this.mainMethod.invoke(null, req);
      if (out == null) {
        printAndRespond(rc, new NullPointerException("The action returned null"));
      }else{
        rc.response().setStatusCode(HTTP_OK).putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(out.toString());
      }
    } catch (IllegalAccessException e) {
      printAndRespond(rc, e);
    } catch (InvocationTargetException e) {
      printAndRespond(rc, e.getCause());
    }

  }

  private void printAndRespond(RoutingContext rc, Throwable e) {
    rc.response().setStatusCode(HTTP_INTERNAL_SERVER_ERROR).end("An error has occurred (see logs for details): " + e);
  }

  static void writeCodeOnFileSystem(Vertx vertx, byte[] binary, Handler<AsyncResult<Path>> completionHandler) {
    try {
      final File file = File.createTempFile("useraction", ".jar");
      vertx.fileSystem().writeFile(file.getAbsolutePath(), Buffer.buffer(binary), res -> {
        if (res.failed()) {
          completionHandler.handle(Future.failedFuture(res.cause()));
        } else {
          completionHandler.handle(Future.succeededFuture(file.toPath()));
        }
      });
    } catch (IOException e) {
      completionHandler.handle(Future.failedFuture(e));
    }
  }

  public static void main(String[] args) throws InterruptedException {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new Proxy());
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void augmentEnv(Map<String, String> newEnv) {
      try {
          for (Class cl : Collection.class.getDeclaredClasses()) {
              if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                  Field field = cl.getDeclaredField("m");
                  field.setAccessible(true);
                  Object obj = field.get(System.getenv());
                  Map<String, String> map = (Map<String, String>) obj;
                  map.putAll(newEnv);
              }
          }
      } catch (Exception e) {
        // Not handled.
      }
  }

}
