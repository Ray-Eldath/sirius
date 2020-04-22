package ray.eldath.sirius.trace

import ray.eldath.sirius.core.ContainAssert
import ray.eldath.sirius.core.EqualAssert
import ray.eldath.sirius.core.RangeAssert
import ray.eldath.sirius.trace.InvalidSchemaException.MultipleAnyBlockException
import ray.eldath.sirius.trace.Tracer.LocationBasedTracer.JsonObjectLocator
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.AnyValidationScope
import ray.eldath.sirius.type.Validatable
import ray.eldath.sirius.util.toOrdinal

typealias StringProducer = () -> String

interface ExceptionLocator {

    fun locate(): StringProducer

    operator fun invoke() = locate()()

    companion object {
        @PublishedApi
        internal fun jsonObjectLocator(element: Validatable, depth: Int, key: String) =
            JsonObjectLocator(element, depth, key)
    }
}

@PublishedApi
internal object Tracer {

    @PublishedApi
    internal object FreeTracer {

        @PublishedApi
        internal fun multipleAnyBlock(scope: AnyValidationScope, depth: Int): MultipleAnyBlockException =
            MultipleAnyBlockException(
                "[${name(scope)}] only one any{} block could be provided at ${depth(depth) + 1}"
            )
    }

    @PublishedApi
    internal object LocationBasedTracer {

        class JsonObjectLocator(
            private val element: Validatable,
            private val depth: Int,
            private val key: String
        ) : ExceptionLocator {

            override fun locate(): StringProducer = { "${keyName(element, key)} at ${depth(depth)}" }
        }


        fun equal(assert: EqualAssert<*>, locator: ExceptionLocator) = InvalidValueException(
            header("equal", assert.propertyName, locator()) +
                    "\n trace: ${assert.actual}(actual) should be ${assert.expected}(expected)"
        )

        fun contain(assert: ContainAssert<*>, locator: ExceptionLocator) = InvalidValueException(
            header("contain", assert.propertyName, locator()) +
                    "\n trace: ${assert.element}(actual) should be contained in ${assert.container}(expected)"
        )

        fun range(assert: RangeAssert<*>, locator: ExceptionLocator): InvalidValueException {
            val actual = assert.actual
            val header = if (actual.start == actual.endInclusive) actual.start.toString() else actual.toString()

            return InvalidValueException(
                header("range", assert.propertyName, locator()) +
                        "\n trace: $header(actual) should be contained in ${assert.bigger}(expected)"
            )
        }

        fun lambda(
            index: Int,
            purpose: String = "",
            isBuiltIn: Boolean = false,
            locator: ExceptionLocator
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

            return InvalidValueException(header("lambda", propertyName, locator()) + trace.toString())
        }

        fun any(depth: Int) =
            InvalidValueException(
                "[JsonObject] all validation failed in [any block] defined for JsonObject at ${depth(depth)}"
            )

        private fun header(
            type: String,
            propertyName: String,
            location: String
        ): String {
            val p = if (propertyName.isNotEmpty()) " of property \"$propertyName\"" else ""
            return "[JsonObject] $type validation failed$p for JsonObject element at $location"
        }

        internal fun typeMismatch(element: AnyValidationPredicate, actual: Any, locator: ExceptionLocator) =
            element.type.actualType.javaObjectType.simpleName.let {
                ValidationException.TypeMismatchException(
                    "[JsonObject] type mismatch at ${locator()}" +
                            "\n trace: ${actual.javaClass.simpleName}(actual) should be $it(expected)"
                )
            }

        @PublishedApi
        internal fun invalidAssert(reason: String = "", locator: ExceptionLocator) =
            InvalidSchemaException.InvalidAssertException(
                "[JsonObject] assert validation failed at ${locator()}" +
                        if (reason.isNotEmpty()) "\n trace: $reason" else ""
            )

        internal fun missingRequiredElement(locator: ExceptionLocator) =
            ValidationException.MissingRequiredElementException(
                "[JsonObject] missing required element ${locator()}"
            )

        internal fun nullPointer(locator: ExceptionLocator) =
            InvalidValueException.NullPointerException(
                "[JsonObject] non-null element ${locator()} is set to `null`"
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

    private fun name(scope: AnyValidationScope) = scope.type.displayName

    private fun name(predicate: AnyValidationPredicate) = predicate.type.displayName
}