#!/usr/bin/env python
"""Java Action Builder
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
"""

from __future__ import print_function
import os
import sys
import codecs
import subprocess

def copy(src, dst):
    with codecs.open(src, 'r', 'utf-8') as s:
        body = s.read()
        with codecs.open(dst, 'w', 'utf-8') as d:
            d.write(body)

# if there is an exec copy to main.java
# then copy the launcher in place
def sources(launcher, source_dir, main):
    # source and dest
    src = "%s/exec" % source_dir
    main_java = "%s/%s.java" %  (source_dir, main)
    # copy exec to <main>.java is not there
    if os.path.isfile(src):
        copy(src,main_java)

    # copy the actual executor
    exec_java = "%s/exec__.java" % source_dir
    with codecs.open(launcher, 'r', 'utf-8') as s:
        with codecs.open(exec_java, 'w', 'utf-8') as d:
            body = s.read()
            body = body.replace("output = main.main(object);",
                                "output = %s.main(object);" % main)
            d.write(body)
    return [exec_java, main_java]

def javac(sources, classpath, target_dir):
    cmd = [ "javac",
            "-cp", classpath,
            "-d", target_dir
    ]+sources
    p = subprocess.Popen(cmd,
                         stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE)
    (o, e) = p.communicate()
    if isinstance(o, bytes) and not isinstance(o, str):
        o = o.decode('utf-8')
    if isinstance(e, bytes) and not isinstance(e, str):
        e = e.decode('utf-8')
    ok = True
    if o:
        ok = False
        sys.stdout.write(o)
        sys.stdout.flush()
    if e:
        ok = False
        sys.stderr.write(e)
        sys.stderr.flush()
    return ok


def build(files, target_dir, launcher):
    lib = "/usr/java/lib/"
    classpath = ":".join([lib+x for x in os.listdir(lib)])
    if javac(files, classpath, target_dir):
        cmd = """#!/bin/bash\njava -cp %s:%s exec__\n""" % (classpath, target_dir)
        with codecs.open(launcher, 'w', 'utf-8') as d: d.write(cmd)
        os.chmod(launcher, 0o755)

def assemble(argv):
    main = argv[1]
    source_dir = os.path.abspath(argv[2])
    target_dir = os.path.abspath(argv[3])
    target_file = os.path.abspath("%s/exec" % target_dir)
    launcher = os.path.abspath("/usr/java/src/exec__.java")
    files = sources(launcher, source_dir, main)
    if files:
        build(files, target_dir, target_file)
    sys.stdout.flush()
    sys.stderr.flush()

if __name__ == '__main__':
    if len(sys.argv) < 4:
        sys.stdout.write("usage: <main-class> <source-dir> <target-dir>\n")
        sys.exit(1)
    assemble(sys.argv)
