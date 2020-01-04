package ray.eldath.sirius.trace

import ray.eldath.sirius.core.ContainAssert
import ray.eldath.sirius.core.EqualAssert
import ray.eldath.sirius.core.RangeAssert
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.AnyValidationScope
import ray.eldath.sirius.type.Validatable
import ray.eldath.sirius.util.toOrdinal

internal object Tracer {
    internal object AllCheckpoint {

        internal fun multipleAnyBlock(
            scope: AnyValidationScope,
            depth: Int
        ): InvalidSchemaException.MultipleAnyBlockException =
            InvalidSchemaException.MultipleAnyBlockException(
                "[${name(scope)}] only one any{} block could be provided at ${depth(depth) + 1}"
            )
    }

    internal object JsonObjectCheckpoint {

        fun equal(
            assert: EqualAssert<*>,
            element: AnyValidationPredicate,
            depth: Int,
            key: String
        ) = InvalidValueException(
            header("equal", assert.propertyName, element, depth, key) +
                    "\n trace: ${assert.actual}(actual) should be ${assert.expected}(expected)"
        )

        fun contain(
            assert: ContainAssert<*>,
            element: AnyValidationPredicate,
            depth: Int,
            key: String
        ) = InvalidValueException(
            header("contain", assert.propertyName, element, depth, key) +
                    "\n trace: ${assert.element}(actual) should be contained in ${assert.container}(expected)"
        )

        fun range(
            assert: RangeAssert<*>,
            element: AnyValidationPredicate,
            depth: Int,
            key: String
        ): InvalidValueException {
            val actual = assert.actual
            val header = if (actual.start == actual.endInclusive) actual.start.toString() else actual.toString()

            return InvalidValueException(
                header("range", assert.propertyName, element, depth, key) +
                        "\n trace: $header(actual) should be contained in ${assert.bigger}(expected)"
            )
        }

        fun lambda(
            index: Int,
            element: AnyValidationPredicate,
            depth: Int,
            key: String,
            purpose: String = "",
            isBuiltIn: Boolean = false
        ): InvalidValueException {
            val propertyName = (if (isBuiltIn) "[built-in " else "[") + "lambda test]"

            val trace = StringBuilder("\n trace: ")
            if (isBuiltIn)
                trace.append(purpose)
            else {
                trace.append("the ")
                trace.append(index.toOrdinal())
                trace.append("lambda test defined in current scope is fail.")

                if (purpose.isNotBlank()) {
                    trace.append("\n\t\tthis lambda test has been tagged with a purpose `")
                    trace.append(purpose)
                    trace.append("`.\n")
                }
            }

            return InvalidValueException(header("lambda", propertyName, element, depth, key) + trace.toString())
        }

        fun any(depth: Int) =
            InvalidValueException(
                "[JsonObject] all validation failed in [any block] defined for JsonObject at ${depth(depth)}"
            )

        private fun header(
            type: String,
            propertyName: String,
            element: AnyValidationPredicate,
            depth: Int,
            key: String
        ): String {
            val t = keyName(element, key)
            val d = depth(depth)
            val p = if (propertyName.isNotEmpty()) " of property \"$propertyName\"" else ""
            return "[JsonObject] $type validation failed$p for JsonObject element at $t at $d"
        }

        internal fun typeMismatch(element: AnyValidationPredicate, key: String, depth: Int, actual: Any) =
            Validatable.fromPredicate(element).actualType.javaObjectType.simpleName.let {
                ValidationException.TypeMismatchException(
                    "[JsonObject] type mismatch at ${keyName(element, key)} at ${depth(depth)}" +
                            "\n trace: ${actual.javaClass.simpleName}(actual) should be $it(expected)"
                )
            }

        internal fun invalidAssert(scope: AnyValidationScope, key: String, depth: Int, reason: String = "") =
            InvalidSchemaException.InvalidAssertException(
                "[JsonObject] assert validation failed at ${keyName(scope, key)} at ${depth(depth)}" +
                        if (reason.isNotEmpty()) "\n trace: $reason" else ""
            )

        internal fun missingRequiredElement(element: AnyValidationPredicate, key: String, depth: Int) =
            ValidationException.MissingRequiredElementException(
                "[JsonObject] missing required element ${keyName(element, key)} at ${depth(depth)}"
            )

        internal fun nullPointer(element: AnyValidationPredicate, key: String, depth: Int) =
            InvalidValueException.NullPointerException(
                "[JsonObject] non-null element ${keyName(element, key)} at ${depth(depth)} is set to `null`"
            )

        private fun keyName(pOrs: Any, key: String): String {
            val t = when (pOrs) {
                is AnyValidationPredicate -> name(pOrs)
                is AnyValidationScope -> name(pOrs)
                else -> "?"
            }
            return "($key:$t)"
        }
    }

    private fun depth(depth: Int) =
        if (depth == 0)
            "[root]"
        else "[$depth] f${if (depth == 1) "oo" else "ee"}t from root"

    private fun name(scope: AnyValidationScope) = Validatable.fromScope(scope).displayName

    private fun name(predicate: AnyValidationPredicate) = Validatable.fromPredicate(predicate).displayName
}