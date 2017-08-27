package org.scalafmt
package jsfacade

import scala.util.Try

import config._
import rewrite._

object Config {
  import eu.unicredit.shocon

  private[this] def get[A: shocon.Extractor](
      config: shocon.Config.Value)(path: String, default: A): A =
    config.get(path).flatMap(_.as[A]).getOrElse(default)

  def fromHoconString(s: String): Either[String, ScalafmtConfig] = {
    org.scalafmt.config.Config.fromHoconString(s).toEither.left.map(_.toString)
  }
}
