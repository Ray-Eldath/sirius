@file:Suppress("MemberVisibilityCanBePrivate")

package ray.eldath.sirius.core

import ray.eldath.sirius.type.Predicate

internal object Asserts {
    internal fun <T : Comparable<T>> range(propertyName: String, bigger: Range<T>, value: Range<T>) =
        RangeAssert(bigger, value, propertyName)

    internal fun <T : Comparable<T>> rangeIn(propertyName: String, range: Range<T>, value: T): AnyAssert =
        if (range.start == range.endInclusive)
            equals(propertyName, range.start, value)
        else
            RangeAssert(range, value..value, propertyName)

    internal fun <T> contain(propertyName: String, container: Iterable<T>, element: T) =
        ContainAssert(element, container, propertyName)

    internal fun <T : Comparable<T>> equals(propertyName: String, expected: T, actual: T) =
        EqualAssert(expected, actual, propertyName)
}

internal class ContainAssert<T>(val element: T, val container: Iterable<T>, override val propertyName: String) :
    Assert<T>(propertyName) {

    override fun test(): Boolean = if (container.none()) true else element in container
}

internal class RangeAssert<T : Comparable<T>>(
    val bigger: Range<T>,
    val actual: Range<T>,
    override val propertyName: String
) : Assert<ClosedRange<T>>(propertyName) {

    override fun test(): Boolean = bigger.start <= actual.start && bigger.endInclusive >= actual.endInclusive
}

internal class EqualAssert<T : Comparable<T>>(val expected: T, val actual: T, override val propertyName: String) :
    Assert<T>(propertyName) {

    override fun test(): Boolean = expected == actual
}

internal class AssertWrapper<T>(
    val asserts: List<AnyAssert> = emptyList(),
    val tests: List<Predicate<T>> = emptyList()
)
// may be not as a assert?
// I wrote this due to I was too sleepy at that time. Apologize.

// internal class LambdaAssert<T>(val lambda: Predicate<T>, val input: T) : Assert<Predicate<T>>(lambda, null) {
//     override fun test(): Boolean = lambda.invoke(input)
// }

sealed class Assert<T>(open val propertyName: String) {
    abstract fun test(): Boolean
}

internal typealias AnyAssert = Assert<*>
internal typealias Range<T> = ClosedRange<T>