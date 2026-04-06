package net.mcbrawls.auxil.provider.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.kyori.adventure.key.Key

/**
 * Represents an item model definition, corresponding to the `model` field in items JSON files.
 *
 * @see <a href="https://minecraft.wiki/w/Items_model_definition">Items model definition</a>
 */
sealed class ModelDefinition {
    abstract fun toJson(): JsonObject

    /** Collects all model keys directly referenced within this definition tree. */
    abstract fun collectReferencedModelKeys(): Set<Key>

    /** Common property name constants for [Condition]. */
    object ConditionProperties {
        const val USING_ITEM = "minecraft:using_item"
        const val BROKEN = "minecraft:broken"
        const val DAMAGED = "minecraft:damaged"
        const val HAS_COMPONENT = "minecraft:has_component"
        const val FISHING_ROD_CAST = "minecraft:fishing_rod/cast"
        const val BUNDLE_HAS_SELECTED_ITEM = "minecraft:bundle/has_selected_item"
        const val SELECTED = "minecraft:selected"
        const val CARRIED = "minecraft:carried"
        const val EXTENDED_VIEW = "minecraft:extended_view"
        const val KEYBIND_DOWN = "minecraft:keybind_down"
        const val CUSTOM_MODEL_DATA = "minecraft:custom_model_data"
        const val VIEW_ENTITY = "minecraft:view_entity"
    }

    /** Common property name constants for [Select]. */
    object SelectProperties {
        const val MAIN_HAND = "minecraft:main_hand"
        const val CHARGE_TYPE = "minecraft:charge_type"
        const val TRIM_MATERIAL = "minecraft:trim_material"
        const val BLOCK_STATE = "minecraft:block_state"
        const val DISPLAY_CONTEXT = "minecraft:display_context"
        const val LOCAL_TIME = "minecraft:local_time"
        const val CONTEXT_ENTITY_TYPE = "minecraft:context_entity_type"
        const val CUSTOM_MODEL_DATA = "minecraft:custom_model_data"
    }

    /** Common property name constants for [RangeDispatch]. */
    object RangeProperties {
        const val BUNDLE_FULLNESS = "minecraft:bundle/fullness"
        const val DAMAGE = "minecraft:damage"
        const val COUNT = "minecraft:count"
        const val COOLDOWN = "minecraft:cooldown"
        const val TIME = "minecraft:time"
        const val COMPASS = "minecraft:compass"
        const val CROSSBOW_PULL = "minecraft:crossbow/pull"
        const val USE_DURATION = "minecraft:use_duration"
        const val USE_CYCLE = "minecraft:use_cycle"
        const val CUSTOM_MODEL_DATA = "minecraft:custom_model_data"
    }

    // -------------------------------------------------------------------------
    // Types
    // -------------------------------------------------------------------------

    /** References a model JSON file directly. */
    data class Model(
        val model: Key,
        val tints: List<Tint> = emptyList()
    ) : ModelDefinition() {
        override fun collectReferencedModelKeys() = setOf(model)

        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:model")
            addProperty("model", model.toString())
            if (tints.isNotEmpty()) {
                add("tints", JsonArray().apply { tints.forEach { add(it.toJson()) } })
            }
        }
    }

    /** Renders multiple model definitions layered on top of each other. */
    data class Composite(val models: List<ModelDefinition>) : ModelDefinition() {
        constructor(vararg models: ModelDefinition) : this(models.toList())

        override fun collectReferencedModelKeys() =
            models.flatMapTo(mutableSetOf()) { it.collectReferencedModelKeys() }

        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:composite")
            add("models", JsonArray().apply { models.forEach { add(it.toJson()) } })
        }
    }

    /**
     * Selects a model based on a boolean property.
     *
     * [extras] can supply additional fields required by certain properties
     * (e.g., `component` for [ConditionProperties.HAS_COMPONENT],
     * `index` for [ConditionProperties.CUSTOM_MODEL_DATA]).
     */
    data class Condition(
        val property: String,
        val onTrue: ModelDefinition,
        val onFalse: ModelDefinition,
        val extras: JsonObject = JsonObject()
    ) : ModelDefinition() {
        override fun collectReferencedModelKeys() =
            onTrue.collectReferencedModelKeys() + onFalse.collectReferencedModelKeys()

        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:condition")
            addProperty("property", property)
            extras.entrySet().forEach { (k, v) -> add(k, v) }
            add("on_true", onTrue.toJson())
            add("on_false", onFalse.toJson())
        }
    }

    /**
     * Selects a model based on a discrete property value.
     *
     * [extras] can supply additional fields required by certain properties
     * (e.g., `block_state_property` for [SelectProperties.BLOCK_STATE],
     * `index` for [SelectProperties.CUSTOM_MODEL_DATA]).
     */
    data class Select(
        val property: String,
        val cases: List<Case>,
        val fallback: ModelDefinition? = null,
        val extras: JsonObject = JsonObject()
    ) : ModelDefinition() {
        data class Case(val `when`: String, val model: ModelDefinition)

        override fun collectReferencedModelKeys(): Set<Key> {
            val keys = cases.flatMapTo(mutableSetOf()) { it.model.collectReferencedModelKeys() }
            fallback?.let { keys += it.collectReferencedModelKeys() }
            return keys
        }

        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:select")
            addProperty("property", property)
            extras.entrySet().forEach { (k, v) -> add(k, v) }
            add("cases", JsonArray().apply {
                cases.forEach { case ->
                    add(JsonObject().apply {
                        addProperty("when", case.`when`)
                        add("model", case.model.toJson())
                    })
                }
            })
            fallback?.let { add("fallback", it.toJson()) }
        }
    }

    /**
     * Selects a model based on a numeric property with threshold-based entries.
     *
     * [extras] can supply additional fields required by certain properties
     * (e.g., `wobble`, `natural_days` for [RangeProperties.TIME]).
     */
    data class RangeDispatch(
        val property: String,
        val entries: List<Entry>,
        val fallback: ModelDefinition? = null,
        val scale: Float = 1f,
        val extras: JsonObject = JsonObject()
    ) : ModelDefinition() {
        data class Entry(val threshold: Float, val model: ModelDefinition)

        override fun collectReferencedModelKeys(): Set<Key> {
            val keys = entries.flatMapTo(mutableSetOf()) { it.model.collectReferencedModelKeys() }
            fallback?.let { keys += it.collectReferencedModelKeys() }
            return keys
        }

        override fun toJson() = JsonObject().apply {
            addProperty("type", "minecraft:range_dispatch")
            addProperty("property", property)
            if (scale != 1f) addProperty("scale", scale)
            extras.entrySet().forEach { (k, v) -> add(k, v) }
            add("entries", JsonArray().apply {
                entries.forEach { entry ->
                    add(JsonObject().apply {
                        addProperty("threshold", entry.threshold)
                        add("model", entry.model.toJson())
                    })
                }
            })
            fallback?.let { add("fallback", it.toJson()) }
        }
    }

    /** Renders nothing. */
    data object Empty : ModelDefinition() {
        override fun collectReferencedModelKeys() = emptySet<Key>()
        override fun toJson() = JsonObject().apply { addProperty("type", "minecraft:empty") }
    }

    /** Escape hatch for unsupported or custom model types. */
    data class Raw(val json: JsonObject) : ModelDefinition() {
        override fun collectReferencedModelKeys() = emptySet<Key>()
        override fun toJson() = json
    }
}
