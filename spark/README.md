# Developer guide for spark submodule

This guide is for developers. For most users the `./wsd` command will provide enough functionality to build your own models.
See `./wsd --help` for more information.

## Using spark-submit

1. Run `sbt spark/assembly`

2. Submit the assembled JAR to a standalone Spark instance run:

```
$SPARK_HOME/bin/spark-submit \
 --master "local[*]" \
 --files /path/to/your/local/copy/of/app.conf \
 --conf 'spark.executor.extraJavaOptions=-Dconfig.file=app.conf' \
 --conf 'spark.driver.extraJavaOptions=-Dconfig.file=app.conf' \
 --conf 'spark.driver.memory=200g' \
 --conf 'spark.exector.memory=4g' \
 --conf 'spark.local.dir=/path/to/a/disk/with/space/tmp' \
 spark/target/scala-2.11/wsd-spark-assembly-0.3.0.jar \
 create -n traditional_self
```

## Using SBT

To get help just run `sbt spark/run`

**NOTE**: Once you provide some arguments to the `spark/run` command,
you have to wrap the whole command with **quotes**, i.e.
 
 ```scala
 sbt "spark/run \
  -mem 200000 \
  create -n traditional_self"
 ``` 

otherwise sbt will interpret the arguments by itself and not pass them to the application.
# F.A.Q.

- If during `create` you get an `java.lang.ArrayIndexOutOfBoundsException` exception, you might need to clean the dataset. This is because we use the character `元` as a quoting char (Apache Spark forces us to use one). For cleaning the files you can use: `cat $file | grep -v '元' > ${file%.csv}-cleaned.csv`
- If you get an `java.lang.OutOfMemoryError` you need to increase the `-mem` option for SBT or `--conf 'spark.driver.memory'` for spark-submit.
- Do not worry if you see the error: `org.apache.spark.ContextCleaner - Error in cleaning thread`. This seems to be normal. As long as you find somewhere on the console `success`, everything worked fine.
- If you get an error related to "com.github.fommil.netlib.NativeRefBLAS" you might [need to install `libgfortran3` on your OS](https://www.cloudera.com/documentation/enterprise/5-5-x/topics/spark_mllib.html).  
