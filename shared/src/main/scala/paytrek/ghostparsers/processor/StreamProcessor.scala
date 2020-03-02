package paytrek.ghostparsers.processor

import scala.collection.immutable.TreeMap
import zio._
import zio.blocking.Blocking
import zio.stm._
import zio.interop.catz._
import fs2.Stream

import paytrek.ghostparsers.content.{ matcher => M, reader => R, writer => W }
import paytrek.ghostparsers.{ scripting => S }

abstract class StreamProcessor
    extends Processor.Service[R.Reader with M.Matcher with W.Writer with S.Scripting with Blocking] {
  private def updateTree(t: TreeMap[String, List[Map[String, String]]], n: String, d: Map[String, String]) =
    t ++ TreeMap(n -> (t.getOrElse(n, List.empty[Map[String, String]]) :+ d))

  private def stashedStream(t: TRef[TreeMap[String, List[Map[String, String]]]]) =
    Stream
      .eval(t.get.commit)
      .flatMap(t => Stream.fromIterator[Task](t.valuesIterator))

  override def process: ZIO[R.Reader with M.Matcher with W.Writer with S.Scripting with Blocking, Throwable, Unit] =
    for {
      tree        <- TRef.makeCommit(TreeMap.empty[String, List[Map[String, String]]])
      linesStream <- R.lines
      matchPipe   <- M.rows
      writerPipe  <- W.write
      scripting   <- ZIO.environment[S.Scripting]
      s = linesStream
        .through(matchPipe)
        .evalMap(m => S.execute(m).provideSome[Any](_ => scripting))
        .evalMap {
          case S.ReturnValue(d, None)    => ZIO.some(d)
          case S.ReturnValue(d, Some(n)) => tree.update(c => updateTree(c, n, d)).commit *> ZIO.none
        }
        .collect {
          case Some(d) => d
        }
        .onComplete(stashedStream(tree).evalMap(m => S.execute(m).provideSome[Any](_ => scripting)))
        .through(writerPipe)
      _ <- s.compile.drain
    } yield ()
}
