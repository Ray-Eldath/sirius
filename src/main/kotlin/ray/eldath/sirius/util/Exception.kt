package ray.eldath.sirius.util

import ray.eldath.sirius.type.BaseValidationPredicate
import ray.eldath.sirius.util.SiriusValidationException.MissingRequiredElementException

sealed class SiriusValidationException {
    class MissingRequiredElementException(override val message: String) : Exception(message)
}

object ExceptionAssembler {
    fun assembleJsonObjectMEE( // MEE stands for Missing required Element Exception
        element: BaseValidationPredicate,
        key: String,
        depth: Int
    ): MissingRequiredElementException {
        val t = key + "[" + PredicateType.fromPredicate(element).displayName + "]"
        return MissingRequiredElementException("[JsonObject] missing required element $t at ${assembleDepth(depth)}")
    }

    private fun assembleDepth(depth: Int) =
        if (depth == 0)
            "[root]"
        else "[$depth] foot from root"
}
