package io.github.kisligi.alerts.delivery;

import java.time.Duration;

/** Waits between retries. An interface so tests can check the waits without waiting. */
@FunctionalInterface
public interface Sleeper {

    void sleep(Duration duration) throws InterruptedException;

    Sleeper THREAD_SLEEP = duration -> Thread.sleep(duration);
}
