# Mob Carry (Fabric, Minecraft 26.3)

Pick up hostile mobs (zombies etc.) and carry them on your head, then drop them wherever you like. Handy for iron farms.

- **Pick up:** sneak + right-click a mob with an empty hand
- **Drop:** sneak + right-click a block (placed on that face) or the air (placed in front of you)
- **Config:** Mod Menu -> Mob Carry -> Configure (or `config/mobcarry.json`)

Bosses (Wither, Ender Dragon) and the Warden can never be picked up.

## Build
Needs JDK 25. Push to GitHub and the Actions workflow builds the jar (Actions tab -> latest run -> `mobcarry-jar`).
Locally: `gradle wrapper` once, then `./gradlew build`. Jar ends up in `build/libs/`.
