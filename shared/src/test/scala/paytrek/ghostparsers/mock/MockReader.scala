package paytrek.ghostparsers.mock

import zio._
import zio.blocking.Blocking
import fs2.Stream

import paytrek.ghostparsers.content.reader.Reader

final class MockReader(st: Stream[Task, List[String]]) extends Reader.Service[Blocking] {
  override def lines: ZIO[Blocking, Throwable, Stream[Task, List[String]]] =
    ZIO.effect(st)
}
