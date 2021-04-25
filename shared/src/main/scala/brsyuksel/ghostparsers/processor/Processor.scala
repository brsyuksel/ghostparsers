package brsyuksel.ghostparsers.processor

import zio._
import zio.blocking.Blocking

import brsyuksel.ghostparsers.content.reader.Reader
import brsyuksel.ghostparsers.content.matcher.Matcher
import brsyuksel.ghostparsers.content.writer.Writer
import brsyuksel.ghostparsers.scripting.Scripting

trait Processor {
  val processor: Processor.Service[Reader with Matcher with Writer with Scripting with Blocking]
}

object Processor {
  trait Service[R] {
    def process: ZIO[R, Throwable, Unit]
  }
}
