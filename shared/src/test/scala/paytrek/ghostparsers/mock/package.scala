package paytrek.ghostparsers

import zio._
import zio.clock.Clock
import zio.blocking.Blocking
import nequi.zio.logger.Logger

import paytrek.ghostparsers.content.{ matcher => M, reader => R, writer => W }
import paytrek.ghostparsers.{ processor => P, scripting => S }

package object mock {
  val jobqSuiteEnv: Logger with Clock =
    new Logger with Clock.Live {
      override val logger: Logger.Service[Any] = new Logger.Service[Any] {
        override def trace(a: String): ZIO[Any, Nothing, Unit] = ZIO.unit
        override def debug(a: String): ZIO[Any, Nothing, Unit] = ZIO.unit
        override def info(a: String): ZIO[Any, Nothing, Unit]  = ZIO.unit
        override def warn(a: String): ZIO[Any, Nothing, Unit]  = ZIO.unit
        override def error(a: String): ZIO[Any, Nothing, Unit] = ZIO.unit
      }
    }

  def stProcEnv(p: P.Processor.Service[R.Reader with M.Matcher with W.Writer with S.Scripting with Blocking],
                r: R.Reader.Service[Blocking],
                m: M.Matcher.Service[Any],
                w: W.Writer.Service[Blocking],
                s: S.Scripting.Service[Any])
    : P.Processor with R.Reader with M.Matcher with W.Writer with S.Scripting with Blocking =
    new P.Processor with R.Reader with M.Matcher with W.Writer with S.Scripting with Blocking.Live {
      override val processor
        : P.Processor.Service[R.Reader with M.Matcher with W.Writer with S.Scripting with Blocking] =
        p
      override val reader: R.Reader.Service[Blocking]  = r
      override val matcher: M.Matcher.Service[Any]     = m
      override val writer: W.Writer.Service[Blocking]  = w
      override val scripting: S.Scripting.Service[Any] = s
    }
}
