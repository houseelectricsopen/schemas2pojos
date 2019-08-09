package com.typedpath.schemas2pojos

import java.nio.file.Path
import java.util.*

fun writeTypescript(schemaDefinitions: Map<String, SchemaDefinition>,
                    destinationRootPath: Path, schema2TyLpescriptTypeName: (String?, String?) -> String?) {
    writeLanguage(schemaDefinitions, destinationRootPath,  schema2TyLpescriptTypeName, "ts", ::typescriptSource, ::typescriptEnumSource, ::dashedFileName)
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


private fun typescriptEnumSource(enumDef: EnumTypeDefinition): String {
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

private fun typescriptSource(schemaDef: SchemaDefinition, propertyToTypeString: (SchemaDefinition.PropertySpec) -> String,
                             parent: SchemaDefinition?
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
                """import { ${it.key.impliedCapitalizedShortName()} } from '${strPath.replace('\\', '/')}/${dashedFileName(it.key)}';"""
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

${schemaDef.root.properties
            .filter{ it.isIntrinsicSchema && it.typeDefinition is SchemaDefinition}
            .map{it.typeDefinition as SchemaDefinition}
            .map{ """${typescriptSource(it, propertyToTypeString, null)}"""
            }.joinToString ("""""" ) }

    """.trimIndent()
}
