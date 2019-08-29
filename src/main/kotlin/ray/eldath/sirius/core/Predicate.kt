package ray.eldath.sirius.core

import org.json.JSONObject
import ray.eldath.sirius.core.Asserts.equals
import ray.eldath.sirius.core.Asserts.rangeIn
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.Predicate
import ray.eldath.sirius.util.ExceptionAssembler.VFEAssembler
import ray.eldath.sirius.util.ExceptionAssembler.assembleJsonObjectMEE
import ray.eldath.sirius.util.Util

// overridden values should be prefixed for named argument
data class StringValidationPredicate(
    override val required: Boolean,
    override val tests: List<Predicate<String>>,
    override val depth: Int,
    val lengthRange: IntRange = 0..Int.MAX_VALUE
) : ValidationPredicate<String>(required, tests, depth) {

    override fun test(value: String): AssertWrapper<String> =
        assertsOf(tests, rangeIn(range = lengthRange, value = value.length))

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}

data class BooleanValidationPredicate(
    override val required: Boolean,
    override val depth: Int,
    val expected: Boolean
) : ValidationPredicate<Boolean>(required = required, depth = depth) {

    override fun test(value: Boolean): AssertWrapper<Boolean> =
        assertsOf(tests, equals(expected = expected, actual = value))
}

data class JsonObjectValidationPredicate(
    override val required: Boolean,
    override val tests: List<Predicate<JSONObject>> = emptyList(),
    override val depth: Int,
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    val children: Map<String, AnyValidationPredicate> = emptyMap()
) : ValidationPredicate<JSONObject>(required, tests, depth) {

    // checkpoint
    override fun test(value: JSONObject): AssertWrapper<JSONObject> {
        testChildren(value, children)
        return assertsOf(tests, rangeIn(lengthRange, value.length()))
    }

    private fun testChildren(obj: JSONObject, map: Map<String, AnyValidationPredicate>) {
        for ((key, predicate) in map.entries) {
            if (!obj.has(key))
                if (predicate.required)
                    throw assembleJsonObjectMEE(predicate, key, depth)
                else continue
            else {
                when (predicate) {
                    is StringValidationPredicate -> testChildrenConstrains(key, obj.getString(key), predicate)
                    is JsonObjectValidationPredicate -> testChildrenConstrains(key, obj.getJSONObject(key), predicate)
                    is BooleanValidationPredicate -> testChildrenConstrains(key, obj.getBoolean(key), predicate)
                }
            }
        }
    }

    private fun <T> testChildrenConstrains(key: String, element: T, predicate: ValidationPredicate<T>): Unit =
        predicate.test(element).run {
            tests.forEachIndexed { index, test ->
                if (!test(element))
                    throw VFEAssembler.lambda(index + 1, predicate, key, depth)
            }
            asserts.forEach { testConstrain(it, key, predicate) }
        }

    private fun testConstrain(assert: AnyAssert, key: String, predicate: AnyValidationPredicate) {
        if (!assert.test())
            throw when (assert) {
                is RangeAssert<*> -> VFEAssembler.range(assert, predicate, key, depth)
                is ContainAssert -> VFEAssembler.contain(assert, predicate, key, depth)
                is EqualAssert -> VFEAssembler.equal(assert, predicate, key, depth)
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