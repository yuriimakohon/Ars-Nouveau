package com.hollingsworth.arsnouveau.common.spell.augment;

import com.hollingsworth.arsnouveau.ModConfig;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import javax.annotation.Nullable;

public class AugmentExtract extends AbstractAugment {

    public AugmentExtract() {
        super(ModConfig.AugmentExtractID, "Extract");
    }

    @Override
    public int getManaCost() {
        return 30;
    }

    @Nullable
    @Override
    public Item getCraftingReagent() {
        return Items.SHEARS;
    }

    @Override
    public Tier getTier() {
        return Tier.TWO;
    }

    @Override
    protected String getBookDescription() {
        return "Applies a silk-touch effect to Break and causes Explosion to not destroy blocks that drop. Cannot be combined with Fortune.";
    }
}
