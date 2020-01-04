package ray.eldath.sirius.trace

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
     * [ray.eldath.sirius.core.ValidationScopeWithLength.minLength]
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

internal typealias IVE = InvalidValueException