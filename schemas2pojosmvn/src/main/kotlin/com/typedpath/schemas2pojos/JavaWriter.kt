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
    fun optionalDesignator(p: SchemaDefinition.PropertySpec) = if (p.optional) "?" else ""
    fun oneToManyDesignator(p: SchemaDefinition.PropertySpec) = if (p.isList) "[]" else ""
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
"""${descriptionLine(it)}    private ${propertyToTypeString(it)} ${it.name};
    public void set${it.name.capitalize()}(final ${propertyToTypeString(it)} value) {
        ${it.name} = value;
    }
    public ${propertyToTypeString(it)} get${it.name.capitalize()}() {
        return ${it.name};
    }
"""
    }.joinToString("")}


    public $classShortName (${schemaDef.root.properties.map{"""${propertyToTypeString(it)} ${it.name}"""}.joinToString (", ")}) {
${schemaDef.root.properties.map{"""        this.${it.name} = ${it.name};"""}.joinToString ("""
""")}
    }

${schemaDef.root.properties
            .filter{ it.isIntrinsicSchema && it.typeDefinition is SchemaDefinition}
            .map{it.typeDefinition as SchemaDefinition}
            .map{ """${javaSource(it, propertyToTypeString, schemaDef)}"""
            }.joinToString ("""""" ) }

}
    """.trimIndent()
}
