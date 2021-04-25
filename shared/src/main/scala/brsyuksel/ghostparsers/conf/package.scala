package brsyuksel.ghostparsers

import zio._

package object conf extends Configuration.Service[Configuration] {
  override def load: ZIO[Configuration, Throwable, Config] =
    ZIO.accessM(_.config.load)
}
