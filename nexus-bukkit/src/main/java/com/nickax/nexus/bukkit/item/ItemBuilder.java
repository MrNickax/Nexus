package com.nickax.nexus.bukkit.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Fluent {@link ItemStack} builder. Inherits every common customisation (amount,
 * name, lore, enchantments, flags, unbreakability, custom model data, persistent-data
 * tags) from {@link ItemBuilderBase}; this class only adds the material entry point.
 */
public final class ItemBuilder extends ItemBuilderBase<ItemBuilder> {

    /**
     * Wraps a resolved stack and its meta.
     *
     * @param stack the backing item stack
     * @param meta  the resolved item meta
     */
    private ItemBuilder(@NotNull ItemStack stack, @NotNull ItemMeta meta) {
        super(stack, meta);
    }

    /**
     * Creates a builder for the given material, failing fast if the material does not
     * support item meta.
     *
     * @param material the item material
     * @return a new item builder
     * @throws IllegalArgumentException if the material produces no {@link ItemMeta}
     */
    public static @NotNull ItemBuilder of(@NotNull Material material) {
        ItemStack stack = new ItemStack(material);

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            throw new IllegalArgumentException(material + " has no item meta");
        }

        return new ItemBuilder(stack, meta);
    }
}
