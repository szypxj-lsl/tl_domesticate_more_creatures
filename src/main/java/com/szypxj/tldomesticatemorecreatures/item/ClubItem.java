package com.szypxj.tldomesticatemorecreatures.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

import java.util.UUID;

public final class ClubItem extends Item {
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("b5b2b42a-2c57-4f67-8917-6b3cc95c2a0e");

    public ClubItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }
        double attackDamageModifier = Math.max(0.0D, Config.CLUB_DAMAGE.get()) - 1.0D;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        attackDamageModifier,
                        AttributeModifier.Operation.ADDITION
                )
        );
        return builder.build();
    }
}
