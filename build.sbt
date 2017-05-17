name := """distributed_library"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)
scalaVersion := "2.11.7"
routesGenerator := InjectedRoutesGenerator
libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  filters,
  evolutions,
  "mysql" % "mysql-connector-java" % "5.1.36",
  "com.edulify" %% "geolocation" % "2.1.0",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "com.rabbitmq" % "amqp-client" % "4.1.0"
)

resolvers ++= Seq(
  Resolver.url("Edulify Repository", url("https://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns)
)

fork in run := true
EclipseKeys.preTasks := Seq(compile in Compile)
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources)  // Use .class files instead of generated .scala files for views and routes
EclipseKeys.withSource := true
EclipseKeys.withJavadoc := true