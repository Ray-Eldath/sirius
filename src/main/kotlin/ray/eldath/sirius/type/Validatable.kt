package ray.eldath.sirius.type

import org.json.JSONObject
import kotlin.reflect.KClass

open class Validatable(open val type: ValidatableType)

enum class ValidatableType(
    val displayName: String,
    val actualType: KClass<*>,
    vararg val actualTypeName: String = arrayOf(actualType.javaObjectType.simpleName)
) {
    JSON_OBJECT("JsonObject", JSONObject::class),
    // TODO: JSON_ARRAY("JsonArray", JSONArray::class),
    STRING("string", String::class),
    DECIMAL("decimal", Double::class, "Double", "Float"),
    INTEGER("integer", Long::class, "Integer", "Long"),
    BOOLEAN("boolean", Boolean::class)
}