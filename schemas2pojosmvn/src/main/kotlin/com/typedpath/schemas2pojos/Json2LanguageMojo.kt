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

fun mainx(args: Array<String>) {
    val contextPath="http://justice.gov.uk/core/courts/address.json".replace("http://", "");
    val toPath  ="http://justice.gov.uk/core/courts/courtsDefinitions.json".replace("http://", "");
    // courtsDefinitions.json#/definitions/ukGovPostCode

    System.out.println(Paths.get(contextPath).relativize(Paths.get(toPath)))
    System.out.println(Paths.get(contextPath).parent.relativize(Paths.get(toPath)))

    System.out.println(Paths.get(contextPath).resolve(Paths.get(toPath)))

}


val testList = listOf("**/global/address.json",
        "**/global/courtCentre.json",
        "**/global/courtApplicationParty.json",
        "**/global/organisation.json",
        "**/global/associatedPerson.json",
        "**/global/person.json",
        "**/global/defendant.json",
        "**/global/personDefendant.json",
        "**/global/legalEntityDefendant.json",
        "**/global/judicialResult.json",
        "**/global/nextHearing.json",
        "**/global/delegatedPowers.json",
        "**/global/defendantAlias.json",
        "**/global/offence.json",
        "**/global/prosecutingAuthority.json",
        "**/global/courtsDefinitions.json")




fun main(args: Array<String>) {

    val srcRelativePath = Paths.get("./Schema-2.5")

    val schemaDefs = read(srcRelativePath,
            toFileFilter(listOf("**/global/*.json")))

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

    writeTypescript(schemaDefs, destinationRootPath, "com.coconuts")


}


@Mojo(name = "json2language", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class Json2LanguageMojo : AbstractMojo() {
    @Throws(MojoExecutionException::class, MojoFailureException::class)
    override fun execute() {
    }
}
