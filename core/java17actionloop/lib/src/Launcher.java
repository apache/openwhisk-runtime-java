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


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class Launcher {

    private static String mainClassName = "Main";
    private static String mainMethodName = "main";

    public static void main(String[] args) throws Exception {
//        System.out.println("args are >>");
//        Arrays.stream(args).forEach(System.out::println);
//        System.out.println("<<");
        var orchestrator = new Orchestrator();
        // exit after main class loading if "exit" specified
        // used to check healthy launch after init
        if (args.length > 1 && Objects.equals(args[1], "-exit")) {
            System.exit(0);
        }
        orchestrator.writeData(orchestrator.getMainMethod(args));
    }

    public static class Orchestrator {
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void augmentEnv(Map<String, String> newEnv) {

            try {
                Class cl = Class.forName("java.util.Collections$UnmodifiableMap");
                Field field = cl.getDeclaredField("m");
                field.setAccessible(true);
                Map<String, String> obj = (Map<String, String>) field.get(System.getenv());
                obj.putAll(newEnv);
            } catch (Exception e) {
            }
        }

        private Method getMainMethod(String[] args) throws Exception {
            if (args.length > 0) {
                mainClassName = args[0];
            }
            int pos = mainClassName.indexOf("#");
            setMainMethodName(args, pos);
//            mainClass = Class.forName("test."+mainClassName);
            Class mainClass = Class.forName(mainClassName);
            Method m = mainClass.getMethod(mainMethodName, new Class[]{JsonObject.class});
            m.setAccessible(true);
            int modifiers = m.getModifiers();
            if (m.getReturnType() != JsonObject.class || !Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                throw new NoSuchMethodException(mainMethodName);
            }
           return m;
        }

        private static JsonElement getResponse(AtomicReference<JsonObject> payload,
                                               HashMap<String, String> env,
                                               Method mainMethod) {

            JsonElement response = null;
            try {
                response = invokeMain(payload.get(), env, mainMethod);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            return response;
        }

        private static JsonObject invokeMain(JsonObject arg,
                                             Map<String, String> env,
                                             Method mainMethod) throws Exception {

            augmentEnv(env);
            return (JsonObject) mainMethod.invoke(null, arg);
        }

        private static void setMainMethodName(String[] args,
                                              int pos) {

            if (pos == -1) {
                return;
            }
            if (pos + 1 != mainClassName.length()) {
                mainMethodName = args[0].substring(pos + 1);
            }
            mainClassName = args[0].substring(0, pos);
        }

        private void writeData(Method mainMethod) throws FileNotFoundException {
            JsonObject empty = JsonParser.parseString("{}").getAsJsonObject();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream("/dev/fd/3"), StandardCharsets.UTF_8));
            try (in; out) {
                in.lines().forEach(l -> {
                    JsonElement element = JsonParser.parseString(l);
                    AtomicReference<JsonObject> payload = new AtomicReference<>(empty.deepCopy());
                    HashMap<String, String> env = new HashMap<>();
                    if (element.isJsonObject()) {
                        element.getAsJsonObject().entrySet()
                                .stream()
                                .filter(e -> e.getKey().equals("value"))
                                .forEach(el -> payload.set(el.getValue().getAsJsonObject()));
                        element.getAsJsonObject().entrySet()
                                .stream()
                                .filter(e -> !e.getKey().equals("value"))
                                .forEach(el -> env.put(String.format("__OW_%s", el.getKey().toUpperCase()),
                                        el.getValue().getAsString()));

                    }

                    JsonElement response = getResponse(payload, env, mainMethod);
                    out.println(response.toString());
                    out.flush();
                });
            } catch (NullPointerException npe) {
                System.out.println("the action returned null");
                npe.printStackTrace(System.err);
                JsonObject error = new JsonObject();
                error.addProperty("error", "the action returned null");
                out.println(error);
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                JsonObject error = new JsonObject();
                error.addProperty("error", ex.getMessage());
                out.println(error);
            } finally {
                System.out.flush();
                System.err.flush();
            }
        }
    }
}
