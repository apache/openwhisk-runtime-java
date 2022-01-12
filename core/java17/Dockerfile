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

# Use OpenJDK on Alpine
FROM openjdk:17-alpine
RUN apk add --update vim bash tzdata cmake make musl-dev gcc gettext-dev libintl

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


RUN mkdir -p /javaAction
ADD proxy /javaAction
WORKDIR /javaAction
RUN	rm -rf .classpath .gitignore .gradle .project .settings Dockerfile build \
	&& ./gradlew jar \
	&& rm -rf /javaAction/src

CMD ["/opt/openjdk-17/bin/java", "-Dfile.encoding=UTF-8", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-jar", "build/libs/proxy.jar"]
