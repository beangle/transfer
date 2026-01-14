import org.beangle.parent.Dependencies.*
import org.beangle.parent.Settings.*

ThisBuild / organization := "org.beangle.transfer"
ThisBuild / version := "0.0.2"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/beangle/transfer"),
    "scm:git@github.com:beangle/transfer.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "The Beangle Transfer Library"
ThisBuild / homepage := Some(url("https://beangle.github.io/transfer/index.html"))

val beangle_commons = "org.beangle.commons" % "beangle-commons" % "5.7.0"
val beangle_model = "org.beangle.data" % "beangle-model" % "5.11.5"
val beangle_template = "org.beangle.template" % "beangle-template" % "0.2.2"
val beangle_doc_excel = "org.beangle.doc" % "beangle-doc-excel" % "0.5.0"

lazy val root = (project in file("."))
  .settings(
    name := "beangle-transfer",
    common,
    libraryDependencies ++= Seq(logback_classic % "test", scalatest, beangle_commons, beangle_doc_excel),
    libraryDependencies ++= Seq(beangle_model)
  )

