#!/usr/bin/env bash

CONTENT_ROOT="-Dtds.content.root.path=$TRAVIS_BUILD_DIR/tds/src/test/content"
DOWNLOAD_DIR="-Dtds.download.dir=/tmp/download"
UPLOAD_DIR="-Dtds.upload.dir=/tmp/upload"
SYSTEM_PROPS="$CONTENT_ROOT $DOWNLOAD_DIR $UPLOAD_DIR"

$TRAVIS_BUILD_DIR/gradlew $SYSTEM_PROPS --info --stacktrace testAll
