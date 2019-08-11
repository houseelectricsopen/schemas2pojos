package com.typedpath.schemas2pojos

import java.nio.file.Path
import java.util.*

fun writeJava(schemaDefinitions: Map<String, SchemaDefinition>,
                    destinationRootPath: Path, schema2TyLpescriptTypeName: (String?, String?) -> String?) {
    writeLanguage(schemaDefinitions, destinationRootPath,  schema2TyLpescriptTypeName, "java",
            ::javaSource, ::javaEnumSource, ::javaFileName)
}


private fun javaFileName(typeDef: TypeDefinition): String {
    return typeDef.impliedCapitalizedShortName()
}


private fun javaEnumSource(enumDef: EnumTypeDefinition): String {
    val result: String =
            """
//defines ${enumDef.description ?: enumDef.impliedShortName}
package ${enumDef.impliedPackage};
public enum ${enumDef.impliedCapitalizedShortName()} {
${enumDef.enumValues.map{
                """    ${it}("$it")"""}.joinToString(""",
""") };

  private final String value;

  ${enumDef.impliedCapitalizedShortName()}(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

}
""".trimMargin()
    return result
}

private fun javaSource(schemaDef: SchemaDefinition, propertyToTypeString: (SchemaDefinition.PropertySpec) -> String ,
                       parent: SchemaDefinition? =  null
): String {
    val classShortName = schemaDef.impliedCapitalizedShortName()
    fun descriptionLine(p: SchemaDefinition.PropertySpec) = if (p.description.isNullOrBlank()) "" else """//${p.description}
"""
    //collect all the imports
    val imports = schemaDef.reference2ParentPath
            //primitive types are inline
            .filter{ ! (it.key is PrimitiveTypeDefinition) }
            .map {
                """import ${it.key.impliedPackage}.${it.key.impliedCapitalizedShortName()};"""
            }
            .joinToString("""
""")
    var indentStep = "    "
    var indent = indentStep
    return """
${if (parent==null) """
// created by JavaWriter.kt on ${Date()}
package ${schemaDef.impliedPackage};
$imports
${if (!schemaDef.description.isNullOrBlank()) "//${schemaDef.description}" else ""}
public class $classShortName  implements java.io.Serializable {""" else
"""public static class $classShortName  implements java.io.Serializable {"""
    }

${schemaDef.root.properties.map {
"""${descriptionLine(it)}${indent}private ${propertyToTypeString(it)} ${it.name};
${indent}public void set${it.name.capitalize()}(final ${propertyToTypeString(it)} value) {
${indent}${indentStep}${it.name} = value;
${indent}}
${indent}public ${propertyToTypeString(it)} get${it.name.capitalize()}() {
${indent}${indentStep}return ${it.name};
${indent}}
"""
    }.joinToString("")}

${indent}public $classShortName (${schemaDef.root.properties.map{"""${propertyToTypeString(it)} ${it.name}"""}.joinToString (", ")}) {
${schemaDef.root.properties.map{"""${indent}${indentStep}this.${it.name} = ${it.name};"""}.joinToString ("""
""")}
${indent}}

${schemaDef.root.properties
            .filter{ it.isIntrinsicSchema && it.typeDefinition is SchemaDefinition}
            .map{it.typeDefinition as SchemaDefinition}
            .map{ """${javaSource(it, propertyToTypeString, schemaDef)}"""
            }.joinToString ("""""" ) }

${builderSource(schemaDef, propertyToTypeString, indent, indentStep)}

}



${indent}""".trimIndent()
}

private fun builderSource(schemaDef: SchemaDefinition, propertyToTypeString: (SchemaDefinition.PropertySpec) -> String,
                          indent: String, indentStep: String) : String{
return """
${indent}public static Builder ${schemaDef.startLowerCaseShortName()}() {
${indent}${indentStep}return new ${schemaDef.impliedCapitalizedShortName()}.Builder();
${indent}}

${indent}public static class Builder {
${schemaDef.root.properties.map {
"""${indent}private ${propertyToTypeString(it)} ${it.name};
${indent}public void with${it.name.capitalize()}(final ${propertyToTypeString(it)} value) {
${indent}${indentStep}${it.name} = value;
${indent}}
"""
    }.joinToString("")}

${indent}public ${schemaDef.impliedCapitalizedShortName()} build() {
${indent}${indentStep}return new ${schemaDef.impliedCapitalizedShortName()}(${schemaDef.root.properties.map {
    """${it.name}"""
}.joinToString(", ")});
${indent}}
}



${indent}""".trimIndent()

}


