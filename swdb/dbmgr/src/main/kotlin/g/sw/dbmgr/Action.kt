package g.sw.dbmgr

enum class Action
{
    QUERY,
    INSERT;

    companion object
    {
        fun parse(string: String) = entries.firstOrNull { it.toString().equals(string, true) }?: error("Unknown action $string")
    }
}
