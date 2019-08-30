package ray.eldath.sirius.core

import ray.eldath.sirius.config.SiriusValidationConfig
import ray.eldath.sirius.type.Predicate
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

        if (!inst.isAssertsValid())
            throw ExceptionAssembler.assembleJsonObjectIAE(scope = inst, key = key, depth = d)
        return inst.apply(initializer).build()
    }
}

internal fun <T> assertsOf(tests: List<Predicate<T>> = emptyList(), vararg asserts: AnyAssert) =
    AssertWrapper(asserts.toList(), tests)