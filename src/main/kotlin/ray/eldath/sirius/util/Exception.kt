package ray.eldath.sirius.util

import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.AnyValidationScope
import ray.eldath.sirius.type.ConstrainType
import ray.eldath.sirius.util.SiriusValidationException.InvalidConstrainException
import ray.eldath.sirius.util.SiriusValidationException.MissingRequiredElementException

internal sealed class SiriusValidationException {
    internal class InvalidConstrainException(override val message: String) : Exception(message)
    internal class MissingRequiredElementException(override val message: String) : Exception(message)
}

internal object ExceptionAssembler {
    fun assembleJsonObjectICE(scope: AnyValidationScope, key: String, depth: Int): ICE {
        val t = key + "[" + ConstrainType.fromScope(scope).displayName + "]"
        return InvalidConstrainException("[JsonObject] constrains validation failed at $t at ${assembleDepth(depth)}")
    }

    fun assembleJsonObjectMEE( // MEE stands for Missing required Element Exception
        element: AnyValidationPredicate,
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
