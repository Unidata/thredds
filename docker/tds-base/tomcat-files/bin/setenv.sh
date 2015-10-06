#!/bin/sh
#
#
CATALINA_HOME="/usr/local/tomcat"
export CATALINA_HOME

CATALINA_BASE="/usr/local/tomcat"
export CATALINA_BASE

JAVA_HOME="/usr"
export JAVA_HOME

#
# Some commonly used JAVA_OPTS settings:

CONTENT_ROOT=-Dtds.content.root.path=/usr/local/tomcat/content
NORMAL="-d64 -Xmx4090m -Xms512m -server -ea"
MAX_PERM_GEN="-XX:MaxPermSize=256m"
HEAP_DUMP="-XX:+HeapDumpOnOutOfMemoryError"
HEADLESS="-Djava.awt.headless=true"
JAVA_PREFS_SYSTEM_ROOT="-Djava.util.prefs.systemRoot=$CATALINA_HOME/content/thredds/javaUtilPrefs -Djava.util.prefs.userRoot=$CATALINA_HOME/content/thredds/javaUtilPrefs"

# Standard setup.
#
JAVA_OPTS="$CONTENT_ROOT $NORMAL $MAX_PERM_GEN $HEAP_DUMP $HEADLESS $JAVA_PREFS_SYSTEM_ROOT"

export JAVA_OPTS
