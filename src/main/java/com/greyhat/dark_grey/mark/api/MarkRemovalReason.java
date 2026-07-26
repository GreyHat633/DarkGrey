package com.greyhat.dark_grey.mark.api;

public enum MarkRemovalReason {
    EXPIRED,
    DECAYED_TO_ZERO,
    CLEANSED,
    CONSUMED,
    ENTITY_DEATH,
    COMMAND,
    SCRIPT,
    UNKNOWN_TYPE,
    INVALID_DATA,
    WORLD_UNLOAD
}
