package xadd.optimization;

/**
 * Created by skin on 2016-08-23.
 */
public class OptimisationResult {

    private Double maxValue;
    private Double argMax;
    private Double cpuTime;

    /**
     *
     * @param maxValue
     * @param argMax
     * @param cpuTime
     */
    public OptimisationResult(Double maxValue, Double argMax, Double cpuTime) {
        this.maxValue = maxValue;
        this.argMax = argMax;
        this.cpuTime = cpuTime;
    }

    /**
     *
     * @param maxValue
     * @param argMax
     */
    public OptimisationResult(Double maxValue, Double argMax) {
        this(maxValue, argMax, null);
    }

    /**
     *
     * @param maxValue
     */
    public OptimisationResult(Double maxValue) {
        this(maxValue, null, null);
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public Double getArgMax() {
        return argMax;
    }

    public Double getCpuTime() {
        return cpuTime;
    }

}
