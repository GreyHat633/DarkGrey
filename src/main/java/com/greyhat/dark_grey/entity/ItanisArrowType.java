package com.greyhat.dark_grey.entity;

public enum ItanisArrowType {

    NORMAL,
    BONUS,
    CHARGE_AUTO,
    CHARGE_ORBIT,
    FULL_CHARGE;

    public static ItanisArrowType fromInt(int id) {
        if (id >= 0 && id < values().length) {
            return values()[id];
        }
        return NORMAL;
    }
}
