package brsyuksel.ghostparsers.content.writer

import java.nio.file.{ Files, Paths }

import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import zio.test._
import cats.effect.Blocker
import fs2.{ io => fs2io, text, Stream }

private object testData {
  val tempFile: ZIO[Any, Throwable, String] =
    ZIO.effect(Files.createTempFile("gp-", ".tmp").toString)

  val stream1 =
    Stream(Map("a" -> "1", "b" -> "2"), Map("a" -> "11", "b" -> "22"), Map("a" -> "111", "b" -> "222")).covary[Task]
}

object FileWriterSuits
    extends DefaultRunnableSpec(
      suite("file writer")(
        testM("writes keys as a header and values as rows") {
          val res = for {
            tmp <- testData.tempFile
            w = new FileWriter(tmp)
            wS <- w.write
            _  <- testData.stream1.through(wS).compile.drain
            ec <- zio.blocking.blockingExecutor.map(_.asEC)
            written <- fs2io.file
              .readAll[Task](Paths.get(tmp), Blocker.liftExecutionContext(ec), 4096)
              .through(text.utf8Decode)
              .through(text.lines)
              .compile
              .toList
          } yield {
            assert(written, Assertion.isNonEmpty) &&
            assert(written, Assertion.equalTo(List("a,b", "1,2", "11,22", "111,222")))
          }
          res.provideSome[Any](_ => Blocking.Live)
        }
      )
    )
