package org.scalafmt.cli

import java.io.Writer

class TermDisplay(
    out: Writer,
    val fallbackMode: Boolean
) extends Cache.Logger

object TermDisplay {
  def defaultFallbackMode: Boolean = true
}
