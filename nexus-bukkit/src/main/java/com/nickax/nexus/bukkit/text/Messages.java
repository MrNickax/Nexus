package com.nickax.nexus.bukkit.text;

import com.nickax.nexus.api.lang.Lang;
import com.nickax.nexus.api.lang.Placeholder;
import com.nickax.nexus.api.text.TextFormat;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Formats and sends messages to players and the console. This is the consumer-facing
 * text API: it takes plain {@link String} markup and a {@link TextFormat}, never an
 * Adventure component, so it is callable from any plugin regardless of how that plugin
 * bundles (or relocates) Adventure. Formatting happens inside Nexus with the chosen
 * dialect; the resulting component is sent through the shared audiences (Spigot/Paper
 * safe). Obtain via {@code BukkitNexus.messages()}.
 */
public interface Messages {

    /**
     * Formats {@code miniMessage} as MiniMessage and sends it to a single recipient.
     *
     * @param target      the recipient (player or console)
     * @param miniMessage the MiniMessage markup
     */
    void send(@NotNull CommandSender target, @NotNull String miniMessage);

    /**
     * Formats {@code markup} with the given dialect and sends it to a single recipient.
     *
     * @param target the recipient (player or console)
     * @param format the markup dialect
     * @param markup the markup line
     */
    void send(@NotNull CommandSender target, @NotNull TextFormat format, @NotNull String markup);

    /**
     * Formats each line with the given dialect, joins them with newlines, and sends the
     * result to a single recipient.
     *
     * @param target the recipient (player or console)
     * @param format the markup dialect
     * @param lines  the markup lines
     */
    void send(@NotNull CommandSender target, @NotNull TextFormat format, @NotNull List<String> lines);

    /**
     * Formats {@code markup} with the given dialect and sends it to every online player.
     *
     * @param format the markup dialect
     * @param markup the markup line
     */
    void broadcast(@NotNull TextFormat format, @NotNull String markup);

    /**
     * Formats each line with the given dialect, joins them with newlines, and sends the
     * result to every online player.
     *
     * @param format the markup dialect
     * @param lines  the markup lines
     */
    void broadcast(@NotNull TextFormat format, @NotNull List<String> lines);

    /**
     * Formats {@code miniMessage} as MiniMessage and sends it to a recipient's action bar.
     *
     * @param target      the recipient (player or console)
     * @param miniMessage the MiniMessage markup
     */
    void sendActionBar(@NotNull CommandSender target, @NotNull String miniMessage);

    /**
     * Formats {@code markup} with the given dialect and sends it to a recipient's action bar.
     *
     * @param target the recipient (player or console)
     * @param format the markup dialect
     * @param markup the markup line
     */
    void sendActionBar(@NotNull CommandSender target, @NotNull TextFormat format, @NotNull String markup);

    /**
     * Resolves a localized message for the recipient's locale, formats it with the given
     * dialect, and sends it as chat. The locale comes from the recipient's client (for
     * players) or the lang's default locale (for the console). Placeholder values are
     * escaped by the lang so they cannot inject markup.
     *
     * @param target       the recipient
     * @param lang         the lang providing the localized templates
     * @param format       the markup dialect to format the resolved template with
     * @param key          the message key
     * @param placeholders placeholders to substitute
     */
    void send(@NotNull CommandSender target, @NotNull Lang lang, @NotNull TextFormat format,
              @NotNull String key, @NotNull Placeholder @NotNull ... placeholders);

    /**
     * Resolves a localized message for the recipient's locale, formats it with the given
     * dialect, and sends it to the recipient's action bar.
     *
     * @param target       the recipient
     * @param lang         the lang providing the localized templates
     * @param format       the markup dialect
     * @param key          the message key
     * @param placeholders placeholders to substitute
     */
    void sendActionBar(@NotNull CommandSender target, @NotNull Lang lang, @NotNull TextFormat format,
                       @NotNull String key, @NotNull Placeholder @NotNull ... placeholders);
}
