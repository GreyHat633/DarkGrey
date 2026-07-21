package com.greyhat.dark_grey.component;

public enum ItanisMode {

    RAPID,
    CHARGE;

    public static ItanisMode fromInt(int id) {
        if (id == 1) return CHARGE;
        return RAPID;
    }
}
