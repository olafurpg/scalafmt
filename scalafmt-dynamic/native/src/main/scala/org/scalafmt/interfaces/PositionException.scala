package org.scalafmt.interfaces

abstract class PositionException(message: String, cause: Throwable)
    extends Exception(message, cause) {
  def shortMessage: String
  def startLine: Int
  def startCharacter: Int
  def endLine: Int
  def endCharacter: Int
}
