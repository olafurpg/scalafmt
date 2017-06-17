package org.scalafmt.cli

import scala.io.Codec
import scala.io.Source
import java.io.File
import java.io.InputStream
import scala.meta.inputs.Input
import org.scalafmt.Error.MisformattedFile
import org.scalafmt.util.AbsoluteFile
import org.scalafmt.util.FileOps

sealed abstract class InputMethod {
  def isSbt = filename.endsWith(".sbt")
  def readInput(implicit codec: Codec): Input
  def filename: String
  def write(formatted: String,
            original: Array[Char],
            options: CliOptions): Unit
}

object InputMethod {

  object StdinCode {
    def apply(assumeFilename: String, inputStream: InputStream): StdinCode = {
      StdinCode.apply(
        assumeFilename,
        Source.fromInputStream(inputStream).getLines().mkString("\n")
      )
    }
  }
  case class StdinCode(filename: String, input: String) extends InputMethod {
    def readInput(implicit codec: Codec): Input =
      Input.LabeledString(filename, input)
    override def write(code: String,
                       original: Array[Char],
                       options: CliOptions): Unit = {
      options.common.out.print(code)
    }
  }
  case class FileContents(file: AbsoluteFile) extends InputMethod {
    override def filename = file.path
    def readInput(implicit codec: Codec): Input = Input.File(file.jfile)
    override def write(formatted: String,
                       original: Array[Char],
                       options: CliOptions): Unit = {
      val codeChanged = !formatted.toCharArray.sameElements(original)
      if (options.testing) {
        if (codeChanged)
          throw MisformattedFile(new File(filename),
                                 options.config.onTestFailure)
        else Unit
      } else if (options.inPlace) {
        if (codeChanged) FileOps.writeFile(filename, formatted)
        else Unit
      } else {
        options.common.out.print(formatted)
      }
    }
  }
}
