import Dependencies._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

def scala211 = "2.11.12"
def scala212 = "2.12.8"

inThisBuild(
  List(


    organization := "org.scalameta",
    homepage := Some(url("https://github.com/scalameta/scalafmt")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@gmail.com",
        url("https://geirsson.com")
      )
    ),
    scalaVersion := scala212,
    crossScalaVersions := List(scala212, scala211),
    resolvers += Resolver.sonatypeRepo("public"),
    libraryDependencies ++= List(
      scalatest.value % Test,
      "org.scalacheck" %% "scalacheck" % "1.13.5",
      scalametaTestkit % Test
    )
  )
)
lazy val nativeSettings = List(
  nativeLinkStubs := true,
  scalaVersion := scala211,
  crossScalaVersions := List(scala211)
)

name := "scalafmtRoot"
skip in publish := true
addCommandAlias("downloadIdea", "intellij/updateIdea")

commands += Command.command("ci-test") { s =>
  val scalaVersion = sys.env.get("TEST") match {
    case Some("2.11") => scala211
    case _ => scala212
  }
  val docsTest = if (scalaVersion == scala212) "docs/run" else "version"
  s"++$scalaVersion" ::
    s"tests/test" ::
    s"coreJS/test" ::
    docsTest ::
    s
}

lazy val dynamic = crossProject(JVMPlatform, NativePlatform)
  .in(file("scalafmt-dynamic"))
  .settings(
    moduleName := "scalafmt-dynamic",
    description := "Implementation of scalafmt-interfaces",
    buildInfoSettings,
    buildInfoPackage := "org.scalafmt.dynamic",
    buildInfoObject := "BuildInfo"
  )
  .nativeSettings(
    nativeSettings,
    libraryDependencies ++= List(
      "org.ekrich" %% "sconfig" % "0.8.0"
    )
  )
  .jvmSettings(
    libraryDependencies ++= List(
      "com.geirsson" %% "coursier-small" % "1.3.1",
      "com.typesafe" % "config" % "1.3.3",
      scalatest.value % Test,
      scalametaTestkit % Test
    )
  )
  .jvmConfigure(_.dependsOn(interfaces))
  .enablePlugins(BuildInfoPlugin)
lazy val dynamicJVM = dynamic.jvm
lazy val dynamicNative = dynamic.native

lazy val interfaces = project
  .in(file("scalafmt-interfaces"))
  .settings(
    moduleName := "scalafmt-interfaces",
    description := "Dependency-free, pure Java public interfaces to integrate with Scalafmt through a build tool or editor plugin.",
    crossVersion := CrossVersion.disabled,
    autoScalaLibrary := false,
    resourceGenerators.in(Compile) += Def.task {
      val out =
        managedResourceDirectories
          .in(Compile)
          .value
          .head / "scalafmt.properties"
      val props = new java.util.Properties()
      props.put("version", version.value)
      IO.write(props, "scalafmt properties", out)
      List(out)
    }
  )

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("scalafmt-core"))
  .settings(
    moduleName := "scalafmt-core",
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
    ),
    buildInfoSettings,
    libraryDependencies ++= Seq(
      metaconfig.value,
      scalameta.value,
      // scala-reflect is an undeclared dependency of fansi, see #1252.
      // Scalafmt itself does not require scala-reflect.
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )
  .jsSettings(
    libraryDependencies ++= List(
      metaconfigHocon.value,
      scalatest.value % Test // must be here for coreJS/test to run anything
    )
  )
  .nativeSettings(
    scalaVersion := scala211,
    nativeLinkStubs := true,
    libraryDependencies ++= List(
      metaconfigSconfig.value,
      scalatest.value % Test
    )
  )
  .jvmSettings(
    fork.in(run).in(Test) := true,
    libraryDependencies ++= List(
      metaconfigSconfig.value
    )
  )
  .enablePlugins(BuildInfoPlugin)
lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val cli = crossProject(JVMPlatform, NativePlatform)
  .in(file("scalafmt-cli"))
  .settings(
    moduleName := "scalafmt-cli",
    mainClass in assembly := Some("org.scalafmt.cli.Cli"),
    assemblyJarName.in(assembly) := "scalafmt.jar",
    libraryDependencies ++= Seq(
      "com.github.scopt" %%% "scopt" % "3.7.1",
      // undeclared transitive dependency of coursier-small
      "org.scala-lang.modules" %% "scala-xml" % "1.1.1"
    ),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11)) => Seq("-target:jvm-1.8")
        case _ => Seq.empty
      }
    }
  )
  .nativeSettings(nativeSettings)
  .jvmSettings(
    libraryDependencies ++= List(
      "com.martiansoftware" % "nailgun-server" % "0.9.1",
      "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
    )
  )
  .dependsOn(core, dynamic)
lazy val cliJVM = cli.jvm
lazy val cliNative = cli.native

lazy val intellij = project
  .in(file("scalafmt-intellij"))
  .settings(
    TaskKey[Unit]("bloopInstall") := {},
    buildInfoSettings,
    crossScalaVersions := List(scala211),
    skip in publish := true,
    sources in (Compile, doc) := Nil,
    mimaReportBinaryIssues := {},
    ideaBuild := "2016.3.2",
    test := {}, // no need to download IDEA to run all tests.
    ideaEdition := IdeaEdition.Community,
    ideaDownloadDirectory in ThisBuild := baseDirectory.value / "idea",
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
    cleanFiles += ideaDownloadDirectory.value
  )
  .dependsOn(coreJVM, cliJVM)
  .enablePlugins(SbtIdeaPlugin)

lazy val tests = crossProject(JVMPlatform, NativePlatform)
  .in(file("scalafmt-tests"))
  .settings(
    skip in publish := true,
    libraryDependencies ++= Seq(
      // Test dependencies
      "com.lihaoyi" %%% "scalatags" % "0.6.8",
      "org.typelevel" %%% "paiges-core" % "0.2.2",
      scalametaTestkit
    )
  )
  .dependsOn(
    cli
  )
lazy val testsJVM = tests.jvm
lazy val testsNative = tests.native

lazy val benchmarks = project
  .in(file("scalafmt-benchmarks"))
  .settings(
    skip in publish := true,
    moduleName := "scalafmt-benchmarks",
    libraryDependencies ++= Seq(
      scalametaTestkit
    ),
    javaOptions in run ++= Seq(
      "-Djava.net.preferIPv4Stack=true",
      "-XX:+AggressiveOpts",
      "-XX:+UseParNewGC",
      "-XX:+UseConcMarkSweepGC",
      "-XX:+CMSParallelRemarkEnabled",
      "-XX:+CMSClassUnloadingEnabled",
      "-XX:ReservedCodeCacheSize=128m",
      "-XX:MaxMetaspaceSize=1024m",
      "-XX:SurvivorRatio=128",
      "-XX:MaxTenuringThreshold=0",
      "-Xss8M",
      "-Xms512M",
      "-Xmx2G",
      "-server"
    )
  )
  .dependsOn(coreJVM)
  .enablePlugins(JmhPlugin)

lazy val docs = project
  .in(file("scalafmt-docs"))
  .settings(
    crossScalaVersions := List(scala212),
    skip in publish := true,
    mdoc := run.in(Compile).evaluated
  )
  .dependsOn(cliJVM, dynamicJVM)
  .enablePlugins(DocusaurusPlugin)

val V = "\\d+\\.\\d+\\.\\d+"
val ReleaseCandidate = s"($V-RC\\d+).*".r
val Milestone = s"($V-M\\d+).*".r

lazy val stableVersion = Def.setting {
  version.in(ThisBuild).value.replaceAll("\\+.*", "")
}

lazy val buildInfoSettings: Seq[Def.Setting[_]] = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    "scalameta" -> scalametaV,
    "nightly" -> version.value,
    "stable" -> stableVersion.value,
    "scala" -> scalaVersion.value,
    "scala211" -> scala211,
    "coursier" -> coursier,
    "commit" -> sys.process.Process("git rev-parse HEAD").lineStream_!.head,
    "timestamp" -> System.currentTimeMillis().toString,
    scalaVersion,
    sbtVersion
  ),
  buildInfoPackage := "org.scalafmt",
  buildInfoObject := "Versions"
)
