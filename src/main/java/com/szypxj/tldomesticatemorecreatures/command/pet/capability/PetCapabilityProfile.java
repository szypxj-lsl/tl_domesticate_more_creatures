package com.szypxj.tldomesticatemorecreatures.command.pet.capability;

import com.szypxj.tldomesticatemorecreatures.command.pet.provider.PetCommandCompatibilityApi;

import java.util.List;

public record PetCapabilityProfile(
        List<PetCommandCapability> attack,
        List<PetCommandCapability> move,
        List<PetCommandCapability> follow,
        List<PetCommandCapability> defend,
        List<PetCommandCapability> retreat,
        List<PetCommandCapability> land,
        List<PetCommandCompatibilityApi.NativeStateProvider> nativeStates
) {
    public PetCapabilityProfile {
        attack = List.copyOf(attack);
        move = List.copyOf(move);
        follow = List.copyOf(follow);
        defend = List.copyOf(defend);
        retreat = List.copyOf(retreat);
        land = List.copyOf(land);
        nativeStates = List.copyOf(nativeStates);
    }

    public boolean canLand() {
        return !land.isEmpty();
    }
}
