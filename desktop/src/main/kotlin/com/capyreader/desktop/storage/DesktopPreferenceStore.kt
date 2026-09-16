package com.capyreader.desktop.storage

import com.jocmp.capy.preferences.Preference
import com.jocmp.capy.preferences.PreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

class DesktopPreferenceStore(private val file: File) : PreferenceStore {
    private val props = Properties()
    private val prefs = ConcurrentHashMap<String, Preference<*>>()

    init {
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }
    }

    private fun flush() {
        file.parentFile?.mkdirs()
        file.outputStream().use { props.store(it, null) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getString(key: String, defaultValue: String): Preference<String> {
        return prefs.getOrPut(key) {
            DesktopPref(key, defaultValue,
                get = { props.getProperty(key, defaultValue) },
                set = { props.setProperty(key, it); flush() },
                del = { props.remove(key); flush() },
                has = { props.containsKey(key) },
            )
        } as Preference<String>
    }

    @Suppress("UNCHECKED_CAST")
    override fun getLong(key: String, defaultValue: Long): Preference<Long> {
        return prefs.getOrPut(key) {
            DesktopPref(key, defaultValue,
                get = { props.getProperty(key)?.toLongOrNull() ?: defaultValue },
                set = { props.setProperty(key, it.toString()); flush() },
                del = { props.remove(key); flush() },
                has = { props.containsKey(key) },
            )
        } as Preference<Long>
    }

    @Suppress("UNCHECKED_CAST")
    override fun getInt(key: String, defaultValue: Int): Preference<Int> {
        return prefs.getOrPut(key) {
            DesktopPref(key, defaultValue,
                get = { props.getProperty(key)?.toIntOrNull() ?: defaultValue },
                set = { props.setProperty(key, it.toString()); flush() },
                del = { props.remove(key); flush() },
                has = { props.containsKey(key) },
            )
        } as Preference<Int>
    }

    @Suppress("UNCHECKED_CAST")
    override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
        return prefs.getOrPut(key) {
            DesktopPref(key, defaultValue,
                get = { props.getProperty(key)?.toFloatOrNull() ?: defaultValue },
                set = { props.setProperty(key, it.toString()); flush() },
                del = { props.remove(key); flush() },
                has = { props.containsKey(key) },
            )
        } as Preference<Float>
    }

    @Suppress("UNCHECKED_CAST")
    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        return prefs.getOrPut(key) {
            DesktopPref(key, defaultValue,
                get = { props.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue },
                set = { props.setProperty(key, it.toString()); flush() },
                del = { props.remove(key); flush() },
                has = { props.containsKey(key) },
            )
        } as Preference<Boolean>
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        return prefs.getOrPut(key) {
            DesktopPref(key, defaultValue,
                get = {
                    val raw = props.getProperty(key) ?: return@DesktopPref defaultValue
                    if (raw.isEmpty()) emptySet() else raw.split("\u001F").toSet()
                },
                set = { props.setProperty(key, it.joinToString("\u001F")); flush() },
                del = { props.remove(key); flush() },
                has = { props.containsKey(key) },
            )
        } as Preference<Set<String>>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> {
        return prefs.getOrPut(key) {
            DesktopPref(key, defaultValue,
                get = {
                    val raw = props.getProperty(key) ?: return@DesktopPref defaultValue
                    try { deserializer(raw) } catch (_: Exception) { defaultValue }
                },
                set = { props.setProperty(key, serializer(it)); flush() },
                del = { props.remove(key); flush() },
                has = { props.containsKey(key) },
            )
        } as Preference<T>
    }

    override fun clearAll() {
        props.clear()
        prefs.clear()
        flush()
    }
}

private class DesktopPref<T>(
    private val key: String,
    private val default: T,
    private val get: () -> T,
    private val set: (T) -> Unit,
    private val del: () -> Unit,
    private val has: () -> Boolean,
) : Preference<T> {
    private val _flow = MutableStateFlow(get())

    override fun key() = key
    override fun get() = get.invoke()
    override fun set(value: T) {
        set.invoke(value)
        _flow.value = value
    }
    override fun isSet() = has()
    override fun delete() {
        del()
        _flow.value = default
    }
    override fun defaultValue() = default

    override fun changes(): Flow<T> = _flow

    override fun stateIn(scope: CoroutineScope): StateFlow<T> = _flow
}
