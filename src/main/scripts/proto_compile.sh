#!/bin/bash

# Compile instance.proto in src/main/resources and generate the Java file in src/main/java/io.datanapis.xbrl.model.Xbrl
# To compile "cd src/main/scripts; ./proto_compile.sh"
/usr/local/bin/protoc --java_out=../java -I ../resources instance.proto