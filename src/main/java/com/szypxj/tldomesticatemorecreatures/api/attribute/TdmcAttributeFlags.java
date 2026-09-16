package com.szypxj.tldomesticatemorecreatures.api.attribute;

public record TdmcAttributeFlags(
        boolean visible,
        boolean upgradeable,
        boolean randomAssignable,
        boolean resettable,
        boolean tamingBonusEligible,
        boolean inheritable,
        boolean dynamicDisplay
) {
    public static final TdmcAttributeFlags DEFAULT = new TdmcAttributeFlags(
            true,
            false,
            false,
            false,
            false,
            false,
            false
    );

    public TdmcAttributeFlags withVisible(boolean value) {
        return new TdmcAttributeFlags(value, upgradeable, randomAssignable, resettable, tamingBonusEligible, inheritable, dynamicDisplay);
    }

    public TdmcAttributeFlags withUpgradeable(boolean value) {
        return new TdmcAttributeFlags(visible, value, randomAssignable, resettable, tamingBonusEligible, inheritable, dynamicDisplay);
    }

    public TdmcAttributeFlags withRandomAssignable(boolean value) {
        return new TdmcAttributeFlags(visible, upgradeable, value, resettable, tamingBonusEligible, inheritable, dynamicDisplay);
    }

    public TdmcAttributeFlags withResettable(boolean value) {
        return new TdmcAttributeFlags(visible, upgradeable, randomAssignable, value, tamingBonusEligible, inheritable, dynamicDisplay);
    }

    public TdmcAttributeFlags withTamingBonusEligible(boolean value) {
        return new TdmcAttributeFlags(visible, upgradeable, randomAssignable, resettable, value, inheritable, dynamicDisplay);
    }

    public TdmcAttributeFlags withInheritable(boolean value) {
        return new TdmcAttributeFlags(visible, upgradeable, randomAssignable, resettable, tamingBonusEligible, value, dynamicDisplay);
    }

    public TdmcAttributeFlags withDynamicDisplay(boolean value) {
        return new TdmcAttributeFlags(visible, upgradeable, randomAssignable, resettable, tamingBonusEligible, inheritable, value);
    }
}
