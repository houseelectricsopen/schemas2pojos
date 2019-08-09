package com.typedpath.schemas2pojos

import java.nio.file.Path


fun writeLanguage(schemaDefinitions: Map<String, SchemaDefinition>,
                  destinationRootPath: Path, schema2TypescriptTypeName: (String?, String?) -> String?,
                  fileExtension: String,
                  convertToClassSource : (SchemaDefinition, (SchemaDefinition.PropertySpec) -> String, parent: SchemaDefinition?)->String,
                  convertToEnumSource : (EnumTypeDefinition)->String,
                  toShortFileName: (TypeDefinition) -> String) {
    // check references
    schemaDefinitions.entries.forEach {
        writeLanguage(it.key, it.value, destinationRootPath,  schema2TypescriptTypeName, fileExtension, convertToClassSource, convertToEnumSource, toShortFileName)
        it.value.innerDefinitions.forEach {
            writeLanguage(it.key, it.value, destinationRootPath,  schema2TypescriptTypeName, fileExtension, convertToClassSource, convertToEnumSource, toShortFileName)
        }
    }
}

private fun writeLanguage(id: String, schemaDef: SchemaDefinition, destinationRootPath: Path,
                          schemaTypeName2TypescriptType: (String?, String?) -> String?, fileExtension: String,
                          convertToClassSource : (SchemaDefinition, (SchemaDefinition.PropertySpec) -> String, parent: SchemaDefinition?)->String,
                          convertToEnumSource : (EnumTypeDefinition)->String,
                          toShortFileName: (TypeDefinition) -> String

                            ) {
    println("$id ${schemaDef.impliedPackage} ${schemaDef.impliedShortName}   <= ${schemaDef.srcFile}")
    var destinationParentPath = destinationRootPath;
    destinationParentPath = destinationParentPath.resolve(fileExtension)
    schemaDef.impliedPackage.split(".").forEach {
        destinationParentPath = destinationParentPath.resolve(it)
    }
    val destinationPath = destinationParentPath.resolve("${toShortFileName(schemaDef)}.$fileExtension")
    val file = destinationPath.toFile()
    file.parentFile.mkdirs()

    fun propertyToTypeString(property: SchemaDefinition.PropertySpec): String {
        var result =
                if (property.typeDefinition != null && property.typeDefinition is PrimitiveTypeDefinition) schemaTypeName2TypescriptType(property.typeDefinition!!.impliedShortName, property.format)
                else if (property.typeDefinition != null && property.typeDefinition is SchemaDefinition) property.typeDefinition!!.impliedCapitalizedShortName()
                else if (property.typeDefinition != null && property.typeDefinition is EnumTypeDefinition) "string"
                //else if (property.typeName==null) throw RuntimeException("property has null typeName specified for ${schemaDef.srcFile}.${property}")
                else {
                    val theType = schemaTypeName2TypescriptType(property.typeName, property.format)
                    if (theType==null) {
                        throw RuntimeException("no mapping for type \"${property.typeName}\"  typeDef:${property.typeDefinition}  in  ${schemaDef.srcFile}.${property}")
                    } else theType
                }
        return result!!
    }
    //TODO make typescriptEnumSource a parameter
    destinationPath.toFile().writeText(convertToClassSource(schemaDef, ::propertyToTypeString, null))

    //write all the enums

    schemaDef.definitions
            .filter { it.value is EnumTypeDefinition }
            .map { it.value as EnumTypeDefinition }
            .forEach {
                val enumDestinationPath = destinationParentPath.resolve("${toShortFileName(it)}.$fileExtension")
                enumDestinationPath.toFile().writeText(convertToEnumSource(it))
            }
}




