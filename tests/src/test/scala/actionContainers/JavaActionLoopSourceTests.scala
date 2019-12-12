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

package actionContainers

import actionContainers.ActionContainer.withContainer
import common.WskActorSystem
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JavaActionLoopSourceTests extends BasicActionRunnerTests with WskActorSystem {

  val image = "actionloop-java-v8"

  // Helpers specific to java actions
  override def withActionContainer(env: Map[String, String] = Map.empty)(
    code: ActionContainer => Unit): (String, String) = withContainer(image, env)(code)

  behavior of "Java actionloop"

  override val testNoSourceOrExec = {
    TestConfig("")
  }

  override val testNotReturningJson = {
    // skip this test since and add own below (see Nuller)
    TestConfig("", skipTest = true)
  }

  override val testEnv = {
    TestConfig(
      """
          |package example;
          |
          |import com.google.gson.JsonObject;
          |
          |public class HelloWhisk {
          |     public static JsonObject main(JsonObject args) {
          |         JsonObject response = new JsonObject();
          |         response.addProperty("api_host", System.getenv("__OW_API_HOST"));
          |         response.addProperty("api_key", System.getenv("__OW_API_KEY"));
          |         response.addProperty("namespace", System.getenv("__OW_NAMESPACE"));
          |         response.addProperty("action_name", System.getenv("__OW_ACTION_NAME"));
          |         response.addProperty("action_version", System.getenv("__OW_ACTION_VERSION"));
          |         response.addProperty("activation_id", System.getenv("__OW_ACTIVATION_ID"));
          |         response.addProperty("deadline", System.getenv("__OW_DEADLINE"));
          |         return response;
          |     }
          |}
        """.stripMargin.trim,
      main = "example.HelloWhisk")
  }

  override val testEcho = {
    TestConfig(
      """
          |package example;
          |
          |import com.google.gson.JsonObject;
          |
          |public class HelloWhisk {
          |     public static JsonObject main(JsonObject args) {
          |         System.out.println("hello stdout");
          |         System.err.println("hello stderr");
          |         return args;
          |     }
          |}
        """.stripMargin.trim,
      "example.HelloWhisk")

  }

  override val testUnicode = {
    TestConfig(
      """
          |package example;
          |
          |import com.google.gson.JsonObject;
          |
          |public class HelloWhisk {
          |     public static JsonObject main(JsonObject args) {
          |         String delimiter = args.getAsJsonPrimitive("delimiter").getAsString();
          |         JsonObject response = new JsonObject();
          |         String str = delimiter + " ☃ " + delimiter;
          |         System.out.println(str);
          |         response.addProperty("winter", str);
          |         return response;
          |     }
          |}
        """.stripMargin,
      "example.HelloWhisk")
  }

  def echo(main: String = "main") = {
    s"""
        |package example;
        |
        |import com.google.gson.JsonObject;
        |
        |public class HelloWhisk {
        |     public static JsonObject $main(JsonObject args) {
        |         return args;
        |     }
        |}
      """.stripMargin.trim
  }

  override val testInitCannotBeCalledMoreThanOnce = {
    TestConfig(echo(), "example.HelloWhisk")
  }

  override val testEntryPointOtherThanMain = {
    TestConfig(echo("naim"), "example.HelloWhisk#naim")
  }

  override val testLargeInput = {
    TestConfig(echo(), "example.HelloWhisk")
  }

}
