import org.beangle.parent.Dependencies.*
import org.beangle.parent.Settings.*

organization := "org.beangle.transfer"
version := "0.0.8"

scmInfo := Some(
  ScmInfo(
    uri("https://github.com/beangle/transfer"),
    "scm:git@github.com:beangle/transfer.git"
  )
)

developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = uri("http://github.com/duantihua")
  )
)

description := "The Beangle Transfer Library"
homepage := Some(uri("https://beangle.github.io/transfer/index.html"))

val beangle_commons = "org.beangle.commons" % "beangle-commons" % "6.3.2"
val beangle_model = "org.beangle.data" % "beangle-data-model" % "5.12.8"
val beangle_template = "org.beangle.template" % "beangle-template" % "0.2.11"
val beangle_doc_excel = "org.beangle.doc" % "beangle-doc-excel" % "0.5.13"

lazy val root = (project in file("."))
  .settings(
    name := "beangle-transfer",
    common,
    libraryDependencies ++= Seq(logback_classic % "test", scalatest, beangle_commons, beangle_doc_excel),
    libraryDependencies ++= Seq(beangle_model)
  )
