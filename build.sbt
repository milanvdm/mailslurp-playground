///////////////////////////////////////////////////////////////////////////////////////////////////
// Commands
///////////////////////////////////////////////////////////////////////////////////////////////////

addCommandAlias("update", ";dependencyUpdates; reload plugins; dependencyUpdates; reload return")
addCommandAlias("fmt", ";scalafixAll;scalafmtSbt;scalafmtAll")
addCommandAlias("fmtCheck", ";scalafixAll --check;scalafmtSbtCheck;scalafmtCheckAll")

///////////////////////////////////////////////////////////////////////////////////////////////////
// Projects
///////////////////////////////////////////////////////////////////////////////////////////////////

lazy val commonSettings = Seq(
  organization := "me.milanvdm",
  scalaVersion := "2.13.10",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  ThisBuild / scalafixDependencies += Dependencies.scalafixOrganiseImports,
  libraryDependencies ++= Seq(
    compilerPlugin(Dependencies.kindProjector)
  ),
  coverageExcludedFiles := "^.*src_managed.*$",
  ThisBuild / scapegoatVersion := "2.0.0",
  scapegoatConsoleOutput := false,
  scapegoatIgnoredFiles := Seq(
    ".*/src_managed/.*",
    ".*/protobuf/.*"
  )
)

lazy val integrationTestSettings =
  Defaults.itSettings ++ inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest))

lazy val root = Project(id = "root", base = file("."))
  .settings(commonSettings)
  .aggregate(
    service,
  )
  .disablePlugins(RevolverPlugin)

lazy val service = Project(id = s"mailslurp-playground", base = file("service"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.catBird,
      Dependencies.cats,
      Dependencies.enumeratum,
      Dependencies.mailSlurp,

      // Test
      Dependencies.specs2
    )
  )
  .configs(IntegrationTest extend Test)
  .settings(integrationTestSettings)
  .settings(Revolver.settings)
