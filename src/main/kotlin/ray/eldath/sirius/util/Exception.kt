package ray.eldath.sirius.util

import ray.eldath.sirius.core.ContainAssert
import ray.eldath.sirius.core.EqualAssert
import ray.eldath.sirius.core.RangeAssert
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.AnyValidationScope
import ray.eldath.sirius.type.Validatable
import ray.eldath.sirius.util.SiriusValidationException.*

internal sealed class SiriusValidationException {
    internal class InvalidConstrainException(override val message: String) : Exception(message)
    internal class ValidationFailedException(override val message: String) : Exception(message)
    internal class MissingRequiredElementException(override val message: String) : Exception(message)
}

internal object ExceptionAssembler {
    internal object VFEAssembler {
        fun equal(assert: EqualAssert<*>, element: AnyValidationPredicate, key: String, depth: Int) =
            VFE(
                assembleJsonObjectK("equal", element, key, depth) +
                        "\n trace: ${assert.actual}(actual) should be ${assert.expected}(expected)"
            )

        fun contain(assert: ContainAssert<*>, element: AnyValidationPredicate, key: String, depth: Int) =
            VFE(
                assembleJsonObjectK("contain", element, key, depth) +
                        "\ntrace: ${assert.element}(actual) should be contained in ${assert.container}(expected)"
            )

        fun range(assert: RangeAssert<*>, element: AnyValidationPredicate, key: String, depth: Int): VFE {
            val actual = assert.actual
            val header = if (actual.start == actual.endInclusive) actual.start.toString() else actual.toString()
            return VFE(
                assembleJsonObjectK("range", element = element, key = key, depth = depth) +
                        "\ntrace: $header(actual) should be contained in ${assert.bigger}(expected)"
            )
        }

        fun lambda(index: Int, element: AnyValidationPredicate, key: String, depth: Int): VFE {
            val trace = "\ntrace: the ${index.toOrdinal()} lambda test defined in current scope is failed"
            return VFE(assembleJsonObjectK("lambda", element = element, key = key, depth = depth) + trace)
        }

        private fun assembleJsonObjectK(
            name: String,
            element: AnyValidationPredicate,
            key: String,
            depth: Int
        ): String {
            val k = assembleKeyT(element, key)
            return "[JsonObject] $name validation failed for JsonObject element at $k at ${assembleDepth(depth)}"
        }
    }

    fun assembleJsonObjectICE(scope: AnyValidationScope, key: String, depth: Int): ICE =
        ICE("[JsonObject] assert validation failed at ${assembleKeyT(scope, key)} at ${assembleDepth(depth)}")

    fun assembleJsonObjectMEE(element: AnyValidationPredicate, key: String, depth: Int): MEE =
        MEE("[JsonObject] missing required element ${assembleKeyT(element, key)} at ${assembleDepth(depth)}")

    private fun assembleKeyT(pOrs: Any, key: String): String {
        val t = when (pOrs) {
            is AnyValidationPredicate -> Validatable.fromPredicate(pOrs).displayName
            is AnyValidationScope -> Validatable.fromScope(pOrs).displayName
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