package net.bennysmith.simplywands.item.custom;

import net.bennysmith.simplywands.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

public class MagneticWand extends Item {
    // Indicates whether the wand is active
    private boolean isActive = false;

    public MagneticWand(Properties properties) {
        super(properties);
        NeoForge.EVENT_BUS.register(this);
    }

    // Determines if the item has the "foil" effect (glowing) based on its active state
    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive;
    }

    // Called when the player uses the wand (right-clicks)
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {    // Ensure this only runs on the server side
            toggleActive(level, player);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    public void toggleActive(Level level, Player player) {
        isActive = !isActive;
        playToggleSound(level, player);
        displayToggleMessage(player);
    }

    private void playToggleSound(Level level, Player player) {
        BlockPos blockPos = player.blockPosition();
        if (isActive) {
            level.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F); // Normal pitch when activated
            level.playSound(player, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F); //
        } else {
            level.playSound(null, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.5F); // Lower pitch when deactivated
            level.playSound(player, blockPos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.5F);
        }
    }

    private void displayToggleMessage(Player player) {
        if (isActive) {
            player.displayClientMessage(Component.literal("Magnet on").withStyle(ChatFormatting.GREEN), true); // Display "Magnet on" in green color
        } else {
            player.displayClientMessage(Component.literal("Magnet off").withStyle(ChatFormatting.RED), true); // Display "Magnet off" in red color
        }
    }

    // Event handler: Called every tick for each player
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Check if the player has the wand in their inventory
        boolean hasMagneticWand = player.getInventory().contains(new ItemStack(this));

        // Only attract items if the wand is in the inventory and active
        if (hasMagneticWand && isActive) {
            attractItems(player);
        } else if (!hasMagneticWand && isActive) {
            // Deactivate the wand if it's not in the inventory
            isActive = false;
        }
    }

    // Handles the attraction of nearby items towards the player
    private void attractItems(Player player) {
        Level level = player.level();
        // Find all items within the configured range around the player
        double range = Config.magneticWandRange;
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(range));

        // Move each item towards the player
        for (ItemEntity item : items) {
            double dx = player.getX() - item.getX();
            double dy = player.getY() - item.getY();
            double dz = player.getZ() - item.getZ();

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);           // Calculate distance
            double speed = Config.magneticWandSpeed;                            // Set the speed of attraction

            // Set the item's movement vector towards the player
            item.setDeltaMovement(dx / distance * speed, dy / distance * speed, dz / distance * speed);
        }
    }

    // Tooltip
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.simplywands.magnetic_wand.tooltip"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    // Disallow enchanting
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }
}
