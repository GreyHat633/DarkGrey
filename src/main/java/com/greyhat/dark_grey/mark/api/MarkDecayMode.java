package com.greyhat.dark_grey.mark.api;

/**
 * Defines how a mark loses stacks after its per-application stable period ends.
 */
public enum MarkDecayMode {

    /**
     * Enters a decay phase and repeatedly loses stacks at the mark type's own interval.
     */
    CONTINUOUS,

    /**
     * Loses stacks immediately, then starts another stable period if stacks remain.
     */
    INSTANT
}
