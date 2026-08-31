package eu.clarin.sru.fcs.aggregator.core;

public interface ShutdownParams {
    default long getExecutorShutdownTimeout() {
        return 1_000; // 1sec
    }
}
