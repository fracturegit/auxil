package net.mcbrawls.auxil.test

import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.ResourcePackGenerator
import net.mcbrawls.auxil.provider.font.Font
import net.mcbrawls.auxil.provider.font.FontProvider
import net.mcbrawls.auxil.provider.sound.SoundProvider
import net.mcbrawls.auxil.resource
import java.io.File

object AuxilTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val generator = ResourcePackGenerator.builder()
            .description("Piss lizard")
            .source(resource("source") ?: error("No source folder found"))
            .build()

        generator.add({
            SoundProvider.builder()
                .addKeys(Key.key("test", "one/thingy"))
                .build()
        })

        val fontId = Key.key("test", "pinch")
        generator.add({
            FontProvider.builder()
                .add(
                    fontId,
                    Font(
                        fontId,
                        7.0,
                        4.0
                    )
                )
                .build()
        })

        File("out.zip").writeBytes(generator.generate().bytes)
    }
}
