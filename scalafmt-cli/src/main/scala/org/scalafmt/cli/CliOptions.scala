package org.scalafmt.cli

import java.io.InputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicReference
import org.scalafmt.config.Config
import org.scalafmt.config.FilterMatcher
import org.scalafmt.config.ProjectFiles
import org.scalafmt.config.ScalafmtConfig
import org.scalafmt.util.AbsoluteFile
import org.scalafmt.util.FileOps
import org.scalafmt.util.GitOps
import org.scalafmt.util.GitOpsImpl
import org.scalafmt.util.LogLevel

object CliOptions {
  val default = CliOptions()

  /**
    * Tries to read configuration from
    *
    * 1. .scalafmt.conf in root dir of current git repo
    *     IF the following setting is enabled: project.git = true
    * 2. .scalafmt.conf from init.commong.workingDirectory
    *
    * I am happy to add alternative fallback methods for other VCS.
    *
    * WARNING. Throws an exception if the .scalafmt.conf error exists but
    * contains an error. Why? Because this method is only supposed to be
    * called directly from main.
    */
  def auto(args: Array[String], init: CliOptions)(
      parsed: CliOptions): CliOptions = {
    val style: Option[ScalafmtConfig] = if (init.config != parsed.config) {
      Option(parsed.config)
    } else {
      tryCurrentDirectory(parsed).orElse(tryGit(parsed))
    }
    val newMode = if (parsed.testing) Stdout else parsed.writeMode
    parsed.copy(
      writeMode = newMode,
      config = style.getOrElse(parsed.config)
    )
  }

  private def getConfigJFile(file: AbsoluteFile): AbsoluteFile =
    file / ".scalafmt.conf"

  private def tryDirectory(options: CliOptions)(
      dir: AbsoluteFile): Option[ScalafmtConfig] = {
    for {
      configFile <- Option(getConfigJFile(dir))
      if configFile.jfile.isFile
      parsedConfig <- {
        Config
          .fromHoconString(FileOps.readFile(configFile))
          .toEither
          .right
          .toOption
      }
    } yield parsedConfig
  }

  private def tryGit(options: CliOptions): Option[ScalafmtConfig] = {
    options.gitOps.rootDir.flatMap(tryDirectory(options))
  }

  private def tryCurrentDirectory(
      options: CliOptions): Option[ScalafmtConfig] = {
    tryDirectory(options)(options.common.workingDirectory)
  }
}
case class CommonOptions(
    workingDirectory: AbsoluteFile = AbsoluteFile.userDir,
    out: PrintStream = System.out,
    in: InputStream = System.in,
    err: PrintStream = System.err
) {
  // This may be ugly but it's a simple fix for how the scalafmt cli is organized.
  // Ideally, the exit code should be properly propagated as it's done in
  // for example scalafix. FWIW, the scalac reporter works similar to this.
  private var _hasError = new AtomicReference[Boolean](false)
  def error(exception: Throwable): Unit = {
    val str = new StringWriter()
    exception.printStackTrace(new PrintWriter(str))
    error(str.toString)
  }
  def error(msg: String): Unit = {
    _hasError.set(true)
    err.println(LogLevel.error + s" $msg")
  }
  def warning(msg: String): Unit = err.println(LogLevel.warn + s" $msg")
  def hasError: Boolean = _hasError.get()
}

case class CliOptions(
    config: ScalafmtConfig = ScalafmtConfig.default,
    range: Set[Range] = Set.empty[Range],
    customFiles: Seq[AbsoluteFile] = Nil,
    customExcludes: Seq[String] = Nil,
    writeMode: WriteMode = Override,
    testing: Boolean = false,
    stdIn: Boolean = false,
    quiet: Boolean = false,
    debug: Boolean = false,
    git: Option[Boolean] = None,
    nonInteractive: Boolean = false,
    diff: Option[String] = None,
    assumeFilename: String = "stdin.scala", // used when read from stdin
    migrate: Option[AbsoluteFile] = None,
    common: CommonOptions = CommonOptions(),
    gitOpsConstructor: AbsoluteFile => GitOps = x => new GitOpsImpl(x)
) {

  val inPlace: Boolean = writeMode == Override

  val fileFetchMode: FileFetchMode = {

    diff.map(DiffFiles(_)).getOrElse {
      val isGit: Boolean = git.getOrElse(config.project.git)
      if (isGit) GitFiles else RecursiveSearch
    }
  }

  val files: Seq[AbsoluteFile] =
    if (customFiles.isEmpty)
      Seq(common.workingDirectory)
    else
      customFiles

  val gitOps: GitOps = gitOpsConstructor(common.workingDirectory)
  def withProject(projectFiles: ProjectFiles): CliOptions = {
    this.copy(config = config.copy(project = projectFiles))
  }

  def withFiles(files: Seq[AbsoluteFile]): CliOptions = {
    this.copy(customFiles = files)
  }

  lazy val filterMatcher: FilterMatcher =
    FilterMatcher(
      config.project.includeFilters,
      config.project.excludeFilters ++ customExcludes
    )
}
