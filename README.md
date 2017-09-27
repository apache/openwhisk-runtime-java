#Apache OpenWhisk runtimes for java
[![Build Status](https://travis-ci.org/apache/incubator-openwhisk-runtime-java.svg?branch=master)](https://travis-ci.org/apache/incubator-openwhisk-runtime-java)


### Give it a try today
To use as a docker action
```
bx wsk action update myAction myAction.jar --docker openwhisk/java8action:1.0.0
```
This works on any deployment of Apache OpenWhisk

### To use on deployment that contains the rutime as a kind
To use as a kind action
```
bx wsk action update myAction myAction.jar --kind java:8
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


