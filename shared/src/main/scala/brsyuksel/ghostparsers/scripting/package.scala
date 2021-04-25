package brsyuksel.ghostparsers

import zio._

package object scripting extends Scripting.Service[Scripting] {
  override def execute(m: Map[String, String]): ZIO[Scripting, Throwable, ReturnValue] =
    ZIO.accessM(_.scripting.execute(m))

  override def execute(l: List[Map[String, String]]): ZIO[Scripting, Throwable, Map[String, String]] =
    ZIO.accessM(_.scripting.execute(l))
}
