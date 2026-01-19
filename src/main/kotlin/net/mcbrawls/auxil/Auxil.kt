package net.mcbrawls.auxil

import net.kyori.adventure.key.Key
import java.io.File

object Auxil {
    const val NAMESPACE = "auxil"

    fun generateSources(sourceFiles: Set<File>): Map<Key, () -> ByteArray> {
        return buildMap {
            sourceFiles.forEach { folderRoot ->
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

                        this[key] = file::readBytes
                    }
            }
        }
    }
}
