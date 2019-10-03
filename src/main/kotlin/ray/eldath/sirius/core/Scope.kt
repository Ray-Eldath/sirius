package ray.eldath.sirius.core

import org.apache.commons.lang3.StringUtils
import org.json.JSONObject
import ray.eldath.sirius.config.SiriusValidationConfig
import ray.eldath.sirius.core.PredicateBuildInterceptor.jsonObjectIntercept
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.BasicOption
import ray.eldath.sirius.type.LambdaTest
import ray.eldath.sirius.type.TopClassValidationScopeMarker
import ray.eldath.sirius.util.ExceptionAssembler
import ray.eldath.sirius.util.StringCase
import ray.eldath.sirius.util.StringCase.*

private const val max = Int.MAX_VALUE
private val maxRange = 0..max

class JsonObjectValidationScope(override val depth: Int, private val config: SiriusValidationConfig) :
    ValidationScopeWithLengthAndTestsProperty<JSONObject, JsonObjectValidationPredicate>(depth, config) {

    private val children = hashMapOf<String, AnyValidationPredicate>()
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

    override fun build(): JsonObjectValidationPredicate =
        JsonObjectValidationPredicate(
            tests = this.buildLambdaTests(),
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
    ValidationScopeWithLengthAndTestsProperty<String, StringValidationPredicate>(depth, config) {

    private var isEmptyAccepted = true
    private var isAllBlankAccepted = true

    val nonEmpty: Unit
        get() = run { isEmptyAccepted = false }

    val nonBlank: Unit
        get() = run { isAllBlankAccepted = false }

    private val expectedList = mutableListOf<String>()
    private var expectedIgnoreCase = false

    /**
     * The string should literally equal to one of the given strings. If this function is called
     * more than once, all given string will be treated as the expected strings. **But if** only
     * one string is given in any invocation, the string will become the only expected string.
     *
     * That is to say, if you define a scope:
     * ```kotlin
     * expected("a", "b", "c")
     * expected("d", "e")
     * // here, "a", "b", "c", "d" and "e" are expected.
     * // but if you continue...
     * expected("f", "g")
     * expected("h")
     * // here only "h" is expected because in the last invocation, only one string is provided.
     * ```
     *
     * @param expected list of expected strings.
     * @param ignoreCase ignore cases for *every* given expected string.
     */
    fun expected(vararg expected: String, ignoreCase: Boolean = false) {
        expectedIgnoreCase = ignoreCase
        if (expected.size == 1)
            expectedList.clear()
        expectedList += expected
    }

    fun regex(regex: Regex) {
        builtInAcceptIf("the string must matches the given regex $regex") { it.matches(regex) }
    }

    fun requireCase(case: StringCase) {
        val predicate: (String) -> Boolean =
            when (case) {
                LOWER_CASE -> { str -> StringUtils.isAllLowerCase(str) }
                UPPER_CASE -> { str -> StringUtils.isAllUpperCase(str) }
                PASCAL_CASE -> { str -> str.matches(StringCase.pascalCaseRegex) }
                CAMEL_CASE -> { str -> str.matches(StringCase.camelCaseRegex) }
            }
        builtInAcceptIf("the string must is $case") { StringUtils.isAsciiPrintable(it) && predicate(it) }
    }

    // TODO: 首尾有空格

    private val allPrefixes = mutableListOf<String>()
    private var prefixIgnoreCase = false
    private val allSuffixes = mutableListOf<String>()
    private var suffixIgnoreCase = false

    /**
     * The string must starts with one of the specific prefixes.
     *
     * @param prefixes the string should be prefixed with any one of the specific prefixes.
     * @param ignoreCase ignore cases for *every* given prefix.
     */
    fun startsWithAny(vararg prefixes: String, ignoreCase: Boolean = false) {
        allPrefixes.addAll(prefixes)
        prefixIgnoreCase = ignoreCase
    }

    /**
     * The string must ends with one of the specific suffixes.
     *
     * @param suffixes the string should be suffixed with any one of the specific suffixes.
     * @param ignoreCase ignore cases for *every* given suffix.
     */
    fun endsWithAny(vararg suffixes: String, ignoreCase: Boolean = false) {
        allSuffixes.addAll(suffixes)
        suffixIgnoreCase = ignoreCase
    }

    private fun integrateTests() {
        if (!isEmptyAccepted)
            builtInAcceptIf("the string must contain content") { it.isNotEmpty() }
        if (!isAllBlankAccepted)
            builtInAcceptIf("the string must consist of characters except for whitespace") { it.isNotBlank() }

        if (expectedList.isNotEmpty())
            builtInAcceptIf("the string must literally equal to one of the given string: ${expectedList.joinToString()}") {
                expectedList.any { expected -> it == expected }
            }

        if (allPrefixes.isNotEmpty())
            builtInAcceptIf("the string must starts with one of the given prefixes: ${allPrefixes.joinToString()}") {
                allPrefixes.any { prefix -> it.startsWith(prefix, prefixIgnoreCase) }
            }

        if (allSuffixes.isNotEmpty())
            builtInAcceptIf("the string must ends with one of the given suffixes: ${allSuffixes.joinToString()}") {
                allSuffixes.any { suffix -> it.endsWith(suffix, suffixIgnoreCase) }
            }
    }

    override fun build(): StringValidationPredicate {
        integrateTests()

        return StringValidationPredicate(
            lengthRange = this.buildRange(),
            required = isRequired,
            nullable = isNullable,
            tests = this.buildLambdaTests(),
            depth = depth
        )
    }

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

sealed class ValidationScopeWithLengthAndTestsProperty<E, T : ValidationPredicate<E>>(
    override val depth: Int,
    config: SiriusValidationConfig
) : ValidationScopeWithLengthProperty<T>(depth, config) {
    private val lambdaTests = arrayListOf<LambdaTest<E>>()

    fun acceptIf(purpose: String = "", lambda: (E) -> Boolean) {
        lambdaTests += LambdaTest(lambda, purpose)
    }

    protected fun builtInAcceptIf(purpose: String, lambda: (E) -> Boolean) {
        lambdaTests += LambdaTest(lambda, purpose, true)
    }

    fun buildLambdaTests() = lambdaTests.toList()
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