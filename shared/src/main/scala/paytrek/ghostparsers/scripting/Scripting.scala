package paytrek.ghostparsers.scripting

import zio._

trait Scripting extends Serializable {
  val scripting: Scripting.Service[Any]
}

object Scripting {
  trait Service[R] {
    def execute(m: Map[String, String]): ZIO[R, Throwable, ReturnValue]
    def execute(l: List[Map[String, String]]): ZIO[R, Throwable, Map[String, String]]
  }
}
