package actionContainers


object MyJarBuilder extends ResourceHelpers.JarBuilder {
  def mkBase64Jar(sources: Seq[(Seq[String], String)]): String = {
    // Note that this pipeline doesn't delete any of the temporary files.
    val binDir = compile(sources)
    val jarPath = makeJarFromDir(binDir)
    val base64 = readAsBase64(jarPath)
    base64
  }

  def mkBase64Jar(source: (Seq[String], String)): String = {
    mkBase64Jar(Seq(source))
  }
}
