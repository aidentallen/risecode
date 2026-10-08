# risecode project instructions

Target Minecraft 26.2 and Java 25. Preserve the unobfuscated Fabric/Loom setup.
Run `python3 scripts/fetch-dependencies.py` before a clean build, then
`./gradlew check build`. Real-client tests require a graphical environment.

Modules own EventBus subscriptions and release them on disable. Fabric mixins
adapt game hooks into the EventBus. Preserve native terrain/fluid geometry,
transparency ordering, and optional renderer compatibility.

Keep runtime configurations, personal assets, build caches, and generated
release binaries out of Git. Preserve third-party licenses and attribution.

When present, read `AGENTS.local.md` for machine-specific installation
instructions and existing user authorization. That file is excluded from Git.
