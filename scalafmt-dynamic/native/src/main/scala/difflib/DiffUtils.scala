package difflib

import java.{util => ju}

object DiffUtils {
  def diff(original: ju.List[String], revised: ju.List[String]): Patch[String] =
    new Patch[String] {
      def getDeltas: ju.List[String] = ju.Collections.emptyList()
    }
  def generateUnifiedDiff(
      original: String,
      revised: String,
      originalLines: ju.List[String],
      patch: Patch[String],
      contextSize: Int
  ): ju.List[String] = ju.Collections.emptyList()
}

trait Patch[T] {
  def getDeltas: ju.List[T]
}
