package ray.eldath.sirius.core

import org.json.JSONObject
import ray.eldath.sirius.core.Constrains.equals
import ray.eldath.sirius.core.Constrains.rangeIn
import ray.eldath.sirius.type.AnyValidationPredicate
import ray.eldath.sirius.type.Predicate
import ray.eldath.sirius.util.ExceptionAssembler.assembleJsonObjectMEE
import ray.eldath.sirius.util.Util

data class StringValidationPredicate(
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    override val required: Boolean,
    override val tests: List<Predicate<String>>,
    override val depth: Int
) : ValidationPredicate<String>(required, tests, depth) {

    override fun test(value: String): List<AnyConstrain> = listOf(rangeIn(range = lengthRange, value = value.length))
    // : tests.all { it.invoke(value) }

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

data class JsonObjectValidationPredicate(
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    val children: Map<String, AnyValidationPredicate> = emptyMap(),
    override val required: Boolean,
    override val tests: List<Predicate<JSONObject>> = emptyList(),
    override val depth: Int
) : ValidationPredicate<JSONObject>(required, tests, depth) {

    override fun test(value: JSONObject): List<AnyConstrain> =
        listOf(rangeIn(lengthRange, value.length())) + testChildren(value, children)

    // : tests.all { it.invoke(value) }

    private fun testChildren(obj: JSONObject, map: Map<String, AnyValidationPredicate>): List<AnyConstrain> {
        val r = arrayListOf<AnyConstrain>()
        for ((key, value) in map.entries) {
            if (!obj.has(key))
                if (value.required)
                    throw assembleJsonObjectMEE(value, key, depth)
                else continue
            else
                r += when (value) {
                    is StringValidationPredicate -> value.test(obj.getString(key))
                    is JsonObjectValidationPredicate -> value.test(obj.getJSONObject(key))
                    is BooleanValidationPredicate -> value.test(obj.getBoolean(key))
                }
        }
        return r
    }

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}

// left due to sealed class's constrain
sealed class ValidationPredicate<T>(
    open val required: Boolean = false,
    open val tests: List<Predicate<T>> = emptyList(),
    open val depth: Int
) {
    abstract fun test(value: T): List<AnyConstrain>
}