package ray.eldath.sirius.core

import org.json.JSONObject
import ray.eldath.sirius.core.PredicateBuildInterceptor.jsonObjectIntercept
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.Predicate
import ray.eldath.sirius.type.RequireOption
import ray.eldath.sirius.type.TopClassValidationScopeMarker
import ray.eldath.sirius.util.ExceptionAssembler

private const val max = Int.MAX_VALUE
private val maxRange = 0..max

class JsonObjectValidationScope(override val depth: Int) : ValidationScope<JsonObjectValidationPredicate>(depth) {
    private val children = mutableMapOf<String, AnyValidationPredicate>()
    private val tests = mutableListOf<Predicate<JSONObject>>()
    private var _any: JsonObjectValidationScope? = null

    var lengthRange = maxRange

    infix fun String.string(block: StringValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth)
    }

    infix fun String.jsonObject(block: JsonObjectValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth)
    }

    infix fun String.boolean(block: BooleanValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth)
    }

    fun any(block: JsonObjectValidationScope.() -> Unit) {
        if (_any != null)
            throw ExceptionAssembler.assembleMABE(this, depth)
        else {
            _any = JsonObjectValidationScope(depth + 1)
            _any?.apply(block)
        }
    }

    fun test(predicate: Predicate<JSONObject>) {
        tests += predicate
    }

    override fun build(): JsonObjectValidationPredicate =
        JsonObjectValidationPredicate(
            tests = this.tests,
            children = this.children,
            lengthRange = this.lengthRange,
            required = this.isRequired,
            depth = depth,
            any = _any
        )

    override fun isAssertsValid(): Boolean = lengthRange in maxRange
}

class BooleanValidationScope(override val depth: Int) : ValidationScope<BooleanValidationPredicate>(depth) {
    var expected = false
        set(value) {
            expectedInitialized = true
            field = value
        }

    private var expectedInitialized = false

    override fun build(): BooleanValidationPredicate = BooleanValidationPredicate(isRequired, depth, expected)

    override fun isAssertsValid(): Boolean = !expectedInitialized
}

class StringValidationScope(override val depth: Int) : ValidationScope<StringValidationPredicate>(depth) {
    private val tests = mutableListOf<Predicate<String>>()
    private val expectedList = mutableListOf<String>()

    var lengthRange = 0..max

    var minLength = 0
    var maxLength = max

    fun expected(vararg expected: String) {
        if (expected.size == 1)
            expectedList.clear()
        expectedList += expected
    }

    fun test(predicate: Predicate<String>) {
        tests += predicate
    }

    override fun build(): StringValidationPredicate =
        StringValidationPredicate(
            expectedValue = expectedList,
            lengthRange = if (lengthRange != maxRange) lengthRange else minLength..maxLength,
            required = isRequired,
            tests = tests,
            depth = depth
        )

    override fun isAssertsValid(): Boolean = minLength..maxLength in maxRange && lengthRange in maxRange
}

private operator fun <E : Comparable<E>, T : ClosedRange<E>> T.contains(larger: T): Boolean =
    this.start >= larger.start && this.endInclusive <= larger.endInclusive


// left due to sealed class's constraint
@TopClassValidationScopeMarker
sealed class ValidationScope<T : AnyValidationPredicate>(open val depth: Int) : RequireOption() {
    internal abstract fun build(): T
    internal abstract fun isAssertsValid(): Boolean
}