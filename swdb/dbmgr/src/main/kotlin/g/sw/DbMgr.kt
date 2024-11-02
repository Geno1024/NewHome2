package g.sw

import g.sw.dbmgr.Action
import g.sw.dbmgr.ConditionClause
import g.sw.dbmgr.ts.Location
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
        var condition = query["condition"]
            ?.split(",")
            ?.map {
                with (it.split(":", limit = 3)) {
                    Triple(this[0], ConditionClause.parse(this[1]), this[2])
                }
            }
            ?:emptyList()

        val clazz = tableToJavaClass(table) ?: run {
            println("!")
            exitProcess(1)
        }
        row = filterRow(clazz, row)
        condition = filterCondition(clazz, condition)

        val targetString: String

        when (action)
        {
            Action.QUERY ->
            {
                targetString = "SELECT " +
                    (row.takeIf(List<*>::isNotEmpty)?.joinToString(", ") ?: "*") +
                    " FROM " +
                    clazz.thisClassName().substringAfter("${DbMgr::class.java.packageName.replace('.', '/')}/dbmgr/ts/") +
                    (condition.takeIf(List<*>::isNotEmpty)?.joinToString(" AND ", prefix = " WHERE ") { "${it.first}${it.second.tuner(it.third)}" } ?: "")
            }
        }

        println(targetString)

        connection.createStatement().use { statement ->
            when (action)
            {
                Action.QUERY ->
                {

                }
            }

            statement.execute(targetString)
            statement.resultSet.use { resultSet ->
                while (resultSet.next())
                {
                    println(Location().parse(resultSet))
                }
            }
        }
    }
}
