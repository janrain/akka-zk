organization := "com.janrain"

name := "akka-zk"

version := "0.4-SNAPSHOT"

scalaVersion := "2.10.3"

libraryDependencies += "org.apache.zookeeper" % "zookeeper" % "3.4.5" excludeAll(
  ExclusionRule(organization = "javax.jms", name = "jms"),
  ExclusionRule(organization = "com.sun.jdmk", name = "jmxtools"),
  ExclusionRule(organization = "com.sun.jmx", name = "jmxri"),
  ExclusionRule(organization = "javax.mail", name = "mail"),
  ExclusionRule(organization = "junit", name = "junit"),
  ExclusionRule(organization = "log4j", name = "log4j"),
  ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"))

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.3"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.3.3"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.3" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.4" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.7"

libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % "1.7.5"

publishTo <<= version { (v: String) =>
  val r = "https://janrain.artifactoryonline.com/janrain/"
  Some(if (v.trim.endsWith("SNAPSHOT")) ("snapshots" at r + "janrain-snapshots")
  else ("releases" at r + "janrain-releases"))
}

scalacOptions += "-deprecation"