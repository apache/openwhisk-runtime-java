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
FROM golang:1.17-alpine AS builder_source
ARG GO_PROXY_GITHUB_USER=apache
ARG GO_PROXY_GITHUB_BRANCH=master
RUN apk add --update git
RUN git clone --branch ${GO_PROXY_GITHUB_BRANCH} \
   https://github.com/${GO_PROXY_GITHUB_USER}/openwhisk-runtime-go /src ;\
   cd /src ; env GO111MODULE=on CGO_ENABLED=0 go build main/proxy.go && \
   mv proxy /bin/proxy

# or build it from a release
FROM golang:1.17-alpine AS builder_release
ARG GO_PROXY_RELEASE_VERSION=1.16@1.18.0
RUN apk add --update curl
RUN curl -sL \
  https://github.com/apache/openwhisk-runtime-go/archive/{$GO_PROXY_RELEASE_VERSION}.tar.gz\
  | tar xzf -\
  && cd openwhisk-runtime-go-*/main\
  && GO111MODULE=on go build -o /bin/proxy

# Use OpenJDK on Alpine
FROM openjdk:17-alpine

ENV MUSL_LOCALE_DEPS cmake make musl-dev gcc gettext-dev libintl

# select the builder to use
ARG GO_PROXY_BUILD_FROM=release

RUN apk add --update python3 py3-pip vim bash tzdata cmake make musl-dev gcc gettext-dev libintl


RUN cp /usr/share/zoneinfo/America/New_York /etc/localtime
RUN rm -r /usr/share/zoneinfo/Africa && \
    rm -r /usr/share/zoneinfo/Antarctica && \
    rm -r /usr/share/zoneinfo/Arctic && \
    rm -r /usr/share/zoneinfo/Asia && \
    rm -r /usr/share/zoneinfo/Atlantic && \
    rm -r /usr/share/zoneinfo/Australia && \
    rm -r /usr/share/zoneinfo/Europe  && \
    rm -r /usr/share/zoneinfo/Indian && \
    rm -r /usr/share/zoneinfo/Mexico && \
    rm -r /usr/share/zoneinfo/Pacific && \
    rm -r /usr/share/zoneinfo/Chile && \
    rm -r /usr/share/zoneinfo/Canada
RUN echo "America/New_York" >  /etc/timezone


ENV TZ America/New_York
ENV LANG="en_US" \
	LANGUAGE="en_US" \
	LC_ALL="en_US"

RUN mkdir -p /javaAction/action /usr/java/src /usr/java/lib
RUN ln -sf /usr/bin/python3 /usr/bin/python
WORKDIR /javaAction

COPY --from=builder_source /bin/proxy /usr/bin/proxy_source
COPY --from=builder_release /bin/proxy /usr/bin/proxy_release
RUN mv /usr/bin/proxy_${GO_PROXY_BUILD_FROM} /usr/bin/proxy

ADD build/libs/launcher.jar /usr/java/lib/launcher.jar
ADD bin/compile /usr/bin/compile
ENV OW_COMPILER=/usr/bin/compile
ENV OW_SAVE_JAR=exec.jar
ENTRYPOINT /usr/bin/proxy
