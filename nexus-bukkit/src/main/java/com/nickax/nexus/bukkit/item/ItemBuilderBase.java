package com.nickax.nexus.bukkit.item;

import com.nickax.nexus.api.text.Text;
import com.nickax.nexus.bukkit.text.LegacyText;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared fluent base for item builders. Holds the {@link ItemStack} and its
 * {@link ItemMeta} and implements every customisation common to all items — amount,
 * display name, lore, enchantments, item flags, unbreakability, custom model data and
 * persistent-data tags — so that {@link ItemBuilder} and {@link SkullBuilder} expose
 * the same surface without duplicating it.
 * <p>
 * Names and lore are authored as MiniMessage and applied via legacy-section
 * serialization so the result renders on Spigot and Paper alike; string tags are
 * stored in the persistent data container under the {@code nexus} namespace. Fluent
 * methods return the concrete builder type via {@link #self()}.
 *
 * @param <SELF> the concrete builder type returned from every fluent method
 */
public abstract class ItemBuilderBase<SELF extends ItemBuilderBase<SELF>> {

    /** The backing stack being configured; cloned out by {@link #build()}. */
    protected final ItemStack stack;

    /** The meta changes accumulate on before {@link #build()} writes it back. */
    protected final ItemMeta meta;

    /**
     * Stores the stack and the meta the subclass resolved for it.
     *
     * @param stack the backing item stack
     * @param meta  the item meta to accumulate changes on
     */
    protected ItemBuilderBase(@NotNull ItemStack stack, @NotNull ItemMeta meta) {
        this.stack = stack;
        this.meta = meta;
    }

    /**
     * Returns this builder as its concrete {@code SELF} type for fluent chaining.
     *
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    protected final @NotNull SELF self() {
        return (SELF) this;
    }

    /**
     * Sets the stack size of the item.
     *
     * @param amount the stack size
     * @return this builder
     */
    public @NotNull SELF amount(int amount) {
        stack.setAmount(amount);
        return self();
    }

    /**
     * Sets the display name from a MiniMessage string.
     *
     * @param miniMessage the MiniMessage display name
     * @return this builder
     */
    public @NotNull SELF name(@NotNull String miniMessage) {
        return name(Text.of(miniMessage));
    }

    /**
     * Sets the display name from an Adventure component, serialized to legacy section codes.
     *
     * @param component the display name component
     * @return this builder
     */
    public @NotNull SELF name(@NotNull Component component) {
        meta.setDisplayName(LegacyText.serialize(component));
        return self();
    }

    /**
     * Sets the item's lore from MiniMessage strings, replacing any existing lore.
     *
     * @param miniMessageLines the MiniMessage lore lines
     * @return this builder
     */
    public @NotNull SELF lore(@NotNull String @NotNull ... miniMessageLines) {
        List<String> lines = new ArrayList<>(miniMessageLines.length);
        for (String line : miniMessageLines) {
            lines.add(LegacyText.serialize(Text.of(line)));
        }
        meta.setLore(lines);
        return self();
    }

    /**
     * Adds an enchantment to the item, bypassing level restrictions.
     *
     * @param enchantment the enchantment to apply
     * @param level       the enchantment level
     * @return this builder
     */
    public @NotNull SELF enchant(@NotNull Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return self();
    }

    /**
     * Adds the given item flags to the item meta.
     *
     * @param flags item flags to add
     * @return this builder
     */
    public @NotNull SELF flags(@NotNull ItemFlag @NotNull ... flags) {
        meta.addItemFlags(flags);
        return self();
    }

    /**
     * Sets whether the item is unbreakable.
     *
     * @param unbreakable {@code true} to make the item unbreakable
     * @return this builder
     */
    public @NotNull SELF unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return self();
    }

    /**
     * Sets the custom model data value for resource-pack overrides.
     *
     * @param modelData the custom model data integer
     * @return this builder
     */
    public @NotNull SELF model(int modelData) {
        meta.setCustomModelData(modelData);
        return self();
    }

    /**
     * Sets a string tag in the persistent data container under the {@code nexus} namespace.
     *
     * @param key   the tag key (must match {@code [a-z0-9._-]})
     * @param value the string value
     * @return this builder
     * @throws IllegalArgumentException if the key is not a valid namespaced key
     */
    public @NotNull SELF tag(@NotNull String key, @NotNull String value) {
        NamespacedKey namespacedKey = NamespacedKey.fromString("nexus:" + key);
        if (namespacedKey == null) {
            throw new IllegalArgumentException("Invalid PDC key: '" + key + "' (must match [a-z0-9._-])");
        }
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, value);
        return self();
    }

    /**
     * Applies the accumulated meta to the item stack and returns a clone. The builder
     * remains usable after calling this method.
     *
     * @return the built item stack
     */
    public @NotNull ItemStack build() {
        stack.setItemMeta(meta);
        return stack.clone();
    }
}
