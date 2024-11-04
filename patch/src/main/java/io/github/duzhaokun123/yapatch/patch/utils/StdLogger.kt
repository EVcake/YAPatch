package io.github.duzhaokun123.yapatch.patch.utils

object StdLogger: Logger {
    override fun info(message: String) {
        println(message)
    }

    override fun warn(message: String) {
        println(message)
    }

    override fun error(message: String) {
        println(message)
    }

    override fun onProgress(progress: Int, total: Int) {
        if (total == 0) return
        println("Progress: $progress/$total")
        print("\u001b[A")
        if (progress == total) {
            println()
        }
    }
}