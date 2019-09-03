package ray.eldath.sirius.util

import ray.eldath.sirius.core.ContainAssert
import ray.eldath.sirius.core.EqualAssert
import ray.eldath.sirius.core.RangeAssert
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.AnyValidationScope
import ray.eldath.sirius.type.Validatable
import ray.eldath.sirius.type.Validatable.JSON_ARRAY
import ray.eldath.sirius.type.Validatable.JSON_OBJECT
import ray.eldath.sirius.util.InvalidSchemaException.MultipleAnyBlockException

// exceptions should be public
sealed class SiriusException(override val message: String) : Exception(message)

sealed class InvalidSchemaException(override val message: String) : SiriusException(message) {
    class InvalidAssertException(override val message: String) : InvalidSchemaException(message)
    class MultipleAnyBlockException(override val message: String) : InvalidSchemaException(message)
}

sealed class ValidationException(override val message: String) : SiriusException(message) {
    class MissingRequiredElementException(override val message: String) : ValidationException(message)
}

open class InvalidValueException(override val message: String) : ValidationException(message) {
    class NullPointerException(override val message: String) : InvalidValueException(message)
}

internal object ExceptionAssembler {
    internal object IVEAssembler {
        fun anyBlock(depth: Int, label: Validatable = JSON_OBJECT) =
            IVE("[${label.displayName}] all validation failed in [any block] defined for ${label.displayName} at ${assembleDepth(depth)}")

        fun equal(assert: EqualAssert<*>, element: AnyValidationPredicate, depth: Int, vararg args: Any, label: Validatable = JSON_OBJECT) = IVE(
            assembleK("equal", assert.propertyName, element, depth, label, args) +
                    "\n trace: ${assert.actual}(actual) should be ${assert.expected}(expected)"
        )

        fun contain(assert: ContainAssert<*>, element: AnyValidationPredicate, depth: Int, vararg args: Any, label: Validatable = JSON_OBJECT) = IVE(
            assembleK("contain", assert.propertyName, element, depth, label, args) +
                    "\n trace: ${assert.element}(actual) should be contained in ${assert.container}(expected)"
        )

        fun range(assert: RangeAssert<*>, element: AnyValidationPredicate, depth: Int, vararg args: Any, label: Validatable = JSON_OBJECT): IVE {
            val actual = assert.actual
            val header = if (actual.start == actual.endInclusive) actual.start.toString() else actual.toString()
            return IVE(
                assembleK("range", assert.propertyName, element, depth, label, args) +
                        "\n trace: $header(actual) should be contained in ${assert.bigger}(expected)"
            )
        }

        fun lambda(index: Int, element: AnyValidationPredicate, depth: Int, vararg args: Any, label: Validatable = JSON_OBJECT): IVE {
            val trace = "\n trace: the ${index.toOrdinal()} lambda test defined in current scope is failed"
            return IVE(assembleK("lambda", "[test block]", element, depth, label, args) + trace)
        }

        private fun assembleK(
            type: String, propertyName: String, element: AnyValidationPredicate, depth: Int, label: Validatable, args: Array<out Any>
        ): String {
            return when (label) {
                JSON_OBJECT -> {
                    require(args.size == 1 && args[0] is String)
                    assembleJsonObjectK(type, propertyName, element, args[0] as String, depth)
                }
                JSON_ARRAY -> {
                    require(args.isEmpty())
                    TODO()
                }
                else -> throw IllegalArgumentException("Failed property requirement.")
            }
        }

        private fun assembleJsonObjectK(
            type: String, propertyName: String, element: AnyValidationPredicate, key: String, depth: Int
        ): String {
            val t = assembleKeyT(element, key)
            val d = assembleDepth(depth)
            return "[JsonObject] $type validation of property \"$propertyName\" failed for JsonObject element at $t at $d"
        }
    }

    internal fun assembleMABE(scope: AnyValidationScope, depth: Int): MultipleAnyBlockException =
        MultipleAnyBlockException("[${name(scope)}] only one any{} block could provided at ${assembleDepth(depth) + 1}")

    internal fun assembleJsonObjectIAE(scope: AnyValidationScope, key: String, depth: Int): IAE =
        IAE("[JsonObject] assert validation failed at ${assembleKeyT(scope, key)} at ${assembleDepth(depth)}")

    internal fun assembleJsonObjectMEE(element: AnyValidationPredicate, key: String, depth: Int): MEE =
        MEE("[JsonObject] missing required element ${assembleKeyT(element, key)} at ${assembleDepth(depth)}")

    internal fun assembleJsonObjectNPE(element: AnyValidationPredicate, key: String, depth: Int) =
        NPE("[JsonObject] non-null element ${assembleKeyT(element, key)} at ${assembleDepth(depth)} is set to `null`")

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

internal typealias MEE = ValidationException.MissingRequiredElementException
internal typealias IAE = InvalidSchemaException.InvalidAssertException
internal typealias IVE = InvalidValueException
internal typealias NPE = InvalidValueException.NullPointerException