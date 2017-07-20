import Dependencies._
import sbt.Keys._

name := "wsd-root"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

resolvers += "Will's bintray" at "https://dl.bintray.com/willb/maven/"
resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"


// Decreases startup time substantially, see: https://github.com/sbt/sbt/issues/413
updateOptions := updateOptions.value.withCachedResolution(true)

offline := true

// TODO clarify if this is the correct approach for global settings?
lazy val globalSettings = Seq(
  organization := "de.tudarmstadt.lt",
  version := "0.3.0",
  scalaVersion := "2.11.8"
  // logLevel in compile := Level.Warn // TODO does not work
)

lazy val common = (project in file("common")).
  settings(globalSettings: _*).
  settings(
    name := "wsd-common",
    libraryDependencies ++= commonDeps,
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
      case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    // Only for convience in: sbt common/console
    initialCommands in console:= """
      |import de.tudarmstadt.lt.wsd.common._
      |import de.tudarmstadt.lt.wsd.common.model._
      |import de.tudarmstadt.lt.wsd.common.Implicits._
      |import scalikejdbc._
      |import scalikejdbc.config._
      |DBs.setupAll()""".stripMargin
  )

lazy val api = (project in file("api")).
  enablePlugins(PlayScala).
  settings(globalSettings: _*).
  settings(
    name := "wsd-api",
    libraryDependencies ++= apiDeps,
    // Needed to automatically restart docker container after api crashed.
    javaOptions in Universal ++= Seq("-Dpidfile.path=/dev/null")
      // Copied from http://skinny-framework.org/documentation/orm.html
  ).
  dependsOn(common)

lazy val spark = (project in file("spark")).
  enablePlugins(JavaAppPackaging).
  settings(globalSettings: _*).
  settings(
    name := "wsd-spark",
    libraryDependencies ++= sparkDeps,
    // Silence verbose spark logger as described in https://jaceklaskowski.gitbooks.io/mastering-apache-spark/content/spark-logging.html
    // Note: the warning "Class path contains multiple SLF4J bindings." prevented it from wokring and was finally solved with
    //       the help of this blog post: http://www.roberthorvick.com/2016/11/22/sbt-exclude-slf4j-transitive-dependency/
    fork in run:= true,
    javaOptions in run ++= Seq(
      // "-Dlog4j.debug=true", // Only needed when settings seem not to work,
      // prints "Class path contains multiple SLF4J bindings." as mentioned above
      "-Dlog4j.configuration=log4j.properties"
    ),
    outputStrategy := Some(StdoutOutput),
    // Only for convience in: sbt spark/console
    initialCommands in console := """
      import org.apache.spark.sql.SparkSession
      val spark = {
        SparkSession
          .builder()
          .appName("WSP Console")
          .config("spark.master", "local[*]")
          .enableHiveSupport()
          .getOrCreate()
      }
      import spark.implicits._
      import de.tudarmstadt.lt.wsd.pipeline._
      import com.typesafe.config.ConfigFactory
      val config = ConfigFactory.load()
      """
  ).
  dependsOn(common).
  settings(
    // Adding back "provided" dependencies to `sbt spark/run` as shown in http://stackoverflow.com/a/21803413
    run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)),

    // Do not run tests during assembly
    test in assembly := {}
  )