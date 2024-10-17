package g.sw.dbmgr

import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

interface Line
{
    fun getSQLiteType(type: Class<out Any>) = when (type)
    {
        Boolean::class.java -> Integer::class.java
        Int::class.java -> Integer::class.java
        else -> type
    }

    fun cast(value: Any, type: Class<out Any>): Any = when (type)
    {
        Boolean::class.java -> value as Integer != 0
        Int::class.java -> value as Integer
        else -> value
    }

    fun parse(line: ResultSet): Line? = this.apply{
        this::class.memberProperties.map {
            it as KMutableProperty1<Line, Any?>
            it.isAccessible = true
            val typ = (it.returnType.classifier as KClass<*>).java
            val value = line.getObject(it.name.replace(Regex("[A-Z]")) { "_${it.value.lowercase()}" }, getSQLiteType(typ))
            it.set(this, cast(value, typ))
        }
    }
}
