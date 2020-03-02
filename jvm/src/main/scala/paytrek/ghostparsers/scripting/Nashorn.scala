package paytrek.ghostparsers.scripting

import javax.script.ScriptEngineManager
import javax.script.Invocable

import zio._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

abstract class Nashorn(source: String) extends Scripting.Service[Any] {
  implicit val dataEncoder: Encoder[ReturnValue] = deriveEncoder[ReturnValue]
  implicit val dataDecoder: Decoder[ReturnValue] = deriveDecoder[ReturnValue]

  private lazy val engine = {
    val e = new ScriptEngineManager().getEngineByName("nashorn")
    e.eval(source)
    e.asInstanceOf[Invocable]
  }

  private def callFn(fn: String, param: String): ZIO[Any, Throwable, String] =
    ZIO.effect(engine.invokeFunction(fn, param).asInstanceOf[String])

  override def execute(m: Map[String, String]): ZIO[Any, Throwable, ReturnValue] =
    ZIO.effect(m.asJson.noSpaces) >>= (s => callFn("map", s)) >>= (j =>
      ZIO
        .fromEither(decode[ReturnValue](j))
        .mapError(t => new MalformedJsonDataError(t.getMessage)))

  override def execute(l: List[Map[String, String]]): ZIO[Any, Throwable, Map[String, String]] =
    ZIO.effect(l.asJson.noSpaces) >>= (s => callFn("reduce", s)) >>= (j =>
      ZIO
        .fromEither(decode[Map[String, String]](j))
        .mapError(t => new MalformedJsonDataError(t.getMessage)))
}
