package ray.eldath.sirius.core

import org.json.JSONObject
import ray.eldath.sirius.core.Constrains.equals
import ray.eldath.sirius.core.Constrains.rangeIn
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.Predicate
import ray.eldath.sirius.util.ExceptionAssembler.VFEAssembler
import ray.eldath.sirius.util.ExceptionAssembler.assembleJsonObjectMEE
import ray.eldath.sirius.util.Util

data class StringValidationPredicate(
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    override val required: Boolean,
    override val tests: List<Predicate<String>>,
    override val depth: Int
) : ValidationPredicate<String>(required, tests, depth) {

    override fun test(value: String): List<AnyConstrain> = listOf(rangeIn(range = lengthRange, value = value.length))
    // TODO: tests.all { it.invoke(value) }

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}

data class BooleanValidationPredicate(
    val expected: Boolean,
    override val required: Boolean,
    override val depth: Int
) : ValidationPredicate<Boolean>(required = required, depth = depth) {

    override fun test(value: Boolean): List<AnyConstrain> =
        listOf(equals(expected = expected, actual = value))
}

open class JsonObjectValidationPredicate(
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    val children: Map<String, AnyValidationPredicate> = emptyMap(),
    override val required: Boolean,
    override val tests: List<Predicate<JSONObject>> = emptyList(),
    override val depth: Int
) : ValidationPredicate<JSONObject>(required, tests, depth) {

    // checkpoint
    override fun test(value: JSONObject): List<AnyConstrain> {
        testChildren(value, children)
        return listOf(rangeIn(lengthRange, value.length()))
    }

    // TODO: tests.all { it.invoke(value) }

    private fun testChildren(obj: JSONObject, map: Map<String, AnyValidationPredicate>) {
        for ((key, value) in map.entries) {
            if (!obj.has(key))
                if (value.required)
                    throw assembleJsonObjectMEE(value, key, depth)
                else continue
            else
                when (value) {
                    is StringValidationPredicate ->
                        value.test(obj.getString(key)).forEach { testChildrenEach(it, key, value) }
                    is JsonObjectValidationPredicate ->
                        value.test(obj.getJSONObject(key)).forEach { testChildrenEach(it, key, value) }
                    is BooleanValidationPredicate ->
                        value.test(obj.getBoolean(key)).forEach { testChildrenEach(it, key, value) }
                }
        }
    }

    private fun testChildrenEach(constrain: AnyConstrain, key: String, element: AnyValidationPredicate) {
        if (!constrain.test())
            throw when (constrain) {
                is LambdaConstrain<*> -> VFEAssembler.lambda(constrain, element, key, depth)
                is RangeConstrain<*> -> VFEAssembler.range(constrain, element, key, depth)
                is ContainConstrain -> VFEAssembler.contain(constrain, element, key, depth)
                is EqualConstrain -> VFEAssembler.equal(constrain, element, key, depth)
            }
    }

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}

// left due to sealed class's constrain
sealed class ValidationPredicate<T>(
    open val required: Boolean = false,
    open val tests: List<Predicate<T>> = emptyList(),
    open val depth: Int
) {
    internal abstract fun test(value: T): List<AnyConstrain>
}