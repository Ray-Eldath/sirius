package ray.eldath.sirius.core

import ray.eldath.sirius.config.SiriusValidationConfig
import ray.eldath.sirius.type.LambdaTest
import ray.eldath.sirius.util.ExceptionAssembler

object PredicateBuildInterceptor {
    internal inline fun <E, reified T : ValidationScope<out E>> jsonObjectIntercept(
        initializer: T.() -> Unit,
        key: String,
        depth: Int,
        config: SiriusValidationConfig
    ): E {
        val d = depth + 1
        val inst = T::class.java
            .getDeclaredConstructor(Int::class.java, SiriusValidationConfig::class.java)
            .newInstance(d, config)

        val applied = inst.apply(initializer)

        inst.isAssertsValid().filterKeys { !it }.map { it.value }.firstOrNull()?.let {
            throw ExceptionAssembler.jsonObjectInvalidAssert(scope = inst, key = key, depth = d, reason = it)
        }
        return applied.build()
    }
}

internal fun <T> assertsOf(tests: List<LambdaTest<T>> = emptyList(), vararg asserts: AnyAssert) =
    AssertWrapper(asserts.toList(), tests)