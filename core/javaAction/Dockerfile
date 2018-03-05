FROM adoptopenjdk/openjdk8-openj9:jdk8u162-b12_openj9-0.8.0

RUN rm -rf /var/lib/apt/lists/* && apt-get clean && apt-get update \
	&& apt-get install -y --no-install-recommends locales \
	&& rm -rf /var/lib/apt/lists/* \
	&& locale-gen en_US.UTF-8

ENV LANG="en_US.UTF-8" \
	LANGUAGE="en_US:en" \
	LC_ALL="en_US.UTF-8" \
	VERSION=8 \
	UPDATE=162 \
	BUILD=12

ADD proxy /javaAction

RUN cd /javaAction \
	&& rm -rf .classpath .gitignore .gradle .project .settings Dockerfile build \
	&& ./gradlew oneJar \
	&& rm -rf /javaAction/src \
	&& ./compileClassCache.sh

CMD ["java", "-Dfile.encoding=UTF-8", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-jar", "/javaAction/build/libs/javaAction-all.jar"]
