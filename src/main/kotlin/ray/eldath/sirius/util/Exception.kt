package ray.eldath.sirius.util

import ray.eldath.sirius.core.BaseValidationPredicate
import ray.eldath.sirius.core.JsonObjectValidationPredicate
import ray.eldath.sirius.core.StringValidationPredicate
import ray.eldath.sirius.util.SiriusValidationException.MissingRequiredElementException

sealed class SiriusValidationException {
    class MissingRequiredElementException(override val message: String) : Exception(message)
}

object ExceptionAssembler {
    fun assembleMissingException(
        element: BaseValidationPredicate,
        key: String,
        depth: Int
    ): MissingRequiredElementException {
        val t = key + "[" + matchType(element) + "]"
        return MissingRequiredElementException("[JsonObject] missing required element $t at ${assembleDepth(depth)}")
    }

    private fun assembleDepth(depth: Int) =
        if (depth == 0)
            "[root]"
        else "[$depth] foot from root"

    private fun matchType(predicate: BaseValidationPredicate): String =
        when (predicate) {
            is StringValidationPredicate -> "string"
            is JsonObjectValidationPredicate -> "JsonObject"
        }
}
