import sbt.*

object Dependencies {

  object Versions {
    val catbird = "22.12.0"
    val cats = "2.9.0"
    val enumeratum = "1.7.2"
    val enumeratumCirce = "1.7.2"
    val mailSlurp = "15.17.4"

    // Test
    val specs2 = "4.19.2"

    // Compiler
    val kindProjector = "0.13.2"
    val scalafixOrganiseImports = "0.6.0"
  }

  val catBird = "org.typelevel"                      %% "catbird-util"                % Versions.catbird
  val cats = "org.typelevel"                         %% "cats-core"                   % Versions.cats
  val enumeratum = "com.beachape"                    %% "enumeratum"                  % Versions.enumeratum
  val enumeratumCirce = "com.beachape"               %% "enumeratum-circe"            % Versions.enumeratumCirce
  val mailSlurp = "com.mailslurp" % "mailslurp-client-java" % Versions.mailSlurp

  // Test
  val specs2 = "org.specs2" %% "specs2-core" % Versions.specs2

  // Compiler
  val kindProjector = "org.typelevel"                   % "kind-projector"   % Versions.kindProjector cross CrossVersion.full
  val scalafixOrganiseImports = "com.github.liancheng" %% "organize-imports" % Versions.scalafixOrganiseImports
}
