package net.mcbrawls.auxil

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import com.google.gson.Gson
import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourcePackGenerator(
    val meta: ResourcePack.Meta,
    sources: Set<File>,
) {
    private val resources: MutableMap<Key, PackResource> = mutableMapOf()

    private val sources: Map<Key, ByteArray> = buildMap {
        sources.forEach { folderRoot ->
            folderRoot.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val filePath = file.relativeTo(folderRoot).path
                    val namespace = filePath.substringBefore(File.separator)
                    val path = filePath
                        .substringAfter(namespace)
                        .removePrefix(File.separator)
                        .split(File.separator)
                        .joinToString("/")
                    val key = Key.key(namespace, path)

                    this[key] = file.readBytes()
                }
        }
    }

    fun add(key: Key, resource: PackResource): ResourcePackGenerator {
        resources[key] = resource
        return this
    }

    fun add(provider: ResourceProvider): ResourcePackGenerator {
        val resources = provider.collectFiles(sources)
        resources.forEach(::add)
        return this
    }

    fun generate(): ResourcePack {
        val files = buildMap {
            this["pack.mcmeta"] = generateMetaBytes()
            meta.logo?.let { logo -> this["pack.png"] = logo.readBytes() }

            resources.forEach { (key, resource) ->
                val namespace = key.namespace()
                val path = key.value()
                this["assets/$namespace/$path"] = resource.createBytes(sources)
            }
        }

        val packBytes = createZip(files)
        return ResourcePack(packBytes)
    }

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

        /**
         * Creates a zip file from a map of paths to their file byte arrays.
         * @return a zip byte array
         */
        fun createZip(files: Map<String, ByteArray>): ByteArray {
            val outputStream = ByteArrayOutputStream()
            val zipOutputStream = ZipOutputStream(outputStream)

            zipOutputStream.use { stream ->
                for ((fileName, fileContent) in files) {
                    val entry = ZipEntry(fileName)
                    entry.time = 0
                    stream.putNextEntry(entry)
                    stream.write(fileContent)
                    stream.closeEntry()
                }
            }

            return outputStream.toByteArray()
        }
    }
}
