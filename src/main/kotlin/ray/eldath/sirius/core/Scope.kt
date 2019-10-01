package ray.eldath.sirius.core

import org.json.JSONObject
import ray.eldath.sirius.config.SiriusValidationConfig
import ray.eldath.sirius.core.PredicateBuildInterceptor.jsonObjectIntercept
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.BasicOption
import ray.eldath.sirius.type.Predicate
import ray.eldath.sirius.type.TopClassValidationScopeMarker
import ray.eldath.sirius.util.ExceptionAssembler

private const val max = Int.MAX_VALUE
private val maxRange = 0..max

class JsonObjectValidationScope(override val depth: Int, private val config: SiriusValidationConfig) :
    ValidationScopeWithLengthProperty<JsonObjectValidationPredicate>(depth, config) {

    private val children = hashMapOf<String, AnyValidationPredicate>()
    private val tests = mutableListOf<Predicate<JSONObject>>()
    private var _any: JsonObjectValidationScope? = null

    infix fun String.string(block: StringValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth, config = config)
    }

    infix fun String.jsonObject(block: JsonObjectValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth, config = config)
    }

    infix fun String.boolean(block: BooleanValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth, config = config)
    }

    fun any(block: JsonObjectValidationScope.() -> Unit) {
        if (_any != null)
            throw ExceptionAssembler.multipleAnyBlock(this, depth)
        else {
            _any = JsonObjectValidationScope(depth + 1, config)
            _any?.apply(block)
        }
    }

    fun test(predicate: Predicate<JSONObject>) {
        tests += predicate
    }

    override fun build(): JsonObjectValidationPredicate =
        JsonObjectValidationPredicate(
            tests = tests,
            children = children,
            lengthRange = this.buildRange(), // inherit
            required = isRequired,
            nullable = isNullable,
            depth = depth,
            any = _any
        )

    override fun isAssertsValid(): Boolean = lengthRange in maxRange
}

class BooleanValidationScope(override val depth: Int, config: SiriusValidationConfig) :
    ValidationScope<BooleanValidationPredicate>(depth, config) {

    var expected = false
        set(value) {
            expectedInitialized = true
            field = value
        }

    private var expectedInitialized = false

    override fun build() = BooleanValidationPredicate(isRequired, isNullable, depth, expected)

    override fun isAssertsValid(): Boolean = !expectedInitialized
}

class StringValidationScope(override val depth: Int, config: SiriusValidationConfig) :
    ValidationScopeWithLengthProperty<StringValidationPredicate>(depth, config) {

    private val tests = mutableListOf<Predicate<String>>()
    private val expectedList = mutableListOf<String>()

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
            lengthRange = this.buildRange(),
            required = isRequired,
            nullable = isNullable,
            tests = tests,
            depth = depth
        )

    override fun isAssertsValid(): Boolean = minLength..maxLength in maxRange && lengthRange in maxRange
}

private operator fun <E : Comparable<E>, T : ClosedRange<E>> T.contains(larger: T): Boolean =
    this.start >= larger.start && this.endInclusive <= larger.endInclusive


// left due to sealed class's constraint
@TopClassValidationScopeMarker
sealed class ValidationScope<T : AnyValidationPredicate>(open val depth: Int, config: SiriusValidationConfig) :
    BasicOption(config.requiredByDefault, config.nullableByDefault) {

    internal abstract fun build(): T
    internal abstract fun isAssertsValid(): Boolean
}

sealed class ValidationScopeWithLengthProperty<T : AnyValidationPredicate>(
    override val depth: Int,
    config: SiriusValidationConfig
) : ValidationScope<T>(depth, config) {
    var lengthExact = 0
    var lengthRange = 0..max

    var minLength = 0
    var maxLength = max

    fun buildRange(): IntRange =
        when {
            lengthExact != 0 -> lengthExact..lengthExact
            lengthRange != maxRange -> lengthRange
            else -> minLength..maxLength
        }
}