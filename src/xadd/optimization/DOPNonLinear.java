package xadd.optimization;

import java.util.Collection;
import java.util.Set;

public class DOPNonLinear implements IOptimisationTechnique {
    @Override
    public OptimisationResult run(OptimisationDirection maxMin, String objective, Set<String> variables,
                                  Collection<String> constraints,
                                    Collection<String> lowerBounds, Collection<String> upperBounds) {
        return null;
    }
}
