package brsyuksel.ghostparsers.processor

import zio._
import zio.test._
import fs2.{ Pipe, Stream }

import brsyuksel.ghostparsers.mock
import brsyuksel.ghostparsers.content.matcher.{ FileMatcher, FileParameters }

object testData {
  def pipe1(r: Ref[List[Map[String, String]]]): Pipe[Task, Map[String, String], Unit] =
    _.evalMap(m => r.update(_ ::: List(m)).unit)

  val stream1 = Stream(List("key", "val"), List("a", "aa"), List("b", "bb")).covary[Task]
  val stream2 = Stream(List("key", "val"), List("a", "aa"), List("b", "bb"), List("a", "aaa")).covary[Task]
}

object StreamProcessorSuits
    extends DefaultRunnableSpec(
      suite("stream processor")(
        testM("process writes inputs directly to output after matching and scripting if groupBy is not provided") {
          val mr = new mock.MockReader(testData.stream1)
          val mm = new FileMatcher(FileParameters())
          for {
            r <- Ref.make[List[Map[String, String]]](List.empty[Map[String, String]])
            writerPipe = testData.pipe1(r)
            w          = new mock.MockWriter(writerPipe)
            s          = new mock.MockScripting(List.empty[String])
            proc       = new StreamProcessor {}
            env        = mock.stProcEnv(proc, mr, mm, w, s)
            _     <- proc.process.provideSome[Any](_ => env)
            lines <- r.get
          } yield {
            assert(lines, Assertion.isNonEmpty) &&
            assert(lines, Assertion.contains(Map("key" -> "A", "val" -> "AA"))) &&
            assert(lines, Assertion.contains(Map("key" -> "B", "val" -> "BB")))
          }
        },
        testM("writes not grouped inputs directly and then writes grouped inputs as reduced") {
          val mr = new mock.MockReader(testData.stream2)
          val mm = new FileMatcher(FileParameters())
          for {
            r <- Ref.make[List[Map[String, String]]](List.empty[Map[String, String]])
            writerPipe = testData.pipe1(r)
            w          = new mock.MockWriter(writerPipe)
            s          = new mock.MockScripting(List("A"))
            proc       = new StreamProcessor {}
            env        = mock.stProcEnv(proc, mr, mm, w, s)
            _     <- proc.process.provideSome[Any](_ => env)
            lines <- r.get
          } yield {
            assert(lines, Assertion.isNonEmpty) &&
            assert(lines, Assertion.contains(Map("key" -> "A", "val" -> "AAAAA"))) &&
            assert(lines, Assertion.contains(Map("key" -> "B", "val" -> "BB")))
          }
        }
      ))
