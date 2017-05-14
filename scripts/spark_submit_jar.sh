#!/usr/bin/env bash
mkdir -p /tmp/spark-events
spark_home=${SPARK_HOME:-/opt/spark-2.1.0-bin-hadoop2.7}

# Note: Please include the following paramters if you have copied sample-app.conf to app.conf and want to use it!
# --files app.conf --conf spark.executor.extraJavaOptions=-Dconfig.file=app.conf --conf spark.driver.extraJavaOptions=-Dconfig.file=app.conf

$spark_home/bin/spark-submit \
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
