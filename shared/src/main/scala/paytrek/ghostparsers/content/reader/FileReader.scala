package paytrek.ghostparsers.content.reader

import java.io.{ File, FileInputStream }
import java.nio.file.Paths

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

import zio._
import zio.blocking.Blocking
import zio.interop.catz._
import cats.effect.Blocker
import fs2.{ Pipe, Stream, text, io => fs2io }
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

private sealed trait StreamLines {
  def lines: Stream[Task, List[String]]
}

private final class CSV(path: String, delimiter: Char)(ec: ExecutionContext) extends StreamLines {
  def lines: Stream[Task, List[String]] =
    fs2io.file
      .readAll[Task](Paths.get(path), Blocker.liftExecutionContext(ec), 4096)
      .through(text.utf8Decode)
      .through(text.lines)
      .filter(_.nonEmpty)
      .map(_.split(delimiter).toList)
      .map(_.map(_.trim))
}

private abstract class Excel(path: String) {
  def rows: Pipe[Task, FileInputStream, Row] = ???

  def lines: Stream[Task, List[String]] =
    Stream
      .bracket(
        Task.effect(new FileInputStream(new File(path)))
      )(
        f => Task.effect(f.close)
      )
      .through(rows)
      .map(_.cellIterator.asScala.toList.map(_.toString))
      .map(_.map(_.trim))
}

private final class XLS(path: String, sheetAt: Int) extends Excel(path) with StreamLines {
  override def rows: Pipe[Task, FileInputStream, Row] =
    _.map(new HSSFWorkbook(_))
      .map(_.getSheetAt(sheetAt))
      .flatMap(s => Stream.fromIterator[Task](s.rowIterator.asScala))
}

private final class XLSX(path: String, sheetAt: Int) extends Excel(path) with StreamLines {
  override def rows: Pipe[Task, FileInputStream, Row] =
    _.map(new XSSFWorkbook(_))
      .map(_.getSheetAt(sheetAt))
      .flatMap(s => Stream.fromIterator[Task](s.rowIterator.asScala))
}

final class FileReader(parameters: FileParameters) extends Reader.Service[Blocking] {
  private def getReader(ec: ExecutionContext): Task[StreamLines] =
    parameters match {
      case FileParameters(p, d: FileTypeParameters.CSV) =>
        Task.succeed(new CSV(p, d.delimiter)(ec))
      case FileParameters(p, d: FileTypeParameters.Excel) if p.endsWith("xls") =>
        Task.succeed(new XLS(p, d.sheetAt))
      case FileParameters(p, d: FileTypeParameters.Excel) =>
        Task.succeed(new XLSX(p, d.sheetAt))
      case _ =>
        Task.fail(new IllegalArgumentException("invalid file type"))
    }

  override def lines: ZIO[Blocking, Throwable, Stream[Task, List[String]]] =
    for {
      b <- zio.blocking.blockingExecutor.map(_.asEC)
      r <- getReader(b)
    } yield r.lines

}
