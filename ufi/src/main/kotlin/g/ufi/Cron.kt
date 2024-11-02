package g.ufi

import g.ufi.utils.readStringUntil
import java.io.InputStream
import java.io.OutputStream

data class Cron(
    var minute: String = "*",
    var hour: String = "*",
    var day: String = "*",
    var month: String = "*",
    var weekDay: String = "*"
) : UFI
{
    companion object
    {
        fun read(string: String) = with (Regex("(\\S+) (\\S+) (\\S+) (\\S+) (\\S+)").matchEntire(string)!!) {
            Cron(
                groupValues[1],
                groupValues[2],
                groupValues[3],
                groupValues[4],
                groupValues[5]
            )
        }
    }

    override fun write(output: OutputStream) = output.write(toString().toByteArray())

    override fun read(input: InputStream): Cron = Cron(
        input.readStringUntil(' '),
        input.readStringUntil(' '),
        input.readStringUntil(' '),
        input.readStringUntil(' '),
        input.readStringUntil(' ')
    )

    override fun toString(): String = "$minute $hour $day $month $weekDay"
}
