package ray.eldath.sirius.util

import ray.eldath.sirius.core.ContainAssert
import ray.eldath.sirius.core.EqualAssert
import ray.eldath.sirius.core.RangeAssert
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.AnyValidationScope
import ray.eldath.sirius.type.Validatable
import ray.eldath.sirius.util.SiriusValidationException.*
import ray.eldath.sirius.util.SiriusValidationException.InvalidSchemaException.MultipleAnyBlockException

internal sealed class SiriusValidationException(override val message: String) : Exception(message) {

    internal sealed class InvalidSchemaException(override val message: String) : SiriusValidationException(message) {
        internal class InvalidAssertException(override val message: String) : InvalidSchemaException(message)
        internal class MultipleAnyBlockException(override val message: String) : InvalidSchemaException(message)
    }

    internal class ValidationFailedException(override val message: String) : SiriusValidationException(message)
    internal class MissingRequiredElementException(override val message: String) : SiriusValidationException(message)
}

internal object ExceptionAssembler {
    internal object VFEAssembler {
        fun anyBlock(depth: Int) =
            VFE("[JsonObject] all validation failed in [any block] defined for JsonObject at ${assembleDepth(depth)}")

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
            key: String = "",
            depth: Int
        ): String {
            val k = assembleKeyT(element, key)
            return "[JsonObject] $name validation failed for JsonObject element at $k at ${assembleDepth(depth)}"
        }
    }

    internal fun assembleMABE(scope: AnyValidationScope, depth: Int): MultipleAnyBlockException =
        MultipleAnyBlockException("[${name(scope)}] only one any{} block could provided at ${assembleDepth(depth) + 1}")

    internal fun assembleJsonObjectIAE(scope: AnyValidationScope, key: String, depth: Int): IAE =
        IAE("[JsonObject] assert validation failed at ${assembleKeyT(scope, key)} at ${assembleDepth(depth)}")

    internal fun assembleJsonObjectMEE(element: AnyValidationPredicate, key: String, depth: Int): MEE =
        MEE("[JsonObject] missing required element ${assembleKeyT(element, key)} at ${assembleDepth(depth)}")

    private fun assembleKeyT(pOrs: Any, key: String): String {
        val t = when (pOrs) {
            is AnyValidationPredicate -> name(pOrs)
            is AnyValidationScope -> name(pOrs)
            else -> "?"
        }
        return "($key:$t)"
    }

    private fun name(scope: AnyValidationScope) = Validatable.fromScope(scope).displayName
    private fun name(predicate: AnyValidationPredicate) = Validatable.fromPredicate(predicate).displayName

    private fun assembleDepth(depth: Int) =
        if (depth == 0)
            "[root]"
        else "[$depth] f${if (depth == 1) "oo" else "ee"}t from root"
}

internal typealias MEE = MissingRequiredElementException
internal typealias IAE = InvalidSchemaException.InvalidAssertException
internal typealias VFE = ValidationFailedException