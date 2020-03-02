package paytrek.ghostparsers.content.matcher

import zio._
import fs2.{ Pipe, Stream }

final class FileMatcher(parameters: FileParameters) extends Matcher.Service[Any] {
  private def getHeader(l: Stream[Task, List[String]]): Stream[Task, List[String]] =
    parameters.header match {
      case Some(h) => Stream(h).covary[Task].repeat
      case None    => l.drop(parameters.headerStartsAt).head.repeat
    }

  private def rowStream: Pipe[Task, List[String], Map[String, String]] =
    in => {
      val h = getHeader(in)
      val r = in.drop(parameters.rowsStartAt)
      h.zip(r) map { case (h, r) => h.zip(r).toMap }
    }

  override def rows: ZIO[Any, Throwable, Pipe[Task, List[String], Map[String, String]]] =
    ZIO.effect(rowStream)
}
