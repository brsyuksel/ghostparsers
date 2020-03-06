package paytrek.ghostparsers.mock

import zio._
import zio.blocking.Blocking
import fs2.Pipe

import paytrek.ghostparsers.content.writer.Writer

final class MockWriter(p: Pipe[Task, Map[String, String], Unit]) extends Writer.Service[Blocking] {
  override def write: ZIO[Blocking, Throwable, Pipe[Task, Map[String, String], Unit]] =
    ZIO.effect(p)
}
