package ray.eldath.sirius.core

import org.apache.commons.lang3.CharUtils
import org.apache.commons.lang3.StringUtils
import org.intellij.lang.annotations.Language
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
import ray.eldath.sirius.util.StringContentPattern
import ray.eldath.sirius.util.StringContentPattern.*

private const val max = Int.MAX_VALUE
private val maxRange = 0..max

class JsonObjectValidationScope(override val depth: Int, private val config: SiriusValidationConfig) :
    ValidationScopeWithLengthAndTestsProperty<JSONObject, JsonObjectValidationPredicate>(depth, config) {

    private val children = hashMapOf<String, AnyValidationPredicate>()
    private val regexChildren = hashMapOf<Regex, AnyValidationPredicate>()
    private var _any: JsonObjectValidationScope? = null

    infix fun String.jsonObject(block: JsonObjectValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth, config = config)
    }

    infix fun String.string(block: StringValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth, config = config)
    }

    infix fun String.integer(block: IntegerValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth, config = config)
    }

    /**
     * For convenience. This will take `required` and `nullable` from [SiriusValidationConfig].
     */
    infix fun String.boolean(excepted: Boolean) {
        boolean { expected = excepted }
    }

    infix fun String.boolean(block: BooleanValidationScope.() -> Unit) {
        children += this to jsonObjectIntercept(block, key = this, depth = depth, config = config)
    }

    //

    /**
     * Two ways to denote a regex key matching pattern:
     *  - Any string prefixed with `+`.
     *  - Construct `Regex` explicitly. (exp. `Regex("...") string { ... }`)
     */
    operator fun @receiver: Language("RegExp") String.unaryPlus() = Regex(this)

    infix fun Regex.jsonObject(block: JsonObjectValidationScope.() -> Unit) {
        regexChildren += this to jsonObjectIntercept(block, key = this.toString(), depth = depth, config = config)
    }

    infix fun Regex.string(block: StringValidationScope.() -> Unit) {
        regexChildren += this to jsonObjectIntercept(block, key = this.toString(), depth = depth, config = config)
    }

    infix fun Regex.integer(block: IntegerValidationScope.() -> Unit) {
        regexChildren += this to jsonObjectIntercept(block, key = this.toString(), depth = depth, config = config)
    }

    infix fun Regex.boolean(excepted: Boolean) {
        boolean { expected = excepted }
    }

    infix fun Regex.boolean(block: BooleanValidationScope.() -> Unit) {
        regexChildren += this to jsonObjectIntercept(block, key = this.toString(), depth = depth, config = config)
    }

    fun any(block: JsonObjectValidationScope.() -> Unit) {
        if (_any != null)
            throw ExceptionAssembler.multipleAnyBlock(this, depth)
        else {
            _any = JsonObjectValidationScope(depth + 1, config)
            _any?.apply(block)
        }
    }

    /**
     * Set a regex pattern that all keys of this JsonObject should match.
     *
     * @param regex the regex pattern
     */
    fun requireKeyPattern(regex: Regex) {
        builtInAcceptIf("all keys in the JsonObject should match the given regex $regex") {
            it.keySet().all { key -> regex.matches(key) }
        }
    }

    override fun build(): JsonObjectValidationPredicate =
        JsonObjectValidationPredicate(
            tests = this.buildLambdaTests(),
            children = children,
            regexChildren = regexChildren,
            lengthRange = this.buildLengthRange(), // inherit
            required = isRequired,
            nullable = isNullable,
            depth = depth,
            any = _any
        )

    override fun isAssertsValid(): Map<Boolean, String> = mapOf(isRangeValid())
}

class BooleanValidationScope(override val depth: Int, config: SiriusValidationConfig) :
    ValidationScope<BooleanValidationPredicate>(depth, config) {

    var expected = false
        set(value) {
            expectedInitialized = true
            field = value
        }

    private var expectedInitialized = false

    override fun build() = BooleanValidationPredicate(isRequired, isNullable, depth, expected, expectedInitialized)
}

class IntegerValidationScope(override val depth: Int, config: SiriusValidationConfig) :
    ValidationScopeWithLengthAndTestsProperty<Long, IntegerValidationPredicate>(depth, config) {

    var min = 0
    var max = Long.MAX_VALUE

    private val expectedList = mutableListOf<Long>()

    /**
     * As in [ray.eldath.sirius.core.StringValidationScope.expected], this function will add all
     * provided integer to the expected list, excepts when only one integer provided in an invocation
     * at any time. In that case, the only provided integer will become the only one expected.
     *
     * @param expected list of expected value
     */
    fun expected(vararg expected: Long) {
        if (expected.size == 1)
            expectedList.clear()
        expectedList += expected.toTypedArray()
    }

    override fun build(): IntegerValidationPredicate =
        IntegerValidationPredicate(
            isRequired,
            isNullable,
            buildLambdaTests(),
            depth,
            min..max,
            buildLengthRange(),
            expectedList
        )
}

class StringValidationScope(override val depth: Int, private val config: SiriusValidationConfig) :
    ValidationScopeWithLengthAndTestsProperty<String, StringValidationPredicate>(depth, config) {

    val nonEmpty: Unit
        get() {
            builtInAcceptIf("the string must contain content") { it.isNotEmpty() }
        }

    val nonBlank: Unit
        get() {
            builtInAcceptIf("the string must consist of characters except for whitespace") { it.isNotBlank() }
        }

    val noWhitespaceSurrounded: Unit
        get() {
            builtInAcceptIf("the string cannot have leading or trailing whitespace") { it.trim() == it }
        }

    private val expectedList = mutableListOf<String>()
    private var expectedIgnoreCase = false

    /**
     * The string should literally equal to one of the given strings. If this function is called
     * more than once, all given string will be treated as the expected strings, **excepts** when
     * only one string provided in an invocation at any time. In that case, the only provided
     * string will become the only one expected.
     *
     * That is to say, if you define a scope:
     *
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

    fun matches(@Language(value = "RegExp") regex: String) {
        builtInAcceptIf("the string must matches the given regex $regex") { it.matches(Regex(regex)) }
    }

    private val contentRequirements = hashSetOf<StringContentPattern>()

    /**
     * Require the string consists of the given types of chars.
     * *Any* passed requirement will induce the string passed.
     *
     * @param contentPattern the types of chars. See [StringContentPattern]
     * @see [StringContentPattern]
     */
    fun requireContent(vararg contentPattern: StringContentPattern) {
        contentRequirements.addAll(contentPattern)
    }

    private fun integrateContentRequirements() {
        val handlers = contentRequirements.map<StringContentPattern, (Char) -> Boolean> {
            // KotlinFrontEndException thrown if use unbound MH here: KT-25585
            when (it) {
                ALPHA -> { char -> CharUtils.isAsciiAlpha(char) }
                SPACE -> { char -> char == ' ' }
                NUMBER -> { char -> CharUtils.isAsciiNumeric(char) }
                ASCII -> { char -> CharUtils.isAscii(char) }
                NON_ASCII -> { char -> !CharUtils.isAscii(char) }
            }
        }

        builtInAcceptIf("all chars of the string must is one of the following type: $contentRequirements") { str ->
            str.all { char ->
                handlers.any { handler -> handler(char) }
            }
        }
    }

    /**
     * Require the string is the given case pattern.
     *
     * @param case the target case pattern. See [StringCase]
     * @see [StringCase]
     */
    fun requireCase(case: StringCase) {
        val predicate: (String) -> Boolean =
            when (case) {
                LOWER_CASE -> { str -> StringUtils.isAllLowerCase(str) }
                UPPER_CASE -> { str -> StringUtils.isAllUpperCase(str) }
                PASCAL_CASE -> { str -> str.matches(StringCase.pascalCaseRegex) }
                CAMEL_CASE -> { str -> str.matches(StringCase.camelCaseRegex) }
                SNAKE_CASE ->
                    { str -> StringUtils.isMixedCase(str) && str.matches(StringCase.snakeCaseRegex) }
                SCREAMING_SNAKE_CASE ->
                    { str -> StringUtils.isMixedCase(str) && str.matches(StringCase.screamingSnakeCaseRegex) }
            }
        builtInAcceptIf("the string must is $case") { StringUtils.isAsciiPrintable(it) && predicate(it) }
    }

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
        if (config.stringNonBlankByDefault) nonBlank
        if (config.stringNonEmptyByDefault) nonEmpty

        if (contentRequirements.isNotEmpty()) integrateContentRequirements()

        if (expectedList.isNotEmpty())
            builtInAcceptIf("the string must literally equal to one of the given string: $expectedList") {
                expectedList.any { expected -> it == expected }
            }

        if (allPrefixes.isNotEmpty())
            builtInAcceptIf("the string must starts with one of the given prefixes: $allPrefixes") {
                allPrefixes.any { prefix -> it.startsWith(prefix, prefixIgnoreCase) }
            }

        if (allSuffixes.isNotEmpty())
            builtInAcceptIf("the string must ends with one of the given suffixes: $allSuffixes") {
                allSuffixes.any { suffix -> it.endsWith(suffix, suffixIgnoreCase) }
            }
    }

    override fun build(): StringValidationPredicate {
        integrateTests()

        return StringValidationPredicate(
            lengthRange = this.buildLengthRange(),
            required = isRequired,
            nullable = isNullable,
            tests = this.buildLambdaTests(),
            depth = depth
        )
    }

    /**
     * It is unreasonable to require a string that only consists of ASCII chars
     * while requiring it only consists of non-ASCII chars.
     */
    override fun isAssertsValid(): Map<Boolean, String> = mapOf(
        isRangeValid(),
        Pair(
            !(contentRequirements.containsAll(listOf(ASCII, NON_ASCII))),
            "requiring a string that only consists of ASCII chars while requiring it only consists of non-ASCII chars is unreasonable."
        )
    )
}

private operator fun <E : Comparable<E>, T : ClosedRange<E>> T.contains(smaller: T): Boolean =
    start <= smaller.start && endInclusive >= smaller.endInclusive


// left due to sealed class's constraint
@TopClassValidationScopeMarker
sealed class ValidationScope<T : AnyValidationPredicate>(open val depth: Int, config: SiriusValidationConfig) :
    BasicOption(config.requiredByDefault, config.nullableByDefault) {

    internal abstract fun build(): T
    internal open fun isAssertsValid(): Map<Boolean, String> = mapOf(true to "")
}

sealed class ValidationScopeWithLengthAndTestsProperty<E, T : ValidationPredicate<E>>(
    override val depth: Int,
    config: SiriusValidationConfig
) : ValidationScopeWithLengthProperty<T>(depth, config) {
    private val lambdaTests = arrayListOf<LambdaTest<E>>()

    /**
     * If the predicate returns `true` for a given validatable element, the element
     * will be accept, or it will be reject by throwing a [ray.eldath.sirius.util.InvalidValueException]
     *
     * @param purpose (optional) the purpose of this lambda test, will be contained
     *                  in the exception if the predicate returns `false`.
     * @param lambda the predicate.
     */
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

    /**
     * Derive a range from [lengthExact], [lengthRange], [minLength] and [maxLength] with
     * the following priority:
     *
     *   1. If [lengthExact] is set, the target length is [lengthExact].
     *   2. If [lengthRange] is set, the target length range is [lengthRange].
     *   3. Otherwise, the target length range is from [minLength] to [maxLength].
     *      Note that there are default values for both two.
     *
     * @sample buildLengthRange
     */
    protected fun buildLengthRange(): IntRange =
        when {
            lengthExact != 0 -> lengthExact..lengthExact
            lengthRange != maxRange -> lengthRange
            else -> minLength..maxLength
        }

    protected fun isRangeValid(): Pair<Boolean, String> {
        val r = (lengthExact in maxRange && minLength..maxLength in maxRange && lengthRange in maxRange)
        return r to "length range must contained in $maxRange"
    }
}