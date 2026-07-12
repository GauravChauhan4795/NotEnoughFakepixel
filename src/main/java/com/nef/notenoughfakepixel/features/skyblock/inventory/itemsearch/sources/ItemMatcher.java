package com.nef.notenoughfakepixel.features.skyblock.inventory.itemsearch.sources;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

public class ItemMatcher {

    public static boolean matches(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem()) return false;
        if (a.getItemDamage() != b.getItemDamage()) return false;

        NBTTagCompound extraA = getExtraAttributes(a);
        NBTTagCompound extraB = getExtraAttributes(b);

        String idA = extraA != null && extraA.hasKey("id") ? extraA.getString("id") : null;
        String idB = extraB != null && extraB.hasKey("id") ? extraB.getString("id") : null;
        if (!Objects.equals(idA, idB)) return false;

        String modifierA = extraA != null && extraA.hasKey("modifier") ? extraA.getString("modifier") : null;
        String modifierB = extraB != null && extraB.hasKey("modifier") ? extraB.getString("modifier") : null;
        if (!Objects.equals(modifierA, modifierB)) return false;

        if (!enchantsMatch(a, b)) return false;

        return customNameMatch(a, b);
    }

    private static NBTTagCompound getExtraAttributes(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        return tag.hasKey("ExtraAttributes") ? tag.getCompoundTag("ExtraAttributes") : null;
    }

    private static boolean enchantsMatch(ItemStack a, ItemStack b) {
        NBTTagCompound tagA = a.getTagCompound();
        NBTTagCompound tagB = b.getTagCompound();
        boolean hasA = tagA != null && tagA.hasKey("ench");
        boolean hasB = tagB != null && tagB.hasKey("ench");
        if (hasA != hasB) return false;
        if (!hasA) return true;
        return tagA.getTagList("ench", 10).toString().equals(tagB.getTagList("ench", 10).toString());
    }

    private static boolean customNameMatch(ItemStack a, ItemStack b) {
        String nameA = getCustomName(a);
        String nameB = getCustomName(b);
        return Objects.equals(nameA, nameB);
    }

    private static String getCustomName(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
        return display.hasKey("Name") ? display.getString("Name") : null;
    }
}