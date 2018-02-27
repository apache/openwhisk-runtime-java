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

import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author kameshs
 */
public class JarLoaderTest {

    @Test
    public void testMainFromManifest()  {
        try {
            String jarFileStr = encodeJarFile("jar-with-manifest-main.jar");
            Path jarPath = JarLoader.saveBase64EncodedFile(new ByteArrayInputStream(jarFileStr.getBytes()));
            JarLoader jarLoader = new JarLoader(jarPath, "");
            JsonObject response = jarLoader.invokeMain(new JsonObject(), Collections.emptyMap());
            assertNotNull(response);
            assertEquals("{\"greetings\":\"Hello! Welcome to OpenWhisk\"}",response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
       
    }
    
    @Test
    public void testMainViaParam()  {
        try {
            /** START - This is to Simulation how it done via Proxy **/
            String jarFileStr = encodeJarFile("jar-without-manifest-main.jar");
            /** END  - Proxy Simulation **/
            Path jarPath = JarLoader.saveBase64EncodedFile(new ByteArrayInputStream(jarFileStr.getBytes()));
            JarLoader jarLoader = new JarLoader(jarPath, "com.example.FunctionApp");
            JsonObject response = jarLoader.invokeMain(new JsonObject(), Collections.emptyMap());
            assertNotNull(response);
            assertEquals("{\"greetings\":\"Hello! Welcome to OpenWhisk\"}",response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }



    private String encodeJarFile(String path) throws URISyntaxException, IOException {
        /** START - This is to Simulation how it done via Proxy **/
        Path coreJarPath = Paths.get(getClass().getClassLoader().getResource(path).toURI());
        Base64.Encoder encoder = Base64.getEncoder();
        /** END  - Proxy Simulation **/
        return encoder.encodeToString(Files.readAllBytes(coreJarPath));
    }


}