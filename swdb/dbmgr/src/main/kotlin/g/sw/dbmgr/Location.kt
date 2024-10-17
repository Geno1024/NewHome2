package g.sw.dbmgr

import java.time.LocalDateTime

data class Location(
    var id: Int = 0,
    var name: String = "",
    var level: Int = 0,
    var parentId: Int = 0,
    var createTime: LocalDateTime? = null,
    var updateTime: LocalDateTime? = null,
    var deleted: Boolean = false
) : Line
