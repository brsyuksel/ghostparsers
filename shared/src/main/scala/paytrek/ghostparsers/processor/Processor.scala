package paytrek.ghostparsers.processor

import zio._
import zio.blocking.Blocking

import paytrek.ghostparsers.content.reader.Reader
import paytrek.ghostparsers.content.matcher.Matcher
import paytrek.ghostparsers.content.writer.Writer
import paytrek.ghostparsers.scripting.Scripting

trait Processor {
  val processor: Processor.Service[Reader with Matcher with Writer with Scripting with Blocking]
}

object Processor {
  trait Service[R] {
    def process: ZIO[R, Throwable, Unit]
  }
}
