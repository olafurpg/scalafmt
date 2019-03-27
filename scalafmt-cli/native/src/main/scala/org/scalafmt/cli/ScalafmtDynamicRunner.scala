package org.scalafmt.cli

import java.nio.file.Path
import java.io.PrintWriter

object ScalafmtDynamicRunner extends ScalafmtRunner {
  def run(
      options: CliOptions,
      termDisplayMessage: String
  ): ExitCode = ExitCode.Ok
}
