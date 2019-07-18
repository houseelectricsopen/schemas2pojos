package com.typedpath.schemas2pojos

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.nio.file.Path
import java.nio.file.Paths

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.util.stream.Collectors

private fun pathMatcher(filter: String): PathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filter.trim { it <= ' ' })

private fun toFileFilter(strIncludes: List<String>): (File) -> Boolean {
    val actualIncludes =
            if (strIncludes.isEmpty()) {
                listOf("**/*.json")
            } else {
                strIncludes
            }
    val sourceIncludes = actualIncludes.stream().map({ f -> pathMatcher(f) }).collect(
            Collectors.toList<PathMatcher>()
    )
    return { f: File ->
        sourceIncludes.any { p -> p.matches(f.toPath()) }
    }
}

/**for debugging */
fun main(args: Array<String>) {

    val srcRelativePath = Paths.get("./src")

    val schemaDefs = read(srcRelativePath,
            toFileFilter(listOf("**/samples/*.json")))

    schemaDefs.entries.forEach {
        println("${it.value.srcFile.path} => ${it.value.id} => ${it.value.root.fullname}")
        it.value.root.properties.forEach {
            println("  prop: ${it} ");
        }
        it.value.definitions.forEach {
            println("  def:  ${it}")
        }
    }

    val destinationRootPath = Paths.get("target/generated")

    fun schema2TypescriptTypeName(schemaName: String): String?  = if (schemaName.equals("string")) "string"
                                                                  else if (schemaName.equals("int")) "number"
                                                                  else null

    writeTypescript(schemaDefs, destinationRootPath, "com.coconuts", ::schema2TypescriptTypeName)

}

@Mojo(name = "json2Typescript", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class Json2LanguageMojo : AbstractMojo() {
    @Parameter(defaultValue = "", property = "sourceRoot", required = true)
    private val sourceRoot: String? = null

    @Parameter(defaultValue = "", property = "destinationRoot", required = true)
    private val destinationRoot: String? = null

    @Parameter(defaultValue = "", property = "sourceIncludes", required = false)
    private val sourceIncludes: String? = null


    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
       val srcRelativePath = Paths.get(sourceRoot)

        println("searching $srcRelativePath with includes $sourceIncludes")

        val schemaDefs = read(srcRelativePath,
                toFileFilter(if (sourceIncludes==null) listOf("*/**/json") else sourceIncludes.split(",") ))

        schemaDefs.entries.forEach {
            println("${it.value.srcFile.path} => ${it.value.id} => ${it.value.root.fullname}")
            it.value.root.properties.forEach {
                println("  prop: ${it} ");
            }
            it.value.definitions.forEach {
                println("  def:  ${it}")
            }
        }

        val destinationRootPath = Paths.get(destinationRoot)

        fun schema2TypescriptTypeName(schemaName: String): String?  = if (schemaName.equals("string")) "string"
        else if (schemaName.equals("int")) "number"
        else null

        writeTypescript(schemaDefs, destinationRootPath, "com.coconuts", ::schema2TypescriptTypeName)

    }
}
