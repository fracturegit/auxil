package net.mcbrawls.auxil.provider.sound

import com.google.gson.JsonObject
import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.resource.PackResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SoundProviderTest {
    @Test fun `sound resource with no subtitle has none in json`() {
        val provider = SoundProvider.builder()
            .add(SoundResource.create(Key.key("test", "no_subtitle")))
            .build()

        val files = provider.collectFiles(emptyMap())
        val soundsJson = files[Key.key("test", "sounds.json")] as PackResource.RawJson
        val entry = (soundsJson.json as JsonObject).getAsJsonObject("no_subtitle")

        assertNull(entry.get("subtitle"))
    }

    @Test fun `sound resource with subtitle emits it in json`() {
        val subtitleKey = Key.key("test", "subtitles.test.captioned")
        val resource = SoundResource.create(Key.key("test", "captioned")).copy(subtitle = subtitleKey)

        val provider = SoundProvider.builder()
            .add(resource)
            .build()

        val files = provider.collectFiles(emptyMap())
        val soundsJson = files[Key.key("test", "sounds.json")] as PackResource.RawJson
        val entry = (soundsJson.json as JsonObject).getAsJsonObject("captioned")

        assertEquals("test:subtitles.test.captioned", entry.get("subtitle").asString)
    }
}
