#!/bin/bash
#### Construct Class Cache with HTTP Server classes by starting the server ####
java "-Xshareclasses:cacheDir=/javaSharedCache/" "-Xquickstart" "-jar" "/javaAction/build/libs/javaAction-all.jar" &
HTTP_PID=$!
sleep 2
kill $HTTP_PID
