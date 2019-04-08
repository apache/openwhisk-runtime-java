<!--
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
-->

# Apache OpenWhisk runtimes for java

[![Build Status](https://travis-ci.org/apache/incubator-openwhisk-runtime-java.svg?branch=master)](https://travis-ci.org/apache/incubator-openwhisk-runtime-java)

## Changelogs
- [Java 8 CHANGELOG.md](core/java8/CHANGELOG.md)


## Quick Java Action
A Java action is a Java program with a method called `main` that has the exact signature as follows:
```java
public static com.google.gson.JsonObject main(com.google.gson.JsonObject);
```

For example, create a Java file called `Hello.java` with the following content:

```java
import com.google.gson.JsonObject;

public class Hello {
    public static JsonObject main(JsonObject args) {
        String name = "stranger";
        if (args.has("name"))
            name = args.getAsJsonPrimitive("name").getAsString();
        JsonObject response = new JsonObject();
        response.addProperty("greeting", "Hello " + name + "!");
        return response;
    }
}
```
In order to compile, test and archive Java files, you must have a [JDK 8](http://openjdk.java.net/install/) installed locally.

Then, compile `Hello.java` into a JAR file `hello.jar` as follows:
```
javac Hello.java
```
```
jar cvf hello.jar Hello.class
```

**Note:** [google-gson](https://github.com/google/gson) must exist in your Java CLASSPATH when compiling the Java file.

You need to specify the name of the main class using `--main`. An eligible main
class is one that implements a static `main` method as described above. If the
class is not in the default package, use the Java fully-qualified class name,
e.g., `--main com.example.MyMain`.

If needed you can also customize the method name of your Java action. This
can be done by specifying the Java fully-qualified method name of your action,
e.q., `--main com.example.MyMain#methodName`

### Create the Java Action
To use as a docker action:
```
wsk action update helloJava hello.jar --main Hello --docker openwhisk/java8action
```
This works on any deployment of Apache OpenWhisk

To use on a deployment of OpenWhisk that contains the runtime as a kind:
```
wsk action update helloJava hello.jar --main Hello --kind java:8
```

### Invoke the Java Action
Action invocation is the same for Java actions as it is for Swift and JavaScript actions:

```
wsk action invoke --result helloJava --param name World
```

```json
  {
      "greeting": "Hello World!"
  }
```

## Local development
```
./gradlew core:java8:distDocker
```
This will produce the image `whisk/java8action`

Build and Push image
```
docker login
./gradlew core:java8:distDocker -PdockerImagePrefix=$prefix-user -PdockerRegistry=docker.io
```

Deploy OpenWhisk using ansible environment that contains the kind `java:8`
Assuming you have OpenWhisk already deploy localy and `OPENWHISK_HOME` pointing to root directory of OpenWhisk core repository.

Set `ROOTDIR` to the root directory of this repository.

Redeploy OpenWhisk
```
cd $OPENWHISK_HOME/ansible
ANSIBLE_CMD="ansible-playbook -i ${ROOTDIR}/ansible/environments/local"
$ANSIBLE_CMD setup.yml
$ANSIBLE_CMD couchdb.yml
$ANSIBLE_CMD initdb.yml
$ANSIBLE_CMD wipe.yml
$ANSIBLE_CMD openwhisk.yml
```

Or you can use `wskdev` and create a soft link to the target ansible environment, for example:
```
ln -s ${ROOTDIR}/ansible/environments/local ${OPENWHISK_HOME}/ansible/environments/local-java
wskdev fresh -t local-java
```

### Testing
Install dependencies from the root directory on $OPENWHISK_HOME repository
```
pushd $OPENWHISK_HOME
./gradlew install
podd $OPENWHISK_HOME
```

Using gradle to run all tests
```
./gradlew :tests:test
```
Using gradle to run some tests
```
./gradlew :tests:test --tests *ActionContainerTests*
```
Using IntelliJ:
- Import project as gradle project.
- Make sure working directory is root of the project/repo

#### Using container image to test
To use as docker action push to your own dockerhub account
```
docker tag whisk/java8action $user_prefix/java8action
docker push $user_prefix/java8action
```
Then create the action using your the image from dockerhub
```
wsk action update helloJava hello.jar --main Hello --docker $user_prefix/java8action
```
The `$user_prefix` is usually your dockerhub user id.



# License
[Apache 2.0](LICENSE.txt)

# Disclaimer

Apache OpenWhisk Runtime Java is an effort undergoing incubation at The Apache Software Foundation (ASF), sponsored by the Apache Incubator. Incubation is required of all newly accepted projects until a further review indicates that the infrastructure, communications, and decision making process have stabilized in a manner consistent with other successful ASF projects. While incubation status is not necessarily a reflection of the completeness or stability of the code, it does indicate that the project has yet to be fully endorsed by the ASF.
