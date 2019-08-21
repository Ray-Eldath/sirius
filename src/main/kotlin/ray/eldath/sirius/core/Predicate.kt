package ray.eldath.sirius.core

import org.json.JSONObject
import ray.eldath.sirius.type.BaseValidationPredicate
import ray.eldath.sirius.type.Predicate
import ray.eldath.sirius.util.ExceptionAssembler
import ray.eldath.sirius.util.Util

data class StringValidationPredicate(
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    override val required: Boolean,
    override val tests: List<Predicate<String>>,
    override val depth: Int
) : ValidationPredicate<String>(required, tests, depth) {

    override fun test(value: String): Boolean =
        tests.all { it.invoke(value) } &&
                value.length in lengthRange

    override fun toString(): String = Util.reflectionToStringWithStyle(this)
}

data class JsonObjectValidationPredicate(
    val lengthRange: IntRange = 0..Int.MAX_VALUE,
    val children: Map<String, BaseValidationPredicate> = emptyMap(),
    override val required: Boolean,
    override val tests: List<Predicate<JSONObject>> = emptyList(),
    override val depth: Int
) : ValidationPredicate<JSONObject>(required, tests, depth) {

    override fun test(value: JSONObject): Boolean =
        tests.all { it.invoke(value) } &&
                value.length() in lengthRange &&
                testChildren(value, children)

    private fun testChildren(obj: JSONObject, map: Map<String, BaseValidationPredicate>): Boolean =
        map.all { (key, value) ->
            if (!obj.has(key))
                if (value.required) throw ExceptionAssembler.assembleJsonObjectMEE(
                    value,
                    key,
                    depth
                ) else true
            else
                when (value) {
                    is StringValidationPredicate -> value.test(obj.getString(key))
                    is JsonObjectValidationPredicate -> value.test(obj.getJSONObject(key))
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
    abstract fun test(value: T): Boolean
}