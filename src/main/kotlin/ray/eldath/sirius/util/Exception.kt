package ray.eldath.sirius.util

import ray.eldath.sirius.core.ContainAssert
import ray.eldath.sirius.core.EqualAssert
import ray.eldath.sirius.core.RangeAssert
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.AnyValidationScope
import ray.eldath.sirius.type.Validatable
import ray.eldath.sirius.type.Validatable.JSON_ARRAY
import ray.eldath.sirius.type.Validatable.JSON_OBJECT
import ray.eldath.sirius.util.InvalidSchemaException.InvalidAssertException
import ray.eldath.sirius.util.InvalidSchemaException.MultipleAnyBlockException
import ray.eldath.sirius.util.InvalidValueException.NullPointerException
import ray.eldath.sirius.util.ValidationException.MissingRequiredElementException
import ray.eldath.sirius.util.ValidationException.TypeMismatchException

// exceptions should be public
/**
 * Root exception, the parent type of all exceptions that could be thrown from Sirius.
 */
sealed class SiriusException(override val message: String) : Exception(message)

/**
 * If the schema built with Sirius DSL *itself* is invalid somewhere, [InvalidSchemaException] will be thrown.
 */
sealed class InvalidSchemaException(override val message: String) : SiriusException(message) {
    /**
     * If one of your asserts you set in a test block is invalid (like set
     * [ray.eldath.sirius.core.ValidationScopeWithLengthProperty.minLength]
     * less than `0`), [InvalidAssertException] will be thrown.
     */
    class InvalidAssertException(override val message: String) : InvalidSchemaException(message)

    /**
     * Only one `any {...}` block can appear in one test block.
     */
    class MultipleAnyBlockException(override val message: String) : InvalidSchemaException(message)
}

/**
 * If the incoming JSON is invalid. [ValidationException] will be thrown.
 *
 * **If and only if** the required element is missing, it's type is incorrect or the value of the
 * element is invalid will cause this exception to be thrown.
 */
sealed class ValidationException(override val message: String) : SiriusException(message) {
    /**
     * If an element is required by set [ray.eldath.sirius.core.ValidationScope.required]
     * or inherit from configuration ([ray.eldath.sirius.config.SiriusValidationConfig.requiredByDefault])
     * but can not found in the incoming JSON, [MissingRequiredElementException] will be thrown.
     */
    class MissingRequiredElementException(override val message: String) : ValidationException(message)

    /**
     * If an element matches the index of an element (like they have the same key
     * in `JsonObject`) but their type is different, [TypeMismatchException] will be thrown.
     */
    class TypeMismatchException(override val message: String) : ValidationException(message)
}

/**
 * If a nonnull element is `null`, or it's content mismatches your schema,
 * [InvalidValueException] will be thrown.
 */
open class InvalidValueException(override val message: String) : ValidationException(message) {
    class NullPointerException(override val message: String) : InvalidValueException(message)
}

internal object ExceptionAssembler {
    internal object IVEAssembler {
        /*
        args:
            JSONObject: [key: String]
         */
        fun anyBlock(depth: Int, label: Validatable = JSON_OBJECT) =
            IVE("[${label.displayName}] all validation failed in [any block] defined for ${label.displayName} at ${depth(depth)}")

        fun equal(assert: EqualAssert<*>, element: AnyValidationPredicate, depth: Int, vararg args: Any, label: Validatable = JSON_OBJECT) = IVE(
            header("equal", assert.propertyName, element, depth, label, args) +
                    "\n trace: ${assert.actual}(actual) should be ${assert.expected}(expected)"
        )

        fun contain(assert: ContainAssert<*>, element: AnyValidationPredicate, depth: Int, vararg args: Any, label: Validatable = JSON_OBJECT) = IVE(
            header("contain", assert.propertyName, element, depth, label, args) +
                    "\n trace: ${assert.element}(actual) should be contained in ${assert.container}(expected)"
        )

        fun range(assert: RangeAssert<*>, element: AnyValidationPredicate, depth: Int, vararg args: Any, label: Validatable = JSON_OBJECT): IVE {
            val actual = assert.actual
            val header = if (actual.start == actual.endInclusive) actual.start.toString() else actual.toString()
            return IVE(
                header("range", assert.propertyName, element, depth, label, args) +
                        "\n trace: $header(actual) should be contained in ${assert.bigger}(expected)"
            )
        }

        /*
        * args:
        * [..., purpose: String, isBuiltIn: Boolean]
         */
        fun lambda(index: Int, element: AnyValidationPredicate, depth: Int, vararg args: Any, label: Validatable = JSON_OBJECT): IVE {
            require(args.size in 1..3)

            val isBuiltIn = args.size == 3 && args[1] is String && args[2] is Boolean && args[2] as Boolean
            val propertyName = (if (isBuiltIn) "[built-in " else "[") + "lambda test]"
            val purpose =
                if (args.size == 2 && args[1] is String) // have purpose set but not isn't built-in
                    "\n\t\tthis lambda test has been tagged with a purpose `${args[1] as String}`.\n"
                else ""

            val trace =
                "\n trace: " +
                        (if (isBuiltIn)
                            args[1] as String
                        else "the ${index.toOrdinal()} lambda test defined in current scope is failed.") + purpose

            return IVE(header("lambda", propertyName, element, depth, label, args) + trace)
        }

        private fun header(
            type: String,
            propertyName: String,
            element: AnyValidationPredicate,
            depth: Int,
            label: Validatable,
            args: Array<out Any>
        ): String = when (label) {
            JSON_OBJECT -> {
                require(args.isNotEmpty() && args[0] is String)
                jsonObjectExceptionHeader(type, propertyName, element, args[0] as String /* key: String */, depth)
            }
            JSON_ARRAY -> {
                require(args.isEmpty())
                TODO()
            }
            else -> throw IllegalArgumentException("Failed property requirement.")
        }

        private fun jsonObjectExceptionHeader(type: String, propertyName: String = "", element: AnyValidationPredicate, key: String, depth: Int): String {
            val t = keyName(element, key)
            val d = depth(depth)
            val p = if (propertyName.isNotEmpty()) " of property \"$propertyName\"" else ""
            return "[JsonObject] $type validation failed$p for JsonObject element at $t at $d"
        }
    }

    internal fun multipleAnyBlock(scope: AnyValidationScope, depth: Int): MultipleAnyBlockException =
        MultipleAnyBlockException("[${name(scope)}] only one any{} block could provided at ${depth(depth) + 1}")

    internal fun jsonObjectTypeMismatch(element: AnyValidationPredicate, key: String, depth: Int, actual: Any) =
        Validatable.fromPredicate(element).actualType.javaObjectType.simpleName.let {
            TypeMismatchException(
                "[JsonObject] type mismatch at ${keyName(element, key)} at ${depth(depth)}" +
                        "\n trace: ${actual.javaClass.simpleName}(actual) should be $it(expected)"
            )
        }

    internal fun jsonObjectInvalidAssert(scope: AnyValidationScope, key: String, depth: Int, reason: String = "") =
        InvalidAssertException(
            "[JsonObject] assert validation failed at ${keyName(scope, key)} at ${depth(depth)}" +
                    if (reason.isNotEmpty()) "\n trace: $reason" else ""
        )

    internal fun jsonObjectMissingRequiredElement(element: AnyValidationPredicate, key: String, depth: Int) =
        MissingRequiredElementException("[JsonObject] missing required element ${keyName(element, key)} at ${depth(depth)}")

    internal fun jsonObjectNPE(element: AnyValidationPredicate, key: String, depth: Int) =
        NullPointerException("[JsonObject] non-null element ${keyName(element, key)} at ${depth(depth)} is set to `null`")

    private fun keyName(pOrs: Any, key: String): String {
        val t = when (pOrs) {
            is AnyValidationPredicate -> name(pOrs)
            is AnyValidationScope -> name(pOrs)
            else -> "?"
        }
        return "($key:$t)"
    }

    private fun name(scope: AnyValidationScope) = Validatable.fromScope(scope).displayName
    private fun name(predicate: AnyValidationPredicate) = Validatable.fromPredicate(predicate).displayName

    private fun depth(depth: Int) =
        if (depth == 0)
            "[root]"
        else "[$depth] f${if (depth == 1) "oo" else "ee"}t from root"
}

internal typealias IVE = InvalidValueException