package server_core;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class User {
    final String name;
    final AtomicLong credits = new AtomicLong(0);
    final AtomicLong creditsSpent = new AtomicLong(0);
    final AtomicLong runsCount = new AtomicLong(0);
    final AtomicInteger mainUploaded = new AtomicInteger(0);
    final AtomicInteger helperContrib = new AtomicInteger(0);
    volatile long lastSeenMs = System.currentTimeMillis();

    User(String name) { this.name = name; }
}