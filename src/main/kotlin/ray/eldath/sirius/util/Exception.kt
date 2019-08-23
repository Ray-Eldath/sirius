package ray.eldath.sirius.util

import ray.eldath.sirius.type.BaseValidationPredicate
import ray.eldath.sirius.type.BaseValidationScope
import ray.eldath.sirius.util.SiriusValidationException.InvalidConstrainException
import ray.eldath.sirius.util.SiriusValidationException.MissingRequiredElementException

internal sealed class SiriusValidationException {
    class InvalidConstrainException(override val message: String) : Exception(message)
    class MissingRequiredElementException(override val message: String) : Exception(message)
}

internal object ExceptionAssembler {
    fun assembleJsonObjectICE(scope: BaseValidationScope, key: String, depth: Int): ICE {
        val t = key + "[" + ConstrainType.fromScope(scope).displayName + "]"
        return InvalidConstrainException("[JsonObject] constrains validation failed at $t at ${assembleDepth(depth)}")
    }

    fun assembleJsonObjectMEE( // MEE stands for Missing required Element Exception
        element: BaseValidationPredicate,
        key: String,
        depth: Int
    ): MEE {
        val t = key + "[" + ConstrainType.fromPredicate(element).displayName + "]"
        return MissingRequiredElementException("[JsonObject] missing required element $t at ${assembleDepth(depth)}")
    }

    private fun assembleDepth(depth: Int) =
        if (depth == 0)
            "[root]"
        else "[$depth] foot from root"
}

internal typealias MEE = MissingRequiredElementException
internal typealias ICE = InvalidConstrainException
