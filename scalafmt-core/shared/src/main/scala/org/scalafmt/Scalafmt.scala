package org.scalafmt

import metaconfig.Configured
import scala.meta.Dialect
import scala.meta.inputs.Input
import scala.util.control.NonFatal
import org.scalafmt.config.Config
import org.scalafmt.Error.PreciseIncomplete
import org.scalafmt.config.FormatEvent.CreateFormatOps
import org.scalafmt.config.LineEndings.preserve
import org.scalafmt.config.LineEndings.windows
import org.scalafmt.config.ScalafmtConfig
import org.scalafmt.internal.BestFirstSearch
import org.scalafmt.internal.FormatOps
import org.scalafmt.internal.FormatWriter
import org.scalafmt.rewrite.Rewrite

/**
  * WARNING. This API is discouraged when integrating with Scalafmt from a build tool
  * or editor plugin. It is recommended to use the `scalafmt-dynamic` module instead.
  */
object Scalafmt {

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) sys.error("missing file")
    import java.nio.file._
    def read(name: String): String = 
      new String(Files.readAllBytes(Paths.get(name)))
    val config = org.scalafmt.config.Config.fromHoconString(read(".scalafmt.conf")).get
    val code = read(args(1))
    val count = args(0).toInt
    val results = new Array[Long](count)
    0.until(count).foreach { i =>
      val start = System.nanoTime()
      format(code, config)
      val end = System.nanoTime()
      results(i) = end - start
    }
    results.foreach(println)
  }

  private val WindowsLineEnding = "\r\n"
  private val UnixLineEnding = "\n"

  /**
    * Format Scala code using scalafmt.
    *
    * WARNING. This API is discouraged when integrating with Scalafmt from a build tool
    * or editor plugin. It is recommended to use the `scalafmt-dynamic` module instead.
    *
    * @param code Code string to format.
    * @param style Configuration for formatting output.
    * @param range EXPERIMENTAL. Format a subset of lines.
    * @return [[Formatted.Success]] if successful,
    *         [[Formatted.Failure]] otherwise. If you are OK with throwing
    *         exceptions, use [[Formatted.Success.get]] to get back a
    *         string.
    */
  @noinline def format(
      code: String,
      style: ScalafmtConfig,
      range: Set[Range],
      filename: String
  ): Formatted = {
    try {
      val runner = style.runner
      if (code.matches("\\s*")) Formatted.Success(System.lineSeparator())
      else {
        val isWindows = containsWindowsLineEndings(code)
        val unixCode = if (isWindows) {
          code.replaceAll(WindowsLineEnding, UnixLineEnding)
        } else {
          code
        }
        val toParse = Rewrite(Input.VirtualFile(filename, unixCode), style)
        val tree = runner.dialect(toParse).parse(runner.parser).get
        val formatOps = new FormatOps(tree, style)
        runner.eventCallback(CreateFormatOps(formatOps))
        val formatWriter = new FormatWriter(formatOps)
        val search = new BestFirstSearch(formatOps, range, formatWriter)
        val partial = search.getBestPath
        val formattedString = formatWriter.mkString(partial.splits)
        val correctedFormattedString =
          if ((style.lineEndings == preserve && isWindows) ||
            style.lineEndings == windows) {
            formattedString.replaceAll(UnixLineEnding, WindowsLineEnding)
          } else {
            formattedString
          }
        if (partial.reachedEOF) {
          Formatted.Success(correctedFormattedString)
        } else {
          val pos = formatOps.tokens(partial.splits.length).left.pos
          throw PreciseIncomplete(pos, correctedFormattedString)
        }
      }
    } catch {
      // TODO(olafur) add more fine grained errors.
      case NonFatal(e) => Formatted.Failure(e)
    }
  }

  def format(
      code: String,
      style: ScalafmtConfig = ScalafmtConfig.default,
      range: Set[Range] = Set.empty[Range]
  ): Formatted = {
    format(code, style, range, "<input>")
  }

  def parseHoconConfig(configString: String): Configured[ScalafmtConfig] =
    Config.fromHoconString(configString, None)

  private[this] def containsWindowsLineEndings(code: String): Boolean =
    code.contains(WindowsLineEnding)

  /** Utility method to change dialect on ScalafmtConfig.
    *
    * Binary compatibility is guaranteed between releases, unlike with ScalafmtConfig.copy.
    **/
  def configWithDialect(
      config: ScalafmtConfig,
      dialect: Dialect
  ): ScalafmtConfig =
    config.withDialect(dialect)

  def configForSbt(
      config: ScalafmtConfig
  ): ScalafmtConfig =
    config.forSbt
}
