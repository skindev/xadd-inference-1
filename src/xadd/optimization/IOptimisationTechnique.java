package xadd.optimization;

import java.util.*;

/**
 *
 */
public interface IOptimisationTechnique {


    /**
     *
     * @param objective
     * @param variables
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     */
    OptimisationResult run(String objective, Set<String> variables, Collection<String> constraints,
                                     Collection<String> lowerBounds, Collection<String> upperBounds);
}
