package org.scalafmt.util

import scala.util.Try
import scala.util.control.NonFatal

import java.io.File
import java.nio.file.Files
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.collection.JavaConverters._

trait GitOps {
  def diff(branch: String): Seq[AbsoluteFile]
  def lsTree(dir: AbsoluteFile): Seq[AbsoluteFile]
  def rootDir: Option[AbsoluteFile]
}

object GitOps {
  def apply(): GitOps = new GitOpsImpl(AbsoluteFile.userDir)
}

case class GitOpsImpl(private[util] val workingDirectory: AbsoluteFile)
    extends GitOps {

  private[util] def exec(cmd: Seq[String]): Try[Seq[String]] = {
    val gitRes: Try[String] = Try {
      try {
        val process = new ProcessBuilder(cmd.asJava)
          .directory(workingDirectory.jfile)
          .start()
        val out = new StringBuilder()
        val reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))
        try {
          var line = ""
          while ({
            line = reader.readLine()
            line != null
          }) {
            out.append(line).append("\n")
          }
        } finally {
          reader.close()
        }
        val exit = process.waitFor()
        if (exit != 0) sys.error(s"exit=$exit")
        out.toString()
      } catch {
        case NonFatal(e) =>
          throw new RuntimeException(s"command: ${cmd.mkString(" ")}", e)
      }
    }
    // Predef.augmentString = work around scala/bug#11125 on JDK 11
    gitRes.map(augmentString(_).lines.toSeq)
  }

  override def lsTree(dir: AbsoluteFile): Seq[AbsoluteFile] =
    rootDir.fold(Seq.empty[AbsoluteFile]) { rtDir =>
      exec(
        Seq(
          "git",
          "ls-files",
          "--full-name",
          dir.path
        )
      ).toOption.toSeq.flatten.map(f => rtDir / f)
    }

  override def rootDir: Option[AbsoluteFile] = {
    val cmd = Seq(
      "git",
      "rev-parse",
      "--show-toplevel"
    )
    for {
      Seq(rootPath) <- exec(cmd).toOption
      file <- AbsoluteFile.fromPath(rootPath)
      if file.jfile.isDirectory
    } yield file
  }

  override def diff(branch: String): Seq[AbsoluteFile] = {
    val cmd = Seq(
      "git",
      "diff",
      "--name-only",
      "--diff-filter=d",
      branch
    )
    for {
      root <- rootDir.toSeq
      path <- exec(cmd).toOption.toSeq.flatten
    } yield AbsoluteFile.fromFile(new File(path), root)
  }
}
