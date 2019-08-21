@file:Suppress("MemberVisibilityCanBePrivate")

package ray.eldath.sirius.core

class JsonObjectValidationScope(private val depth: Int) : RequireOption(),
    ValidationScope {
    private val children = mutableMapOf<String, BaseValidationPredicate>()

    var length = 0..Int.MAX_VALUE

    infix fun String.string(block: StringValidationScope.() -> Unit) {
        children += this to StringValidationScope(depth + 1).apply(block).build()
    }

    infix fun String.jsonObject(block: JsonObjectValidationScope.() -> Unit) {
        children += this to JsonObjectValidationScope(depth + 1).apply(block).build()
    }

    fun build(): JsonObjectValidationPredicate =
        JsonObjectValidationPredicate(
            children = this.children,
            lengthRange = this.length,
            required = this.isRequired,
            depth = depth
        )
}

class StringValidationScope(private val depth: Int) : RequireOption(),
    ValidationScope {
    private val tests = mutableListOf<Predicate<String>>()

    var length = 0..Int.MAX_VALUE

    var minLength = 0
    var maxLength = Int.MAX_VALUE

    fun test(predicate: Predicate<String>) {
        tests += predicate
    }

    fun build(): StringValidationPredicate {
        val lengthRange =
            if (length != 0..Int.MAX_VALUE)
                length
            else if (
                minLength != 0 && maxLength == Int.MAX_VALUE ||
                minLength == 0 && maxLength != Int.MAX_VALUE
            )
                minLength..maxLength
            else
                0..Int.MAX_VALUE
        return StringValidationPredicate(
            required = this.isRequired,
            lengthRange = lengthRange,
            tests = this.tests,
            depth = depth
        )
    }
}