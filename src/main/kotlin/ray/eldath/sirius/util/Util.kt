package ray.eldath.sirius.util

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.json.JSONObject

fun Int.toOrdinal() = this.toString() + if (this % 100 in 11..13) "th" else when (this % 10) {
    1 -> "st"
    2 -> "nd"
    3 -> "rd"
    else -> "th"
}

fun JSONObject.getStillLong(key: String) = this.getNumber(key).toLong()

object Util {
    fun reflectionToStringWithStyle(obj: Any): String =
        ToStringBuilder.reflectionToString(
            obj,
            object : ToStringStyle() {
                init {
                    this.isUseShortClassName = true
                    this.isUseIdentityHashCode = false
                    this.contentStart = "["
                    this.fieldSeparator = System.lineSeparator() + "  "
                    this.isFieldSeparatorAtStart = true
                    this.contentEnd = System.lineSeparator() + "]"
                }
            })
}