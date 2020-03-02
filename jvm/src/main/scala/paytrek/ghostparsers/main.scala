package paytrek.ghostparsers

import paytrek.ghostparsers.scripting.Scripting

object main extends mainApp {
  override val scriptingEngine: String                            = "jvm-nashorn"
  override val scriptingFactory: String => Scripting.Service[Any] = new scripting.Nashorn(_) {}
}
