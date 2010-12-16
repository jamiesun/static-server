#!/bin/sh

if [ -d "../jdk1.5.0_14" ]; then
  PROJ_HOME=../jdk1.5.0_14
else
  PROJ_HOME=$JAVA_HOME
fi

echo "PROJ_HOME=$PROJ_HOME"

# Check $PROJ_HOME/bin/java is Exist
if [ ! -f "$PROJ_HOME/bin/java" ]; then
  return
fi

# Check $PROJ_HOME/bin/statics is Exist
if [ ! -f "$PROJ_HOME/bin/statics" ]; then
  cp $PROJ_HOME/bin/java $PROJ_HOME/bin/statics
fi

# define CLASSPATH
CLASS_PATH=$PROJ_HOME/lib/dt.jar:$PROJ_HOME/lib/tools.jar

# container pagage
CLASS_PATH=$CLASS_PATH:./lib/statics.jar
CLASS_PATH=$CLASS_PATH:./lib/xSocket.jar
CLASS_PATH=$CLASS_PATH:./lib/log4j.jar
CLASS_PATH=$CLASS_PATH:./lib/commons-logging.jar
CLASS_PATH=$CLASS_PATH:./lib/jmagick.jar
CLASS_PATH=$CLASS_PATH:./lib/xlightweb.jar
CLASS_PATH=$CLASS_PATH:./lib/picocontainer-1.3.jar
CLASS_PATH=$CLASS_PATH:./lib/gson-1.5.jar
CLASS_PATH=$CLASS_PATH:./lib/je-4.0.103.jar
CLASS_PATH=$CLASS_PATH:./lib/xSocket-multiplexed-2.1.7.jar

exec nohup $PROJ_HOME/bin/statics -server -Xms64m -Xmx1024m  -classpath $CLASS_PATH com.ly.statics.server.Main &


