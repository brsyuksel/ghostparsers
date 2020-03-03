package paytrek.ghostparsers.content.matcher

import zio._
import zio.interop.catz._
import zio.test._
import fs2.Stream

private object testData {
  val stream1 = Stream(List("a", "b", "c"), List("1", "2", "3"), List("11", "22", "33"), List("111", "222", "333"))
    .covary[Task]
}

object FileMatcherSuits
    extends DefaultRunnableSpec(
      suite("file matcher suits")(
        testM("matches first line and remains if not any optional param provided") {
          val fp = FileParameters()
          val fm = new FileMatcher(fp)
          for {
            rowsPipe <- fm.rows
            data     <- testData.stream1.through(rowsPipe).compile.toList
          } yield {
            assert(data, Assertion.isNonEmpty) &&
            assert(data.head, Assertion.equalTo(Map("a" -> "1", "b"   -> "2", "c"   -> "3"))) &&
            assert(data.last, Assertion.equalTo(Map("a" -> "111", "b" -> "222", "c" -> "333")))
          }
        },
        testM("matches provided header instead of in stream") {
          val fp = FileParameters(header = Some(List("aa", "bb", "cc")))
          val fm = new FileMatcher(fp)
          for {
            rowsPipe <- fm.rows
            data     <- testData.stream1.through(rowsPipe).compile.toList
          } yield {
            assert(data, Assertion.isNonEmpty) &&
            assert(data.head, Assertion.equalTo(Map("aa" -> "1", "bb"   -> "2", "cc"   -> "3"))) &&
            assert(data.last, Assertion.equalTo(Map("aa" -> "111", "bb" -> "222", "cc" -> "333")))
          }
        },
        testM("skips lines of row to match with header") {
          val fp = FileParameters(rowsStartAt = 2L)
          val fm = new FileMatcher(fp)
          for {
            rowsPipe <- fm.rows
            data     <- testData.stream1.through(rowsPipe).compile.toList
          } yield {
            assert(data, Assertion.isNonEmpty) &&
            assert(data.head, Assertion.equalTo(Map("a" -> "11", "b"  -> "22", "c"  -> "33"))) &&
            assert(data.last, Assertion.equalTo(Map("a" -> "111", "b" -> "222", "c" -> "333")))
          }
        },
        testM("skips lines in order to fetch header") {
          val st = Stream(List("x", "y", "z")).covary[Task] ++ testData.stream1
          val fp = FileParameters(headerStartsAt = 1L, rowsStartAt = 2L)
          val fm = new FileMatcher(fp)
          for {
            rowsPipe <- fm.rows
            data     <- st.through(rowsPipe).compile.toList
          } yield {
            assert(data, Assertion.isNonEmpty) &&
            assert(data.head, Assertion.equalTo(Map("a" -> "1", "b"   -> "2", "c"   -> "3"))) &&
            assert(data.last, Assertion.equalTo(Map("a" -> "111", "b" -> "222", "c" -> "333")))
          }
        }
      ))
