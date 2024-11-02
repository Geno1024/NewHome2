package g.sw

import g.sw.dbmgr.Action
import g.sw.dbmgr.ConditionClause
import g.sw.dbmgr.Line
import g.ufi.JavaClass
import java.io.File
import java.sql.DriverManager
import kotlin.system.exitProcess

object DbMgr
{
    fun tableToJavaClass(table: String): JavaClass?
    {
        val l = DbMgr::class.java.protectionDomain.codeSource.location
        return when (l.protocol.lowercase())
        {
            "file" ->
            {
                File("${l.file}${DbMgr::class.java.packageName.replace('.', '/')}/dbmgr/ts/$table.class")
                    .takeIf { it.exists() }
                    ?.inputStream()
                    ?.use {
                        JavaClass.read(it)
                    }
            }
            else -> null
        }
    }

    fun filterRow(table: JavaClass, rows: List<String>): List<String>
    {
        val allRows = table.fields.map {
            (table.constantPool[it.nameIndex.toInt()] as JavaClass.Constant.Utf8).toString()
        }
        return rows.filter(allRows::contains)
    }

    fun filterCondition(table: JavaClass, condition: List<Triple<String, ConditionClause, String>>): List<Triple<String, ConditionClause, String>>
    {
        if (condition.isEmpty())
        {
            return emptyList()
        }
        val allRows = table.fields.map {
            (table.constantPool[it.nameIndex.toInt()] as JavaClass.Constant.Utf8).toString()
        }
        return condition.filter {
            allRows.contains(it.first)
        }
    }


    @JvmStatic
    fun main(args: Array<String>)
    {
        val connection = DriverManager.getConnection("jdbc:sqlite:nh2.db")
        val query = args.associate { with (it.split("=", limit = 2)) { this[0] to (getOrNull(1)?:"true") } }

        val action = Action.parse(query["action"]?:"query")
        val table = query["table"]
            ?: run {
                System.err.println("No table name")
                exitProcess(1)
            }
        var row = query["row"]
            ?.split(",")
            ?:listOf("*")
        var conditions = query["condition"]
            ?.split(",")
            ?.map {
                with (it.split(":", limit = 3)) {
                    when (size)
                    {
                        3 ->
                        {
                            Triple(this[0], ConditionClause.parse(this[1]), this[2])
                        }
                        2 ->
                        {
                            Triple(this[0], ConditionClause.ASSIGN, this[1])
                        }
                        else ->
                        {
                            Triple(this[0], ConditionClause.NONE, "")
                        }
                    }
                }
            }
            ?:emptyList()

        val clazz = tableToJavaClass(table) ?: run {
            println("!")
            exitProcess(1)
        }
        row = filterRow(clazz, row)
        conditions = filterCondition(clazz, conditions)

        val targetString = when (action)
        {
            Action.QUERY ->
            {
                "SELECT " +
                    (row.takeIf(List<*>::isNotEmpty)?.joinToString(", ") ?: "*") +
                    " FROM " +
                    clazz.thisClassName().substringAfter("${DbMgr::class.java.packageName.replace('.', '/')}/dbmgr/ts/") +
                    (conditions.takeIf(List<*>::isNotEmpty)?.joinToString(" AND ", prefix = " WHERE ") { "${it.first}${it.second.tuner(it.third)}" } ?: "")
            }
            Action.INSERT ->
            {
                "INSERT INTO " +
                    clazz.thisClassName().substringAfter("${DbMgr::class.java.packageName.replace('.', '/')}/dbmgr/ts/") +
                    conditions.joinToString(separator = ", ", prefix = "(", postfix = ")", transform = Triple<String, ConditionClause, String>::first) +
                    " VALUES " +
                    conditions.joinToString(separator = ", ", prefix = "(", postfix = ")") { condition ->
                        when ((clazz.constantPool[clazz.fields.find { field ->
                            (clazz.constantPool[field.nameIndex.toInt()] as JavaClass.Constant.Utf8).toString() == condition.first
                        }?.descriptorIndex?.toInt()?:clazz.thisClass.toInt()] as JavaClass.Constant.Utf8).toString())
                        {
                            "Ljava/lang/String;" -> "'${condition.third}'"
                            else -> condition.third
                        }
                    }
            }
        }

        println(targetString)

        connection.createStatement().use { statement ->
            statement.execute(targetString)
            val line = Class.forName(clazz.thisClassName().replace('/', '.')).getConstructor().newInstance() as Line
            statement.resultSet.use { resultSet ->
                while (resultSet?.next() == true)
                {
                    println(line.parse(resultSet))
                }
            }
        }
    }
}
