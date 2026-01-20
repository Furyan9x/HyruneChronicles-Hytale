package dev.hytalemodding.origins.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SyncManager {
    // A queue of simple Runnables (code blocks)
    private static final Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<>();

    /**
     * Schedule a task to run on the main World Thread during the next tick.
     * Safe to call from any thread (Commands, Database, etc).
     */
    public static void runSync(Runnable task) {
        pendingTasks.offer(task);
    }

    /**
     * Process all queued tasks.
     * THIS MUST ONLY BE CALLED BY THE TICKING SYSTEM.
     */
    public static void processQueue() {
        Runnable task;
        while ((task = pendingTasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println("[Origins] Error executing sync task: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}