package paytrek.ghostparsers

import zio._
import zio.clock.Clock
import nequi.zio.logger.Logger

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
}
