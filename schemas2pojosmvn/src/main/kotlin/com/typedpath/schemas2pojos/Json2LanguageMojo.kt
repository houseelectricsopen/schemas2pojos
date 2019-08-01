package com.typedpath.schemas2pojos

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.nio.file.Paths

@Mojo(name = "json2Typescript", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class Json2LanguageMojo : AbstractMojo() {

    private val logPrefix = javaClass.name;

    @Parameter(defaultValue = "", property = "sourceRoot", required = true)
    private val sourceRoot: String? = null

    @Parameter(defaultValue = "", property = "destinationRoot", required = true)
    private val destinationRoot: String? = null

    @Parameter(defaultValue = "", property = "sourceIncludes", required = false)
    private val sourceIncludes: String? = null

    class TypeMapping(var from : String?=null, var format: String?=null, var to: String?=null)

    @Parameter(property = "typeMappings", required = true)
    private val typeMappings: List<TypeMapping>? = listOf()

    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
        val srcRelativePath = Paths.get(sourceRoot)


        println(" searching $srcRelativePath with includes $sourceIncludes")

        val schemaDefs = read(srcRelativePath,
                toFileFilter(if (sourceIncludes == null) listOf("*/**/json") else sourceIncludes.split(",")))

        println("$logPrefix found ${schemaDefs.size} files in sourceRoot(path):${srcRelativePath.toAbsolutePath()} sourceIncludes:$sourceIncludes")

        schemaDefs.entries.forEach {
            println("$logPrefix  ${it.value.srcFile.path} => ${it.value.id} => ${it.value.root.fullname}")
            it.value.root.properties.forEach {
                println("  prop: ${it} ");
            }
            it.value.definitions.forEach {
                println("  def:  ${it}")
            }
        }

        val destinationRootPath = Paths.get(destinationRoot)

        fun schema2TypescriptTypeName(schemaName: String?, format: String?): String? {

            val matches = typeMappings!!
                    .filter { (it.from ==null || it.from.equals(schemaName)) && (it.format==null || it.format.equals(format))  }
                    .map { it.to }
            return if (matches.size==0) null else matches.first()
        }
        /*if (schemaName.equals("string")) "string"
        else if (schemaName.equals("int")) "number"
        else null*/

        typeMappings!!.forEach {
            println("$logPrefix typeMapping: type:${it.from}, format:${it.format}=>${it.to}")
        }

        writeTypescript(schemaDefs, destinationRootPath, "com.coconuts", ::schema2TypescriptTypeName)

    }
}