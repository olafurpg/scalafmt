package docs

import metaconfig.Conf
import metaconfig.ConfEncoder
import metaconfig.Configured
import org.scalafmt.config.ScalafmtConfig
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import mdoc.Reporter
import mdoc.StringModifier

class DefaultsModifier extends StringModifier {
  override val name: String = "defaults"
  private val default =
    ConfEncoder[ScalafmtConfig].writeObj(ScalafmtConfig.default)

  override def process(
      info: String,
      code: Input,
      reporter: Reporter
  ): String = {
    if (info == "all") {
      val result =
        Conf.printHocon(ScalafmtConfig.default)
      "```\n" + result + "\n```"
    } else {
      def default(key: String): String = {
        val path = key.split("\\.").toList
        val down = path.foldLeft(this.default.dynamic)(_ selectDynamic _)
        down.asConf match {
          case Configured.Ok(value) =>
            value.toString()
            "`" + key + " = " + value.toString + "`\n"
          case Configured.NotOk(e) =>
            reporter.error(Position.Range(code, 0, 0), e.toString())
            "fail"
        }
      }
      val lines = code.text.trim.lines.toList.map(default)
      lines match {
        case line :: Nil =>
          "Default: " + line
        case _ =>
          lines.mkString("Defaults:\n* ", "\n* ", "\n")
      }
    }
  }
}
