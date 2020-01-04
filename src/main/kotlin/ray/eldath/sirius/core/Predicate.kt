package ray.eldath.sirius.core

import org.json.JSONObject
import org.json.JSONTokener
import ray.eldath.sirius.core.Asserts.contain
import ray.eldath.sirius.core.Asserts.equals
import ray.eldath.sirius.core.Asserts.rangeIn
import ray.eldath.sirius.trace.SiriusException
import ray.eldath.sirius.trace.Tracer.JsonObjectCheckpoint
import ray.eldath.sirius.trace.Tracer.JsonObjectCheckpoint.missingRequiredElement
import ray.eldath.sirius.trace.Tracer.JsonObjectCheckpoint.nullPointer
import ray.eldath.sirius.trace.Tracer.JsonObjectCheckpoint.typeMismatch
import ray.eldath.sirius.trace.ValidationException
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.LambdaTest
import ray.eldath.sirius.type.TopPredicate
import ray.eldath.sirius.type.Validatable
import ray.eldath.sirius.util.Util

// overridden values should be prefixed for named argument
class BooleanValidationPredicate(
    override val required: Boolean,
    override val nullable: Boolean,
    override val depth: Int,
    val expected: Boolean,
    val expectedInitialized: Boolean // `expected` is set or not
) : ValidationPredicate<Boolean>(required = required, nullable = nullable, depth = depth) {

    override fun test(value: Boolean): AssertWrapper<Boolean> =
        if (!expectedInitialized) assertsOf(tests)
        else assertsOf(tests, equals("expected", expected = expected, actual = value))
}

class StringValidationPredicate(
    override val required: Boolean,
    override val nullable: Boolean,
    override val tests: List<LambdaTest<String>> = emptyList(),
    override val depth: Int,
    val lengthRange: IntRange = 0..Int.MAX_VALUE
) : ValidationPredicate<String>(required, nullable, tests, depth) {

    override fun test(value: String): AssertWrapper<String> =
        assertsOf(
            tests,
            rangeIn("length", range = lengthRange, value = value.length)
        )

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}

class DecimalValidationPredicate(
    override val required: Boolean,
    override val nullable: Boolean,
    override val tests: List<LambdaTest<Double>>,
    override val depth: Int,
    val valueRange: Range<Double> = 0.0..Double.MAX_VALUE,
    val precisionRange: IntRange = 0..Int.MAX_VALUE,
    val expected: List<Double> = emptyList()
) : ValidationPredicate<Double>(required, nullable, tests, depth) {

    override fun test(value: Double): AssertWrapper<Double> =
        assertsOf(
            tests,
            contain("value", expected, value),
            rangeIn("value", valueRange, value),
            rangeIn("precision", precisionRange, value.toString().split(".")[1].count())
        )
}

class IntegerValidationPredicate(
    override val required: Boolean,
    override val nullable: Boolean,
    override val tests: List<LambdaTest<Long>>,
    override val depth: Int,
    val valueRange: LongRange = 0..Long.MAX_VALUE,
    val digitsRange: IntRange = 0..Int.MAX_VALUE,
    val expected: List<Long> = emptyList()
) : ValidationPredicate<Long>(required, nullable, tests, depth) {

    override fun test(value: Long): AssertWrapper<Long> {
        var int = value
        var digits = 0
        do {
            int /= 10
            digits++
        } while (int > 0)

        return assertsOf(
            tests,
            contain("value", expected, value),
            rangeIn("value", valueRange, value),
            rangeIn("digits", digitsRange, digits)
        )
    }
}

class JsonObjectValidationPredicate(
    override val required: Boolean,
    override val nullable: Boolean,
    override val tests: List<LambdaTest<JSONObject>> = emptyList(),
    override val depth: Int,
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    val children: Map<String, AnyValidationPredicate> = emptyMap(),
    val regexChildren: Map<Regex, AnyValidationPredicate> = emptyMap(),
    val any: JsonObjectValidationScope? = null
) : ValidationPredicate<JSONObject>(required, nullable, tests, depth), TopPredicate<JSONObject> {

    // checkpoint
    override fun final(value: String): Boolean =
        ((JSONTokener(value).nextValue()) as JSONObject).run(::final)

    override fun final(value: JSONObject): Boolean {
        val wrapper = this.test(value)
        testLambdaTests(wrapper.tests, this, "[jsonObject]", value)
        return testAsserts(wrapper.asserts, "[jsonObject]", this)
    }

    // priority: any > tests > asserts
    override fun test(value: JSONObject): AssertWrapper<JSONObject> {
        if (any != null)
            testAnyBlock(value, any.build())
        children.forEach { (key, predicate) -> testChildrenPredicate(value, key, predicate) }
        regexChildren.forEach { (regex, predicate) ->
            value.keySet().filter { regex.matches(it) }.forEach { testChildrenPredicate(value, it, predicate) }
        }

        return assertsOf(tests, rangeIn("length", lengthRange, value.length()))
    }

    private fun testAnyBlock(obj: JSONObject, anyBlock: JsonObjectValidationPredicate) {
        val entries = anyBlock.children.entries
        var counter = 0
        entries.forEach { (key, predicate) ->
            try {
                testChildrenPredicate(obj, key, predicate)
                return // even only one child passed, the whole block passed.
            } catch (e: SiriusException) {
                counter += 1
            }
        }

        if (counter == entries.size)
            throw JsonObjectCheckpoint.any(depth)
    }

    private fun testChildrenPredicate(obj: JSONObject, key: String, predicate: AnyValidationPredicate) {
        val isNull = obj.isNull(key)

        // check existence
        throwIf(!obj.has(key) && predicate.required) { missingRequiredElement(predicate, key, depth) }
        // check nullability
        throwIf(isNull && !predicate.nullable) { nullPointer(predicate, key, depth) }

        if (!isNull) {
            obj.get(key).let {
                // check type
                throwIf(!Validatable.fromPredicate(predicate).actualTypeName.contains(it.javaClass.simpleName)) {
                    typeMismatch(predicate, key, depth, it)
                }
            }

            when (predicate) {
                is JsonObjectValidationPredicate -> testChildrenAsserts(key, obj.getJSONObject(key), predicate)
                is DecimalValidationPredicate -> testChildrenAsserts(key, obj.getDouble(key), predicate)
                is IntegerValidationPredicate -> testChildrenAsserts(key, obj.getLong(key), predicate)
                is StringValidationPredicate -> testChildrenAsserts(key, obj.getString(key), predicate)
                is BooleanValidationPredicate -> testChildrenAsserts(key, obj.getBoolean(key), predicate)
            }
        }
    }

    private inline fun throwIf(condition: Boolean, throws: () -> ValidationException) =
        if (condition) throw throws() else Unit

    private fun <T> testChildrenAsserts(key: String, element: T, predicate: ValidationPredicate<T>): Unit =
        predicate.test(element).run {
            testLambdaTests(tests, predicate, key, element)
            testAsserts(asserts, key, predicate)
        }

    private fun <T> testLambdaTests(
        tests: List<LambdaTest<T>>,
        predicate: ValidationPredicate<T>,
        key: String,
        element: T
    ) =
        tests.forEachIndexed { index, (lambdaTest, purpose, isBuiltIn) ->
            if (!lambdaTest(element))
                throw JsonObjectCheckpoint.lambda(index + 1, predicate, depth, key, purpose, isBuiltIn)
        }.let { true }

    private fun testAsserts(asserts: List<AnyAssert>, key: String, predicate: AnyValidationPredicate) =
        asserts.forEach {
            if (!it.test())
                throw when (it) {
                    is RangeAssert<*> -> JsonObjectCheckpoint.range(it, predicate, depth, key)
                    is ContainAssert -> JsonObjectCheckpoint.contain(it, predicate, depth, key)
                    is EqualAssert -> JsonObjectCheckpoint.equal(it, predicate, depth, key)
                }
        }.let { true }

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}


// left due to sealed class's constraint
sealed class ValidationPredicate<T>(
    open val required: Boolean = false,
    open val nullable: Boolean = false,
    open val tests: List<LambdaTest<T>> = emptyList(),
    //
    open val depth: Int
) {
    internal abstract fun test(value: T): AssertWrapper<T>
}