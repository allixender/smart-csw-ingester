/*
 * Copyright (c) 2011-2017 Interfaculty Department of Geoinformatics, University of
 * Salzburg (Z_GIS) & Institute of Geological and Nuclear Sciences Limited (GNS Science)
 * in the SMART Aquifer Characterisation (SAC) programme funded by the New Zealand
 * Ministry of Business, Innovation and Employment (MBIE)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker._
import com.sksamuel.scapegoat.sbt._
import com.sksamuel.scapegoat.sbt.ScapegoatSbtPlugin.autoImport._
import scoverage.ScoverageKeys._

name := """smart-csw-ingester"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala,SiteScaladocPlugin,JavaAppPackaging,DockerPlugin)

scalaVersion := "2.11.7"

val luceneVersion = "6.1.0"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,

//  "com.gilt" % "lib-lucene-sugar_2.11" % "0.2.3",

  "org.apache.lucene" % "lucene-core" % luceneVersion,
  "org.apache.lucene" % "lucene-analyzers-common" % luceneVersion,
  "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
  "org.apache.lucene" % "lucene-spatial" % luceneVersion,
  "org.apache.lucene" % "lucene-spatial-extras" % luceneVersion,
//  "com.vividsolutions" % "jts" 	% "1.14",
  "org.locationtech.spatial4j" % "spatial4j" % "0.6",

  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  specs2 % Test
)

scalacOptions in ThisBuild ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code",
  "-language:reflectiveCalls"
)

fork in run := true

// -----------------
// coverage, style and dependency checks

// Scala style task for compile, config file is scalastyle-config.xml
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value
(compile in Compile) <<= (compile in Compile) dependsOn compileScalastyle

// Scala style task to run with tests
lazy val testScalastyle = taskKey[Unit]("testScalastyle")
testScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Test).toTask("").value
(test in Test) <<= (test in Test) dependsOn testScalastyle

scapegoatVersion := "1.2.0"

scapegoatOutputPath := "target/site/scapegoat"

// scalacOptions only for the scapegoat task
scalacOptions in Scapegoat ++= Seq("-P:scapegoat:overrideLevels:TraversableHead=Warning:OptionGet=Warning")

coverageEnabled := true

lazy val coverageCopyTask = TaskKey[Unit]("copy-coverage")

coverageCopyTask := {
  println(s"Copying: ./target/scala-2.11/scoverage-report/ to ./target/site")
  val result = Seq("cp", "-r", "./target/scala-2.11/scoverage-report", "./target/site/scoverage-report") !!
}

dependencyCheckOutputDirectory := Some(file("target/site/dep-sec"))

// Use e.g. yEd to format the graph
dependencyGraphMLFile := file("target/site/dep-sec/dependencies.graphml")

// Use e.g.graphviz to render
dependencyDotFile := file("target/site/dep-sec/dependencies.dot")


// -----------------
// publish docs on github

// site.includeScaladoc()
target in Compile in doc := baseDirectory.value / "target/site/api"

// Puts ScalaDoc output in `target/site/latest/api`
siteSubdirName in SiteScaladoc := "latest/api"

ghpages.settings

git.remoteRepo := "git@github.com:ZGIS/smart-csw-ingester.git"

// -----------------
// packaging options

version in Docker := version.value
maintainer in Docker := "allixender@googlemail.com"
dockerBaseImage in Docker := "java:8-jre"
dockerBaseImage := "java:8-jre"

javaOptions in Universal ++= Seq(
  // others will be added as app parameters
 // "-DapplyEvolutions.default=true",
  "-Dconfig.resource=application.conf"
  //"-Dapplication.base_url=http://test.smart-project.info/"
)