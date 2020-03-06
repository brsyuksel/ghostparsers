package paytrek.ghostparsers.mock

import zio._
import paytrek.ghostparsers.scripting.{ ReturnValue, Scripting }

final class MockScripting(groupIds: List[String]) extends Scripting.Service[Any] {
  override def execute(m: Map[String, String]): ZIO[Any, Throwable, ReturnValue] =
    ZIO.effect(m.map(t => (t._1, t._2.toUpperCase))) >>= { m =>
      val k = m.getOrElse("key", "")
      groupIds.contains(k) match {
        case true =>
          ZIO.effect(ReturnValue(m, m.get("key")))
        case false =>
          ZIO.effect(ReturnValue(m))
      }
    }

  override def execute(l: List[Map[String, String]]): ZIO[Any, Throwable, Map[String, String]] =
    ZIO.effect {
      l.foldLeft(Map.empty[String, String]) {
        case (acc, e) =>
          val accV = acc.getOrElse("val", "")
          val eV   = e.getOrElse("val", "")
          acc ++ Map("key" -> e.getOrElse("key", ""), "val" -> (accV + eV))
      }
    }
}
