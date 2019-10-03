package ray.eldath.sirius.util

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import ray.eldath.sirius.util.StringCase.CAMEL_CASE
import ray.eldath.sirius.util.StringCase.PASCAL_CASE

/**
 * If the string contains one non-ASCII char, the test about the case will fail directly.
 *
 * @property PASCAL_CASE all chars are letters, and are pascal case, like "PascalCase", "PAscalCase".
 *                        *Note that "TeX" or "ABC" isn't pascal case while "TEx" is*.
 * @property CAMEL_CASE all chars are letters, and are camel case, like "camelCase", "camelCAse".
 *                       *Note that "testE" isn't camel case while "testEx" is*.
 */
enum class StringCase {
    LOWER_CASE, UPPER_CASE, PASCAL_CASE, CAMEL_CASE;

    internal companion object {
        internal val pascalCaseRegex = Regex("^[A-Z][a-z]+(?:[A-Z][a-z]+)*\$")
        internal val camelCaseRegex = Regex("^[a-z]+(?:[A-Z][a-z]+)+\$")
    }
}

fun Int.toOrdinal() = this.toString() + if (this % 100 in 11..13) "th" else when (this % 10) {
    1 -> "st"
    2 -> "nd"
    3 -> "rd"
    else -> "th"
}

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