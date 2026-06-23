package com.nickax.nexus.bukkit.item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builder for a player-head {@link ItemStack}. Inherits the full item surface (name,
 * lore, amount, enchantments, flags, unbreakability, custom model data, persistent-data
 * tags) from {@link ItemBuilderBase}, and adds head-specific ownership: a head can be
 * pointed at an {@link OfflinePlayer} via {@link #of(OfflinePlayer)} /
 * {@link #owner(OfflinePlayer)}, or given an arbitrary skin from a base64 texture value
 * via {@link #texture(String)}.
 */
public final class SkullBuilder extends ItemBuilderBase<SkullBuilder> {

    /** Matches the SKIN url inside a decoded base64 textures payload, tolerating other keys. */
    private static final Pattern SKIN_URL = Pattern.compile("\"SKIN\"\\s*:\\s*\\{[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"");

    private final SkullMeta skullMeta;

    /**
     * Wraps a resolved head stack and its skull meta.
     *
     * @param stack the player-head item stack
     * @param meta  the resolved skull meta (also handed to the base as item meta)
     */
    private SkullBuilder(@NotNull ItemStack stack, @NotNull SkullMeta meta) {
        super(stack, meta);
        this.skullMeta = meta;
    }

    /**
     * Creates a skull builder for the given owner, resolving the {@link SkullMeta} and
     * setting the owning player immediately.
     *
     * @param owner the player whose head this item represents
     * @return a new skull builder
     * @throws IllegalStateException if {@code PLAYER_HEAD} unexpectedly lacks a
     *                               {@link SkullMeta} (should never happen in practice)
     */
    public static @NotNull SkullBuilder of(@NotNull OfflinePlayer owner) {
        SkullBuilder builder = create();
        builder.skullMeta.setOwningPlayer(owner);
        return builder;
    }

    /**
     * Creates a skull builder showing the skin encoded in a base64 textures value (the
     * {@code value} field of a {@code textures} game-profile property, as published by
     * head databases). The SKIN url is decoded from the payload and applied to a fresh,
     * randomly-identified profile, so the head renders the texture without belonging to
     * any real player.
     *
     * @param base64 the base64-encoded textures value
     * @return a new skull builder
     * @throws IllegalStateException    if {@code PLAYER_HEAD} unexpectedly lacks a {@link SkullMeta}
     * @throws IllegalArgumentException if the value is not valid base64 or has no SKIN url
     */
    public static @NotNull SkullBuilder texture(@NotNull String base64) {
        SkullBuilder builder = create();
        applyTexture(builder.skullMeta, base64);
        return builder;
    }

    /**
     * Changes the player whose head this item represents.
     *
     * @param owner the new owning player
     * @return this builder
     */
    public @NotNull SkullBuilder owner(@NotNull OfflinePlayer owner) {
        skullMeta.setOwningPlayer(owner);
        return this;
    }

    /**
     * Creates an empty player-head builder, resolving its {@link SkullMeta}.
     *
     * @return a new skull builder with no owner or texture set
     * @throws IllegalStateException if {@code PLAYER_HEAD} unexpectedly lacks a {@link SkullMeta}
     */
    private static @NotNull SkullBuilder create() {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);

        ItemMeta itemMeta = stack.getItemMeta();
        if (!(itemMeta instanceof SkullMeta meta)) {
            throw new IllegalStateException("PLAYER_HEAD has no SkullMeta");
        }

        return new SkullBuilder(stack, meta);
    }

    /**
     * Applies a base64 textures value to the skull meta by decoding its SKIN url and
     * setting it on a fresh, randomly-identified {@link PlayerProfile}.
     *
     * @param meta   the skull meta to modify
     * @param base64 the base64-encoded textures value
     * @throws IllegalArgumentException if the value is not valid base64 or has no SKIN url
     */
    private static void applyTexture(@NotNull SkullMeta meta, @NotNull String base64) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(skinUrl(base64));
        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
    }

    /**
     * Decodes a base64 textures value and extracts its SKIN url.
     *
     * @param base64 the base64-encoded textures value
     * @return the SKIN url
     * @throws IllegalArgumentException if the value is not valid base64, has no SKIN url,
     *                                  or the url is malformed
     */
    private static @NotNull URL skinUrl(@NotNull String base64) {
        String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);

        Matcher matcher = SKIN_URL.matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Base64 texture has no SKIN url: " + json);
        }

        try {
            return URI.create(matcher.group(1)).toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Base64 texture has a malformed SKIN url: " + matcher.group(1), e);
        }
    }
}
