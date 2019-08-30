package ray.eldath.sirius.core

import org.json.JSONObject
import ray.eldath.sirius.core.Asserts.contain
import ray.eldath.sirius.core.Asserts.equals
import ray.eldath.sirius.core.Asserts.rangeIn
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.Predicate
import ray.eldath.sirius.type.TopPredicate
import ray.eldath.sirius.util.ExceptionAssembler.VFEAssembler
import ray.eldath.sirius.util.ExceptionAssembler.assembleJsonObjectMEE
import ray.eldath.sirius.util.SiriusValidationException
import ray.eldath.sirius.util.Util

// overridden values should be prefixed for named argument
class StringValidationPredicate(
    override val required: Boolean,
    override val tests: List<Predicate<String>>,
    override val depth: Int,
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    val expectedValue: List<String>
) : ValidationPredicate<String>(required, tests, depth) {

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
    override val depth: Int,
    val expected: Boolean
) : ValidationPredicate<Boolean>(required = required, depth = depth) {

    override fun test(value: Boolean): AssertWrapper<Boolean> =
        assertsOf(tests, equals("expected", expected = expected, actual = value))
}

class JsonObjectValidationPredicate(
    override val required: Boolean,
    override val tests: List<Predicate<JSONObject>> = emptyList(),
    override val depth: Int,
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    val children: Map<String, AnyValidationPredicate> = emptyMap(),
    val any: JsonObjectValidationScope? = null
) : ValidationPredicate<JSONObject>(required, tests, depth), TopPredicate<JSONObject> {

    // checkpoint
    override fun final(value: JSONObject): Boolean {
        val wrapper = this.test(value)
        testAsserts(wrapper.asserts, "[root]", this)
        return testTests(wrapper.tests, this, "[root]", value)
    }

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
            } catch (e: SiriusValidationException) {
                counter += 1
            }
        }

        if (counter == entries.size)
            throw VFEAssembler.anyBlock(depth)
    }

    private fun testChildrenPredicate(obj: JSONObject, entry: Map.Entry<String, AnyValidationPredicate>) {
        val key = entry.key
        val predicate = entry.value

        if (!obj.has(key))
            if (predicate.required)
                throw assembleJsonObjectMEE(predicate, key, depth)
            else return
        else
            when (predicate) {
                is StringValidationPredicate -> testChildrenAsserts(key, obj.getString(key), predicate)
                is JsonObjectValidationPredicate -> testChildrenAsserts(key, obj.getJSONObject(key), predicate)
                is BooleanValidationPredicate -> testChildrenAsserts(key, obj.getBoolean(key), predicate)
            }
    }

    private fun <T> testChildrenAsserts(key: String, element: T, predicate: ValidationPredicate<T>): Unit =
        predicate.test(element).run {
            testTests(tests, predicate, key, element)
            testAsserts(asserts, key, predicate)
        }

    private fun <T> testTests(tests: List<Predicate<T>>, predicate: ValidationPredicate<T>, key: String, element: T) =
        tests.forEachIndexed { index, test ->
            if (!test(element))
                throw VFEAssembler.lambda(index + 1, predicate, "[test block]", key, depth)
        }.let { true }

    private fun testAsserts(asserts: List<AnyAssert>, key: String, predicate: AnyValidationPredicate): Unit =
        asserts.forEach {
            if (!it.test())
                throw when (it) {
                    is RangeAssert<*> -> VFEAssembler.range(it, predicate, it.propertyName, key, depth)
                    is ContainAssert -> VFEAssembler.contain(it, predicate, it.propertyName, key, depth)
                    is EqualAssert -> VFEAssembler.equal(it, predicate, it.propertyName, key, depth)
                }
        }

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}


// left due to sealed class's constraint
sealed class ValidationPredicate<T>(
    open val required: Boolean = false,
    open val tests: List<Predicate<T>> = emptyList(),
    open val depth: Int
) {
    internal abstract fun test(value: T): AssertWrapper<T>
}