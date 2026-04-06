package net.mcbrawls.auxil.provider.model

import com.google.gson.JsonObject

sealed class Tint {
    abstract fun toJson(): JsonObject

    /** Colors the model based on the item's dye color. */
    data class Dye(val default: Int = -1) : Tint() {
        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:dye")
            addProperty("default", default)
        }
    }

    /** Colors the model based on the biome grass color. */
    data class Grass(val default: Int = -1) : Tint() {
        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:grass")
            addProperty("default", default)
        }
    }

    /** Colors the model based on the item's firework color. */
    data class Firework(val default: Int = -1) : Tint() {
        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:firework")
            addProperty("default", default)
        }
    }

    /** Colors the model based on the item's potion color. */
    data class Potion(val default: Int = -1) : Tint() {
        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:potion")
            addProperty("default", default)
        }
    }

    /** Colors the model based on the map's color. */
    data class MapColor(val default: Int = -1) : Tint() {
        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:map_color")
            addProperty("default", default)
        }
    }

    /** Colors the model based on the team color. */
    data class TeamColor(val default: Int = -1) : Tint() {
        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:team")
            addProperty("default", default)
        }
    }

    /** Colors the model based on a custom_model_data float color entry. */
    data class CustomModelData(val index: Int = 0, val default: Int = -1) : Tint() {
        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:custom_model_data")
            addProperty("index", index)
            addProperty("default", default)
        }
    }

    /** A constant ARGB color. */
    data class Constant(val value: Int) : Tint() {
        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:constant")
            addProperty("value", value)
        }
    }
}
