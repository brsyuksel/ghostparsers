package brsyuksel.ghostparsers

import zio._
import zio.blocking.Blocking

import brsyuksel.ghostparsers.content.reader.Reader
import brsyuksel.ghostparsers.content.matcher.Matcher
import brsyuksel.ghostparsers.content.writer.Writer
import brsyuksel.ghostparsers.scripting.Scripting

package object processor
    extends Processor.Service[Processor with Reader with Matcher with Writer with Scripting with Blocking] {

  override def process
    : ZIO[Processor with Reader with Matcher with Writer with Scripting with Blocking, Throwable, Unit] =
    ZIO.accessM(_.processor.process)
}
