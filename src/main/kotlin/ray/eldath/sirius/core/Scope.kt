package ray.eldath.sirius.core

import ray.eldath.sirius.core.PredicateBuildInterceptor.jsonObjectIntercept
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.Predicate
import ray.eldath.sirius.type.RequireOption
import ray.eldath.sirius.type.TopClassValidationScopeMarker

private const val max = Int.MAX_VALUE
private val maxRange = 0..max

open class JsonObjectValidationScope(override val depth: Int) : ValidationScope<JsonObjectValidationPredicate>(depth) {
    private val children = mutableMapOf<String, AnyValidationPredicate>()

    var length = maxRange

    infix fun String.string(block: StringValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth)
    }

    infix fun String.jsonObject(block: JsonObjectValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth)
    }

    infix fun String.boolean(block: BooleanValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth)
    }

    override fun build(): JsonObjectValidationPredicate =
        JsonObjectValidationPredicate(
            children = this.children,
            lengthRange = this.length,
            required = this.isRequired,
            depth = depth
        )

    override fun isConstrainsValid(): Boolean = length in maxRange
}

class BooleanValidationScope(override val depth: Int) : ValidationScope<BooleanValidationPredicate>(depth) {
    var expected = false
        set(value) {
            expectedInitialized = true
            field = value
        }

    private var expectedInitialized = false

    override fun build(): BooleanValidationPredicate = BooleanValidationPredicate(isRequired, depth, expected)

    override fun isConstrainsValid(): Boolean = !expectedInitialized
}

class StringValidationScope(override val depth: Int) : ValidationScope<StringValidationPredicate>(depth) {

    private val tests = mutableListOf<Predicate<String>>()

    var length = 0..max

    var minLength = 0
    var maxLength = max

    fun test(predicate: Predicate<String>) {
        tests += predicate
    }

    override fun build(): StringValidationPredicate =
        StringValidationPredicate(
            lengthRange = if (length != maxRange) length else minLength..maxLength,
            required = isRequired,
            tests = tests,
            depth = depth
        )

    override fun isConstrainsValid(): Boolean = minLength..maxLength in maxRange && length in maxRange
}

private operator fun <E : Comparable<E>, T : ClosedRange<E>> T.contains(larger: T): Boolean =
    this.start >= larger.start && this.endInclusive <= larger.endInclusive


// left due to sealed class's constrain
@TopClassValidationScopeMarker
sealed class ValidationScope<T : AnyValidationPredicate>(open val depth: Int) : RequireOption() {
    internal abstract fun build(): T
    internal abstract fun isConstrainsValid(): Boolean
}