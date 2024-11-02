package g.sw.dbmgr

enum class Action
{
    QUERY;

    companion object
    {
        fun parse(string: String) = entries.firstOrNull { it.toString().equals(string, true) }?: error("Unknown action $string")
    }
}
