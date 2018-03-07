<!--
#
# Licensed to the Apache Software Foundation (ASF) under one or more contributor 
# license agreements.  See the NOTICE file distributed with this work for additional 
# information regarding copyright ownership.  The ASF licenses this file to you
# under the Apache License, Version 2.0 (the # "License"); you may not use this 
# file except in compliance with the License.  You may obtain a copy of the License 
# at:
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed 
# under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
# CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations under the License.
#
-->

# Apache OpenWhisk runtimes for java

[![Build Status](https://travis-ci.org/apache/incubator-openwhisk-runtime-java.svg?branch=master)](https://travis-ci.org/apache/incubator-openwhisk-runtime-java)


### Give it a try today
To use as a docker action
```
wsk action update myAction myAction.jar --docker openwhisk/java8action:1.0.0
```
This works on any deployment of Apache OpenWhisk

### To use on deployment that contains the rutime as a kind
To use as a kind action
```
wsk action update myAction myAction.jar --kind java:8
```

### Local development
```
./gradlew core:javaAction:distDocker
```
This will produce the image `whisk/java8action`

Build and Push image
```
docker login
./gradlew core:javaAction:distDocker -PdockerImagePrefix=$prefix-user -PdockerRegistry=docker.io 
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
./gradlew :common:scala:install :core:controller:install :core:invoker:install :tests:install
```

Using gradle for the ActionContainer tests you need to use a proxy if running on Mac, if Linux then don't use proxy options
You can pass the flags `-Dhttp.proxyHost=localhost -Dhttp.proxyPort=3128` directly in gradle command.
Or save in your `$HOME/.gradle/gradle.properties`
```
systemProp.http.proxyHost=localhost
systemProp.http.proxyPort=3128
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
- Add the following Java VM properties in ScalaTests Run Configuration, easiest is to change the Defaults for all ScalaTests to use this VM properties
```
-Dhttp.proxyHost=localhost
-Dhttp.proxyPort=3128
```

#### Using container image to test
To use as docker action push to your own dockerhub account
```
docker tag whisk/java8action $user_prefix/java8action
docker push $user_prefix/java8action
```
Then create the action using your the image from dockerhub
```
wsk action update myAction myAction.jar --docker $user_prefix/java8action
```
The `$user_prefix` is usually your dockerhub user id.



# License
[Apache 2.0](LICENSE.txt)


