package com.typedpath.schemas2pojos

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.nio.file.Paths

@Mojo(name = "schema2language", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class Schema2LanguageMojo : AbstractMojo() {

    private val logPrefix = javaClass.name;

    @Parameter( property = "schemaFormat", required = true)
    private val schemaFormat: String? = null

    @Parameter( property = "destinationFormat", required = true)
    private val destinationFormat: String? = null

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

        val schemaDefs =
                if ("json".equals(schemaFormat)) {
                    readJsonSchema(srcRelativePath,
                            toFileFilter(if (sourceIncludes == null) listOf("*/**/json") else sourceIncludes.split(",")))
                } else throw MojoExecutionException("""schema format "$schemaFormat" is not supported - only "json" is supported""")

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

        fun schema2TypeName(schemaName: String?, format: String?): String? {

            val matches = typeMappings!!
                    .filter { (it.from ==null || it.from.equals(schemaName)) && (it.format==null || it.format.equals(format))  }
                    .map { it.to }
            return if (matches.size==0) null else matches.first()
        }

        typeMappings!!.forEach {
            println("$logPrefix typeMapping: type:${it.from}, format:${it.format}=>${it.to}")
        }

        if ("typescript".equals(destinationFormat)) {
            writeTypescript(schemaDefs, destinationRootPath, ::schema2TypeName)
        } else if ("java".equals(destinationFormat)) {
            writeJava(schemaDefs, destinationRootPath, ::schema2TypeName)
        } else throw MojoExecutionException(""" invalid destinationFormat: "$destinationFormat"  only destination formats "java" and "typescript" are supported    """)

    }
}