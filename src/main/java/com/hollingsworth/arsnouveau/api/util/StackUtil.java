package com.hollingsworth.arsnouveau.api.util;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.item.IRadialProvider;
import com.hollingsworth.arsnouveau.api.item.ISpellHotkeyListener;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class StackUtil {

    public static @NotNull ItemStack getHeldSpellbook(Player playerEntity) {
        ItemStack book = playerEntity.getMainHandItem().getItem() instanceof SpellBook ? playerEntity.getMainHandItem() : null;
        return book == null ? (playerEntity.getOffhandItem().getItem() instanceof SpellBook ? playerEntity.getOffhandItem() : ItemStack.EMPTY) : book;
    }

    public static @Nullable InteractionHand getHeldCasterTool(Player player) {
        InteractionHand casterTool = player.getMainHandItem().getItem() instanceof ICasterTool ? InteractionHand.MAIN_HAND : null;
        return casterTool == null ? (player.getOffhandItem().getItem() instanceof ICasterTool ? InteractionHand.OFF_HAND : null) : casterTool;
    }

    public static @Nullable InteractionHand getHeldCasterTool(Player player, Predicate<ICasterTool> filter){
        if(player.getMainHandItem().getItem() instanceof ICasterTool casterTool && filter.test(casterTool))
            return InteractionHand.MAIN_HAND;
        if(player.getOffhandItem().getItem() instanceof ICasterTool casterTool && filter.test(casterTool))
            return InteractionHand.OFF_HAND;
        return null;
    }

    public static @Nullable InteractionHand getQuickCaster(Player player) {
        InteractionHand casterTool = player.getMainHandItem().getItem() instanceof ISpellHotkeyListener listener && listener.canQuickCast() ? InteractionHand.MAIN_HAND : null;
        return casterTool == null ? (player.getOffhandItem().getItem() instanceof ISpellHotkeyListener listener && listener.canQuickCast() ? InteractionHand.OFF_HAND : null) : casterTool;
    }

    public static @NotNull ItemStack getHeldRadial(Player playerEntity) {
        ItemStack book = playerEntity.getMainHandItem().getItem() instanceof IRadialProvider ? playerEntity.getMainHandItem() : ItemStack.EMPTY;
        return book.isEmpty() ? playerEntity.getOffhandItem() : book;
    }

    public static ItemStack getHeldCasterToolOrEmpty(Player player) {
        ItemStack stack = ItemStack.EMPTY;
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof ICasterTool){
            stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        }else if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ICasterTool){
            stack = player.getItemInHand(InteractionHand.OFF_HAND);
        }
        return stack;
    }
}
