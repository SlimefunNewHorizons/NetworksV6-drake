package io.github.sefiraat.networks.network;

import org.bukkit.Location;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Motor de telemetría de throughput en tiempo real para redes y nodos individuales.
 * Proporciona tasas de items/segundo y recuento acumulado sin contención de hilos.
 */
public class NetworkThroughputTracker {

    private final AtomicLong totalTransferredItems = new AtomicLong(0);

    // Ventana deslizante de 1 segundo
    private volatile long currentSecondEpoch = System.currentTimeMillis() / 1000L;
    private final AtomicInteger currentSecondCount = new AtomicInteger(0);
    private volatile double lastReportedItemsPerSecond = 0.0;

    // Métricas por nodo individual
    private final Map<Location, NodeFlow> nodeFlowMap = new ConcurrentHashMap<>();

    public void recordFlow(@Nullable Location accessor, int amount) {
        if (amount <= 0) {
            return;
        }

        totalTransferredItems.addAndGet(amount);

        long nowSec = System.currentTimeMillis() / 1000L;
        if (nowSec != currentSecondEpoch) {
            synchronized (this) {
                if (nowSec != currentSecondEpoch) {
                    long diff = nowSec - currentSecondEpoch;
                    if (diff == 1) {
                        lastReportedItemsPerSecond = currentSecondCount.get();
                    } else {
                        lastReportedItemsPerSecond = 0.0;
                    }
                    currentSecondEpoch = nowSec;
                    currentSecondCount.set(0);
                }
            }
        }
        currentSecondCount.addAndGet(amount);

        if (accessor != null) {
            nodeFlowMap.computeIfAbsent(accessor, k -> new NodeFlow()).record(amount);
        }
    }

    public double getItemsPerSecond() {
        long nowSec = System.currentTimeMillis() / 1000L;
        if (nowSec - currentSecondEpoch > 2) {
            return 0.0;
        }
        if (nowSec == currentSecondEpoch) {
            return Math.max(lastReportedItemsPerSecond, (double) currentSecondCount.get());
        }
        return lastReportedItemsPerSecond;
    }

    public long getTotalTransferredItems() {
        return totalTransferredItems.get();
    }

    public double getNodeItemsPerSecond(@Nullable Location location) {
        if (location == null) {
            return 0.0;
        }
        NodeFlow flow = nodeFlowMap.get(location);
        return flow != null ? flow.getItemsPerSecond() : 0.0;
    }

    public long getNodeTotalTransferred(@Nullable Location location) {
        if (location == null) {
            return 0L;
        }
        NodeFlow flow = nodeFlowMap.get(location);
        return flow != null ? flow.getTotal() : 0L;
    }

    public void removeNode(@Nullable Location location) {
        if (location != null) {
            nodeFlowMap.remove(location);
        }
    }

    public static class NodeFlow {
        private final AtomicLong total = new AtomicLong(0);
        private volatile long currentSecond = System.currentTimeMillis() / 1000L;
        private final AtomicInteger thisSecond = new AtomicInteger(0);
        private volatile double lastRate = 0.0;

        public void record(int count) {
            total.addAndGet(count);
            long nowSec = System.currentTimeMillis() / 1000L;
            if (nowSec != currentSecond) {
                if (nowSec - currentSecond == 1) {
                    lastRate = thisSecond.get();
                } else {
                    lastRate = 0.0;
                }
                currentSecond = nowSec;
                thisSecond.set(0);
            }
            thisSecond.addAndGet(count);
        }

        public double getItemsPerSecond() {
            long nowSec = System.currentTimeMillis() / 1000L;
            if (nowSec - currentSecond > 2) {
                return 0.0;
            }
            if (nowSec == currentSecond) {
                return Math.max(lastRate, (double) thisSecond.get());
            }
            return lastRate;
        }

        public long getTotal() {
            return total.get();
        }
    }
}
