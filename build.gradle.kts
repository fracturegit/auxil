@file:Suppress("PropertyName")

val brawls_api_version: String by properties
val jaudiotagger_version: String by properties

dependencies {
    api("net.kyori:adventure-key:4.25.0")
    api("net.mcbrawls:brawls-api:$brawls_api_version")
    api("net.jthink:jaudiotagger:$jaudiotagger_version")
    api(project(":codex"))
}
