package org.scalafmt.config

import scala.meta.inputs.Input
import metaconfig.Conf
import metaconfig.Configured
import metaconfig.MetaconfigParser

object PlatformConfig {
  implicit val parser: MetaconfigParser = new MetaconfigParser {
    override def fromInput(input: Input): Configured[Conf] =
      metaconfig.hocon.hoconMetaconfigParser.fromInput(input).map(_.normalize)
  }
}
