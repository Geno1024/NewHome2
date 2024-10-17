package g.sw

import g.sw.dbmgr.Location
import java.sql.DriverManager

object DbMgr
{
    @JvmStatic
    fun main(args: Array<String>)
    {
        val connection = DriverManager.getConnection("jdbc:sqlite:nh2.db")
        connection.createStatement().use { statement ->
            statement.execute("SELECT * FROM Location")
            statement.resultSet.use { resultSet ->
                System.err.println(resultSet.next())
                println(Location().parse(resultSet))
            }
        }
    }
}
