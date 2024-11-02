package g.sw.dbmgr

import g.ufi.Cron
import java.sql.ResultSet
import java.sql.SQLException
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
        Cron::class.java -> String::class.java
        else -> type
    }

    fun cast(value: Any, type: Class<out Any>): Any = when (type)
    {
        Boolean::class.java -> value as Int != 0
        Cron::class.java -> Cron.read(value as String)
        else -> value
    }

    fun parse(line: ResultSet): Line? = this.apply {
        this::class.memberProperties.map { prop ->
            prop as KMutableProperty1<Line, Any?>
            prop.isAccessible = true
            val typ = (prop.returnType.classifier as KClass<*>).java
            val col = prop.name.replace(Regex("[A-Z]")) { cased -> "_${cased.value.lowercase()}" }
            try
            {
                val value = line.getObject(col, getSQLiteType(typ))
                prop.set(this, cast(value, typ))
            }
            catch (_: SQLException)
            {

            }
        }
    }
}
