package g.sw.dbmgr.ts

import g.sw.dbmgr.Line
import g.ufi.Cron
import java.time.LocalDateTime

data class ScheduledTask(
    var id: Int = 0,
    var name: String = "",
    var createTime: LocalDateTime = LocalDateTime.now(),
    var cron: Cron = Cron(),
) : Line
