package paytrek.ghostparsers.content

import zio._
import zio.blocking.Blocking
import fs2.Stream

package object reader extends Reader.Service[Reader with Blocking] {
  override def lines: ZIO[Reader with Blocking, Throwable, Stream[Task, List[String]]] =
    ZIO.accessM(_.reader.lines)
}
