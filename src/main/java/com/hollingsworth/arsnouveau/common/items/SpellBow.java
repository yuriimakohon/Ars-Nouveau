package com.hollingsworth.arsnouveau.common.items;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.PlayerCaster;
import com.hollingsworth.arsnouveau.client.renderer.item.SpellBowRenderer;
import com.hollingsworth.arsnouveau.common.entity.EntitySpellArrow;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.hollingsworth.arsnouveau.setup.ItemsRegistry;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SpellBow extends BowItem implements IAnimatable, ICasterTool {

    public SpellBow(Properties p_40660_) {
        super(p_40660_);
    }

    public SpellBow() {
        super(ItemsRegistry.defaultItemProperties().stacksTo(1));
    }

    public boolean canPlayerCastSpell(ItemStack bow, Player playerentity) {
        ISpellCaster caster = getSpellCaster(bow);
        return new SpellResolver(new SpellContext(playerentity.level, caster.getSpell(), playerentity, new PlayerCaster(playerentity))).withSilent(true).canCast(playerentity);
    }

    public ItemStack findAmmo(Player playerEntity, ItemStack shootable) {
        if (!(shootable.getItem() instanceof ProjectileWeaponItem projectileWeaponItem)) {
            return ItemStack.EMPTY;
        } else {
            Predicate<ItemStack> predicate = projectileWeaponItem.getSupportedHeldProjectiles()
                    .and(i -> !(i.getItem() instanceof SpellArrow) || (i.getItem() instanceof SpellArrow && canPlayerCastSpell(shootable, playerEntity)));
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(playerEntity, predicate);
            if (!itemstack.isEmpty()) {
                return net.minecraftforge.common.ForgeHooks.getProjectile(playerEntity, shootable, itemstack);
            } else {
                predicate = projectileWeaponItem.getAllSupportedProjectiles().and(i ->
                        !(i.getItem() instanceof SpellArrow) ||
                                (i.getItem() instanceof SpellArrow &&
                                        canPlayerCastSpell(shootable, playerEntity)));

                for (int i = 0; i < playerEntity.getInventory().getContainerSize(); ++i) {
                    ItemStack itemstack1 = playerEntity.inventory.getItem(i);
                    if (predicate.test(itemstack1)) {
                        return net.minecraftforge.common.ForgeHooks.getProjectile(playerEntity, shootable, itemstack1);
                    }
                }

                return net.minecraftforge.common.ForgeHooks.getProjectile(playerEntity, shootable, playerEntity.abilities.instabuild ? new ItemStack(Items.ARROW) : ItemStack.EMPTY);
            }
        }
    }

    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        ISpellCaster caster = getSpellCaster(playerIn.getItemInHand(handIn));
        boolean hasAmmo = !findAmmo(playerIn, itemstack).isEmpty();

        InteractionResultHolder<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onArrowNock(itemstack, worldIn, playerIn, handIn, hasAmmo);
        if (ret != null) return ret;

        if (hasAmmo || (caster.getSpell().isValid() && new SpellResolver(new SpellContext(worldIn, caster.getSpell(), playerIn, new PlayerCaster(playerIn))).withSilent(true).canCast(playerIn))) {
            playerIn.startUsingItem(handIn);
            return InteractionResultHolder.consume(itemstack);
        }

        if (!playerIn.abilities.instabuild && !hasAmmo) {
            return InteractionResultHolder.fail(itemstack);
        } else {
            playerIn.startUsingItem(handIn);
            return InteractionResultHolder.consume(itemstack);
        }
    }

    public EntitySpellArrow buildSpellArrow(Level worldIn, Player playerentity, ISpellCaster caster, boolean isSpellArrow) {
        EntitySpellArrow spellArrow = new EntitySpellArrow(worldIn, playerentity);
        spellArrow.spellResolver = new SpellResolver(new SpellContext(worldIn, caster.getSpell(), playerentity, new PlayerCaster(playerentity))).withSilent(true);
        spellArrow.setColors(caster.getColor());
        if (isSpellArrow)
            spellArrow.setBaseDamage(0);
        return spellArrow;
    }

    @Override
    public void releaseUsing(ItemStack bowStack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        //Copied from BowItem, so we can spawn arrows in case there are no items.
        if (!(entityLiving instanceof Player playerentity))
            return;
        boolean isInfinity = playerentity.abilities.instabuild || bowStack.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0;
        ItemStack arrowStack = findAmmo(playerentity, bowStack);

        int useTime = this.getUseDuration(bowStack) - timeLeft;
        useTime = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(bowStack, worldIn, playerentity, useTime, !arrowStack.isEmpty() || isInfinity);
        if (useTime < 0) return;
        boolean canFire = false;
        if (!arrowStack.isEmpty() || isInfinity) {
            if (arrowStack.isEmpty()) {
                arrowStack = new ItemStack(Items.ARROW);
            }
            canFire = true;
        }
        ISpellCaster caster = getSpellCaster(bowStack);
        boolean isSpellArrow = false;
        if (arrowStack.isEmpty() && caster.getSpell().isValid() && new SpellResolver(new SpellContext(worldIn, caster.getSpell(), playerentity, new PlayerCaster(playerentity))).canCast(playerentity)) {
            canFire = true;
            isSpellArrow = true;
        }

        if (!canFire)
            return;

        float f = getPowerForTime(useTime);
        boolean didCastSpell = false;
        if (f >= 0.1D) {
            boolean isArrowInfinite = playerentity.abilities.instabuild || (arrowStack.getItem() instanceof ArrowItem arrowItem && arrowItem.isInfinite(arrowStack, bowStack, playerentity));
            if (!worldIn.isClientSide) {
                ArrowItem arrowitem = (ArrowItem) (arrowStack.getItem() instanceof ArrowItem ? arrowStack.getItem() : Items.ARROW);
                AbstractArrow abstractarrowentity = arrowitem.createArrow(worldIn, arrowStack, playerentity);
                abstractarrowentity = customArrow(abstractarrowentity);

                List<AbstractArrow> arrows = new ArrayList<>();
                SpellResolver resolver = new SpellResolver(new SpellContext(worldIn, caster.modifySpellBeforeCasting(worldIn, entityLiving, InteractionHand.MAIN_HAND, caster.getSpell()), playerentity, new PlayerCaster(playerentity)));
                if (arrowitem == Items.ARROW && resolver.withSilent(true).canCast(playerentity)) {
                    abstractarrowentity = buildSpellArrow(worldIn, playerentity, caster, isSpellArrow);
                    resolver.expendMana();
                    didCastSpell = true;
                } else if (arrowitem instanceof SpellArrow) {
                    if (!(resolver.canCast(playerentity))) {
                        return;
                    } else if (resolver.canCast(playerentity)) {
                        resolver.expendMana();
                        didCastSpell = true;
                    }
                }
                arrows.add(abstractarrowentity);
                if (caster.getSpell().isValid() && didCastSpell) {
                    int numSplits = caster.getSpell().getBuffsAtIndex(0, playerentity, AugmentSplit.INSTANCE);
                    if (abstractarrowentity instanceof EntitySpellArrow arrow) {
                        numSplits = arrow.spellResolver.spell.getBuffsAtIndex(0, playerentity, AugmentSplit.INSTANCE);
                    }

                    for (int i = 1; i < numSplits + 1; i++) {
                        Direction offset = playerentity.getDirection().getClockWise();
                        if (i % 2 == 0) offset = offset.getOpposite();
                        // Alternate sides
                        BlockPos projPos = playerentity.blockPosition().relative(offset, i);
                        projPos = projPos.offset(0, 1.5, 0);
                        EntitySpellArrow spellArrow = buildSpellArrow(worldIn, playerentity, caster, isSpellArrow);
                        spellArrow.setPos(projPos.getX(), spellArrow.blockPosition().getY(), projPos.getZ());
                        arrows.add(spellArrow);
                    }
                }
                for (AbstractArrow arr : arrows) {
                    arr.shootFromRotation(playerentity, playerentity.getXRot(), playerentity.getYRot(), 0.0F, f * 3.0F, 1.0F);
                    if (f >= 1.0F) {
                        arr.setCritArrow(true);
                    }
                    addArrow(arr, bowStack, arrowStack, isArrowInfinite, playerentity);
                }
            }

            worldIn.playSound(null, playerentity.getX(), playerentity.getY(), playerentity.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (worldIn.random.nextFloat() * 0.4F + 1.2F) + f * 0.5F);
            if (didCastSpell)
                caster.playSound(playerentity.getOnPos(), playerentity.level, playerentity, caster.getCurrentSound(), SoundSource.PLAYERS);
            if (!isArrowInfinite && !playerentity.abilities.instabuild) {
                arrowStack.shrink(1);
            }
        }
    }

    public void addArrow(AbstractArrow abstractarrowentity, ItemStack bowStack, ItemStack arrowStack, boolean isArrowInfinite, Player playerentity) {
        int power = bowStack.getEnchantmentLevel(Enchantments.POWER_ARROWS);
        if (power > 0) {
            abstractarrowentity.setBaseDamage(abstractarrowentity.getBaseDamage() + power * 0.5D + 0.5D);
        }

        int punch = bowStack.getEnchantmentLevel(Enchantments.PUNCH_ARROWS);
        if (punch > 0) {
            abstractarrowentity.setKnockback(punch);
        }

        if (bowStack.getEnchantmentLevel(Enchantments.FLAMING_ARROWS) > 0) {
            abstractarrowentity.setSecondsOnFire(100);
        }

        if (isArrowInfinite || playerentity.abilities.instabuild && (arrowStack.getItem() == Items.SPECTRAL_ARROW || arrowStack.getItem() == Items.TIPPED_ARROW)) {
            abstractarrowentity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        }
        playerentity.level.addFreshEntity(abstractarrowentity);
    }

    /**
     * Get the predicate to match ammunition when searching the player's inventory, not their main/offhand
     */
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ARROW_ONLY.or(i -> i.getItem() instanceof SpellArrow);
    }

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        return super.customArrow(arrow);
    }

    public AnimationFactory factory = GeckoLibUtil.createFactory(this);

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip2, TooltipFlag flagIn) {
        getInformation(stack, worldIn, tooltip2, flagIn);
        super.appendHoverText(stack, worldIn, tooltip2, flagIn);
    }

    @Override
    public boolean isScribedSpellValid(ISpellCaster caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        return spell.recipe.stream().noneMatch(s -> s instanceof AbstractCastMethod);
    }

    @Override
    public void sendInvalidMessage(Player player) {
        PortUtil.sendMessageNoSpam(player, Component.translatable("ars_nouveau.bow.invalid"));
    }

    @Override
    public boolean setSpell(ISpellCaster caster, Player player, InteractionHand hand, ItemStack stack, Spell spell) {
        ArrayList<AbstractSpellPart> recipe = new ArrayList<>();
        recipe.add(MethodProjectile.INSTANCE);
        recipe.addAll(spell.recipe);
        spell.recipe = recipe;
        return ICasterTool.super.setSpell(caster, player, hand, stack, spell);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new SpellBowRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }

    @NotNull
    @Override
    public ISpellCaster getSpellCaster(ItemStack stack) {
        return new BasicReductionCaster(stack, (spell -> {
            spell.addDiscount(MethodProjectile.INSTANCE.getCastingCost());
            return spell;
        }));
    }
}
