package paytrek.ghostparsers

import zio._
import zio.clock.Clock
import zio.duration._
import nequi.zio.logger._

import paytrek.ghostparsers.{ jobq => jq }

abstract class mainApp extends App {
  val scriptingEngine: String
  val scriptingFactory: String => scripting.Scripting.Service[Any]

  private val initEnv: Logger with Clock with conf.Configuration =
    new Logger with Clock.Live with conf.Configuration.Live {
      override val logger: Logger.Service[Any] = Slf4jLogger.create.logger
    }

  private def buildEnv(q: jq.JobQ.Service[Clock with Logger]): http.HttpEnv =
    new jq.JobQ with Clock.Live with Logger {
      override val jobq: jq.JobQ.Service[Clock with Logger] = q
      override val logger: Logger.Service[Any]              = Slf4jLogger.create.logger
    }

  private def prog: ZIO[Clock with Logger with conf.Configuration, Throwable, Unit] =
    for {
      c <- conf.load
      _ <- info(s"configuration has been read: $c")
      q <- jq.JobQueue.init(c.queue.capacity, c.queue.ttl)
      _ <- q.clean.retry(Schedule.recurs(3)).repeat(Schedule.spaced(5.seconds)).fork
      env = buildEnv(q)
      w   = new worker
      _ <- ZIO.sequence(
        List.fill(c.worker.size)(
          w.work(c.worker.output, scriptingFactory)
            .forever
            .retry(Schedule.recurs(3))
            .fork
            .unit
            .provideSome[Any](_ => env)))
      http = new http
      _ <- http.server(c.http.host, c.http.port, scriptingEngine, env)
    } yield ()

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    prog
      .provideSome[Any](_ => initEnv)
      .foldM(t => ZIO.effectTotal(println(t)).as(1), _ => ZIO.effectTotal(0))
}
