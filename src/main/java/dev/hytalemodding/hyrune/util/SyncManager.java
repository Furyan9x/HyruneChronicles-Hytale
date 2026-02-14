package dev.hytalemodding.hyrune.util;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * Manager for sync.
 */
public class SyncManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
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
            } catch (RuntimeException e) {
                LOGGER.at(Level.WARNING).log("Error executing sync task: " + e.getMessage());
            }
        }
    }
}

