package com.mojolly.scalate

import org.fusesource.scalate.{ TemplateEngine, TemplateSource, Binding }
import org.fusesource.scalate.util.IOUtil

import java.io.File

/**
 * Uses the Scalate template engine to generate Scala source files for Scalate templates.
 */
class Generator {
  
  var sources: File = _
  var targetDirectory: File = _
  var logConfig: File = _
  var scalateImports : Array[String] = Array.empty
  var scalateBindings: Array[Array[AnyRef]] = Array.empty // weird structure to represent Scalate Binding
  
  lazy val engine = {
    val e = new TemplateEngine
    
    // initialize template engine
    e.importStatements = scalateImports.toList
    e.bindings = (scalateBindings.toList map { b =>
      Binding(b(0).asInstanceOf[String], b(1).asInstanceOf[String], b(2).asInstanceOf[Boolean])
    }) ::: e.bindings
    e
  }

  def execute: Array[File] = {
    System.setProperty("logback.configurationFile", logConfig.toString)
    
    if (sources == null) {
      throw new IllegalArgumentException("The sources property is not properly set")
    }
    if (targetDirectory == null) {
      throw new IllegalArgumentException("The targetDirectory property is not properly set")
    }
    
    engine.packagePrefix = ""

    targetDirectory.mkdirs

    var paths = List.empty[String]
    for (extension <- engine.codeGenerators.keysIterator) {
      paths = collectUrisWithExtension(sources, "", "." + extension) ::: paths
    }

    paths map { uri =>
      val templateFile = new File(sources, uri)
      val path = uri
      val template = TemplateSource.fromFile(templateFile, path)
      val code = engine.generateScala(template).source
      val f = new File(targetDirectory, "/%s.scala".format(path.replaceAll("[.]", "_")))
      f.getParentFile.mkdirs
      IOUtil.writeBinaryFile(f, code.getBytes("UTF-8"))
      f
    } toArray
  }

  protected def collectUrisWithExtension(basedir: File, baseuri: String, extension: String): List[String] = {
    var collected = List[String]()
    if (basedir.isDirectory()) {
      var files = basedir.listFiles();
      if (files != null) {
        for (file <- files) {
          if (file.isDirectory()) {
            collected = collectUrisWithExtension(file, baseuri + "/" + file.getName(), extension) ::: collected;
          } else {
            if (file.getName().endsWith(extension)) {
              collected = baseuri + "/" + file.getName() :: collected
            } else {
            }

          }
        }
      }
    }
    collected
  }
  
}