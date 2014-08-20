package hgm.poly.gm;

import hgm.poly.*;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 7/07/14
 * Time: 8:04 AM
 */
public class Distributions {
    private PolynomialFactory factory;

    public Distributions(PolynomialFactory factory) {
        this.factory = factory;
    }
    /*new PriorHandler(varBaseName, numVars) {
        @Override
        public PiecewisePolynomial makePrior() {
            String[] vars = factory.getAllVars();
            int numVars = vars.length;//factory.numberOfVars();
            //1. prior: pr(W)
            String[] constraints = new String[numVars * 2];
            for (int i = 0; i < factory.numberOfVars(); i++) {
                String w_i = vars[i];//valueVectorName + "_" + i;
                constraints[2 * i] = w_i + "^(1) + " + bound + ">0";
                constraints[2 * i + 1] = "-1*" + w_i + "^(1) + " + bound + ">0";
            }

            ConstrainedPolynomial singeCase = factory.makeConstrainedPolynomial("1", constraints);//only one case...
            return new PiecewisePolynomial(false, singeCase); //otherwise 0
        }

        @Override
        protected Pair<double[], double[]> surroundingLowerUpperBounds() {
            double[] mins = new double[factory.numberOfVars()];
            double[] maxes = new double[factory.numberOfVars()];
            for (int i = 0; i < factory.numberOfVars(); i++) {
                mins[i] = -bound;
                maxes[i] = bound;
            }
            return new Pair<double[], double[]>(mins, maxes);
        }

        @Override
        protected double functionUpperBound() {
            return 1.0;
        }
    };*/

    public PiecewiseExpression createUniformDistribution(String var, String leftBoundStr, String rightBoundStr) {
        return createUniformDistribution(var, factory.makePolynomial(leftBoundStr), factory.makePolynomial(rightBoundStr));
    }

    /**
     *
     * @return U(var; leftBound, rightBound)
     */
    public PiecewiseExpression createUniformDistribution(String var, Polynomial leftBound, Polynomial rightBound){
        Polynomial negLB = leftBound.clone();
        negLB.multiplyScalarInThis(-1.0);
                String[] constraints = new String[]{
                var + "^(1) + " + negLB + ">0",
         "-1*" + var + "^(1) + " + rightBound + ">0"
        };
        ConstrainedExpression singleCase = factory.makeConstrainedPolynomial("1", constraints);
        return new PiecewiseExpression(false, singleCase);
    }

    public PiecewiseExpression<Fraction> createUniformDistributionFraction(String var, String leftBoundStr, String rightBoundStr) {
        return createUniformDistributionFraction(var, factory.makeFraction(leftBoundStr), factory.makeFraction(rightBoundStr));
    }

    public PiecewiseExpression<Fraction> createUniformDistributionFraction(String var, Fraction leftBound, Fraction rightBound){
        Fraction varF = factory.makeFraction(var + "^(1)");
        Fraction f1 = varF.subtract(leftBound);       //x - a >0
        Fraction f2 = rightBound.subtract(varF);      //b - x >0
        Fraction d = factory.makeFraction("1").divide(rightBound.subtract(leftBound));                                     // 1 / b - a
        ConstrainedExpression<Fraction> singleCase = new ConstrainedExpression<Fraction>(d, Arrays.asList(f1, f2));
        return new PiecewiseExpression<Fraction>(false, singleCase);
    }

}