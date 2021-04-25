organization in ThisBuild := "brsyuksel"
scalaVersion in ThisBuild := "2.13.1"
version in ThisBuild := "0.1.0"

lazy val dependencies =
  new {
    lazy val zio            = "dev.zio"         %% "zio"                 % "1.0.0-RC17"
    lazy val zioInteropCatz = "dev.zio"         %% "zio-interop-cats"    % "2.0.0.0-RC10"
    lazy val fs2Core        = "co.fs2"          %% "fs2-core"            % "2.2.1"
    lazy val fs2IO          = "co.fs2"          %% "fs2-io"              % "2.2.1"
    lazy val apachePOIOOXML = "org.apache.poi"  % "poi-ooxml"            % "4.1.2"
    lazy val circeCore      = "io.circe"        %% "circe-core"          % "0.12.3"
    lazy val circeGeneric   = "io.circe"        %% "circe-generic"       % "0.12.3"
    lazy val circeParser    = "io.circe"        %% "circe-parser"        % "0.12.3"
    lazy val circeConfig    = "io.circe"        %% "circe-config"        % "0.7.0"
    lazy val http4sDSL      = "org.http4s"      %% "http4s-dsl"          % "0.21.0"
    lazy val http4sServer   = "org.http4s"      %% "http4s-blaze-server" % "0.21.0"
    lazy val http4sCirce    = "org.http4s"      %% "http4s-circe"        % "0.21.0"
    lazy val zioSlf4j       = "com.nequissimus" %% "zio-slf4j"           % "0.4.1"
    lazy val logbackClassic = "ch.qos.logback"  % "logback-classic"      % "1.2.3"
    lazy val zioTest        = "dev.zio"         %% "zio-test"            % "1.0.0-RC17"
    lazy val zioTestSBT     = "dev.zio"         %% "zio-test-sbt"        % "1.0.0-RC17"
  }

lazy val commonDependencies = Seq(
  dependencies.zio,
  dependencies.zioInteropCatz,
  dependencies.fs2Core,
  dependencies.fs2IO,
  dependencies.apachePOIOOXML,
  dependencies.circeCore,
  dependencies.circeGeneric,
  dependencies.circeParser,
  dependencies.circeConfig,
  dependencies.http4sDSL,
  dependencies.http4sServer,
  dependencies.http4sCirce,
  dependencies.zioSlf4j,
  dependencies.logbackClassic,
  dependencies.zioTest    % "test",
  dependencies.zioTestSBT % "test"
)

lazy val compilerOptions = Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
  "-Ybackend-parallelism",
  "4", // Enable paralellisation — change to desired number!
  "-Ycache-plugin-class-loader:last-modified", // Enables caching of classloaders for compiler plugins
  "-Ycache-macro-class-loader:last-modified" // and macro definitions. This can lead to performance improvements.
)

lazy val settings = Seq(
  scalacOptions ++= compilerOptions,
  libraryDependencies ++= commonDependencies,
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
)

lazy val packSettings = Seq(
  packMain := Map("ghostparsers" -> "brsyuksel.ghostparsers.main"),
  packGenerateWindowsBatFile := false
)

lazy val shared = project
  .settings(settings)

lazy val jvm = project
  .settings(
    settings ++ packSettings
  )
  .enablePlugins(PackPlugin)
  .dependsOn(shared)
  .aggregate(shared)

lazy val graalvm = project
  .settings(
    settings ++ packSettings
  )
  .enablePlugins(PackPlugin)
  .dependsOn(shared)
  .aggregate(shared)

/*
native-image \
  --report-unsupported-elements-at-runtime \
  --initialize-at-build-time=scala.runtime.Statics$VM,ch.qos.logback.core.boolex.JaninoEventEvaluatorBase \
  --allow-incomplete-classpath \
  --language:js \
  --no-server \
  -jar graalvm-assembly-0.1.0.jar
 */
