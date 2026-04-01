package net.mcbrawls.auxil

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import com.google.gson.Gson
import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.obfuscation.ObfuscatedZip
import net.mcbrawls.auxil.provider.ResourceProvider
import java.io.File

class ResourcePackGenerator(
    val meta: ResourcePack.Meta,
    val sourceFiles: Set<File>,
) {
    private val providers: MutableSet<ResourceProvider> = mutableSetOf()

    fun add(vararg providers: ResourceProvider): ResourcePackGenerator {
        this.providers.addAll(providers)
        return this
    }

    fun generate(sources: Map<Key, ByteArray> = generateSources()): ResourcePack {
        val resources = providers.flatMap { it.collectFiles(sources).entries }.associate { it.key to it.value }

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
