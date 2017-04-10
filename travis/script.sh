#!/usr/bin/env bash

if [ $TASK == "docs" ]; then
    echo Building docs
    cd $TRAVIS_BUILD_DIR/docs/website
    ./adoc2html.sh 2> build.log

    if [ -s build.log ]; then
      echo Doc build produced errors:
      cat build.log
      false
    fi
else
  CONTENT_ROOT="-Dtds.content.root.path=$TRAVIS_BUILD_DIR/tds/src/test/content"
  DOWNLOAD_DIR="-Dtds.download.dir=/tmp/download"
  UPLOAD_DIR="-Dtds.upload.dir=/tmp/upload"
  SYSTEM_PROPS="$CONTENT_ROOT $DOWNLOAD_DIR $UPLOAD_DIR"

  $TRAVIS_BUILD_DIR/gradlew $SYSTEM_PROPS --info --stacktrace testAll
fi
