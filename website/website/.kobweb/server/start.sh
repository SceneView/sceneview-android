#!/bin/bash

# Find the kobweb project folder, using this file's location to start form
cd "$(dirname "$0")"
cd ../..

args="-Dkobweb.server.environment=PROD -Dkobweb.site.layout=FULLSTACK -Dio.ktor.development=false -jar .kobweb/server/server.jar"

if [ -n "$KOBWEB_JAVA_HOME" ]; then
    "$KOBWEB_JAVA_HOME/bin/java" $args
elif [ -n "$JAVA_HOME" ]; then
    "$JAVA_HOME/bin/java" $args
else
    java $args
fi