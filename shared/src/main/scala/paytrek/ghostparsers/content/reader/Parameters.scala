package paytrek.ghostparsers.content.reader

sealed trait Parameters

sealed trait FileTypeParameters
object FileTypeParameters {
  case class CSV(delimiter: Char) extends FileTypeParameters
  case class Excel(sheetAt: Int)  extends FileTypeParameters
}

case class FileParameters(path: String, params: FileTypeParameters) extends Parameters
