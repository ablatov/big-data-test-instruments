name := "BigDataUtils"

version := "0.1"

scalaVersion := "2.12.7"

resolvers ++= Seq(
  Resolver.mavenLocal)

testOptions in Test ++= Seq(
  Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports-xml"),
  Tests.Argument(TestFrameworks.ScalaTest, "-eNDXEHLO")
)

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "3.6.0",
  "org.json4s" %% "json4s-native" % "3.6.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.4",
  // org.scalatest
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  // HTTP client
  "org.apache.httpcomponents" % "httpclient" % "4.5.6"

)

excludeDependencies += "org.slf4j" % "slf4j-log4j12"
parallelExecution in Test := false
logBuffered in Test := false
cancelable in Global := true