package brsyuksel.ghostparsers.content.matcher

sealed trait Parameters
final case class FileParameters(header: Option[List[String]] = None, headerStartsAt: Long = 0, rowsStartAt: Long = 1)
    extends Parameters
