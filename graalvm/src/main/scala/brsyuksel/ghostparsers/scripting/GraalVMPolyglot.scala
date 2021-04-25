package brsyuksel.ghostparsers.scripting

import org.graalvm.polyglot.{ Context, Value }

import zio._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

abstract class GraalVMPolyglot(source: String) extends Scripting.Service[Any] {
  implicit val dataEncoder: Encoder[ReturnValue] = deriveEncoder[ReturnValue]
  implicit val dataDecoder: Decoder[ReturnValue] = deriveDecoder[ReturnValue]

  private lazy val ctx = {
    val c = Context
      .newBuilder()
      .allowAllAccess(false)
      .build

    c.eval("js", source)
    c
  }

  private def getFn(name: String): ZIO[Any, Throwable, Value] =
    ZIO.effect(ctx.getBindings("js")) >>= { b =>
      if (!b.hasMember(name))
        ZIO.fail(new FunctionIsNotFoundError(name))
      else
        ZIO.effect(b.getMember(name))
    }

  private def callFn(fn: Value, p: String): ZIO[Any, Throwable, String] =
    ZIO.effect(fn.execute(p).asString())

  private def decodeResult(res: String): ZIO[Any, Throwable, ReturnValue] =
    ZIO.fromEither(decode[ReturnValue](res)).mapError(e => new MalformedJsonDataError(e.getMessage): Throwable)

  private def decodeMap(res: String): ZIO[Any, Throwable, Map[String, String]] =
    ZIO
      .fromEither(decode[Map[String, String]](res))
      .mapError(e => new MalformedJsonDataError(e.getMessage): Throwable)

  private def execute(fnName: String, param: String): ZIO[Any, Throwable, String] =
    for {
      fn  <- getFn(fnName)
      res <- callFn(fn, param)
    } yield res

  override def execute(m: Map[String, String]): ZIO[Any, Throwable, ReturnValue] =
    execute("map", m.asJson.noSpaces) >>= decodeResult

  override def execute(l: List[Map[String, String]]): ZIO[Any, Throwable, Map[String, String]] =
    execute("reduce", l.asJson.noSpaces) >>= decodeMap
}
