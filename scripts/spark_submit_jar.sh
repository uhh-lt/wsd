#!/usr/bin/env bash
mkdir -p /tmp/spark-events



if which spark-submit > /dev/null; then
  cmd=spark-submit
else
  cmd=${SPARK_HOME:-/opt/spark-2.1.0-bin-hadoop2.7}/bin/spark-submit

  # Test command exists
  which ${cmd} > /dev/null
  if test $? -ne 0; then
    echo
    echo "Error: '\$SPARK_HOME' must be set to a valid spark installation, " \
         "or 'spark-submit' be on your path."
    exit 1
  fi
fi

# Note: If you want to use nice-app.conf, please include the following parameters.
# --files nice-app.conf
# --conf spark.executor.extraJavaOptions=-Dconfig.file=nice-app.conf
# --conf spark.driver.extraJavaOptions=-Dconfig.file=nice-app.conf

${cmd} \
 --master "local[*]" \
 --conf 'spark.executor.extraJavaOptions=-Dlog4j.debug=true' \
 --conf 'spark.driver.extraJavaOptions=-Dlog4j.debug=true' \
 --conf 'spark.executor.extraJavaOptions=-Dlog4j.configuration=log4j.properties' \
 --conf 'spark.driver.extraJavaOptions=-Dlog4j.configuration=log4j.properties' \
 --conf 'spark.driver.memory=4g' \
 --conf 'spark.executor.memory=1g' \
 --conf 'spark.local.dir=/tmp' \
 --conf 'spark.eventLog.enabled=true' \
 spark/target/scala-2.11/wsd-spark-assembly-0.3.0.jar "$@"