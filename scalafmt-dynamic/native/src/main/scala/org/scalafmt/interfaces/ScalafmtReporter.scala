package org.scalafmt.interfaces

import java.nio.file.Path
import java.io.PrintWriter

trait ScalafmtReporter {
  def error(file: Path, message: String): Unit
  def error(file: Path, e: Throwable): Unit
  def error(file: Path, message: String, e: Throwable): Unit =
    error(file, new RuntimeException(message, e))
  def excluded(file: Path): Unit
  def missingVersion(config: Path, defaultVersion: String): Unit = {
    val message = String.format(
      "missing setting 'version'. To fix this problem, add the following line to .scalafmt.conf: 'version=%s'.",
      defaultVersion
    )
    error(config, message);
  }
  def parsedConfig(config: Path, scalafmtVersion: String): Unit
  def downloadWriter(): PrintWriter
}
