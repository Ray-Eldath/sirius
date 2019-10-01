package ray.eldath.sirius.core

import org.json.JSONObject
import org.json.JSONTokener
import ray.eldath.sirius.core.Asserts.contain
import ray.eldath.sirius.core.Asserts.equals
import ray.eldath.sirius.core.Asserts.rangeIn
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.Predicate
import ray.eldath.sirius.type.TopPredicate
import ray.eldath.sirius.type.Validatable
import ray.eldath.sirius.util.ExceptionAssembler.IVEAssembler
import ray.eldath.sirius.util.ExceptionAssembler.jsonObjectMissingRequiredElement
import ray.eldath.sirius.util.ExceptionAssembler.jsonObjectNPE
import ray.eldath.sirius.util.ExceptionAssembler.jsonObjectTypeMismatch
import ray.eldath.sirius.util.SiriusException
import ray.eldath.sirius.util.Util
import ray.eldath.sirius.util.ValidationException

// overridden values should be prefixed for named argument
class StringValidationPredicate(
    override val required: Boolean,
    override val nullable: Boolean,
    override val tests: List<Predicate<String>>,
    override val depth: Int,
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    val expectedValue: List<String>
) : ValidationPredicate<String>(required, nullable, tests, depth) {

    override fun test(value: String): AssertWrapper<String> =
        assertsOf(
            tests,
            rangeIn("length", range = lengthRange, value = value.length),
            contain("value", container = expectedValue, element = value)
        )

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}

class BooleanValidationPredicate(
    override val required: Boolean,
    override val nullable: Boolean,
    override val depth: Int,
    val expected: Boolean
) : ValidationPredicate<Boolean>(required = required, nullable = nullable, depth = depth) {

    override fun test(value: Boolean): AssertWrapper<Boolean> =
        assertsOf(tests, equals("expected", expected = expected, actual = value))
}

class JsonObjectValidationPredicate(
    override val required: Boolean,
    override val nullable: Boolean,
    override val tests: List<Predicate<JSONObject>> = emptyList(),
    override val depth: Int,
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    val children: Map<String, AnyValidationPredicate> = emptyMap(),
    val any: JsonObjectValidationScope? = null
) : ValidationPredicate<JSONObject>(required, nullable, tests, depth), TopPredicate<JSONObject> {

    // checkpoint
    override fun final(value: String): Boolean =
        ((JSONTokener(value).nextValue()) as JSONObject).run(::final)

    override fun final(value: JSONObject): Boolean {
        val wrapper = this.test(value)
        testTests(wrapper.tests, this, "[jsonObject]", value)
        return testAsserts(wrapper.asserts, "[jsonObject]", this)
    }

    // priority: any > tests > asserts
    override fun test(value: JSONObject): AssertWrapper<JSONObject> {
        if (any != null)
            testAnyBlock(value, any.build())
        children.entries.forEach { testChildrenPredicate(value, it) }
        return assertsOf(tests, rangeIn("length", lengthRange, value.length()))
    }

    private fun testAnyBlock(obj: JSONObject, anyBlock: JsonObjectValidationPredicate) {
        val entries = anyBlock.children.entries
        var counter = 0
        entries.forEach {
            try {
                testChildrenPredicate(obj, it)
            } catch (e: SiriusException) {
                counter += 1
            }
        }

        if (counter == entries.size)
            throw IVEAssembler.anyBlock(depth)
    }

    private fun Any.typeName() = this.javaClass.name
    private fun AnyValidationPredicate.actualTypeName() = Validatable.fromPredicate(this).actualType.javaObjectType.name

    private fun testChildrenPredicate(obj: JSONObject, entry: Map.Entry<String, AnyValidationPredicate>) {
        val key = entry.key
        val predicate = entry.value
        val isNull = obj.isNull(key)

        // check existence
        throwIf(!obj.has(key) && predicate.required) { jsonObjectMissingRequiredElement(predicate, key, depth) }
        // check nullability
        throwIf(isNull && !predicate.nullable) { jsonObjectNPE(predicate, key, depth) }
        // check type
        obj.get(key).let {
            throwIf(it.typeName() != predicate.actualTypeName()) { jsonObjectTypeMismatch(predicate, key, depth, it) }
        }
        if (!isNull)
            when (predicate) {
                is StringValidationPredicate -> testChildrenAsserts(key, obj.getString(key), predicate)
                is JsonObjectValidationPredicate -> testChildrenAsserts(key, obj.getJSONObject(key), predicate)
                is BooleanValidationPredicate -> testChildrenAsserts(key, obj.getBoolean(key), predicate)
            }
    }

    private inline fun throwIf(condition: Boolean, throws: () -> ValidationException) =
        if (condition) throw throws() else Unit

    private fun <T> testChildrenAsserts(key: String, element: T, predicate: ValidationPredicate<T>): Unit =
        predicate.test(element).run {
            testTests(tests, predicate, key, element)
            testAsserts(asserts, key, predicate)
        }

    private fun <T> testTests(tests: List<Predicate<T>>, predicate: ValidationPredicate<T>, key: String, element: T) =
        tests.forEachIndexed { index, test ->
            if (!test(element))
                throw IVEAssembler.lambda(index + 1, predicate, depth, key)
        }.let { true }

    private fun testAsserts(asserts: List<AnyAssert>, key: String, predicate: AnyValidationPredicate) =
        asserts.forEach {
            if (!it.test())
                throw when (it) {
                    is RangeAssert<*> -> IVEAssembler.range(it, predicate, depth, key)
                    is ContainAssert -> IVEAssembler.contain(it, predicate, depth, key)
                    is EqualAssert -> IVEAssembler.equal(it, predicate, depth, key)
                }
        }.let { true }

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}


// left due to sealed class's constraint
sealed class ValidationPredicate<T>(
    open val required: Boolean = false,
    open val nullable: Boolean = false,
    open val tests: List<Predicate<T>> = emptyList(),
    open val depth: Int
) {
    internal abstract fun test(value: T): AssertWrapper<T>
}