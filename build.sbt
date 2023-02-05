import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("scalacenter", "qrgen", "faveoled@yandex.com"))


scalaVersion := "3.2.1"

enablePlugins(ScalaNativePlugin)

// used as `artifactId`
name := "qrgen"
// used as `groupId`
organization := "io.github.faveoled"

description := "Scala QR generator"

// open source licenses that apply to the project
licenses := Seq("MIT" -> url("https://mit-license.org/"))

// publish to the sonatype repository
publishTo := sonatypePublishToBundle.value

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.15" % Test