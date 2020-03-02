package paytrek.ghostparsers.content

import zio._
import fs2.Pipe

package object matcher extends Matcher.Service[Matcher] {
  override def rows: ZIO[Matcher, Throwable, Pipe[Task, List[String], Map[String, String]]] =
    ZIO.accessM(_.matcher.rows)
}
