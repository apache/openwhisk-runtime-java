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

package org.apache.openwhisk.runtime.java.action;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Proxy {
    private HttpServer server;

    private JarLoader loader = null;

    public Proxy(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), -1);

        this.server.createContext("/init", new InitHandler());
        this.server.createContext("/run", new RunHandler());
        this.server.setExecutor(null); // creates a default executor
    }

    public void start() {
        server.start();
    }

    private static void writeResponse(HttpExchange t, int code, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(code, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void writeError(HttpExchange t, String errorMessage) throws IOException {
        JsonObject message = new JsonObject();
        message.addProperty("error", errorMessage);
        writeResponse(t, 502, message.toString());
    }

    private static void writeLogMarkers() {
        System.out.println("XXX_THE_END_OF_A_WHISK_ACTIVATION_XXX");
        System.err.println("XXX_THE_END_OF_A_WHISK_ACTIVATION_XXX");
        System.out.flush();
        System.err.flush();
    }

    private class InitHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            if (loader != null) {
                String errorMessage = "Cannot initialize the action more than once.";
                System.err.println(errorMessage);
                Proxy.writeError(t, errorMessage);
                return;
            }

            try {
                InputStream is = t.getRequestBody();
                JsonParser parser = new JsonParser();
                JsonElement ie = parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
                JsonObject inputObject = ie.getAsJsonObject();

                if (inputObject.has("value")) {
                    JsonObject message = inputObject.getAsJsonObject("value");
                    if (message.has("main") && message.has("code")) {
                        String mainClass = message.getAsJsonPrimitive("main").getAsString();
                        String base64Jar = message.getAsJsonPrimitive("code").getAsString();

                        // FIXME: this is obviously not very useful. The idea is that we
                        // will implement/use a streaming parser for the incoming JSON object so that we
                        // can stream the contents of the jar straight to a file.
                        InputStream jarIs = new ByteArrayInputStream(base64Jar.getBytes(StandardCharsets.UTF_8));

                        // Save the bytes to a file.
                        Path jarPath = JarLoader.saveBase64EncodedFile(jarIs);

                        // Start up the custom classloader. This also checks that the
                        // main method exists.
                        loader = new JarLoader(jarPath, mainClass);

                        Proxy.writeResponse(t, 200, "OK");
                        return;
                    }
                }

                Proxy.writeError(t, "Missing main/no code to execute.");
                return;
            } catch (Exception e) {
                e.printStackTrace(System.err);
                writeLogMarkers();
                Proxy.writeError(t, "An error has occurred (see logs for details): " + e);
                return;
            }
        }
    }

    private class RunHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            if (loader == null) {
                Proxy.writeError(t, "Cannot invoke an uninitialized action.");
                return;
            }

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            SecurityManager sm = System.getSecurityManager();

            try {
                InputStream is = t.getRequestBody();
                JsonParser parser = new JsonParser();
                JsonObject body = parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))).getAsJsonObject();
                JsonParser json = new JsonParser();
                JsonObject payloadForJsonObject = json.parse("{}").getAsJsonObject();
                JsonArray payloadForJsonArray = json.parse("[]").getAsJsonArray();
                Boolean isJsonObjectParam = true;
                JsonElement inputJsonElement = body.get("value");
                if (inputJsonElement.isJsonObject()) {
                    payloadForJsonObject = inputJsonElement.getAsJsonObject();
                } else {
                    payloadForJsonArray = inputJsonElement.getAsJsonArray();
                    isJsonObjectParam = false;
                }

                HashMap<String, String> env = new HashMap<String, String>();
                Set<Map.Entry<String, JsonElement>> entrySet = body.entrySet();
                for(Map.Entry<String, JsonElement> entry : entrySet){
                    try {
                        if(!entry.getKey().equalsIgnoreCase("value"))
                            env.put(String.format("__OW_%s", entry.getKey().toUpperCase()), entry.getValue().getAsString());
                    } catch (Exception e) {}
                }

                Thread.currentThread().setContextClassLoader(loader);
                System.setSecurityManager(new WhiskSecurityManager());

                Method mainMethod = null;
                String mainMethodName = loader.entrypointMethodName;
                if (isJsonObjectParam) {
                    mainMethod = loader.mainClass.getMethod(mainMethodName, new Class[] { JsonObject.class });
                } else {
                    mainMethod = loader.mainClass.getMethod(mainMethodName, new Class[] { JsonArray.class });
                }
                mainMethod.setAccessible(true);
                int modifiers = mainMethod.getModifiers();
                if ((mainMethod.getReturnType() != JsonObject.class && mainMethod.getReturnType() != JsonArray.class) || !Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                    throw new NoSuchMethodException(mainMethodName);
                }

                // User code starts running here. the return object supports JsonObject and JsonArray both.
                Object output;
                if (isJsonObjectParam) {
                    loader.augmentEnv(env);
                    output = mainMethod.invoke(null, payloadForJsonObject);
                } else {
                    loader.augmentEnv(env);
                    output = mainMethod.invoke(null, payloadForJsonArray);
                }
                // User code finished running here.

                if (output == null) {
                    throw new NullPointerException("The action returned null");
                }

                Proxy.writeResponse(t, 200, output.toString());
                return;
            } catch (InvocationTargetException ite) {
                // These are exceptions from the action, wrapped in ite because
                // of reflection
                Throwable underlying = ite.getCause();
                underlying.printStackTrace(System.err);
                Proxy.writeError(t,
                        "An error has occurred while invoking the action (see logs for details): " + underlying);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                Proxy.writeError(t, "An error has occurred (see logs for details): " + e);
            } finally {
                writeLogMarkers();
                System.setSecurityManager(sm);
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    public static void main(String args[]) throws Exception {
        Proxy proxy = new Proxy(8080);
        proxy.start();
    }
}
