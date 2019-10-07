package ray.eldath.sirius.util

/**
 * Patterns that describe case of a string.
 *
 * If the string contains one non-ASCII char, the test about the case *will fail directly*.
 */
enum class StringCase {
    /**
     * **all** chars are lower case letters.
     *
     * @see [org.apache.commons.lang3.StringUtils.isAllLowerCase]
     */
    LOWER_CASE,
    /**
     * **all** chars are upper case letters.
     *
     * @see [org.apache.commons.lang3.StringUtils.isAllUpperCase]
     */
    UPPER_CASE,
    /**
     * all chars are letters, and are pascal case, like `PascalCase`, `PAscalCase`.
     * *Note that `TeX` isn't pascal case while `TEx` is*.
     */
    PASCAL_CASE,
    /**
     * all chars are letters, and are camel case, like `camelCase`, `camelCAse`.
     * *Note that `testE` isn't camel case while `testEx` is*.
     */
    CAMEL_CASE,
    /**
     * all chars are letters, and are snake case, like `snake_case`. *Note that
     * `phosphorus_` and `phosphorus` isn't snake case while `phosphorus_ray` is*.
     *
     * See [What is Snake Case?](https://en.toolpage.org/tool/snakecase)
     */
    SNAKE_CASE,
    /**
     * all chars are letters and are snake case, but uses capital letters only, like
     * `SCREAMING_SNAKE_CASE`. *Note that `PHOSPHORUS_` and `PHOSPHORUS` isn't
     * screaming snake case while `PHOSPHORUS_RAY` is*.
     *
     * See [What is Snake Case?](https://en.toolpage.org/tool/snakecase)
     */
    SCREAMING_SNAKE_CASE;

    internal companion object {
        internal val pascalCaseRegex = Regex("^[A-Z][a-z]+(?:[A-Z][a-z]+)*\$")
        internal val camelCaseRegex = Regex("^[a-z]+(?:[A-Z][a-z]+)+\$")
        internal val snakeCaseRegex = Regex("^[a-z]+(?:_[a-z]+)*\$")
        internal val screamingSnakeCaseRegex = Regex("^[A-Z]+(?:_[A-Z]+)*\$")
    }
}

/**
 * Patterns that describe valid content of a string.
 */
enum class StringContentPattern {
    ALPHA, SPACE, NUMBER, ASCII, NON_ASCII
}