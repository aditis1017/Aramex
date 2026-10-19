package com.customcharge.persistencelayer.recordtracking;

/**
 * Runs record-tracking side effects without interrupting business flow.
 */
public final class RecordTrackingGuard {

    private RecordTrackingGuard() {
    }

    public static void runSafely(Runnable action) {
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (Exception ignored) {
            // Tracking must never affect business flow.
        }
    }
}
