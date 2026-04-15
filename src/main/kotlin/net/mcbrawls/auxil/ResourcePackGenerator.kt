package net.mcbrawls.auxil

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.obfuscation.ObfuscatedZip
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource
import java.io.File

class ResourcePackGenerator(
    val meta: ResourcePack.Meta,
    val sourceFiles: Set<File>,
) {
    private val providers: MutableList<() -> ResourceProvider> = mutableListOf()

    fun add(vararg providers: () -> ResourceProvider): ResourcePackGenerator {
        this.providers.addAll(providers)
        return this
    }

    fun addDirect(vararg providers: ResourceProvider): ResourcePackGenerator {
        this.providers.addAll(providers.map { provider -> { provider } })
        return this
    }

    fun generate(sources: Map<Key, ByteArray> = generateSources()): ResourcePack {
        val resources = collectResources(sources)

        val files = buildMap {
            this["pack.mcmeta"] = generateMetaBytes()
            meta.logo?.let { logo -> this["pack.png"] = logo.readBytes() }

            resources.forEach { (key, resource) ->
                val namespace = key.namespace()
                val path = key.value()
                resource.createBytes(sources)?.let { bytes ->
                    this["assets/$namespace/$path"] = bytes
                }
            }
        }

        val packBytes = ObfuscatedZip.fromMap(files)
        return ResourcePack(packBytes)
    }

    fun collectResources(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return buildMap {
            providers.map { it() }
                .flatMap { it.collectFiles(sources).entries }
                .forEach { (key, resource) ->
                    val existing = this[key]
                    if (existing is PackResource.RawJson && resource is PackResource.RawJson) {
                        this[key] = PackResource.RawJson(mergeJsonObjects(existing.json, resource.json))
                    } else {
                        this[key] = resource
                    }
                }
        }
    }

    private fun mergeJsonObjects(a: JsonElement, b: JsonElement): JsonObject {
        val result = JsonObject()
        listOf(a, b).filterIsInstance<JsonObject>().forEach { obj ->
            obj.entrySet().forEach { (k, v) ->
                val existing = result.get(k)
                if (existing is JsonArray && v is JsonArray) {
                    val merged = JsonArray()
                    existing.forEach { merged.add(it) }
                    v.forEach { merged.add(it) }
                    result.add(k, merged)
                } else {
                    result.add(k, v)
                }
            }
        }
        return result
    }

    fun generateSources(): Map<Key, ByteArray> = Auxil.generateSources(sourceFiles).mapValues { it.value.invoke() }

    private fun generateMetaBytes(): ByteArray {
        val format = meta.format
        val jsonString = gson.toJson(
            obj {
                "pack" to obj {
                    "description" to meta.description
                    "pack_format" to format
                    "supported_formats" to array(0, format)
                    "min_format" to 0
                    "max_format" to format
                }
            }
        )

        return jsonString.encodeToByteArray()
    }

    class Builder {
        private var meta: ResourcePack.Meta = ResourcePack.Meta()
        private val sources: MutableSet<File> = mutableSetOf()

        fun description(description: String): Builder {
            meta = meta.copy(description = description)
            return this
        }

        fun format(version: Int): Builder {
            meta = meta.copy(format = version)
            return this
        }

        fun logo(file: File): Builder {
            meta = meta.copy(logo = file)
            return this
        }

        fun source(folder: File): Builder {
            require(folder.isDirectory) { "File must be directory" }
            sources.add(folder)
            return this
        }

        fun build(): ResourcePackGenerator {
            return ResourcePackGenerator(meta, sources)
        }
    }

    companion object {
        private val gson = Gson()

        fun builder(): Builder {
            return Builder()
        }
    }
}
