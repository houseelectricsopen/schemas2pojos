package com.typedpath.schemas2pojos

import java.io.File
import java.lang.StringBuilder
import java.nio.file.Path

private fun camelCase(str: String) : String  {
    val sb = StringBuilder()
    var firstWord = true
    var wordStart=true
    for (i in 0..str.length-1) {
        val char = str.get(i)
        if (!char.isDigit() && !char.isLetter()) {
            wordStart=true;
        } else {
            sb.append(if(wordStart && !firstWord) char.toUpperCase() else char)
            wordStart = false
            firstWord = false
        }
    }
    return sb.toString()
}

open class TypeDefinition(val impliedPackage: String, val impliedShortName: String) {
    fun impliedCapitalizedShortName() : String = camelCase(impliedShortName).capitalize()

    fun startLowerCaseShortName() : String {
        val capShort = impliedCapitalizedShortName()
        return "${capShort.substring(0, 1).toLowerCase()}${capShort.substring(1)}"
    }
}



class PrimitiveTypeDefinition(impliedPackage: String, val name: String, val type: String, val description: String? = null, val pattern: String? = null) : TypeDefinition(impliedPackage, type) {
    override fun toString() = "$name, type:$type, desription:$description, pattern:$pattern "
}

class EnumTypeDefinition(impliedPackage: String, impliedShortName: String, val description: String?,
                         val enumValues: List<String>, val type: String) : TypeDefinition(impliedPackage, impliedShortName) {
    override fun toString() = "$impliedPackage.$impliedShortName, enum type:$type, desription:$description, enums:$enumValues "
}

class SchemaDefinition(val srcFile: File, val id: String,
                       val definitions: Map<String, TypeDefinition>, val innerDefinitions: Map<String, SchemaDefinition>,
                       val root: ClassSpec,
                       impliedPackage: String, impliedShortName: String) :
        TypeDefinition(impliedPackage, impliedShortName) {
    var parent: SchemaDefinition? = null
    var description: String? = null
    val reference2ParentPath = mutableMapOf<TypeDefinition, Path>()

    val idPath = id.replace("http://", "");
    fun rootIdPath() : String{
        var highest: SchemaDefinition = this
        while (highest.parent!=null) highest = highest.parent!!
        return highest.idPath
    }

    // create a type tree
// TODO make isComplex a var since it depends on a lookup of contained property types
    class PropertySpec(val optional: Boolean, val isList: Boolean, val isComplex: Boolean, val name: String,
                       val isIntrinsicSchema: Boolean
    ) {
        val camelCaseName = camelCase(name).substring(0, 1).toLowerCase() + camelCase(name).substring(1)
        var typeName: String? = null
        var format: String? = null
        var typeRef: String? = null
        var description: String? = null
        var typeDefinition: TypeDefinition? = null

        override fun toString() = "name=$name typeName=$typeName format=$format typeRef=$typeRef optional=$optional isList=$isList description=$description"

    }

    class ClassSpec(val properties: List<PropertySpec>, val fullname: String)

}
