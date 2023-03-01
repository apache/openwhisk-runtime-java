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

# build go proxy from source
FROM golang:1.20 AS builder_source
ARG GO_PROXY_GITHUB_USER=apache
ARG GO_PROXY_GITHUB_BRANCH=master
RUN git clone --branch ${GO_PROXY_GITHUB_BRANCH} \
   https://github.com/${GO_PROXY_GITHUB_USER}/openwhisk-runtime-go /src ;\
   cd /src ; env GO111MODULE=on CGO_ENABLED=0 go build main/proxy.go && \
   mv proxy /bin/proxy

# or build it from a release
FROM golang:1.20 AS builder_release
ARG GO_PROXY_RELEASE_VERSION=1.20@1.21.0
RUN curl -sL \
  https://github.com/apache/openwhisk-runtime-go/archive/{$GO_PROXY_RELEASE_VERSION}.tar.gz\
  | tar xzf -\
  && cd openwhisk-runtime-go-*/main\
  && GO111MODULE=on CGO_ENABLED=0 go build -o /bin/proxy

# Use AdoptOpenJDK's JDK8, OpenJ9, ubuntu
FROM ibm-semeru-runtimes:open-8u362-b09-jdk-focal

# select the builder to use
ARG GO_PROXY_BUILD_FROM=release

RUN rm -rf /var/lib/apt/lists/* \
    && apt-get clean \
    && apt-get update \
    && apt-get -y --no-install-recommends upgrade \
    && apt-get -y --no-install-recommends install locales python vim \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && locale-gen en_US.UTF-8

ENV LANG="en_US.UTF-8" \
	LANGUAGE="en_US:en" \
	LC_ALL="en_US.UTF-8" \
	VERSION=8 \
	UPDATE=222 \
	BUILD=10

RUN locale-gen en_US.UTF-8 ;\
    mkdir -p /javaAction/action /usr/java/src /usr/java/lib

WORKDIR /javaAction

COPY --from=builder_source /bin/proxy /bin/proxy_source
COPY --from=builder_release /bin/proxy /bin/proxy_release
RUN mv /bin/proxy_${GO_PROXY_BUILD_FROM} /bin/proxy

ADD https://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.9.0/gson-2.9.0.jar /usr/java/lib/gson-2.9.0.jar
ADD lib/src/Launcher.java /usr/java/src/Launcher.java
RUN cd /usr/java/src ;\
    javac -cp /usr/java/lib/gson-2.9.0.jar Launcher.java ;\
    jar cvf /usr/java/lib/launcher.jar *.class
ADD bin/compile /bin/compile
ENV OW_COMPILER=/bin/compile
ENV OW_SAVE_JAR=exec.jar
ENTRYPOINT /bin/proxy
