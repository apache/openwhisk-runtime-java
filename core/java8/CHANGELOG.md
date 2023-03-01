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

# Java 8 OpenWhisk Runtime Container
# next release
 - use `ibm-semeru-runtimes:open-8u362-b09-jdk-focal` as baseimage
 - update gson version to 2.9.0

# 1.19.0
 - Use adoptopenjdk/openjdk8-openj9:x86_64-ubuntu-jdk8u332-b09_openj9-0.32.0

## 1.18.0
  - Use adoptopenjdk/openjdk8-openj9:x86_64-ubuntu-jdk8u322-b06_openj9-0.30.0

## 1.17.0
  - Resolve akka versions explicitly. (#124, #123)

## 1.16.0
  - Use adoptopenjdk/openjdk8-openj9:x86_64-ubuntu-jdk8u282-b08_openj9-0.24.0

## 1.15.0
  - Include latest security fixes with every build.
  - Use adoptopenjdk/openjdk8-openj9:x86_64-ubuntu-jdk8u262-b10_openj9-0.21.0

## 1.14.0
  - Support for __OW_ACTION_VERSION (openwhisk/4761)
  - Use adoptopenjdk/openjdk8-openj9:x86_64-ubuntu-jdk8u222-b10_openj9-0.15.1

## 1.13.0-incubating
  - Use jdk x86_64-ubuntu-jdk8u181-b13_openj9-0.9.0

## 1.1.4
Changes:
- Update jdk adoptopenjdk/openjdk8-openj9:x86_64-ubuntu-jdk8u212-b04_openj9-0.14.2 [jdk8u181-b13_openj9-0.9.0](https://hub.docker.com/r/adoptopenjdk/openjdk8-openj9/tags/)
  Starting with [openj9-0.11.0}(https://github.com/eclipse/openj9/blob/b44844b02466ddf195eb9d8d6587ed89374a5f2a/doc/release-notes/0.11/0.11.md) container awareness is activated by default. This means, when the VM is running in a container, and a memory limit is set, the VM allocates more memory to the Java heap. Depending on the size of the memory limit.

## 1.1.3
Changes:
- Update jdk x86_64-ubuntu-jdk8u181-b13_openj9-0.9.0 and push latest hash [jdk8u181-b13_openj9-0.9.0](https://hub.docker.com/r/adoptopenjdk/openjdk8-openj9/tags/) [#77](https://github.com/apache/openwhisk-runtime-java/pull/77/files)

## 1.1.2
Changes:
-  Update run handler to accept more environment variables [#67](https://github.com/apache/openwhisk-runtime-java/pull/67)

## 1.1.1
Changes:
- Adds log markers.
- Improve error handling for improper initialization.

## 1.1.0
Changes:
- Replaced oracle [jdk8u131-b11](http://download.oracle.com/otn-pub/java/jdk/"${VERSION}"u"${UPDATE}"-b"${BUILD}"/d54c1d3a095b4ff2b6607d096fa80163/server-jre-"${VERSION}"u"${UPDATE}"-linux-x64.tar.gz) with OpenJDK [adoptopenjdk/openjdk8-openj9:jdk8u162-b12_openj9-0.8.0](https://hub.docker.com/r/adoptopenjdk/openjdk8-openj9)

## 1.0.1
Changes:
- Allow custom name for main Class

## 1.0.0
Changes:
- Initial release
