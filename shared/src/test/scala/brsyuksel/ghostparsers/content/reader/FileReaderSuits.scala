package brsyuksel.ghostparsers.content.reader

import zio.interop.catz._
import zio.blocking.Blocking
import zio.test._

object FileReaderSuits
    extends DefaultRunnableSpec(
      suite("file reader suits")(
        testM("file reader should return lines of csv") {
          val tf = getClass.getResource("/samples/comma.csv").getPath
          val fp = FileParameters(tf, FileTypeParameters.CSV(','))
          val fr = new FileReader(fp)
          val res = for {
            stream <- fr.lines
            lines  <- stream.compile.toList
          } yield {
            assert(lines, Assertion.isNonEmpty) &&
            assert(lines.length, Assertion.equalTo(6)) &&
            assert(lines.head, Assertion.equalTo(List("a", "b", "c", "d", "e"))) &&
            assert(lines.last, Assertion.equalTo(List("5", "6", "7", "8", "9")))
          }
          res.provideSome[Any](_ => Blocking.Live)
        },
        testM("file reader should return lines of csv has different delimiter") {
          val tf = getClass.getResource("/samples/multiplier.csv").getPath
          val fp = FileParameters(tf, FileTypeParameters.CSV('*'))
          val fr = new FileReader(fp)
          val res = for {
            stream <- fr.lines
            lines  <- stream.compile.toList
          } yield {
            assert(lines, Assertion.isNonEmpty) &&
            assert(lines.length, Assertion.equalTo(6)) &&
            assert(lines.head, Assertion.equalTo(List("a", "b", "c", "d", "e"))) &&
            assert(lines.last, Assertion.equalTo(List("5", "6", "7", "8", "9")))
          }
          res.provideSome[Any](_ => Blocking.Live)
        },
        testM("file reader should return lines of xlsx file") {
          val tf = getClass.getResource("/samples/excel-xlsx.xlsx").getPath
          val fp = FileParameters(tf, FileTypeParameters.Excel(0))
          val fr = new FileReader(fp)
          val res = for {
            stream <- fr.lines
            lines  <- stream.compile.toList
          } yield {
            assert(lines, Assertion.isNonEmpty) &&
            assert(lines.length, Assertion.equalTo(6)) &&
            assert(lines.head, Assertion.equalTo(List("a", "b", "c", "d", "e"))) &&
            assert(lines.last, Assertion.equalTo(List("5", "6", "7", "8", "9")))
          }
          res.provideSome[Any](_ => Blocking.Live)
        },
        testM("file reader should return lines of xls file") {
          val tf = getClass.getResource("/samples/excel-xls.xls").getPath
          val fp = FileParameters(tf, FileTypeParameters.Excel(0))
          val fr = new FileReader(fp)
          val res = for {
            stream <- fr.lines
            lines  <- stream.compile.toList
          } yield {
            assert(lines, Assertion.isNonEmpty) &&
            assert(lines.length, Assertion.equalTo(6)) &&
            assert(lines.head, Assertion.equalTo(List("a", "b", "c", "d", "e"))) &&
            assert(lines.last, Assertion.equalTo(List("5.0", "6.0", "7.0", "8.0", "9.0")))
          }
          res.provideSome[Any](_ => Blocking.Live)
        }
      ))
