package ray.eldath.sirius.util

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

object Util {
    fun reflectionToStringWithStyle(obj: Any): String =
        ToStringBuilder.reflectionToString(obj,
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