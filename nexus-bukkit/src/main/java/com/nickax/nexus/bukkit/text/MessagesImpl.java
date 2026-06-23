package com.nickax.nexus.bukkit.text;

import com.nickax.nexus.api.lang.Lang;
import com.nickax.nexus.api.lang.Placeholder;
import com.nickax.nexus.api.text.TextFormat;
import com.nickax.nexus.api.text.TextFormatter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Default {@link Messages}: formats markup with the requested {@link TextFormatter}
 * and sends the resulting component through the shared {@link BukkitAudiences}. All
 * Adventure handling stays inside this class so callers only ever pass strings.
 */
public final class MessagesImpl implements Messages {

    /** Serializes components to legacy section codes (with §x hex) for the Spigot action-bar API. */
    private static final LegacyComponentSerializer ACTION_BAR_LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final BukkitAudiences audiences;

    /**
     * Creates the messaging service over a shared audiences provider.
     *
     * @param audiences the shared audiences provider
     */
    public MessagesImpl(@NotNull BukkitAudiences audiences) {
        this.audiences = audiences;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(@NotNull CommandSender target, @NotNull String miniMessage) {
        send(target, TextFormat.MINI_MESSAGE, miniMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(@NotNull CommandSender target, @NotNull TextFormat format, @NotNull String markup) {
        audiences.sender(target).sendMessage(TextFormatter.of(format).deserialize(markup));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(@NotNull CommandSender target, @NotNull TextFormat format, @NotNull List<String> lines) {
        audiences.sender(target).sendMessage(joinLines(format, lines));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(@NotNull TextFormat format, @NotNull String markup) {
        Component message = TextFormatter.of(format).deserialize(markup);
        broadcast(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(@NotNull TextFormat format, @NotNull List<String> lines) {
        broadcast(joinLines(format, lines));
    }

    /**
     * Formats each line and joins them into a single component separated by newlines.
     *
     * @param format the markup dialect
     * @param lines  the markup lines
     * @return the joined component
     */
    private Component joinLines(@NotNull TextFormat format, @NotNull List<String> lines) {
        List<Component> components = TextFormatter.of(format).deserialize(lines);
        Component result = Component.empty();
        boolean first = true;
        for (Component line : components) {
            if (first) {
                result = line;
                first = false;
            } else {
                result = result.append(Component.newline()).append(line);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendActionBar(@NotNull CommandSender target, @NotNull String miniMessage) {
        sendActionBar(target, TextFormat.MINI_MESSAGE, miniMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendActionBar(@NotNull CommandSender target, @NotNull TextFormat format, @NotNull String markup) {
        deliverActionBar(target, TextFormatter.of(format).deserialize(markup));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(@NotNull CommandSender target, @NotNull Lang lang, @NotNull TextFormat format,
                     @NotNull String key, @NotNull Placeholder @NotNull ... placeholders) {
        String markup = lang.resolve(localeOf(target), key, placeholders);
        audiences.sender(target).sendMessage(TextFormatter.of(format).deserialize(markup));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendActionBar(@NotNull CommandSender target, @NotNull Lang lang, @NotNull TextFormat format,
                              @NotNull String key, @NotNull Placeholder @NotNull ... placeholders) {
        String markup = lang.resolve(localeOf(target), key, placeholders);
        deliverActionBar(target, TextFormatter.of(format).deserialize(markup));
    }

    /**
     * Delivers an action bar. For players it uses the Spigot chat API
     * ({@code player.spigot().sendMessage(ChatMessageType.ACTION_BAR, ...)}) rather than
     * {@link BukkitAudiences}: because Adventure is relocated here, the platform cannot use
     * the server's native action-bar facet (the player implements the server's unrelocated
     * {@code Audience}, not the relocated one), and its packet facets break on newer server
     * versions, so action bars silently fail to show. The Spigot path is version-independent.
     * The console (non-player) falls back to the audiences path.
     *
     * @param target    the recipient
     * @param component the already-formatted component
     */
    private void deliverActionBar(@NotNull CommandSender target, @NotNull Component component) {
        if (target instanceof Player player) {
            String legacy = ACTION_BAR_LEGACY.serialize(component);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(legacy));
        } else {
            audiences.sender(target).sendActionBar(component);
        }
    }

    /**
     * Returns the bare locale code for a recipient (e.g. {@code "es"} from {@code "es_ES"}),
     * or an empty string for non-players so the lang falls back to its default locale.
     *
     * @param target the recipient
     * @return the bare locale code, or an empty string for non-players
     */
    private static String localeOf(@NotNull CommandSender target) {
        if (target instanceof Player player) {
            String locale = player.getLocale();
            int separator = locale.indexOf('_');
            return separator > 0 ? locale.substring(0, separator) : locale;
        }
        return "";
    }

    /**
     * Sends an already-formatted component to every online player.
     *
     * @param message the component to send
     */
    private void broadcast(@NotNull Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            audiences.player(player).sendMessage(message);
        }
    }
}
