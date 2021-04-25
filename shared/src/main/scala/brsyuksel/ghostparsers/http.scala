package brsyuksel.ghostparsers

import java.time.Instant

import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.http4s.circe._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import nequi.zio.logger.Logger

import brsyuksel.ghostparsers.{ jobq => jq }

final class http {
  implicit val rts = new DefaultRuntime {}

  def server(host: String, port: Int, engine: String, env: http.HttpEnv): Task[Unit] =
    BlazeServerBuilder[Task]
      .bindHttp(port, host)
      .withHttpApp(http.buildRoutes(engine, env).orNotFound)
      .serve
      .compile
      .drain
}

object http {
  type HttpEnv = jq.JobQ with zio.clock.Clock with Logger

  private val dsl = Http4sDsl[Task]
  import dsl._

  import response._

  private def buildRoutes(engine: String, env: HttpEnv) = HttpRoutes.of[Task] {
    case GET -> Root / "_health" =>
      val res = for {
        size <- jq.queued.provideSome[Any](_ => env)
        h = Health(size, engine, "If you've had a dose of a freaky eods, baby, you better call, ghostparsers!")
      } yield h
      res.foldM(t => InternalServerError(ErrorResponse(t.getMessage).asJson), h => Ok(h.asJson))

    case req @ POST -> Root =>
      val res = for {
        payload <- req.as[Json]
        jReq = jq.JobRequest(payload)
        jStatus <- jq.enqueue(jReq).provideSome[Any](_ => env)
      } yield jStatus
      res.foldM(t => BadRequest(ErrorResponse(t.getMessage).asJson), o => Created(o.asJson))

    case GET -> Root / UUIDVar(uuid) =>
      jq.getStatus(uuid)
        .provideSome[Any](_ => env)
        .flatMap(jo => ZIO.fromOption(jo))
        .foldM(_ => NotFound(), o => Ok(o.asJson))
  }
}

object response {
  case class ErrorResponse(message: String)
  implicit val errorEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]

  case class Health(queued: Int, engine: String, message: String)
  implicit val healthEncoder: Encoder[Health] = deriveEncoder[Health]

  implicit val jobStatusEncoder: Encoder[jq.JobStatus] =
    Encoder.forProduct5("id", "status", "updated_at", "result", "completed_at") { s =>
      (s.id.toString,
       s.status.toString.toLowerCase,
       Instant.ofEpochMilli(s.updatedAt),
       s.result,
       s.completedAt.map(Instant.ofEpochMilli))
    }
}
