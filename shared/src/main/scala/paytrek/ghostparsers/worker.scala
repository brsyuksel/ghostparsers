package paytrek.ghostparsers

import zio._
import zio.clock.Clock
import zio.blocking.Blocking
import io.circe._
import io.circe.syntax._
import nequi.zio.logger.Logger

import paytrek.ghostparsers.content.{ reader => R }
import paytrek.ghostparsers.content.{ matcher => M }
import paytrek.ghostparsers.content.{ writer => W }
import paytrek.ghostparsers.{ processor => P, scripting => S }

final class worker {
  import worker._

  type ProcEnv = P.Processor with R.Reader with M.Matcher with W.Writer with S.Scripting with Blocking

  private def buildProcEnv(rP: R.FileParameters,
                           mP: M.FileParameters,
                           source: String,
                           output: String,
                           scriptingFactory: String => S.Scripting.Service[Any]): ProcEnv =
    new P.Processor with R.Reader with M.Matcher with W.Writer with S.Scripting with Blocking.Live {
      override val processor = new P.StreamProcessor {}
      override val reader    = new R.FileReader(rP)
      override val matcher   = new M.FileMatcher(mP)
      override val writer    = new W.FileWriter(output)
      override val scripting = scriptingFactory(source)
    }

  def work(
      outputPath: String,
      scriptingFactory: String => S.Scripting.Service[Any]): ZIO[jobq.JobQ with Clock with Logger, Throwable, Unit] =
    for {
      job           <- jobq.dequeue
      readerParams  <- json2FileReaderParams(job.jobRequest.body)
      matcherParams <- json2FileMatcherParams(job.jobRequest.body)
      source        <- json2Source(job.jobRequest.body)
      output  = s"$outputPath/${job.id.toString}.csv"
      procEnv = buildProcEnv(readerParams, matcherParams, source, output, scriptingFactory)
      res <- P.process
        .provideSome[Any](_ => procEnv)
        .fold(t => Result.Failed(t.getMessage).asJson, _ => Result.Succeed(output).asJson)
      _ <- jobq.complete(job.id, res)
    } yield ()
}

object worker {
  def json2FileReaderParams(json: Json): ZIO[Any, Throwable, R.FileParameters] =
    for {
      f <- ZIO
        .fromOption(json.hcursor.get[String]("file").toOption)
        .asError(new IllegalArgumentException("file not found"): Throwable)
      format <- ZIO
        .fromOption(json.hcursor.get[String]("format").toOption)
        .asError(new IllegalArgumentException("format not found"): Throwable)
      fp <- format match {
        case "csv" =>
          ZIO
            .fromOption(json.hcursor.downField("options").get[Char]("delimiter").toOption)
            .asError(new IllegalArgumentException("delimiter not found"): Throwable) >>= { d =>
            ZIO.effect(R.FileTypeParameters.CSV(d))
          }
        case "excel" =>
          ZIO
            .fromOption(json.hcursor.downField("options").get[Int]("sheet_at").toOption)
            .asError(new IllegalArgumentException("sheet_at not found"): Throwable) >>= { s =>
            ZIO.effect(R.FileTypeParameters.Excel(s))
          }
      }
    } yield R.FileParameters(f, fp)

  def json2FileMatcherParams(json: Json): ZIO[Any, Throwable, M.FileParameters] =
    for {
      h <- ZIO.effect(json.hcursor.downField("options").get[List[String]]("header").toOption)
      hs <- ZIO
        .fromOption(json.hcursor.downField("options").get[Long]("header_starts_at").toOption)
        .asError(new IllegalArgumentException("header_starts_at not found"): Throwable)
      rs <- ZIO
        .fromOption(json.hcursor.downField("options").get[Long]("rows_start_at").toOption)
        .asError(new IllegalArgumentException("rows_start_at not found"): Throwable)
    } yield M.FileParameters(h, hs, rs)

  def json2Source(json: Json): ZIO[Any, Throwable, String] =
    ZIO
      .fromOption(json.hcursor.get[String]("source").toOption)
      .asError(new IllegalArgumentException("source not found"): Throwable)

  sealed trait Result
  object Result {
    final case class Succeed(path: String)   extends Result
    final case class Failed(message: String) extends Result
  }

  implicit val succeedEncoder: Encoder[Result.Succeed] = (a: Result.Succeed) =>
    Json.obj(
      ("succeed", Json.fromBoolean(true)),
      ("path", Json.fromString(a.path))
  )
  implicit val failedEncoder: Encoder[Result.Failed] = (a: Result.Failed) =>
    Json.obj(
      ("succeed", Json.fromBoolean(false)),
      ("message", Json.fromString(a.message))
  )
}
