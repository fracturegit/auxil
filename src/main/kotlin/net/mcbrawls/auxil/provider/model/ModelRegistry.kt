package net.mcbrawls.auxil.provider.model

import net.kyori.adventure.key.Key
import net.mcbrawls.api.registry.Registry

/**
 * A [Registry] of model keys that can carry optional [ModelDefinition] overrides.
 * Extend this instead of [Registry] for any registry passed to [ModelProvider].
 */
abstract class ModelRegistry : Registry<Key>() {
    private val definitions: MutableMap<Key, ModelDefinition> = mutableMapOf()

    /**
     * Registers a model key and optionally associates a custom [ModelDefinition]
     * for the generated items JSON.
     */
    protected fun registerModel(id: String, key: Key, definition: ModelDefinition? = null): Key {
        val registered = register(id, key)
        if (definition != null) definitions[registered] = definition
        return registered
    }

    fun collectDefinitions(): Map<Key, ModelDefinition> = definitions.toMap()
}
