package paytrek.ghostparsers

import paytrek.ghostparsers.scripting.Scripting

object main extends mainApp {
  override val scriptingEngine: String                            = "graalvm-polyglot"
  override val scriptingFactory: String => Scripting.Service[Any] = new scripting.GraalVMPolyglot(_) {}
}
