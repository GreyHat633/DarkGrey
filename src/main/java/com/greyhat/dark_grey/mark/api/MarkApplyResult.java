package com.greyhat.dark_grey.mark.api;

public final class MarkApplyResult {

    public final boolean success;
    public final boolean firstApplication;
    public final boolean reachedMax;
    public final boolean wasAlreadyMax;

    public final int oldStacks;
    public final int requestedStacks;
    public final int actualAddedStacks;
    public final int newStacks;

    public final String failureReason;

    public MarkApplyResult(boolean success, boolean firstApplication, boolean reachedMax, boolean wasAlreadyMax,
        int oldStacks, int requestedStacks, int actualAddedStacks, int newStacks, String failureReason) {
        this.success = success;
        this.firstApplication = firstApplication;
        this.reachedMax = reachedMax;
        this.wasAlreadyMax = wasAlreadyMax;
        this.oldStacks = oldStacks;
        this.requestedStacks = requestedStacks;
        this.actualAddedStacks = actualAddedStacks;
        this.newStacks = newStacks;
        this.failureReason = failureReason;
    }
}
