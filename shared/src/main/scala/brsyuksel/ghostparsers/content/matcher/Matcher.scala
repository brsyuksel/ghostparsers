package brsyuksel.ghostparsers.content.matcher

import zio._
import fs2.Pipe

trait Matcher {
  val matcher: Matcher.Service[Any]
}

object Matcher {
  trait Service[R] {
    def rows: ZIO[R, Throwable, Pipe[Task, List[String], Map[String, String]]]
  }
}
