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

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.*;
import java.security.Permission;
import java.lang.reflect.InvocationTargetException;

class Launcher {

    private static String mainClassName = "Main";
    private static String mainMethodName = "main";
    private static Class mainClass = null;
    private static Method mainMethod = null;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void augmentEnv(Map<String, String> newEnv) {
        try {
            for (Class cl : Collections.class.getDeclaredClasses()) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(System.getenv());
                    Map<String, String> map = (Map<String, String>) obj;
                    map.putAll(newEnv);
                }
            }
        } catch (Exception e) {}
    }

    private static void initMain(String[] args) throws Exception {
        if(args.length > 0)
            mainClassName = args[0];
        int pos = mainClassName.indexOf("#");
        if(pos != -1) {
            if(pos + 1 != mainClassName.length())
                mainMethodName = args[0].substring(pos+1);
            mainClassName = args[0].substring(0,pos);
        }

        mainClass = Class.forName(mainClassName);
        Method[] methods = mainClass.getDeclaredMethods();
        Boolean existMain = false;
        for(Method method: methods) {
            if (method.getName().equals(mainMethodName)) {
                existMain = true;
                break;
            }
        }
        if (!existMain) {
            throw new NoSuchMethodException(mainMethodName);
        }
    }

    private static Object invokeMain(JsonElement arg, Map<String, String> env) throws Exception {
        augmentEnv(env);
        return mainMethod.invoke(null, arg);
    }

    private static SecurityManager defaultSecurityManager = null;
    private static void installSecurityManager() {
        defaultSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission p) {
                // Not throwing means accepting anything.
            }

            @Override
            public void checkPermission(Permission p, Object ctx) {
                // Not throwing means accepting anything.
            }

            @Override
            public void checkExit(int status) {
                super.checkExit(status);
                throw new SecurityException("System.exit(" + status + ") called from within an action.");
            }
        });
    }

    private static void uninstallSecurityManager() {
        if(defaultSecurityManager != null) {
            System.setSecurityManager(defaultSecurityManager);
        }
    }

    public static void main(String[] args) throws Exception {

        initMain(args);

        // exit after main class loading if "exit" specified
        // used to check healthy launch after init
        if(args.length >1 && args[1] == "-exit")
            System.exit(0);

        // install a security manager to prevent exit
        installSecurityManager();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, "UTF-8"));
        PrintWriter out = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream("/dev/fd/3"), "UTF-8"));
        JsonParser json = new JsonParser();
        JsonObject emptyForJsonObject = json.parse("{}").getAsJsonObject();
        JsonArray emptyForJsonArray = json.parse("[]").getAsJsonArray();
        Boolean isJsonObjectParam = true;
        String input = "";
        while (true) {
            try {
                input = in.readLine();
                if (input == null)
                    break;
                JsonElement element = json.parse(input);
                JsonObject payloadForJsonObject = emptyForJsonObject.deepCopy();
                JsonArray payloadForJsonArray = emptyForJsonArray.deepCopy();
                HashMap<String, String> env = new HashMap<String, String>();
                if (element.isJsonObject()) {
                    // collect payload and environment
                    for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                        if (entry.getKey().equals("value")) {
                            if (entry.getValue().isJsonObject())
                                payloadForJsonObject = entry.getValue().getAsJsonObject();
                            else {
                                payloadForJsonArray = entry.getValue().getAsJsonArray();
                                isJsonObjectParam = false;
                            }
                        } else {
                            env.put(String.format("__OW_%s", entry.getKey().toUpperCase()),
                                    entry.getValue().getAsString());
                        }
                    }
                    augmentEnv(env);
                }

                Method m = null;
                if (isJsonObjectParam) {
                    m = mainClass.getMethod(mainMethodName, new Class[] { JsonObject.class });
                } else {
                    m = mainClass.getMethod(mainMethodName, new Class[] { JsonArray.class });
                }
                m.setAccessible(true);
                int modifiers = m.getModifiers();
                if ((m.getReturnType() != JsonObject.class && m.getReturnType() != JsonArray.class) || !Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                    throw new NoSuchMethodException(mainMethodName);
                }
                mainMethod = m;

                Object response;
                if (isJsonObjectParam) {
                    response = invokeMain(payloadForJsonObject, env);
                } else {
                    response = invokeMain(payloadForJsonArray, env);
                }
                out.println(response.toString());
            } catch(NullPointerException npe) {
                System.out.println("the action returned null");
                npe.printStackTrace(System.err);
                JsonObject error = new JsonObject();
                error.addProperty("error", "the action returned null");
                out.println(error.toString());
                out.flush();
            } catch(InvocationTargetException ite) {
                Throwable ex = ite;
                if(ite.getCause() != null)
                    ex = ite.getCause();
                ex.printStackTrace(System.err);
                JsonObject error = new JsonObject();
                error.addProperty("error", ex.getMessage());
                out.println(error.toString());
                out.flush();
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                JsonObject error = new JsonObject();
                error.addProperty("error", ex.getMessage());
                out.println(error.toString());
                out.flush();
            }
            out.flush();
            System.out.flush();
            System.err.flush();
        }
        uninstallSecurityManager();
    }
}

