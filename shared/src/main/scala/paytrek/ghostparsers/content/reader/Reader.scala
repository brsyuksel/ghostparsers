package paytrek.ghostparsers.content.reader

import zio._
import zio.blocking.Blocking
import fs2.Stream

trait Reader {
  val reader: Reader.Service[Blocking]
}

object Reader {
  trait Service[R] {
    def lines: ZIO[R, Throwable, Stream[Task, List[String]]]
  }
}
