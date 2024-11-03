package io.github.duzhaokun123.yapatch.patch

data class Metadata(
    val originalAppComponentFactory: String?,
    val modules: List<String>,
    val originalSignature: String,
    val sigbypassLevel: Int,
    val loader: String?,
    val manager: String = "io.github.duzhaokun123.yapatch"
) {
}