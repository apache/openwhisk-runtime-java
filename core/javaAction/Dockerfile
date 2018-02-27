FROM docker.io/fabric8/java-jboss-openjdk8-jdk

ENV AB_OFF=true \
    AB_ENABLED="" \
    JAVA_APP_NAME=javaAction-all.jar \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8

ADD ./proxy/build/libs/proxy-all.jar /deployments/javaAction-all.jar
