package com.typedpath.schemas2pojos

import java.nio.file.Path
import java.util.*

fun writeTypescript(schemaDefinitions: Map<String, SchemaDefinition>,
                    destinationRootPath: Path, destinationPackage: String, schema2TypescriptTypeName: (String) -> String?) {
    // check references
    schemaDefinitions.entries.forEach {
        writeTypescript(it.key, it.value, destinationRootPath, destinationPackage, schema2TypescriptTypeName)
        it.value.innerDefinitions.forEach {
            writeTypescript(it.key, it.value, destinationRootPath, destinationPackage, schema2TypescriptTypeName)
        }
    }
}

private fun dashedFileName(schemaDef: TypeDefinition): String {
    val sb = StringBuilder()
    if (schemaDef.impliedShortName != null) {
        for (c in schemaDef.impliedShortName) {
            if (Character.isUpperCase(c) && sb.length > 0) {
                sb.append("-")
            }
            sb.append(Character.toLowerCase(c))
        }
    }
    return sb.toString()
}

//TODO use or lose destinationPackage, or change to package remapper function
private fun writeTypescript(id: String, schemaDef: SchemaDefinition, destinationRootPath: Path, destinationPackage: String,
                            schemaTypeName2TypescriptType: (String) -> String?
                            ) {
//    val destinationPackage = typescriptPackage(destinationRootPath, id)
    println("$id ${schemaDef.impliedPackage} ${schemaDef.impliedShortName}   <= ${schemaDef.srcFile}")
    var destinationParentPath = destinationRootPath;
    destinationParentPath = destinationParentPath.resolve("ts")
    schemaDef.impliedPackage.split(".").forEach {
        destinationParentPath = destinationParentPath.resolve(it)
    }
    val destinationPath = destinationParentPath.resolve("${dashedFileName(schemaDef)}.ts")
    val file = destinationPath.toFile()
    file.parentFile.mkdirs()

    fun propertyToTypeString(property: SchemaDefinition.PropertySpec): String {
        var result =
                if (property.typeDefinition != null && property.typeDefinition is PrimitiveTypeDefinition) property.typeDefinition!!.impliedShortName
                else if (property.typeDefinition != null && property.typeDefinition is SchemaDefinition) property.typeDefinition!!.impliedCapitalizedShortName()
                else if (property.typeDefinition != null && property.typeDefinition is EnumTypeDefinition) "string"
                else if (property.typeName==null) throw RuntimeException("property has null typeName specified for ${schemaDef.srcFile}.${property}")
                else {
                    val theType = schemaTypeName2TypescriptType(property.typeName!!)
                    if (theType==null) {
                        throw RuntimeException("no mapping for type \"${property.typeName}\" in  ${schemaDef.srcFile}.${property}")
                    } else theType
                }
        return result!!
    }
    destinationPath.toFile().writeText(typescriptSource(schemaDef, ::propertyToTypeString))

    //write all the enums

    schemaDef.definitions
            .filter { it.value is EnumTypeDefinition }
            .map { it.value as EnumTypeDefinition }
            .forEach {
                val enumDestinationPath = destinationParentPath.resolve("${dashedFileName(it)}.ts")
                enumDestinationPath.toFile().writeText(typescriptSource(it))
            }
}

private fun typescriptSource(enumDef: EnumTypeDefinition): String {
    val result: String =
            """
//defines ${enumDef.description ?: enumDef.impliedShortName}
export enum ${enumDef.impliedShortName} {
${enumDef.enumValues.map{
"""    $it = "$it""""}.joinToString(""",
""") }
}
""".trimMargin()
    return result
}

private fun typescriptSource(schemaDef: SchemaDefinition, propertyToTypeString: (SchemaDefinition.PropertySpec) -> String
                             ): String {
    val classShortName = schemaDef.impliedCapitalizedShortName()
    fun optionalDesignator(p: SchemaDefinition.PropertySpec) = if (p.optional) "?" else ""
    fun oneToManyDesignator(p: SchemaDefinition.PropertySpec) = if (p.isList) "[]" else ""
    fun descriptionLine(p: SchemaDefinition.PropertySpec) = if (p.description.isNullOrBlank()) "" else """//${p.description}
"""
    //collect all the imports
    val imports = schemaDef.reference2ParentPath
            // enums are currently mapped to string
            .filter{ ! (it.key is EnumTypeDefinition) }
            //primitive types are inline
            .filter{ ! (it.key is PrimitiveTypeDefinition) }
            .map {
                val strPath = if (it.value.parent == null) "." else it.value.parent.toString()
                """import { ${it.key.impliedCapitalizedShortName()} } from '${strPath}/${dashedFileName(it.key)}';"""
            }
            .joinToString("""
""")
    return """
// created by LanguageWriter.kt on ${Date()}
$imports
${if (!schemaDef.description.isNullOrBlank()) "//${schemaDef.description}" else ""}
export interface $classShortName {
${schemaDef.root.properties.map {
        """${descriptionLine(it)}    ${it.name}${optionalDesignator(it)} : ${propertyToTypeString(it)}${oneToManyDesignator(it)};
"""
    }.joinToString("")}
}
    """.trimIndent()
}



