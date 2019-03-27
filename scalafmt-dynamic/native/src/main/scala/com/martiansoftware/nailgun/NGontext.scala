package com.martiansoftware.nailgun

import java.io.InputStream
import java.io.PrintStream

abstract class NGContext {
  def in: InputStream
  def out: PrintStream
  def err: PrintStream
  def exit(n: Int): Unit
  def getWorkingDirectory: String
  def getArgs: Array[String]
}
