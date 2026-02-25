package com.intuit.playerui.lang.dsl.tagged

/**
 * Base interface for tagged values (bindings and expressions).
 * These represent dynamic values that are resolved at runtime by Player-UI.
 *
 * @param T Phantom type parameter for type-safe usage (not used at runtime)
 */
sealed interface TaggedValue<T> {
    /**
     * Returns the raw value without formatting.
     */
    fun toValue(): String

    /**
     * Returns the formatted string representation.
     * For bindings: "{{path}}"
     * For expressions: "@[expr]@"
     */
    override fun toString(): String
}

/**
 * Represents a data binding in Player-UI.
 * Bindings reference paths in the data model.
 *
 * Example: binding<String>("user.name") produces "{{user.name}}"
 *
 * @param T The expected type of the bound value (phantom type)
 * @property path The data path to bind to
 */
class Binding<T>(
    private val path: String,
) : TaggedValue<T> {
    override fun toValue(): String = path

    override fun toString(): String = "{{$path}}"

    /**
     * Returns the path with _index_ placeholders replaced.
     */
    fun withIndex(
        index: Int,
        depth: Int = 0,
    ): Binding<T> {
        val placeholder = if (depth == 0) "_index_" else "_index${depth}_"
        return Binding(path.replace(placeholder, index.toString()))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Binding<*>) return false
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()
}

/**
 * Represents an expression in Player-UI.
 * Expressions are evaluated at runtime.
 *
 * Example: expression<Boolean>("user.age >= 18") produces "@[user.age >= 18]@"
 *
 * @param T The expected return type of the expression (phantom type)
 * @property expr The expression string
 */
class Expression<T>(
    private val expr: String,
) : TaggedValue<T> {
    init {
        validateSyntax(expr)
    }

    override fun toValue(): String = expr

    override fun toString(): String = "@[$expr]@"

    /**
     * Returns the expression with _index_ placeholders replaced.
     */
    fun withIndex(
        index: Int,
        depth: Int = 0,
    ): Expression<T> {
        val placeholder = if (depth == 0) "_index_" else "_index${depth}_"
        return Expression(expr.replace(placeholder, index.toString()))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Expression<*>) return false
        return expr == other.expr
    }

    override fun hashCode(): Int = expr.hashCode()

    private fun validateSyntax(expression: String) {
        var openParens = 0
        expression.forEachIndexed { index, char ->
            when (char) {
                '(' -> {
                    openParens++
                }

                ')' -> {
                    openParens--
                    if (openParens < 0) {
                        throw IllegalArgumentException(
                            "Unexpected ) at character $index in expression: $expression",
                        )
                    }
                }
            }
        }
        if (openParens > 0) {
            throw IllegalArgumentException("Expected ) in expression: $expression")
        }
    }
}

/**
 * Creates a binding to a data path.
 */
fun <T> binding(path: String): Binding<T> = Binding(path)

/**
 * Creates an expression.
 */
fun <T> expression(expr: String): Expression<T> = Expression(expr)

/**
 * Checks if a value is a TaggedValue.
 */
fun isTaggedValue(value: Any?): Boolean = value is TaggedValue<*>

// Operator overloads for Expression<Number>

operator fun Expression<Number>.plus(other: Any): Expression<Number> = add(this, other)

operator fun Expression<Number>.minus(other: Any): Expression<Number> = subtract(this, other)

operator fun Expression<Number>.times(other: Any): Expression<Number> = multiply(this, other)

operator fun Expression<Number>.div(other: Any): Expression<Number> = divide(this, other)

@Suppress("ktlint:standard:chain-method-continuation")
operator fun Expression<Boolean>.not(): Expression<Boolean> = com.intuit.playerui.lang.dsl.tagged.not(this)

// Infix comparison operators for TaggedValue

infix fun <T> TaggedValue<T>.eq(other: Any): Expression<Boolean> = equal(this, other)

infix fun <T> TaggedValue<T>.neq(other: Any): Expression<Boolean> = notEqual(this, other)

// Binding path composition

operator fun Binding<*>.div(segment: String): Binding<Any> = Binding("${toValue()}.$segment")

fun <T> Binding<List<T>>.indexed(depth: Int = 0): Binding<T> {
    val placeholder = if (depth == 0) "_index_" else "_index${depth}_"
    return Binding("${toValue()}.$placeholder")
}
