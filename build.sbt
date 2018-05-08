import Dependencies._
import sbtcrossproject.crossProject

def scala211 = "2.11.12"
def scala212 = "2.12.6"

inThisBuild(
  List(
    organization := "com.geirsson", // not org.scalameta because that's a breaking change
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
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= List(
      scalatest.value % Test,
      scalacheck % Test
    )
  )
)

name := "scalafmtRoot"
skip in publish := true
addCommandAlias("downloadIdea", "intellij/updateIdea")

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("scalafmt-core"))
  .settings(
    moduleName := "scalafmt-core",
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
    ),
    buildInfoSettings,
    fork.in(run).in(Test) := true,
    libraryDependencies ++= Seq(
      metaconfig.value,
      scalameta.value
    )
  )
  .jsSettings(
    libraryDependencies += metaconfigHocon.value
  )
  .jvmSettings(
    libraryDependencies += metaconfigTypesafe.value
  )
  .enablePlugins(BuildInfoPlugin)
lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val cli = project
  .in(file("scalafmt-cli"))
  .settings(
    moduleName := "scalafmt-cli",
    mainClass in assembly := Some("org.scalafmt.cli.Cli"),
    assemblyJarName.in(assembly) := "scalafmt.jar",
    libraryDependencies ++= Seq(
      "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0",
      "com.martiansoftware" % "nailgun-server" % "0.9.1",
      "com.github.scopt" %% "scopt" % "3.5.0"
    )
  )
  .dependsOn(coreJVM)

lazy val big = project
  .in(file("scalafmt-big"))
  .settings(
    moduleName := "scalafmt-big",
    ThisBuild / crossScalaVersions := List(scala212),
    mimaReportBinaryIssues := {},
    shadeSettings
  )
  .dependsOn(cli)

def isOnly(scalaV: String) = Seq(
  ThisBuild / scalaVersion := scalaV,
  ThisBuild / crossScalaVersions := Seq(scalaV)
)

lazy val shadeSettings: List[Setting[_]] = List(
  assemblyOption.in(assembly) ~= { _.copy(includeScala = false) },
  assemblyShadeRules.in(assembly) := Seq(
    ShadeRule
      .rename(
        "scala.meta.**" -> "org.scalafmt.shaded.meta.@1",
        "fastparse.**" -> "org.scalafmt.shaded.fastparse.@1"
      )
      .inAll,
    ShadeRule
      .zap(
        "scalapb.**",
        "com.trueaccord.**"
      )
      .inAll
  ),
  artifact.in(Compile, packageBin) := artifact.in(Compile, assembly).value,
  pomPostProcess := { (node: scala.xml.Node) =>
    new scala.xml.transform.RuleTransformer(
      new scala.xml.transform.RewriteRule {
        override def transform(node: scala.xml.Node): scala.xml.NodeSeq =
          node match {
            case e: scala.xml.Elem
                if e.label == "dependency" &&
                  e.child.exists { child =>
                    child.label == "artifactId" &&
                    child.text.startsWith("scalafmt")
                  } =>
              scala.xml.Comment(s"shaded scalafmt-cli dependency.")
            case _ => node
          }
      }
    ).transform(node).head
  }
) ++ addArtifact(artifact.in(Compile, packageBin), assembly).settings

lazy val intellij = project
  .in(file("scalafmt-intellij"))
  .settings(
    buildInfoSettings,
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
  .dependsOn(coreJVM, cli)
  .enablePlugins(SbtIdeaPlugin)

lazy val tests = project
  .in(file("scalafmt-tests"))
  .settings(
    skip in publish := true,
    libraryDependencies ++= Seq(
      // Test dependencies
      "com.lihaoyi" %% "scalatags" % "0.6.3",
      "org.typelevel" %% "paiges-core" % "0.2.0",
      scalametaTestkit
    )
  )
  .dependsOn(
    cli
  )

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
      "-XX:MaxPermSize=1024m",
      "-Xss8M",
      "-Xms512M",
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

lazy val readme = scalatex
  .ScalatexReadme(
    projectId = "readme",
    wd = file(""),
    url = "https://github.com/scalameta/scalafmt/tree/master",
    source = "Readme"
  )
  .settings(
    git.remoteRepo := "git@github.com:scalameta/scalafmt.git",
    siteSourceDirectory := target.value / "scalatex",
    skip in publish := true,
    publish := {
      ghpagesPushSite
        .dependsOn(run.in(Compile).toTask(" --validate-links"))
        .value
    },
    test := {
      run.in(Compile).toTask(" --validate-links").value
    },
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-eval" % "6.41.0"
    )
  )
  .enablePlugins(GhpagesPlugin)
  .dependsOn(
    coreJVM,
    cli
  )

lazy val website = project
  .enablePlugins(PreprocessPlugin, TutPlugin)
  .settings(
    allSettings,
    noPublish,
    tutSourceDirectory := baseDirectory.value / ".." / "docs",
    sourceDirectory in Preprocess := tutTargetDirectory.value,
    target in Preprocess := target.value / "docs",
    preprocess in Preprocess := (preprocess in Preprocess).dependsOn(tut).value,
    preprocessVars in Preprocess := Map(
      "VERSION" -> version.value
    )
  )
  .dependsOn(coreJVM, cli)

def CiCommand(name: String)(commands: List[String]): Command =
  Command.command(name) { initState =>
    commands.foldLeft(initState) {
      case (state, command) => ci(command) :: state
    }
  }
def ci(command: String) = s"plz ${sys.env("CI_SCALA_VERSION")} $command"

def shouldPublishToBintray: Boolean = {
  if (!new File(sys.props("user.home") + "/.bintray/.credentials").exists)
    false
  else if (sys.props("publish.sonatype") != null) false
  else if (sys.env.contains("CI_PULL_REQUEST")) false
  else true
}

lazy val noDocs = Seq(
  sources in (Compile, doc) := Nil
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint"
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  bintrayRepository := "maven",
  bintrayOrganization := Some("scalameta"),
  publishTo := {
    if (shouldPublishToBintray) publishTo.in(bintray).value
    else {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }
  },
  publishArtifact in Test := false,
  mimaPreviousArtifacts := Set(
    organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % "1.0.0"
  ),
  mimaBinaryIssueFilters ++= Mima.ignoredABIProblems,
  licenses := Seq(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
  ),
  homepage := Some(url("https://github.com/scalameta/scalafmt")),
  autoAPIMappings := true,
  apiURL := Some(url("https://olafurpg.github.io/scalafmt/docs/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/scalameta/scalafmt"),
      "scm:git:git@github.com:scalameta/scalafmt.git"
    )
  ),
  pomExtra :=
    <developers>
        <developer>
          <id>olafurpg</id>
          <name>Ólafur Páll Geirsson</name>
          <url>https://geirsson.com</url>
        </developer>
      </developers>
)

lazy val noPublish = Seq(
  mimaPreviousArtifacts := Set.empty,
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

val V = "\\d+\\.\\d+\\.\\d+"
val ReleaseCandidate = s"($V-RC\\d+).*".r
val Milestone = s"($V-M\\d+).*".r

lazy val stableVersion = Def.setting {
  val latestStable = "1.5.1"
  version.value match {
    case ReleaseCandidate(_) => latestStable
    case Milestone(_) => latestStable
    case v => v.replaceAll("\\-.*", "")
  }
}

lazy val buildInfoSettings: Seq[Def.Setting[_]] = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    "scalameta" -> scalametaV,
    "nightly" -> version.value,
    "stable" -> stableVersion.value,
    "scala" -> scalaVersion.value,
    "coursier" -> coursier,
    "commit" -> sys.process.Process("git rev-parse HEAD").lineStream_!.head,
    "timestamp" -> System.currentTimeMillis().toString,
    scalaVersion,
    sbtVersion
  ),
  buildInfoPackage := "org.scalafmt",
  buildInfoObject := "Versions"
)
