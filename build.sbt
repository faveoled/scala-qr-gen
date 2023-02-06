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

// dynverSonatypeSnapshots in ThisBuild := true
//publish to the sonatype repository
publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
dynverSonatypeSnapshots in ThisBuild := true
// publishTo := sonatypePublishToBundle.value
sonatypeCredentialHost := "s01.oss.sonatype.org"
// sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.15" % Test