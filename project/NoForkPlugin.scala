import org.jetbrains.sbtidea.SbtIdeaPlugin
import sbt.Def
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

// Workaround for sbt-idea-plugin settings fork := true which breaks sbt-scalajs
// https://github.com/JetBrains/sbt-idea-plugin/commit/d56a8c0641329eddeb340baa0a15a3786d0c73f8#commitcomment-29963247
object NoForkPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin && SbtIdeaPlugin
  override def projectSettings: Seq[Def.Setting[_]] = List(
    fork.in(Test) := false
  )
}
