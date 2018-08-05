import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import org.scalafmt.Scalafmt
import org.scalafmt.config.ScalafmtRunner
import org.scalafmt.config.ScalafmtConfig
import org.scalafmt.config.ScalafmtConfig.default40
import org.scalafmt.config.Config
import scala.meta.Dialect

package object website {

  def plaintext(code: String): String =
    new StringBuilder()
      .append("````\n")
      .append(code)
      .append("\n```")
      .toString()

  val logStream = Files.newOutputStream(
    Paths.get("target", "log.txt"),
    StandardOpenOption.APPEND,
    StandardOpenOption.CREATE)
  val logger = new PrintStream(logStream)

  /** Prints a formatted Scala code block one using the provided configuration,
    * which is added as a comment on top
    *
    * @param code the unformatted code
    * @param config the config as an HOCON string
    */
  def exampleBlock(code: String, config: String*): Unit = {
    val parsedConfig = configFromStrings(config)
    val processedCode = code.replaceAllLiterally("'''", "\"\"\"")
    val formattedCode = Scalafmt.format(processedCode, parsedConfig).get
    val sb = new StringBuilder
    sb.append(configSyntax(config))
      .append('\n')
      .append(formattedCode)
    val result = scalaCode(sb.toString())
    if (result.contains("assumeStandard")) {
      logger.println("================")
      logger.println(code)
      logger.println()
      logger.println(formattedCode)
      logger.println()
      logger.println(result)
    }
    println(result)
  }

  /** Prints two Scala code block next to each other, one with the original code,
    * the other one formatted using the provided configuration
    *
    * @param code the unformatted code
    * @param config the config to format the code (defaults to `default40`)
    */
  def formatExample(
      code: String,
      config: ScalafmtConfig = default40
  ): Unit = {
    val formatted = Scalafmt.format(code, ScalafmtConfig.default40).get
    println(
      s"""
<div class='scalafmt-pair'>
  <div class='before'>

${scalaCode(code)}

  </div>

  <div class='after'>

${scalaCode(formatted)}

  </div>
</div>
"""
    )
  }

  /** Prints the default value of a property
    *
    * @param selector a function to select the default from the config
    */
  def default[A](selector: ScalafmtConfig => A) = {
    val defaultValue = selector(ScalafmtConfig.default)
    println(s"Default: **$defaultValue**")
  }

  private[this] def scalaCode(code: String): String =
    new StringBuilder()
      .append("```scala\n")
      .append(code)
      .append("\n```")
      .toString()

  private[this] def configFromStrings(strings: Seq[String]): ScalafmtConfig = {
    Config
      .fromHoconString(strings.mkString("\n"))
      .get
      .copy(maxColumn = 40, runner = ScalafmtRunner.sbt)
  }

  private[this] def configSyntax(config: Seq[String]): String = {
    config.mkString("// ", "\n//", "")
  }

}
