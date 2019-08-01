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


public fun toFileFilter(strIncludes: List<String>): (File) -> Boolean {
    val actualIncludes =
            if (strIncludes.isEmpty()) {
                listOf("**/*.json")
            } else {
                strIncludes
            }
    fun pathMatcher(filter: String): PathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filter.trim { it <= ' ' })

    val sourceIncludes = actualIncludes.stream().map({ f -> pathMatcher(f) }).collect(
            Collectors.toList<PathMatcher>()
    )
    return { f: File ->
        sourceIncludes.any { p -> p.matches(f.toPath()) }
    }
}

/**for debugging */
fun main(args: Array<String>) {

    val srcRelativePath = Paths.get("./src/main/resources/samples")

    val schemaDefs = read(srcRelativePath,
            toFileFilter(listOf("**/*.json")))

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

    fun schema2TypescriptTypeName(schemaName: String?, format: String?): String?  = if (schemaName!=null && schemaName.equals("string")) "string"
                                                                  else if (schemaName!=null && schemaName.equals("int")) "number"
                                                                  else if (schemaName!=null && schemaName.equals("integer")) "number"
                                                                  else if (schemaName!=null && schemaName.equals("boolean")) "boolean"
                                                                  else if (format.equals("dateTime")) "string"
                                                                  else null

    writeTypescript(schemaDefs, destinationRootPath, "com.coconuts", ::schema2TypescriptTypeName)

}


