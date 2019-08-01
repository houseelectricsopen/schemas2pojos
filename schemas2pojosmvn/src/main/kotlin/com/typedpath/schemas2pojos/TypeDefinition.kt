package com.typedpath.schemas2pojos

import java.io.File
import java.lang.StringBuilder
import java.nio.file.Path

open class TypeDefinition(val impliedShortName: String) {
    fun impliedCapitalizedShortName() : String{
        //camelcase it
        val sb = StringBuilder()
        var wordStart=true
        for (i in 0..impliedShortName.length-1) {
            val char = impliedShortName.get(i)
            if (!char.isDigit() && !char.isLetter()) {
                wordStart=true;
            } else {
                sb.append(if(wordStart) char.toUpperCase() else char)
                wordStart = false
            }
        }
        return sb.toString()
    }
}

class PrimitiveTypeDefinition(val name: String, val type: String, val description: String? = null, val pattern: String? = null) : TypeDefinition(type) {
    override fun toString() = "$name, type:$type, desription:$description, pattern:$pattern "
}

class EnumTypeDefinition(val impliedPackage: String, impliedShortName: String, val description: String?,
                         val enumValues: List<String>, val type: String) : TypeDefinition(impliedShortName) {
    override fun toString() = "$impliedPackage.$impliedShortName, enum type:$type, desription:$description, enums:$enumValues "
}

class SchemaDefinition(val srcFile: File, val id: String,
                       val definitions: Map<String, TypeDefinition>, val innerDefinitions: Map<String, SchemaDefinition>,
                       val root: ClassSpec,
                       val impliedPackage: String, impliedShortName: String) :
        TypeDefinition(impliedShortName) {
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
        var typeName: String? = null
        var format: String? = null
        var typeRef: String? = null
        var description: String? = null
        var typeDefinition: TypeDefinition? = null

        override fun toString() = "name=$name typeName=$typeName format=$format typeRef=$typeRef optional=$optional isList=$isList description=$description"

    }

    class ClassSpec(val properties: List<PropertySpec>, val fullname: String)

}
