package org.ginafro.notenoughfakepixel.features.skyblock.dungeons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.ginafro.notenoughfakepixel.Configuration;
import org.ginafro.notenoughfakepixel.utils.RenderUtils;
import org.ginafro.notenoughfakepixel.utils.ScoreboardUtils;
import org.ginafro.notenoughfakepixel.variables.MobDisplayTypes;

import java.awt.*;

public class ItemSecretsDisplay {

    String idTrainingWeights = "32d530e8-2686-3a8c-bc41-ce3650e12bdf";
    String idTreasureTalisman = "9c287464-1a06-3eed-8974-6dcc511d63b2";
    String idWitherEssence = "96f80a4b-daac-35e7-9c04-1a1544aadeb3";

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!Configuration.dungeonsItemSecretsDisplay) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;
        if (Minecraft.getMinecraft().theWorld == null) return;
        if (!ScoreboardUtils.currentLocation.isDungeon()) return;

        WorldClient world = Minecraft.getMinecraft().theWorld;

        Color color = new Color(
                Configuration.dungeonsItemSecretsColor.getRed(),
                Configuration.dungeonsItemSecretsColor.getGreen(),
                Configuration.dungeonsItemSecretsColor.getBlue(),
                Configuration.dungeonsItemSecretsColor.getAlpha()
        );

        MobDisplayTypes hitboxType;
        if (Configuration.dungeonsItemSecretsBig) hitboxType = MobDisplayTypes.ITEMBIG;
        else {
            hitboxType = MobDisplayTypes.ITEM;
        }

        world.loadedEntityList.forEach(entity -> {
            if (entity == null) return;
            if (entity.getName() == null) return;
            if (entity.getName().equals("item.item.skull.char")){
                NBTTagCompound nbt = entity.serializeNBT();
                if (nbt.hasKey("Item") && nbt.getCompoundTag("Item").hasKey("tag") && nbt.getCompoundTag("Item").getCompoundTag("tag").hasKey("SkullOwner") && nbt.getCompoundTag("Item").getCompoundTag("tag").getCompoundTag("SkullOwner").hasKey("Id")) {
                    String id = nbt.getCompoundTag("Item").getCompoundTag("tag").getCompoundTag("SkullOwner").getString("Id");
                    if (id.equals(idTrainingWeights) || id.equals(idTreasureTalisman)) {
                        GlStateManager.disableDepth();
                        RenderUtils.renderEntityHitbox(
                                entity,
                                event.partialTicks,
                                color,
                                hitboxType);
                        GlStateManager.enableDepth();
                    }
                }
            } else if (entity.getName().equals("item.item.shears") || entity.getName().equals("item.item.monsterPlacer")) {
                GlStateManager.disableDepth();
                RenderUtils.renderEntityHitbox(
                        entity,
                        event.partialTicks,
                        color,
                        hitboxType
                );
                GlStateManager.enableDepth();
            } else if (entity instanceof EntityArmorStand) {
                ItemStack it = ((EntityArmorStand) entity).getEquipmentInSlot(4); // Head slot
                if (it != null && it.getItem() == Items.skull) {
                    NBTTagCompound nbt = it.getTagCompound();
                    if(nbt != null && nbt.hasKey("SkullOwner") && nbt.getCompoundTag("SkullOwner").hasKey("Id")) {
                        String id = nbt.getCompoundTag("SkullOwner").getString("Id");
                        if (id.equals(idWitherEssence)) {
                            GlStateManager.disableDepth();
                            RenderUtils.renderEntityHitbox(
                                    entity,
                                    event.partialTicks,
                                    color,
                                    MobDisplayTypes.WITHERESSENCE
                            );
                            GlStateManager.enableDepth();
                        }
                    }
                }
            }
        });
    }

    /*@SubscribeEvent
    public void onRenderItem(EntityViewRenderEvent event) {
        Entity entity = event.entity;
        if (entity == null) return;
        if (entity.getName() == null) return;
        if (entity.getName().equals("item.item.skull.char")){
            NBTTagCompound nbt = entity.serializeNBT();
            if (nbt.hasKey("Item") && nbt.getCompoundTag("Item").hasKey("tag") && nbt.getCompoundTag("Item").getCompoundTag("tag").hasKey("SkullOwner") && nbt.getCompoundTag("Item").getCompoundTag("tag").getCompoundTag("SkullOwner").hasKey("Id")) {
                String id = nbt.getCompoundTag("Item").getCompoundTag("tag").getCompoundTag("SkullOwner").getString("Id");
                if (id.equals(idTrainingWeights) || id.equals(idTreasureTalisman)) {
                    GlStateManager.disableDepth();
                    GlStateManager.scale(5,5,5);
                    GlStateManager.enableDepth();
                }
            }
        } else if (entity.getName().equals("item.item.shears") || entity.getName().equals("item.item.monsterPlacer")) {
            GlStateManager.disableDepth();
            GlStateManager.scale(5,5,5);
            GlStateManager.enableDepth();
        }
    }*/

    /*
    // TEST ONLY
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        Minecraft.getMinecraft().theWorld.loadedEntityList.forEach(entity -> {
            if (entity == null) return;
            if (entity.getName() == null) return;
            //if (entity.getPosition().getY() == 105) System.out.println(entity.getName());
            if (entity instanceof EntityArmorStand) {
                ItemStack it = ((EntityArmorStand) entity).getEquipmentInSlot(4); // Head slot
                if (it != null && it.getItem() == Items.skull) {
                    NBTTagCompound nbt = it.getTagCompound();
                    if (entity.getPosition().getY() == Minecraft.getMinecraft().thePlayer.getPosition().getY()) {
                        //System.out.println("ARMOR STAND");
                        //System.out.println(entity.getPosition().getX() + ", " + entity.getPosition().getY() + ", " + entity.getPosition().getZ());
                        //System.out.println(nbt);
                    }
                    if(nbt != null && nbt.hasKey("SkullOwner") && nbt.getCompoundTag("SkullOwner").hasKey("Id")) {
                        String id = nbt.getCompoundTag("SkullOwner").getString("Id");
                        //System.out.println(id);
                    }
                }
            } else if (entity.getName().equals("item.item.skull.char")) {
                NBTTagCompound nbt = entity.serializeNBT();
                if (entity.getPosition().getY() == Minecraft.getMinecraft().thePlayer.getPosition().getY()) {
                    //System.out.println("SKULL.CHAR");
                    //System.out.println(entity.getPosition().getX() + ", " + entity.getPosition().getY() + ", " + entity.getPosition().getZ());
                    //System.out.println(nbt);
                }
                if (nbt.hasKey("Item") && nbt.getCompoundTag("Item").hasKey("tag") && nbt.getCompoundTag("Item").getCompoundTag("tag").hasKey("SkullOwner") && nbt.getCompoundTag("Item").getCompoundTag("tag").getCompoundTag("SkullOwner").hasKey("Id")) {
                    String id = nbt.getCompoundTag("Item").getCompoundTag("tag").getCompoundTag("SkullOwner").getString("Id");
                    //System.out.println(id);
                }
            }  else if (entity.getName().equals("item.item.shears") || entity.getName().equals("item.item.monsterPlacer")) {
                NBTTagCompound nbt = entity.serializeNBT();
                System.out.println(nbt);
                System.out.println(((EntityItem) entity).getAge());
                System.out.println(((EntityItem) entity).lifespan);
            }
        });
    }*/
}
