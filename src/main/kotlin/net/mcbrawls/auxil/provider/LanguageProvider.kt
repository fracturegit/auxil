package net.mcbrawls.auxil.provider

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.resource.PackResource

class LanguageProvider(
    val language: Key,
    val translations: Map<String, String>,
) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        val key = Key.key(language.namespace(), "lang/${language.value()}.json")
        return mapOf(key to PackResource.RawJson(obj {
            translations.forEach { (key, value) -> key to value }
        }))
    }
}
