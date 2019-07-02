package com.typedpath.schemas2pojos

import jdk.nashorn.api.scripting.ScriptObjectMirror
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.script.ScriptEngineManager

// TODO checks at this level : duplicate ids in files
// TODO constant for $ref

open class TypeDefinition(val impliedShortName: String) {
    fun impliedCapitalizedShortName() = impliedShortName.substring(0, 1).toUpperCase() + impliedShortName.substring(1)
}

class PrimitiveTypeDefinition(val name: String, val type: String, val description: String? = null, val pattern: String? = null) : TypeDefinition(type) {
    override fun toString() = "$name, type:$type, desription:$description, pattern:$pattern "
}

class EnumTypeDefinition(val impliedPackage: String, impliedShortName: String, val description: String?,
                         val enumValues: List<String>, val type: String) : TypeDefinition(impliedShortName) {
    override fun toString() = "$impliedPackage.$impliedShortName, enum type:$type, desription:$description, enums:$enumValues "
}

class SchemaDefinition(val srcFile: File, val id: String,
                       val definitions: Map<String, TypeDefinition>, val root: ClassSpec,
                       val impliedPackage: String, impliedShortName: String) :
        TypeDefinition(impliedShortName) {
    var description: String? = null
    val reference2ParentPath = mutableMapOf<SchemaDefinition, Path>()

    val idPath = id.replace("http://", "");

    // create a type tree
// TODO make isComplex a var since it depends on a lookup of contained property types
    class PropertySpec(val optional: Boolean, val isList: Boolean, val isComplex: Boolean, val name: String
    ) {
        var typeName: String? = null
        var typeRef: String? = null
        var description: String? = null
        // to do make a type superclass
        var typeDefinition: TypeDefinition? = null

        override fun toString() = "name=$name typeName=$typeName typeRef=$typeRef optional=$optional isList=$isList description=$description"

    }

    class ClassSpec(val properties: List<PropertySpec>, val fullname: String)

}

/**
 * read a schema from json into memory, resolving references
 * kotlin or java would be better choices for schema definition because they make this step unnecessary
 * and make it impossible to define unresolvable references or ambiguous types
 */
fun read(rootPath: Path, filter: (File) -> Boolean):
        Map<String, SchemaDefinition> {
    val jsonFiles = mutableListOf<File>()

    rootPath.toFile().walkTopDown()
            .forEach { f ->
                if (filter(f)) {
                    jsonFiles.add(f)
                }
            }
    val schemaDefs = mutableMapOf<String, SchemaDefinition>()
    jsonFiles.forEach {
        readFlat(it, schemaDefs)
    }
    resolveReferences(schemaDefs)
    //resolve references here
    return schemaDefs
}

@Throws(Exception::class)
fun stringToJson(strJson: String): ScriptObjectMirror {
    val json = "var result = $strJson; result;"
    val factory = ScriptEngineManager()
    val engine = factory.getEngineByName("JavaScript")
    return engine.eval(json) as ScriptObjectMirror
}

private fun readProperty(name: String, jsProperty: ScriptObjectMirror, isOptional: Boolean): SchemaDefinition.PropertySpec {
    var typeNameAttr = jsProperty.get("type") as String?
    var typeRefAttr: String? = null
    var isList = false
    var isComplex = false
    //TODO read minItems
    if ("array".equals(typeNameAttr)) {
        isList = true
        //TODO read items attribute
        val jsItems = jsProperty.get("items") as ScriptObjectMirror
        if (jsItems == null) {
            throw RuntimeException("failed to readFlat array property $name no items property")
        }
        typeRefAttr = jsItems.get("\$ref") as String?

    } else {
        if ("object".equals(typeNameAttr)) isComplex = true
        typeRefAttr = jsProperty.get("\$ref") as String?
        // TODO if there is no ref should try to read contained definition
        // hence isComplex need to be a var
    }

    return SchemaDefinition.PropertySpec(isOptional, isList, isComplex, name).apply {
        description = jsProperty.get("description") as String?
        typeRef = typeRefAttr
        typeName = typeNameAttr
    }
}

private fun readProperties(jsProperties: ScriptObjectMirror, jsRequired: ScriptObjectMirror?): List<SchemaDefinition.PropertySpec> {

    return jsProperties.keys.map { name ->
        readProperty(name, jsProperties.get(name) as ScriptObjectMirror,
                jsRequired == null || !jsRequired.containsValue(name))
    }
}

private fun readTypeDefinition(impliedPackageName: String, name: String, jsDefinition: ScriptObjectMirror): TypeDefinition {
    if ((!jsDefinition.containsKey("type") || jsDefinition.get("type") == null)) {
        throw RuntimeException("type $name has no type")
    }
    var type = jsDefinition.get("type") as String
    val isEnum = jsDefinition.containsKey("enum")
    val description = jsDefinition.get("description") as String?

    return if (isEnum) {
        val enumValues = (jsDefinition.get("enum") as ScriptObjectMirror).values.map { it as String }.toList()
        EnumTypeDefinition(impliedPackageName, name, description, enumValues, type)
    } else {
        return PrimitiveTypeDefinition(name, type, description,
                jsDefinition.get("pattern") as String?)
    }
    //TODO read object definitions too


}

private fun readPrimitiveDefinitions(impliedPackageName: String, jsDefinitions: ScriptObjectMirror): Map<String, TypeDefinition> {

    val result = mutableMapOf<String, TypeDefinition>()
    jsDefinitions.entries.map { Pair(it.key, it.value as ScriptObjectMirror) }
            .filter { !"object".equals(it.second.get("type")) }
            .forEach { result.put(it.first, readTypeDefinition(impliedPackageName, it.first, it.second)) }
    return result
}

private fun jsonToSchemaDef(jsonFile: File): SchemaDefinition {
    val text = jsonFile.readText()
    try {
        val jsonSchema: ScriptObjectMirror = stringToJson(text)
        val id = jsonSchema.get("id").toString()
        val name = id.replace("http://", "").replace(".json", "")
        val lastSlashIndex = name.lastIndexOf('/')
        val impliedShortName = if (lastSlashIndex == -1) name else name.substring(lastSlashIndex + 1)
        val packagePart = if (lastSlashIndex == -1) "" else name.substring(0, lastSlashIndex)
        val impliedPackage = packagePart.split("/").map { part ->
            if (part.contains('.')) {
                part.split('.').reversed().joinToString(".")
            } else part
        }.joinToString(".")

        var properties = listOf<SchemaDefinition.PropertySpec>()
        if (jsonSchema.containsKey("properties")) {
            properties = readProperties(jsonSchema.get("properties") as ScriptObjectMirror, jsonSchema.get("required") as ScriptObjectMirror?)
        }
        var primitiveDefinitions = mapOf<String, TypeDefinition>()
        if (jsonSchema.containsKey("definitions")) {
            primitiveDefinitions = readPrimitiveDefinitions(impliedPackage, jsonSchema.get("definitions") as ScriptObjectMirror)
            //TODO innerClasses = readInnderClasses(jsonSchema.get("definitions") as ScriptObjectMirror)
        }

        val classSpec = SchemaDefinition.ClassSpec(properties, name)
        val result = SchemaDefinition(jsonFile, id, primitiveDefinitions, classSpec, impliedPackage, impliedShortName)
        if (jsonSchema.containsKey("description")) {
            result.description = jsonSchema.get("description").toString()
        }
        return result
    } catch (ex: Exception) {
        throw RuntimeException("failed to readFlat file ${jsonFile.absolutePath}", ex)
    }

}

private fun readFlat(jsonFile: File, schemaDefs: MutableMap<String, SchemaDefinition>) {
    //val text = jsonFile.readText()
    println("**** ${jsonFile.name} / ${jsonFile.absolutePath}")

    val schemaDef = jsonToSchemaDef(jsonFile)
    if (schemaDefs.containsKey(schemaDef.id)) {
        val existingDef: SchemaDefinition = schemaDefs.get(schemaDef.id)!!
        throw RuntimeException("duplicate definitions for ${schemaDef.id} in files ${schemaDef.srcFile.path} and ${existingDef.srcFile.path}")
    }
    schemaDefs.put(schemaDef.id, schemaDef)

    //println("starts with: " + text.substring(0, 100))
    //val (simpleClassname, src) = jsonSchemaString2Kotlin(text, strPackage)
}

private fun findByRelativeId(schemaDefs: MutableMap<String, SchemaDefinition>, context: SchemaDefinition,
                             relativeTypeRef: String): SchemaDefinition? {
    schemaDefs.forEach {
        val relPath = Paths.get(context.idPath).parent.relativize(Paths.get(it.value.idPath))
        if (relPath.toString().equals(relativeTypeRef)) {
            return it.value
        }
    }
    return null
}

//TODO make type super class
private fun resolveReference(schemaDefs: MutableMap<String, SchemaDefinition>, context: SchemaDefinition, typeRef: String): TypeDefinition {
    val hashIndex = typeRef.indexOf('#')
    val rootName = if (-1 != hashIndex) {
        typeRef.substring(0, hashIndex).trim()
    } else {
        typeRef.trim()
    }

    val referredSchema: SchemaDefinition?
    if (schemaDefs.containsKey(rootName)) {
        referredSchema = schemaDefs.get(rootName)
    } else {
        referredSchema = findByRelativeId(schemaDefs, context, rootName)
    }
    if (referredSchema == null) {
        throw RuntimeException("cont resolve $rootName ( $typeRef) in ${context.srcFile}")
    }

    context.reference2ParentPath.put(referredSchema, Paths.get(context.idPath).parent.relativize(Paths.get(referredSchema.idPath)))

    val result: TypeDefinition
    if (hashIndex != -1 && !typeRef.endsWith("#")) {
        val defName = typeRef.substring(hashIndex).replace("#/definitions/", "")
        if (!referredSchema!!.definitions.containsKey(defName)) {

            throw RuntimeException("cant find primitive def $defName in $rootName full ref $typeRef in ${context.srcFile}")
        }
        result = referredSchema!!.definitions.get(defName)!!
    } else {
        result = referredSchema
    }
    return result
}

private fun resolveReferences(schemaDefs: MutableMap<String, SchemaDefinition>) {
// for each property with a ref look up a primitive def
    schemaDefs.forEach {
        val schemaDef = it.value
        it.value.root.properties.forEach {
            if (it.typeRef != null) {
                println("trying to resolve ${schemaDef.root.fullname}.${it.name}::${it.typeRef}")
                val typeDefinition = resolveReference(schemaDefs, schemaDef, it.typeRef!!)
                it.typeDefinition = typeDefinition
            }
        }
    }
}