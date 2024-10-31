package io.github.duzhaokun123.yapatch.patch

object Versions {
    val yapatch = "0.1.1"
    val pine = "0.3.0"
    val pineXposed = "0.2.0"

    override fun toString(): String {
        return """
            YAPatch: $yapatch
            Pine: $pine
            PineXposed: $pineXposed
        """.trimIndent()
    }
}