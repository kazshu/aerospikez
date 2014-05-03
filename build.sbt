import scalariform.formatter.preferences._
import scala.util.Properties

// Project Info //

name := "aerospikez"

description := "Aerospike v3 Scala Client"

organization := "com.github.otrimegistro"

homepage := Some(url("http://github.com/otrimegistro/aerospikez"))

startYear := Some(2014)

licenses := Seq(
  ("MIT License", url("http://raw.github.com/otrimegistro/aerospikez/master/LICENSE"))
)

scmInfo := Some(
  ScmInfo(
    url("https://github.com/otrimegistro/aerospikez"),
    "scm:git:https://github.com/otrimegistro/aerospikez.git",
    Some("scm:git:git@github.com:otrimegistro/aerospikez.git")
  )
)

// Dependencies //

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
    "org.specs2"        %% "specs2"             % "2.3.11"  % "test"
  )
}

// Settings //

scalaVersion := "2.11.0"

crossScalaVersions := Seq("2.10.4", "2.11.0")

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

// Publishing //

publishMavenStyle := true

publishTo := {
  val nexus = "http://nexus-otrimegistro.rhcloud.com/nexus"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

Seq("OPENSHIFT_NEXUS_USER", "OPENSHIFT_NEXUS_PASS") map Properties.envOrNone match {
  case Seq(Some(user), Some(pass)) =>
    credentials += Credentials("Sonatype Nexus Repository Manager", "nexus-otrimegistro.rhcloud.com", user, pass)
  case _ =>
    credentials ~= identity
}

publishArtifact := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <developers>
    <developer>
      <id>otrimegistro</id>
      <name>Omar González</name>
      <url>https://github.com/otrimegistro</url>
    </developer>
  </developers>
)
