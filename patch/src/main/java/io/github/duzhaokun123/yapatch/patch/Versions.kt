package io.github.duzhaokun123.yapatch.patch

object Versions {
    val yapatch = "0.1.5"
    val loader = yapatch
    val pine = "0.3.0"
    val pineXposed = "0.2.0"

    override fun toString(): String {
        return """
            YAPatch: $yapatch
            Loader: $loader
            Pine: $pine
            PineXposed: $pineXposed
        """.trimIndent()
    }
}