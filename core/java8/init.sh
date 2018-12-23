#!/bin/bash
limit_in_bytes=$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes)
heap_size=64
export JAVA_OPTS="-Dfile.encoding=UTF-8 -Xshareclasses:cacheDir=/javaSharedCache,readonly -Xquickstart"
export RESERVED_MEGABYTES=64

# If not default limit_in_bytes in cgroup
if [ "$limit_in_bytes" -ne "9223372036854771712" ]
then
    limit_in_megabytes=$(expr $limit_in_bytes \/ 1048576)
    heap_size=$(expr $limit_in_megabytes - $RESERVED_MEGABYTES)
    export JAVA_OPTS="-Xmx${heap_size}m $JAVA_OPTS"
fi

echo JAVA_OPTS= $JAVA_OPTS

java $JAVA_OPTS -jar /javaAction/build/libs/javaAction-all.jar
