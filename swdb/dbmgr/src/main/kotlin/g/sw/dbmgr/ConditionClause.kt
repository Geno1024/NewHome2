package g.sw.dbmgr

enum class ConditionClause(val tuner: (String) -> String)
{
    EQ({ " = $it" }),
    GT({ " > $it" }),
    GE({ " >= $it" }),
    LT({ " < $it" }),
    LE({ " <= $it" }),
    NE({ " != $it" }),
    EQS({ " = '$it'" }),
    LIKE({ " LIKE '$it'" });

    companion object
    {
        fun parse(string: String) = entries.firstOrNull { clause -> clause.toString().equals(string, true) }?:error("Unknown Condition Clause")
    }
}
