lazy val baseName       = "ReExpo"
//lazy val baseNameL      = baseName.toLowerCase
lazy val projectVersion = "0.1.0-SNAPSHOT"
lazy val gitHost        = "codeberg.org"
lazy val gitUser        = "sciss"
lazy val gitRepo        = baseName

lazy val commonSettings = Seq(
  version      := projectVersion,
  homepage     := Some(url(s"https://$gitHost/$gitUser/$gitRepo")),
  scalaVersion := "3.2.1",
  scalacOptions ++= Seq("-deprecation"),
  licenses     := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
  run / fork   := true,
) ++ assemblySettings

lazy val root = project.in(file("."))
  .aggregate(core, ui)
  .settings(commonSettings)
  .settings(
    name        := baseName,
    description := "Parsers and Processors for Working with Research Catalogue Expositions",
  )

lazy val core = project.in(file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(buildInfoSettings)
  .settings(
    name        := s"$baseName-core",
    description := "Core model",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "core"           % deps.core.sttp,     // HTTP client
      "de.sciss"                      %% "fileutil"       % deps.core.fileUtil, // utility functions
      "de.sciss"                      %% "log"            % deps.core.log,      // text logging
      "de.sciss"                      %% "numbers"        % deps.core.numbers,  // numeric utilities
//      "org.json4s"                    %% "json4s-core"    % deps.core.json4s,   // JSON serialization
//      "org.json4s"                    %% "json4s-native"  % deps.core.json4s,   // JSON serialization
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"    % deps.core.jsoniter,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros"  % deps.core.jsoniter % "provided",
      "org.jsoup"                     %  "jsoup"          % deps.core.jsoup,    // HTML parsing
      "org.rogach"                    %% "scallop"        % deps.core.scallop,  // command line option parsing
      "org.scalatest"                 %% "scalatest"      % deps.test.scalaTest % Test,
    ),
    console / initialCommands := {
      """import de.sciss.reexpo._
        |""".stripMargin
    },
    buildInfoPackage := "de.sciss.reexpo"
  )

lazy val ui = project.in(file("ui"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name        := s"$baseName-ui",
    description := "User interface",
    libraryDependencies ++= Seq(
      "de.sciss" %% "desktop"   % deps.ui.desktop,    // desktop integration
      "de.sciss" %% "swingplus" % deps.ui.swingPlus,  // UI
    ),
    assembly / assemblyJarName := s"$baseName.jar",
  )

lazy val deps = new {
  val core = new {
    val fileUtil        = "1.1.5"
    val json4s          = "4.0.6"
    val jsoniter        = "2.21.2"
    val jsoup           = "1.15.4"
    val log             = "0.1.1"
    val numbers         = "0.2.1"
    val scallop         = "4.1.0"
    val sttp            = "3.8.11"
  }
  val ui = new {
    val desktop         = "0.11.4"
    val swingPlus       = "0.5.0"
  }
  val test = new {
    val scalaTest       = "3.2.15"
  }
}

lazy val buildInfoSettings = Seq(
  // ---- build info ----
  buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
    BuildInfoKey.map(homepage) { case (k, opt)           => k -> opt.get },
    BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
  ),
  buildInfoOptions += BuildInfoOption.BuildTime
)

lazy val assemblySettings = Seq(
  // ---- assembly ----
  assembly / test            := {},
  assembly / target          := baseDirectory.value,
  ThisBuild / assemblyMergeStrategy := {
    case "logback.xml" => MergeStrategy.last
    case PathList("org", "xmlpull", _ @ _*)              => MergeStrategy.first
    case PathList("org", "w3c", "dom", "events", _ @ _*) => MergeStrategy.first // Apache Batik
    case p@PathList(ps@_*) if ps.last endsWith "module-info.class" =>
      MergeStrategy.discard // Jackson, Pi4J
    case p @ PathList(ps @ _*) if ps.last endsWith ".proto" =>
      MergeStrategy.discard // Akka vs Google protobuf what the hell
    case x =>
      val old = (ThisBuild / assemblyMergeStrategy).value
      old(x)
  }
)
