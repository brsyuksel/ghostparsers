package brsyuksel.ghostparsers.conf

import zio._
import io.circe.config.parser
import io.circe.generic.auto._

trait Configuration {
  val config: Configuration.Service[Any]
}

object Configuration {
  trait Service[R] {
    def load: ZIO[R, Throwable, Config]
  }

  trait Live extends Configuration {
    override val config: Service[Any] = new Service[Any] {
      override def load: ZIO[Any, Throwable, Config] = ZIO.fromEither(parser.decode[Config])
    }
  }
}
