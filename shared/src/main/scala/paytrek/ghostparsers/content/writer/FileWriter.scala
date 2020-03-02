package paytrek.ghostparsers.content.writer

import java.nio.file.Paths

import scala.concurrent.ExecutionContext

import zio._
import zio.interop.catz._
import zio.blocking.Blocking
import cats.effect.Blocker
import fs2.{ Pipe, text, io => fs2io }

final class FileWriter(path: String) extends Writer.Service[Blocking] {
  private def writerStream(ec: ExecutionContext): Pipe[Task, Map[String, String], Unit] =
    in => {
      val h = in.head.map(_.toSeq.sortBy(_._1)).map(_.map(_._1).toList)
      val r = in.map(_.toSeq.sortBy(_._1)).map(_.map(_._2).toList)

      (h ++ r)
        .map(_.mkString(","))
        .intersperse("\n")
        .through(text.utf8Encode)
        .through(fs2io.file.writeAll[Task](Paths.get(path), Blocker.liftExecutionContext(ec)))
    }

  override def write: ZIO[Blocking, Throwable, Pipe[Task, Map[String, String], Unit]] =
    for {
      b <- zio.blocking.blockingExecutor.map(_.asEC)
      w = writerStream(b)
    } yield w
}
