package g.sw.dbmgr

import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.system.exitProcess

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

    fun parse(line: ResultSet): Line? = this.apply {
        this::class.memberProperties.map {
            it as  KMutableProperty1<Line, Any?>
            it.isAccessible = true
            val typ = (it.returnType.classifier as KClass<*>).java
            val col = it.name.replace(Regex("[A-Z]")) { "_${it.value.lowercase()}" }
            try
            {
                val value = line.getObject(col, getSQLiteType(typ))
                it.set(this, cast(value, typ))
            }
            catch (_: SQLException)
            {

            }
        }
    }
}
