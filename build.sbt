val akkaHttpVersion       = "10.2.4"
val akkaVersion           = "2.6.13"
val logbackVersion        = "1.2.3"
val playJsonVersion       = "2.9.2"
val scalaLoggingVersion   = "3.9.3"
val scalatestVersion      = "3.2.7"
val h2Version             = "1.4.197"
val slickVersion          = "3.3.3"
val slickMigrationVersion = "0.8.2"
val akkaHttpJsonVersion   = "1.36.0"

name := "akka-book-store"
scalaVersion := "2.13.5"
libraryDependencies ++= Seq(
  "ch.qos.logback"                      % "logback-classic"     % logbackVersion,
  "com.typesafe.akka"                  %% "akka-actor"          % akkaVersion,
  "com.typesafe.akka"                  %% "akka-http"           % akkaHttpVersion,
  "com.typesafe.akka"                  %% "akka-http-core"      % akkaHttpVersion,
  "com.typesafe.akka"                  %% "akka-stream"         % akkaVersion,
  "com.typesafe.play"                  %% "play-json"           % playJsonVersion,
  "com.typesafe.scala-logging"         %% "scala-logging"       % scalaLoggingVersion,
  "com.typesafe.slick"                 %% "slick"               % slickVersion,
  "com.h2database"                      % "h2"                  % h2Version,
  "com.typesafe.akka"                  %% "akka-http-testkit"   % akkaHttpVersion  % Test,
  "com.typesafe.akka"                  %% "akka-slf4j"          % akkaVersion      % Test,
  "com.typesafe.akka"                  %% "akka-stream-testkit" % akkaVersion      % Test,
  "com.typesafe.akka"                  %% "akka-testkit"        % akkaVersion      % Test,
  "org.scalatest"                      %% "scalatest"           % scalatestVersion % Test,
  "io.github.nafg.slick-migration-api" %% "slick-migration-api" % slickMigrationVersion,
  "de.heikoseeberger"                  %% "akka-http-play-json" % akkaHttpJsonVersion
)
