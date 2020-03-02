package paytrek.ghostparsers.scripting

final class FunctionIsNotFoundError(private val name: String) extends Exception(s"function is not found: $name")

final class EvaluationError(private val reason: String) extends Exception(s"function evaluation is failed: $reason")

final class MalformedJsonDataError(private val reason: String)
    extends Exception(s"returned data could not be parsed to type: $reason")
