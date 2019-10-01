package com.typedpath.schemas2pojos

import java.nio.file.Path
import java.util.*

fun writeKotlin(schemaDefinitions: Map<String, SchemaDefinition>,
              destinationRootPath: Path, schema2TypeName: (String?, String?) -> String?) {
    writeLanguage(schemaDefinitions, destinationRootPath.resolve("kotlin"),  schema2TypeName, "kt",
            ::kotlinSource, ::kotlinEnumSource, ::kotlinFileName)
}

private fun kotlinFileName(typeDef: TypeDefinition): String {
    return typeDef.impliedCapitalizedShortName()
}

private fun kotlinEnumSource(enumDef: EnumTypeDefinition): String {
    val result: String =
            """
//defines ${enumDef.description ?: enumDef.impliedShortName}
package ${enumDef.impliedPackage}
public enum ${enumDef.impliedCapitalizedShortName()} {
${enumDef.enumValues.map{
                """    ${it}("$it")"""}.joinToString(""",
""") }

}
""".trimMargin()
    return result
}

private fun kotlinSource(schemaDef: SchemaDefinition, propertyToTypeString: (SchemaDefinition.PropertySpec) -> String,
                         parent: SchemaDefinition? =  null
): String {
    val i : Int? =null
    val classShortName = schemaDef.impliedCapitalizedShortName()
    fun kotlinFriendlyName(str: String) = if (str.equals("object")) "object_" else str
    fun optionality (p: SchemaDefinition.PropertySpec) = if (p.optional) "?" else ""
    //TODO
    fun initialiser (p: SchemaDefinition.PropertySpec) = " = null"
    fun descriptionLine(p: SchemaDefinition.PropertySpec) = if (p.description.isNullOrBlank()) "" else """//${p.description}
"""
    //collect all the imports
    val imports = schemaDef.reference2ParentPath
            //primitive types are inline
            .filter{ ! (it.key is PrimitiveTypeDefinition) }
            .map {
                """import ${it.key.impliedPackage}.${it.key.impliedCapitalizedShortName()}"""
            }
            .joinToString("""
""")
    var indentStep = "    "
    var indent = indentStep

    val propertyTypeToStringWithCardinalty  =
            {
                p:  SchemaDefinition.PropertySpec ->
                   if (p.isList) "kotlin.collections.List<${propertyToTypeString(p)}>" else propertyToTypeString(p)
            }

    return """
${if (parent==null) """
// created by KotlinWriter.kt on ${Date()}
package ${schemaDef.impliedPackage}
$imports
${if (!schemaDef.description.isNullOrBlank()) "//${schemaDef.description}" else ""}
class $classShortName {""" else
"""class $classShortName  {"""
    }
${schemaDef.root.properties.map {
"""${descriptionLine(it)}${indent}var ${kotlinFriendlyName(it.camelCaseName)} :  ${propertyTypeToStringWithCardinalty(it)}${optionality(it)} ${initialiser(it)}
"""
    }.joinToString("")}

${schemaDef.root.properties
            .filter{ it.isIntrinsicSchema && it.typeDefinition is SchemaDefinition}
            .map{it.typeDefinition as SchemaDefinition}
            .map{ """${kotlinSource(it, propertyToTypeString, schemaDef)}"""
            }.joinToString ("""""" ) }

}

${indent}""".trimIndent()
}



