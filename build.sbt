name := "CodeReview2"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  "org.squeryl" % "squeryl_2.10.0-RC5" % "0.9.5-5",
//  "com.h2database" % "h2" % "1.3.174",
  "org.tmatesoft.svnkit" % "svnkit" % "1.7.8",
  "com.googlecode.java-diff-utils" % "diffutils" % "1.2.1",
  cache
)     

play.Project.playScalaSettings
