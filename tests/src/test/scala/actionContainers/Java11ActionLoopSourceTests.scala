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

import common.WskActorSystem
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import spray.json.{JsArray, JsNumber, JsObject}

@RunWith(classOf[JUnitRunner])
class Java11ActionLoopSourceTests extends JavaActionLoopSourceTests with WskActorSystem {

  override val image = "actionloop-java-v11"

  behavior of "Java 11 actionloop"

  it should s"support local-variable syntax for lambda parameters (JEP 323)" in {
    val config = TestConfig(
      """
        |package example;
        |
        |import com.google.gson.JsonArray;
        |import com.google.gson.JsonObject;
        |import java.util.stream.IntStream;
        |
        |/**
        | * Test JEP 323: Local-Variable Syntax for Lambda Parameters
        | * http://openjdk.java.net/jeps/323
        | */
        |public class Java11Jep323Test {
        |    public static JsonObject main(JsonObject args) throws Exception {
        |        JsonObject response = new JsonObject();
        |        JsonArray list = new JsonArray();
        |
        |        IntStream.range(1, 5)
        |                // local-variable syntax
        |                .filter((var i) -> i % 2 == 0)
        |                .forEach(list::add);
        |
        |        response.add("list", list);
        |        return response;
        |    }
        |}
      """.stripMargin,
      main = "example.Java11Jep323Test")

    val (out, err) = withActionContainer() { c =>
      val (initCode, _) = c.init(initPayload(config.code, config.main))
      initCode should be(200)

      val response = JsObject("list" -> JsArray(JsNumber(2), JsNumber(4)))
      val (runCode, out) = c.run(JsObject.empty)
      runCode should be(200)
      out should be(Some(response))
    }

    checkStreams(out, err, {
      case (o, e) =>
        o shouldBe empty
        e shouldBe empty
    })
  }
}
