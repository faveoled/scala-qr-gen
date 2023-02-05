import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("faveoled", "scala-qr-gen", "faveoled@yandex.com"))


scalaVersion := "3.2.1"

enablePlugins(ScalaNativePlugin)

// used as `artifactId`
name := "qrgen"
// used as `groupId`
organization := "io.github.faveoled"

description := "Scala QR generator"

// open source licenses that apply to the project
licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

// publish to the sonatype repository
publishTo := sonatypePublishToBundle.value

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.15" % Test