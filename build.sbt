import scalariform.formatter.preferences._

name := "aerospikez"

organization := "cl.otrimegistro.aerospikez"

startYear := Some(2014)

homepage := Some(url("http://github.com/otrimegistro/aerospikez"))

version := "0.1"

scalaVersion := "2.10.4"

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Releases"  at "http://oss.sonatype.org/content/repositories/releases/"
)

libraryDependencies ++= {
  val scalazV = "7.0.6"
  Seq(
    "org.scalaz"        %% "scalaz-core"        % scalazV,
    "org.scalaz"        %% "scalaz-concurrent"  % scalazV,
    "com.aerospike"     %  "aerospike-client"   % "3.0.23",
    "com.typesafe"      %  "config"             % "1.2.0",
    "org.specs2"        %% "specs2"             % "2.3.10"  % "test"
  )
}

scalacOptions ++= Seq(
  "-feature",
  "-Xfuture",
  "-optimise",
  "-Xmigration",
  "-deprecation",
  "-language:higherKinds",
  "-language:implicitConversions"
)

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, true)
