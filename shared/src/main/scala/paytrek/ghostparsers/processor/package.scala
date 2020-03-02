package paytrek.ghostparsers

import zio._
import zio.blocking.Blocking

import paytrek.ghostparsers.content.reader.Reader
import paytrek.ghostparsers.content.matcher.Matcher
import paytrek.ghostparsers.content.writer.Writer
import paytrek.ghostparsers.scripting.Scripting

package object processor
    extends Processor.Service[Processor with Reader with Matcher with Writer with Scripting with Blocking] {

  override def process
    : ZIO[Processor with Reader with Matcher with Writer with Scripting with Blocking, Throwable, Unit] =
    ZIO.accessM(_.processor.process)
}
