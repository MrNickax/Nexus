# Nexus

Public core for building Minecraft plugins. Nexus is a single dependency that gives a
plugin a batteries-included toolkit — data storage, configuration, commands, GUIs, items,
scheduling, messaging, i18n and more — behind one hub (`BukkitNexus.get()`), so consumer
plugins stay small and ship without bundling their own libraries.

- **Platform:** Paper/Spigot **1.21+**, Folia-supported.
- **Java:** 21.
- **Group:** `com.nickax.nexus`.

## Why Nexus

- **One entry point.** Everything is reached through the hub: `nexus.data()`, `nexus.configs()`,
  `nexus.commands()`, `nexus.menus()`, `nexus.scheduler()`, `nexus.messages()`, `nexus.lang()`,
  `nexus.locks()`, `nexus.messaging(...)`, `nexus.webhooks()`.
- **No fat consumer jars.** Adventure, Redisson, Netty, MongoDB, HikariCP, MySQL and
  SnakeYAML are bundled and relocated inside the Nexus server plugin. Consumer plugins
  declare Nexus as `provided` and ship only their own code.
- **Folia-safe by design.** The scheduler exposes region- and entity-bound scheduling, so
  per-entity work runs on the correct thread on Folia and on the main thread on Spigot/Paper.
- **Reload-safe.** Listeners and commands are registered per owning plugin and can be torn
  down with a single `unregister(plugin)` call, so a PlugMan-style reload leaves no stale or
  duplicate handlers.

## Modules

| Module | Description |
| --- | --- |
| `nexus-api` | Platform-agnostic interfaces and value types (the `com.nickax.nexus.api` surface). |
| `nexus-core` | Default implementations (data, config, lang, locks, messaging, webhooks, services). |
| `nexus-bukkit` | The Bukkit server plugin and Bukkit-specific API (commands, listeners, menus, items, scheduler, messages). |

## Distribution model

The build produces two artifacts from `nexus-bukkit`:

- **`nexus-bukkit-<version>.jar`** — the plain, **unrelocated** library (native `net.kyori`,
  etc.). Consumer plugins depend on this (`provided`) so they compile and test against native
  classes.
- **`Nexus-<version>.jar`** (attached with classifier `plugin`) — the **relocated** server
  plugin with all libraries shaded under `com.nickax.nexus.libs.*`. This is the jar you drop
  into the server's `plugins/` folder.

> **Relocation boundary:** because the running plugin relocates Adventure, never pass or
> receive Adventure types (`Component`, `Audience`, …) to/from Nexus at runtime from a
> consumer. Use the string-based API: `nexus.messages()` takes plain `String` markup plus a
> `TextFormat`, and `nexus.lang()` resolves to `String`/sends directly.

## Getting started (consumer plugin)

**1. Add the dependency.** Install Nexus to your local Maven repository (`mvn install`) or
deploy it to your own repository, then:

```xml
<dependencies>
    <dependency>
        <groupId>com.nickax.nexus</groupId>
        <artifactId>nexus-api</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.nickax.nexus</groupId>
        <artifactId>nexus-bukkit</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**2. Depend on the plugin** in your `plugin.yml`:

```yaml
name: MyPlugin
main: com.example.MyPlugin
depend: [Nexus]
```

**3. Grab the hub** in `onEnable`:

```java
public final class MyPlugin extends JavaPlugin {
    private BukkitNexus nexus;

    @Override
    public void onEnable() {
        nexus = BukkitNexus.get();
        // ... wire your subsystems
    }
}
```

## Feature tour

### Configuration (comment- and order-preserving YAML)

```java
Config config = nexus.configs().load(
        getDataFolder().toPath().resolve("config.yml"),
        getResource("config.yml"));

int interval = config.getInt("reward.interval", 60);
List<String> worlds = config.getStringList("afk-zone.world-list");
```

On regeneration, Nexus keeps your bundled comments and key order, merges in any missing
keys, and only rewrites the file when the rendered result actually differs from disk.

### Internationalisation (i18n)

```java
Lang lang = nexus.lang().builder()
        .defaultLocale("en")
        .bundle("en", Map.of("welcome", "<green>Welcome, {player}!"))
        .bundle("es", Map.of("welcome", "<green>¡Bienvenido, {player}!"))
        .build();

// Resolves to the recipient's client locale and applies the chosen format:
nexus.messages().send(player, lang, TextFormat.MINI_MESSAGE, "welcome",
        Placeholder.of("player", player.getName()));
```

Placeholder values are escaped so user-supplied text can never inject markup. Bundles are a
flat key→template map; load per-locale `lang/<id>.yml` files and resolve per player via the
player's locale.

### Messaging & text formatting

`nexus.messages()` formats plain markup with a `TextFormat` dialect — `MINI_MESSAGE`,
`LEGACY` (`&`/`&#RRGGBB`) or `MIXED` — and delivers it safely on Spigot and Paper:

```java
nexus.messages().send(sender, TextFormat.MINI_MESSAGE, "<gold>Hello!");
nexus.messages().broadcast(TextFormat.MINI_MESSAGE, lines);
nexus.messages().sendActionBar(player, TextFormat.MINI_MESSAGE, "<aqua>Action bar");
```

### Data storage

A typed, cache-backed store with pluggable cache and backend and a configurable write policy:

```java
DataStore<UUID, PlayerData> store = nexus.data()
        .<UUID, PlayerData>store("myplugin_players", PlayerData.class)
        .key(KeyMapper.uuid())
        .memoryCache()                       // or .redisCache(redisSettings)
        .fileBackend()                       // or .mongoBackend(...) / .sqlBackend(...)
        .writePolicy(WritePolicy.writeThrough())  // or writeBehind(Duration.ofSeconds(300))
        .build();

store.getOrCreate(uuid, PlayerData::new)
        .thenCompose(data -> store.update(uuid, d -> { d.addCurrency(10); return d; }));
```

- **Caches:** in-memory or Redis.
- **Backends:** flat file, MongoDB or SQL (HikariCP + MySQL).
- **Write policies:** write-through (safest) or batched write-behind.

### Commands

A fluent command engine with subcommands and arguments, registered under your plugin:

```java
Command reload = Command.named("reload")
        .executes(ctx -> ((BukkitSender) ctx.sender()).bukkit().sendMessage("Reloaded"))
        .build();

Command root = Command.named("myplugin").subcommand(reload).build();
nexus.commands().register(this, root);
// On disable / reload:
nexus.commands().unregister(this);
```

### Listeners

```java
nexus.listeners().register(this, new MyListener());
// Tears down only this plugin's listeners:
nexus.listeners().unregister(this);
```

### Scheduler (Folia-safe)

```java
BukkitScheduler scheduler = nexus.scheduler();
scheduler.entityTimer(player, Duration.ofSeconds(1), Duration.ofSeconds(1), () -> tick(player));
scheduler.asyncLater(Duration.ofSeconds(5), () -> doAsyncWork());
```

Entity- and region-bound tasks run on the player's region thread on Folia and the main
thread on Spigot/Paper.

### Menus (GUIs)

`nexus.menus()` builds inventory GUIs from `Menu`, `Button`, `Slot` and `ClickContext`,
with click handling routed through a managed listener.

### Items

Fluent item builders with the full meta surface (name, lore, amount, enchantments, flags,
unbreakability, custom model data, persistent-data tags):

```java
ItemStack icon = ItemBuilder.of(Material.DIAMOND_SWORD)
        .name("<gradient:#ff0:#f0f>Excalibur")
        .lore("<gray>Legendary blade")
        .enchant(Enchantment.SHARPNESS, 5)
        .build();

// Player heads — by owner or by base64 texture:
ItemStack ownerHead = SkullBuilder.of(player).name("<yellow>Steve").build();
ItemStack customHead = SkullBuilder.texture("eyJ0ZXh0dXJlcyI6...").name("<gold>Custom").build();
```

### Locks, cross-server messaging & webhooks

```java
LockService locks = nexus.locks();                       // local, no Redis required
LockService distributed = nexus.locks(redisSettings);    // Redis-backed
Messaging messaging = nexus.messaging(redisSettings);    // cross-server pub/sub
nexus.webhooks().send(/* Discord webhook */);
```

### Per-plugin scope

```java
NexusScope scope = nexus.scope(this);   // data stores namespaced by plugin; services
                                        // registered through the scope stop together on close()
```

## Building

Requires JDK 21 and Maven.

```bash
mvn clean install
```

This builds all three modules, runs the test suite, installs the unrelocated
`nexus-api` / `nexus-bukkit` artifacts to your local repository, and writes the relocated
server plugin to `nexus-bukkit/target/Nexus-<version>.jar`.

## Project layout

```
nexus/
├── nexus-api/      # platform-agnostic interfaces & value types
├── nexus-core/     # default implementations
└── nexus-bukkit/   # Bukkit plugin + Bukkit-specific API (the server jar)
```

## License

See the repository for license details.
