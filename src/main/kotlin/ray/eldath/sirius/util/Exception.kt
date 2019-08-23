package ray.eldath.sirius.util

import ray.eldath.sirius.core.ContainConstrain
import ray.eldath.sirius.core.EqualConstrain
import ray.eldath.sirius.core.LambdaConstrain
import ray.eldath.sirius.core.RangeConstrain
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.AnyValidationScope
import ray.eldath.sirius.type.ConstrainType
import ray.eldath.sirius.util.SiriusValidationException.*

internal sealed class SiriusValidationException {
    internal class InvalidConstrainException(override val message: String) : Exception(message)
    internal class ValidationFailedException(override val message: String) : Exception(message)
    internal class MissingRequiredElementException(override val message: String) : Exception(message)
}

internal object ExceptionAssembler {
    internal object VFEAssembler {
        fun equal(constrain: EqualConstrain<*>, element: AnyValidationPredicate, key: String, depth: Int) =
            VFE(
                assembleJsonObjectK("equal", element, key, depth) +
                        "\n trace: ${constrain.actual}(actual) should be ${constrain.expected}(expected)"
            )

        fun contain(constrain: ContainConstrain<*>, element: AnyValidationPredicate, key: String, depth: Int) =
            VFE(
                assembleJsonObjectK("contain", element, key, depth) +
                        "\ntrace: ${constrain.element}(actual) should be contained in ${constrain.container}(expected)"
            )

        fun range(constrain: RangeConstrain<*>, element: AnyValidationPredicate, key: String, depth: Int): VFE {
            val actual = constrain.actual
            val header = if (actual.start == actual.endInclusive) actual.start.toString() else actual.toString()
            return VFE(
                assembleJsonObjectK("range", element = element, key = key, depth = depth) +
                        "\ntrace: $header(actual) should be contained in ${constrain.bigger}(expected)"
            )
        }

        // STOPSHIP: incomplete support for test{ }
        fun lambda(constrain: LambdaConstrain<*>, element: AnyValidationPredicate, key: String, depth: Int) =
            VFE(assembleJsonObjectK("lambda", element = element, key = key, depth = depth))

        private fun assembleJsonObjectK(
            name: String,
            element: AnyValidationPredicate,
            key: String,
            depth: Int
        ): String {
            val k = assembleKeyT(element, key)
            return "[$name] $name validation failed for JsonObject element at $k at ${assembleDepth(depth)}"
        }
    }

    fun assembleJsonObjectICE(scope: AnyValidationScope, key: String, depth: Int): ICE =
        ICE("[JsonObject] constrains validation failed at ${assembleKeyT(scope, key)} at ${assembleDepth(depth)}")

    fun assembleJsonObjectMEE(element: AnyValidationPredicate, key: String, depth: Int): MEE =
        MEE("[JsonObject] missing required element ${assembleKeyT(element, key)} at ${assembleDepth(depth)}")

    private fun assembleKeyT(pOrs: Any, key: String): String {
        val t = when (pOrs) {
            is AnyValidationPredicate -> ConstrainType.fromPredicate(pOrs).displayName
            is AnyValidationScope -> ConstrainType.fromScope(pOrs).displayName
            else -> "?"
        }
        return "($key:$t)"
    }

    private fun assembleDepth(depth: Int) =
        if (depth == 0)
            "[root]"
        else "[$depth] f${if (depth == 1) "oo" else "ee"}t from root"
}

internal typealias MEE = MissingRequiredElementException
internal typealias ICE = InvalidConstrainException
internal typealias VFE = ValidationFailedException