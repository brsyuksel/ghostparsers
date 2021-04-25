package brsyuksel.ghostparsers.content

import fs2.Pipe
import zio._
import zio.blocking.Blocking

package object writer extends Writer.Service[Writer with Blocking] {
  override def write: ZIO[Writer with Blocking, Throwable, Pipe[Task, Map[String, String], Unit]] =
    ZIO.accessM(_.writer.write)
}
