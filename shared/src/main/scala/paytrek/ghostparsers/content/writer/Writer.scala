package paytrek.ghostparsers.content.writer

import zio._
import zio.blocking.Blocking
import fs2.Pipe

trait Writer {
  val writer: Writer.Service[Blocking]
}

object Writer {
  trait Service[R] {
    def write: ZIO[R, Throwable, Pipe[Task, Map[String, String], Unit]]
  }
}
