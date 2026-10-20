package io.github.sceneview.demo

// TEMPORARY local A/B switches for #4743 — never committed.
object PerfAb {
    @Volatile var flags: Set<String> = emptySet()
    fun on(flag: String): Boolean = flag in flags
    fun load(dir: java.io.File?) {
        flags = dir?.resolve("perf-ab.txt")?.takeIf { it.exists() }?.readText()
            ?.split(',', ' ', '\n')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        android.util.Log.i("PerfAb", "flags $flags")
    }
}

// TEMPORARY trace sections for #4743 — never committed.
inline fun <T> tr(name: String, block: () -> T): T {
    android.os.Trace.beginSection(name)
    try {
        return block()
    } finally {
        android.os.Trace.endSection()
    }
}
