/* JBernstein: Exploration techniques for polynomial constraint checking
 *
 * Copyright (c) 2012, Chih-Hong Cheng
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Less General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Less General Public License for more details.
 *
 * You should have received a copy of the GNU Less General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package bernstein;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * The solver class for checking polynomial constraints via
 * Bernstein polynomials.
 *
 * Known bugs detected by FindBugs 2.0:
 *                  none (except one for performance improvement)
 *
 * Known bugs detected by CheckStyle:
 *                  1. Lines can exceed 80 characters
 *                  2. Method solveAssumeGuaranteeConstraints has 210 lines,
 *                     which exceeds > 150 lines listed in the standard.
 *                  3. Use variable naming that violate the coding standard
 *                     for b_k_left, b_k_right, r_k.
 *                     (However, it makes the matching between implementation
 *                      and algorithm easier)
 *
 * @author Chih-Hong Cheng
 * @version 0.2
 * @since 0.1
 */
public class Bernstein {

    /** Comparing signs greater than. */
    public static final int GT = 0;
    /** Comparing signs greater or equal. */
    public static final int GE = 1;
    /** Comparing signs less than. */
    public static final int LT = 2;
    /** Comparing signs less or equal than. */
    public static final int LE = 3;
    /** Result of the current Bernstein polynomial: unknown. */
    public static final int UNKNOWN = -1;
    /** Result of the current Bernstein polynomial: false. */
    public static final int FALSE = 0;
    /** Result of the current Bernstein polynomial: true. */
    public static final int TRUE = 1;
    /** Number of bits that can be used in the fraction part of the double. */
    private static final int FRACTION_IN_DOUBLE = 52;
    /** The default precision for the arithmetic truncation due to double
     * in the recursive expansion process. */
    private static final int DEFAULT_PRECISION = -25;
    /** The default precision for the solver to consider the upper and
     * lower-bound to be precise. This is a value computed by considering
     * how errors can propagate in recursion.
     */
    private static final int MAX_DIVISOR = 2;
    /** Maximum precision == no error loss. */
    private static final int MAX_PRECISION = -53;
    /** Allowed error for the imprecision of double. */
    private boolean automaticErrorEstimation = false;
    /** Data structure for indicating whether arithmetic for
    generating counter examples is also shown. */
    private boolean printPreciseCounterExample = false;
    /** Data structure for storing the value of a counter witness. */
    private double[] counterWitness;
    /** Data structure for storing a counter witness (in precise string). */
    private String[] counterWitnessString;
    /** Data structure for fetching binomial coefficients. */
    private int[][] binomialCoefficient;
    /** Data structure for storing the lower-bound for each variable. */
    private double[] lowerBound;
    /** Data structure for storing the upper-bound for each variable. */
    private double[] upperBound;
    // For simple properties: only one polynomial is used in the constraint
    /** Shift index for generating new Bernstein poly (simple). */
    private int[] shiftIndex;
    /** For checking if an index is an endpoint (simple).  */
    private boolean[] isNotEndIndex;
    // For assume-guarantee properties that each property
    // contains a set of polynomials (in assume-guarantee style)
    /** Shift index for generating new Bernstein poly (assume-guarantee). */
    private int[][] shiftIndexPolys;
    /** For checking if an index is an endpoint (assume-guarantee).  */
    private boolean[][] isNotEndIndexPolys;

    /**
     * Set to print the arithmetic for generating counter examples.
     *
     * @param print parameter (true: want to print)
     */
    public final void setPreciseCounterExample(final boolean print) {
        printPreciseCounterExample = print;
    }

    /**
     * Set to print the arithmetic for generating counter examples.
     *
     * @param print parameter (true: want to print)
     */
    public final void setAutomaticErrorEstimation(final boolean automatic) {
        automaticErrorEstimation = automatic;
    }

    /**
     * Solve constraint in simple form: phi(x0,...xm) {>, >=, <, <=} value.
     *
     * @param polynomial polynomial under analysis
     * @param peopertyOperator operator {>, >=, <, <=}
     * @param propertyValue value for comparision
     * @param recursionDepth the maximum allowed expansion for each variable
     * @return description (true, false, unknown)
     */
    public final String solveSimpleConstraint(final String polynomial,
            final int peopertyOperator, final double propertyValue,
            final int recursionDepth) {

        // Parse the problem under investigation


        // Number of variables
        int numOfVar = 0;
        String[] spec = polynomial.split("\\n");
        for (String line : spec) {
            if (line.trim().startsWith("VAR")) {
                numOfVar = Integer.parseInt(line.trim().split("\\s+")[1]);
                break;
            }
        }
        // Specify the highest degree for each variable
        int[] degree = new int[numOfVar];
        // Parse coefficient to derive the highest degree
        for (String line : spec) {
            if (line.trim().startsWith("COEF")) {
                String[] power = line.trim().split("[\\(\\)]");
                String[] varPower = power[1].split(",");
                for (int i = 0; i < varPower.length; i++) {
                    int value = Integer.parseInt(varPower[i].trim());
                    if (value > degree[i]) {
                        degree[i] = value;
                    }
                }
            }
        }

        int highestDegree = 0;
        for (int i = 0; i < degree.length; i++) {
            if (degree[i] > highestDegree) {
                highestDegree = degree[i];
            }
        }

        // Create the polynomial
        Polynomial poly = new Polynomial(numOfVar, degree);
        lowerBound = new double[numOfVar];
        upperBound = new double[numOfVar];

        // Specify the upper and lower bound for each variable
        for (String line : spec) {
            if (line.trim().startsWith("BOUND")) {
                int varIndex = Integer.parseInt(line.trim().split("\\s+")[1].substring(1));
                String[] value = line.trim().split("[\\[,\\]]");
                lowerBound[varIndex] = Double.parseDouble(value[1]);
                upperBound[varIndex] = Double.parseDouble(value[2]);
                poly.lowerBound[varIndex] = lowerBound[varIndex];
                poly.upperBound[varIndex] = upperBound[varIndex];
            }
        }

        boolean isPreciseCoefficientRepresentation = true;
        int numberOfNonzeroCoef = 0;
        double largestCoef = 0;
        for (String line : spec) {
            if (line.trim().startsWith("COEF")) {
                String power = line.trim().split("[\\(\\)]")[1].replaceAll("\\s", "").replaceAll("[,]", "_");
                double value = Double.parseDouble(line.trim().split("[\\(\\)]")[2]);
                poly.parameter[poly.terms.indexOf(power)] = value;

                if (Math.abs(value) > largestCoef) {
                    largestCoef = Math.abs(value);
                }
                // Check if the coefficient is an integer multiple of 16
                if ((value * Math.pow(MAX_DIVISOR, 4)) % 1 != 0) {
                    isPreciseCoefficientRepresentation = false;
                }
                numberOfNonzeroCoef++;
            }
        }

        // Compute the Binomial coefficient (this shall be computed only once
        // when being repeatedly called in SMT procedure)
        binomialCoefficient = new int[highestDegree + 1][highestDegree + 1];
        binom(highestDegree, highestDegree);

        shiftIndex = new int[numOfVar];
        shiftIndex[numOfVar - 1] = 1;
        for (int i = numOfVar - 2; i >= 0; i--) {
            shiftIndex[i] = shiftIndex[i + 1] * (degree[i + 1] + 1);
        }

        isNotEndIndex = new boolean[poly.allTerms.length];
        for (int k = 0; k < poly.allTerms.length; k++) {
            for (int i = 0; i < poly.allTerms[k].length; i++) {
                if (poly.allTerms[k][i] != 0 && poly.allTerms[k][i] != degree[i]) {
                    isNotEndIndex[k] = true;
                    break;
                }
            }
        }

        // Translate into a polynomial with each variable having only [0,1] as its domain
        Polynomial unitPoly = generateRangePreservingDomainUnitPolynomial(poly);

        double propertyValueUpper = propertyValue;
        double propertyValueLower = propertyValue;

        if (automaticErrorEstimation) {
            int imprecisionFromPolyToBernstein = (int) Math.ceil(Math.log(shiftIndex[0]
                    * (poly.highestOrder[poly.variableSize - 1] + 1))
                    / Math.log(2));
            int exponent = getErrorEstimate(poly, unitPoly,
                    isPreciseCoefficientRepresentation,
                    numberOfNonzeroCoef,
                    largestCoef, recursionDepth, imprecisionFromPolyToBernstein);
            if (exponent > 0) {
                return "UNKNOWN (the property is not guaranteed due to "
                        + "the use of double, error too large)\n";
            } else if (exponent != MAX_PRECISION) {
                propertyValueUpper = propertyValue + Math.pow(2, exponent);
                propertyValueLower = propertyValue - Math.pow(2, exponent);
            } else {
                if (((propertyValue * MAX_DIVISOR) % 1) != 0) {
                    System.out.println("However, rhs value can introduce error in double");
                    if (Math.abs(propertyValue) > Math.pow(2, 20)) {
                        return "UNKNOWN (the property is not guaranteed due to "
                                + "the use of large double in rhs)\n";
                    } else {
                        propertyValueUpper = propertyValue + Math.pow(2, DEFAULT_PRECISION);
                        propertyValueLower = propertyValue - Math.pow(2, DEFAULT_PRECISION);
                    }
                }
            }



        }
        // Translate into the Bernstein polynomial form
        Polynomial unitBernstein = generateBernsteinPolynomial(unitPoly,
                peopertyOperator, propertyValueUpper, propertyValueLower, true);
        int result = isPropertyHoldBFS(unitBernstein, peopertyOperator,
                propertyValueUpper, propertyValueLower, 0, recursionDepth, 0);

        if (result
                == TRUE) {
            return "TRUE\n";
        } else if (result
                == FALSE) {
            return "FALSE\n" + printCounterWitness();
        }


        return "UNKNOWN (please increase the recursion depth,\n"
                + "or the property is not guaranteed due to the use of double)\n";
    }

    /**
     * Solve constraints in assume-guarantee style.
     *
     * FIXME: 1. The imprecision detection mechanism due to
     *           the use of "double" is not integrated.
     * FIXME: 2. The data structure is optimized concerning the
     *           set intersection (for detecting counter-examples).
     *
     * @param polynomialConstraints the constraint system under analysis
     * @param recursionDepth the maximum allowed expansion for each variable
     * @return description (true, false, unknown)
     */
    public final String solveAssumeGuaranteeConstraints(
            final String polynomialConstraints, final int recursionDepth) {

        // Parse the problem under investigation

        // Specify the number of variables
        int numOfVar = 0;

        String[] spec = polynomialConstraints.split("\\n");
        for (String line : spec) {
            if (line.trim().startsWith("VAR")) {
                numOfVar = Integer.parseInt(line.trim().split("\\s+")[1]);
                break;
            }
        }

        // Specify the number of assume-guarantee rules
        int numberOfConstraints = 1;
        for (String line : spec) {
            if (line.trim().startsWith("CONJUNCTION")) {
                numberOfConstraints = Integer.parseInt(line.trim().split("\\s+")[1]);
                break;
            }
        }



        // The solver checks whether each assume-guarantee rule holds.
        for (int constraintIndex = 0; constraintIndex < numberOfConstraints; constraintIndex++) {
            // Derive how many assumptions are listed in this constraint (assume-guarantee rule)
            int numberOfAssumptions = 0;
            for (String line : spec) {
                if (line.trim().startsWith("ASSUMP")) {
                    if (Integer.parseInt(line.trim().split("\\s+")[1]) == constraintIndex) {
                        numberOfAssumptions = Integer.parseInt(line.trim().split("\\s+")[2]);
                        break;
                    }
                }
            }
            int numberOfDisjunctiveConstraints = numberOfAssumptions + 1;

            boolean[] isPreciseCoefficientRepresentation = new boolean[numberOfDisjunctiveConstraints];
            for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                isPreciseCoefficientRepresentation[i] = true;
            }
            int[] numberOfNonzeroCoef = new int[numberOfDisjunctiveConstraints];
            double[] largestCoef = new double[numberOfDisjunctiveConstraints];

            int[][] degree = new int[numberOfDisjunctiveConstraints][numOfVar];
            // IMPORTANT:
            // The one indexed "numberOfAssumptions" is for the guarantee, and
            // from 0 to numberOfAssumptions - 1 is for the assumption

            for (String line : spec) {
                if (line.trim().startsWith("COEF")) {

                    if (constraintIndex == Integer.parseInt(line.trim().split("\\s+")[1].substring(1).split("_")[0])) {
                        if (line.trim().split("\\s+")[1].startsWith("A")) {
                            // Assumption
                            int assumptionIndex = Integer.parseInt(line.trim().split("\\s+")[1].substring(1).split("_")[1]);
                            String[] power = line.trim().split("[\\(\\)]");
                            String[] varPower = power[1].split(",");
                            for (int i = 0; i < varPower.length; i++) {
                                int value = Integer.parseInt(varPower[i].trim());
                                if (value > degree[assumptionIndex][i]) {
                                    degree[assumptionIndex][i] = value;
                                }
                            }

                        } else if (line.trim().split("\\s+")[1].startsWith("G")) {
                            // Guarantee
                            // guaranteeIndex =  numberOfAssumptions
                            String[] power = line.trim().split("[\\(\\)]");
                            String[] varPower = power[1].split(",");
                            for (int i = 0; i < varPower.length; i++) {
                                int value = Integer.parseInt(varPower[i].trim());
                                if (value > degree[numberOfAssumptions][i]) {
                                    degree[numberOfAssumptions][i] = value;
                                }
                            }
                        }
                    }
                }
            }

            int highestDegree = 0;
            for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                for (int j = 0; j < degree[i].length; j++) {
                    if (degree[i][j] > highestDegree) {
                        highestDegree = degree[i][j];
                    }
                }
            }
            // Create the polynomial
            ArrayList<Polynomial> polys = new ArrayList<Polynomial>();
            for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                Polynomial poly = new Polynomial(numOfVar, degree[i]);
                poly.id = i;
                polys.add(poly);
            }
            // Specify the upper and lower bound for each variable
            lowerBound = new double[numOfVar];
            upperBound = new double[numOfVar];
            for (String line : spec) {
                if (line.trim().startsWith("BOUND")) {
                    int varIndex = Integer.parseInt(line.trim().split("\\s+")[1].substring(1));
                    String[] value = line.trim().split("[\\[,\\]]");
                    lowerBound[varIndex] = Double.parseDouble(value[1]);
                    upperBound[varIndex] = Double.parseDouble(value[2]);
                    for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                        polys.get(i).lowerBound[varIndex] = lowerBound[varIndex];
                        polys.get(i).upperBound[varIndex] = upperBound[varIndex];
                    }
                }
            }
            for (String line : spec) {
                if (line.trim().startsWith("COEF")) {
                    if (constraintIndex == Integer.parseInt(line.trim().split("\\s+")[1].substring(1).split("_")[0])) {
                        String power = line.trim().split("[\\(\\)]")[1].replaceAll("\\s", "").replaceAll("[,]", "_");
                        double value = Double.parseDouble(line.trim().split("[\\(\\)]")[2]);


                        if (line.trim().split("\\s+")[1].startsWith("A")) {
                            // Assumption
                            int assumptionIndex = Integer.parseInt(line.trim().split("\\s+")[1].substring(1).split("_")[1]);
                            polys.get(assumptionIndex).parameter[polys.get(assumptionIndex).terms.indexOf(power)] = value;

                            if (Math.abs(value) > largestCoef[assumptionIndex]) {
                                largestCoef[assumptionIndex] = Math.abs(value);
                            }
                            // Check if the coefficient is an integer multiple of 16
                            if ((value * Math.pow(MAX_DIVISOR, 4)) % 1 != 0) {
                                isPreciseCoefficientRepresentation[assumptionIndex] = false;
                            }
                            numberOfNonzeroCoef[assumptionIndex]++;


                        } else if (line.trim().split("\\s+")[1].startsWith("G")) {
                            // guaranteeIndex =  numberOfAssumptions
                            polys.get(numberOfAssumptions).parameter[polys.get(numberOfAssumptions).terms.indexOf(power)] = value;

                            if (Math.abs(value) > largestCoef[numberOfAssumptions]) {
                                largestCoef[numberOfAssumptions] = Math.abs(value);
                            }
                            // Check if the coefficient is an integer multiple of 16
                            if ((value * Math.pow(MAX_DIVISOR, 4)) % 1 != 0) {
                                isPreciseCoefficientRepresentation[numberOfAssumptions] = false;
                            }
                            numberOfNonzeroCoef[numberOfAssumptions]++;

                        }
                    }
                }
            }
            // Compute the Binomial coefficient (this shall be computed only once when being repeatedly called in SMT procedure)
            binomialCoefficient = new int[highestDegree + 1][highestDegree + 1];
            binom(highestDegree, highestDegree);

            shiftIndexPolys = new int[numberOfDisjunctiveConstraints][numOfVar];
            isNotEndIndexPolys = new boolean[numberOfDisjunctiveConstraints][];
            int[] propertyOperators = new int[numberOfDisjunctiveConstraints];
            double[] propertyValues = new double[numberOfDisjunctiveConstraints];

            for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                shiftIndexPolys[i][numOfVar - 1] = 1;
                for (int j = numOfVar - 2; j >= 0; j--) {
                    shiftIndexPolys[i][j] = shiftIndexPolys[i][j + 1] * (degree[i][j + 1] + 1);
                }
            }

            for (String line : spec) {
                if (line.trim().startsWith("SIGN")) {
                    if (constraintIndex == Integer.parseInt(line.trim().split("\\s+")[1].substring(1).split("_")[0])) {
                        if (line.trim().split("\\s+")[1].startsWith("A")) {
                            int assumptionIndex = Integer.parseInt(line.trim().split("\\s+")[1].substring(1).split("_")[1]);
                            if (line.trim().split("\\s+")[2].equals("GT")) {
                                propertyOperators[assumptionIndex] = LE;
                            } else if (line.trim().split("\\s+")[2].equals("GE")) {
                                propertyOperators[assumptionIndex] = LT;
                            } else if (line.trim().split("\\s+")[2].equals("LT")) {
                                propertyOperators[assumptionIndex] = GE;
                            } else if (line.trim().split("\\s+")[2].equals("LE")) {
                                propertyOperators[assumptionIndex] = GT;
                            }
                        } else if (line.trim().split("\\s+")[1].startsWith("G")) {
                            if (line.trim().split("\\s+")[2].equals("GT")) {
                                propertyOperators[numberOfAssumptions] = GT;
                            } else if (line.trim().split("\\s+")[2].equals("GE")) {
                                propertyOperators[numberOfAssumptions] = GE;
                            } else if (line.trim().split("\\s+")[2].equals("LT")) {
                                propertyOperators[numberOfAssumptions] = LT;
                            } else if (line.trim().split("\\s+")[2].equals("LE")) {
                                propertyOperators[numberOfAssumptions] = LE;
                            }
                        } else {
                            System.out.println("Error parsing the coefficient: line " + line);
                        }
                    }
                } else if (line.trim().startsWith("VALUE")) {
                    if (constraintIndex == Integer.parseInt(line.trim().split("\\s+")[1].substring(1).split("_")[0])) {
                        if (line.trim().split("\\s+")[1].startsWith("A")) {
                            int assumptionIndex = Integer.parseInt(line.trim().split("\\s+")[1].substring(1).split("_")[1]);
                            propertyValues[assumptionIndex] = Double.parseDouble(line.trim().split("\\s+")[2]);
                        } else if (line.trim().split("\\s+")[1].startsWith("G")) {
                            propertyValues[numberOfAssumptions] = Double.parseDouble(line.trim().split("\\s+")[2]);
                        }
                    }
                }
            }

            for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                isNotEndIndexPolys[i] = new boolean[polys.get(i).allTerms.length];
                for (int j = 0; j < polys.get(i).allTerms.length; j++) {
                    for (int k = 0; k < polys.get(i).allTerms[j].length; k++) {
                        if (polys.get(i).allTerms[j][k] != 0 && polys.get(i).allTerms[j][k] != degree[i][k]) {
                            isNotEndIndexPolys[i][j] = true;
                            break;
                        }
                    }
                }
            }



            // Translate into a polynomial with each variable having only [0,1] as its domain
            ArrayList<Polynomial> unitPolys = new ArrayList<Polynomial>();
            for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                unitPolys.add(generateRangePreservingDomainUnitPolynomial(polys.get(i)));
            }

            // After knowing which index is not an endpoint, perform rewriting on all endpoints in poly.term to endpoint form:
            // E.g., from [0,2,1,3] to [0,1,1,1] 
            // By doing so, as a counter-example needs to be a common endpoint, and polynomials may have different degrees
            // If [0,2,1,3] and [0,2,1,1] are vectors (endpoint) for polynomial 1 and 2, they shall be treated as the same.
            // This preprocessing will speed up the generating the endpoint by pure array fetching.

            for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                for (int j = 0; j < polys.get(i).allTerms.length; j++) {
                    if (isNotEndIndexPolys[i][j] == false) {
                        polys.get(i).terms.set(j, combineTerm(polys.get(i).allTerms[j]));
                    }
                }
            }

            // Translate unitPoly into the Bernstein polynomial form
            ArrayList<Polynomial> unitBernsteinPolys = new ArrayList<Polynomial>();
            for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                unitBernsteinPolys.add(generateBernsteinPolynomial(unitPolys.get(i), propertyOperators[i], propertyValues[i], propertyValues[i], false));
            }

            // FIXME: Here we conservatively set all polynomials to have the 
            // same error bound, which is the worst case of all polynomials.

            int errorExponent = MAX_PRECISION;
            if (automaticErrorEstimation) {
                for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {

                    int imprecisionFromPolyToBernstein =
                            (int) Math.ceil(Math.log(shiftIndexPolys[i][0]
                            * (polys.get(i).highestOrder[polys.get(i).variableSize - 1] + 1))
                            / Math.log(2));

                    int errorExponentPoly = getErrorEstimate(polys.get(i),
                            unitBernsteinPolys.get(i),
                            isPreciseCoefficientRepresentation[i],
                            numberOfNonzeroCoef[i],
                            largestCoef[i], recursionDepth,
                            imprecisionFromPolyToBernstein);

                    if (errorExponentPoly > errorExponent) {
                        errorExponent = errorExponentPoly;
                    }
                }
            }


            double[] propertyValuesUpper = new double[numberOfDisjunctiveConstraints];
            double[] propertyValuesLower = new double[numberOfDisjunctiveConstraints];
            if (errorExponent > 0) {
                return "UNKNOWN (the property is not guaranteed due to "
                        + "the use of double, error too large)\n";
            } else if (errorExponent == MAX_PRECISION) {
                //System.out.println("Overall computation using double is precise");
                for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                    propertyValuesUpper[i] = propertyValues[i];
                    propertyValuesLower[i] = propertyValues[i];
                }
            } else {
                System.out.println("Overall error-estimate: 2^{" + errorExponent + "}");
                for (int i = 0; i < numberOfDisjunctiveConstraints; i++) {
                    propertyValuesUpper[i] = propertyValues[i] + Math.pow(2, errorExponent);
                    propertyValuesLower[i] = propertyValues[i] - Math.pow(2, errorExponent);
                }
            }

            int result = isPropertyHoldBFSDisjunction(unitBernsteinPolys,
                    propertyOperators, propertyValuesUpper, propertyValuesLower,
                    0, recursionDepth, 0);

            if (result == TRUE) {
                //System.out.println("TRUE for constraint " + constraintIndex);
                continue;
            } else if (result == FALSE) {
                return "FALSE for constraint " + constraintIndex + "\n" + printCounterWitness();
            } else {
                return "UNKNOWN (please increase the recursion depth)\n";
            }
        }
        // If all constraints are satisfied, return TRUE.
        return "TRUE for the constraint system \n";
    }

    /**
     * Generate the description for the counter witness.
     *
     * @return description
     */
    final String printCounterWitness() {
        StringBuilder s = new StringBuilder("Counter-witness for the property: ");
        if (printPreciseCounterExample) {
            s.append("\n");
            // s.append("[Precise counter-example]\n");
            for (int i = 0; i < counterWitnessString.length; i++) {
                s.append(counterWitnessString[i]).append(", ");
            }
            s.append("\n");
        } else {
            for (int i = 0; i < counterWitness.length; i++) {
                s.append(counterWitness[i]).append(" ");
            }
            s.append("\n");
        }
        return s.toString();
    }

    /**
     * Generate the actual counter-example that is in the original domain.
     *
     * @param bernsteinPoly Bernstein polynomial under analysis
     * @param vec the endpoint that leads to false
     */
    final void generateCounterExample(final Polynomial bernsteinPoly,
            final int[] vec) {

        if (printPreciseCounterExample) {
            counterWitnessString = new String[vec.length];
            for (int i = 0; i < vec.length; i++) {
                if (vec[i] == 0) {
                    counterWitnessString[i] = String.valueOf(lowerBound[i]) + "+"
                            + String.valueOf(bernsteinPoly.lowerBound[i])
                            + "*(" + String.valueOf(upperBound[i]) + "-(" + String.valueOf(lowerBound[i]) + "))";
                } else {
                    counterWitnessString[i] = String.valueOf(lowerBound[i]) + "+"
                            + String.valueOf(bernsteinPoly.upperBound[i])
                            + "*(" + String.valueOf(upperBound[i]) + "-(" + String.valueOf(lowerBound[i]) + "))";
                }
            }
            printCounterWitness();
        } else {
            counterWitness = new double[vec.length];
            for (int i = 0; i < vec.length; i++) {
                if (vec[i] == 0) {
                    counterWitness[i] = lowerBound[i] + bernsteinPoly.lowerBound[i] * (upperBound[i] - lowerBound[i]);
                } else {
                    counterWitness[i] = lowerBound[i] + bernsteinPoly.upperBound[i] * (upperBound[i] - lowerBound[i]);
                }
            }
            printCounterWitness();
        }
    }

    /**
     * Check if the given property holds for simple properties.
     *
     * The implementation is BFS style; the DFS implementation
     * is no longer in the code tree.
     *
     * @param bernsteinPoly polynomial under analysis
     * @param operator operator sign
     * @param propertyValueUpper upperbound of the value with imprecision
     * @param propertyValueLower lowerbound of the value with imprecision
     * @param currentDepth current depth of recursion
     * @param maxDepth maximum allowed recursive depth
     * @param currentVariableIndex the variable that shall be processed
     *                              with domain refinement
     * @return true (1), false (0), or unknown (-1)
     */
    final int isPropertyHoldBFS(final Polynomial bernsteinPoly,
            final int operator,
            final double propertyValueUpper,
            final double propertyValueLower,
            final int currentDepth,
            final int maxDepth,
            final int currentVariableIndex) {

        if (bernsteinPoly.isEarlyErrorDetected) {
            return FALSE;
        }

        if (currentDepth > maxDepth) {
            return UNKNOWN;
        }

        if (!bernsteinPoly.isEarlyUnknownDetected) {
            boolean isUncertain = false;
            if (operator == GT) {
                for (double s : bernsteinPoly.parameter) {
                    if (s <= propertyValueUpper) {
                        isUncertain = true;
                        break;
                    }
                }
            } else if (operator == GE) {
                for (double s : bernsteinPoly.parameter) {
                    if (s < propertyValueUpper) {
                        isUncertain = true;
                        break;
                    }
                }
            } else if (operator == LT) {
                for (double s : bernsteinPoly.parameter) {
                    if (s >= propertyValueLower) {
                        isUncertain = true;
                        break;
                    }
                }
            } else if (operator == LE) {
                for (double s : bernsteinPoly.parameter) {
                    if (s > propertyValueLower) {
                        isUncertain = true;
                        break;
                    }
                }
            }

            if (!isUncertain) {
                return TRUE;
            }
        }


        // Select one variable to branch (here we perform a BFS, i.e., to branch first on every variable)   
        if (currentVariableIndex >= bernsteinPoly.variableSize) {
            // Perform BFS on the second level
            // Expand on the current level by choosing the next variable in the index
            int i = 0;

            Polynomial lBernstein = expandLeftSimple(bernsteinPoly, i, operator, propertyValueUpper, propertyValueLower);
            int lResult = isPropertyHoldBFS(lBernstein, operator, propertyValueUpper, propertyValueLower, currentDepth + 1, maxDepth, i);

            if (lResult == FALSE) {
                return FALSE;
            } else {
                Polynomial rBernstein = expandRightSimple(bernsteinPoly, i, operator, propertyValueUpper, propertyValueLower);
                int rResult = isPropertyHoldBFS(rBernstein, operator, propertyValueUpper, propertyValueLower, currentDepth + 1, maxDepth, i);

                if (rResult == FALSE) {
                    return FALSE;
                } else if (lResult == TRUE && rResult == TRUE) {
                    return TRUE;
                }
            }

        } else {
            // Expand on the current level by choosing the next variable in the index
            Polynomial lBernstein = expandLeftSimple(bernsteinPoly, currentVariableIndex, operator, propertyValueUpper, propertyValueLower);
            int lResult = isPropertyHoldBFS(lBernstein, operator, propertyValueUpper, propertyValueLower, currentDepth, maxDepth, currentVariableIndex + 1);

            if (lResult == FALSE) {
                return FALSE;
            } else {
                Polynomial rBernstein = expandRightSimple(bernsteinPoly, currentVariableIndex, operator, propertyValueUpper, propertyValueLower);
                int rResult = isPropertyHoldBFS(rBernstein, operator, propertyValueUpper, propertyValueLower, currentDepth, maxDepth, currentVariableIndex + 1);
                if (rResult == FALSE) {
                    return FALSE;
                } else if (lResult == TRUE && rResult == TRUE) {
                    return TRUE;
                }
            }
        }
        return UNKNOWN;

    }

    /**
     * Check if the given property holds for assume-guarantee style properties.
     *
     * The implementation is BFS style; the DFS implementation is no
     * longer in the code tree.
     *
     * @param bernsteinPolys polynomials under analysis
     * @param operator operator sign
     * @param value compared value without considering imprecision
     * @param currentDepth current depth of recursion
     * @param maxDepth maximum allowed recursive depth
     * @param currentVariableIndex the variable that shall be processed
     *                              with domain refinement
     * @return true (1), false (0), or unknown (-1)
     */
    final int isPropertyHoldBFSDisjunction(
            final ArrayList<Polynomial> bernsteinPolys,
            final int[] operator,
            // final double[] value,
            final double[] propertyValuesUpper,
            final double[] propertyValuesLower,
            final int currentDepth,
            final int maxDepth,
            final int currentVariableIndex) {

        // Check if there exists a common element that violates the property.
        for (int constraintIndex = 1; constraintIndex < bernsteinPolys.size(); constraintIndex++) {
            bernsteinPolys.get(0).violationSet.retainAll(bernsteinPolys.get(constraintIndex).violationSet);
        }

        if (!bernsteinPolys.get(0).violationSet.isEmpty()) {
            String[] vector = bernsteinPolys.get(0).violationSet.iterator().next().split("_");
            int[] counterExampleVector = new int[bernsteinPolys.get(0).variableSize];
            for (int i = 0; i < bernsteinPolys.get(0).variableSize; i++) {
                counterExampleVector[i] = Integer.parseInt(vector[i]);
            }
            generateCounterExample(bernsteinPolys.get(0), counterExampleVector);
            return FALSE;
        }

        if (currentDepth > maxDepth) {
            return UNKNOWN;
        }

        boolean[] isUncertain = new boolean[bernsteinPolys.size()];

        for (int constraintIndex = 0; constraintIndex < bernsteinPolys.size(); constraintIndex++) {
            if (bernsteinPolys.get(constraintIndex).isEarlyUnknownDetected) {
                isUncertain[constraintIndex] = true;
            } else {
                if (operator[constraintIndex] == GT) {
                    for (double s : bernsteinPolys.get(constraintIndex).parameter) {
                        if (s <= propertyValuesUpper[constraintIndex]) {
                            isUncertain[constraintIndex] = true;
                            break;
                        }
                    }
                } else if (operator[constraintIndex] == GE) {
                    for (double s : bernsteinPolys.get(constraintIndex).parameter) {
                        if (s < propertyValuesUpper[constraintIndex]) {
                            isUncertain[constraintIndex] = true;
                            break;
                        }
                    }
                } else if (operator[constraintIndex] == LT) {
                    for (double s : bernsteinPolys.get(constraintIndex).parameter) {
                        if (s >= propertyValuesLower[constraintIndex]) {
                            isUncertain[constraintIndex] = true;
                            break;
                        }
                    }
                } else if (operator[constraintIndex] == LE) {
                    for (double s : bernsteinPolys.get(constraintIndex).parameter) {
                        if (s > propertyValuesLower[constraintIndex]) {
                            isUncertain[constraintIndex] = true;
                            break;
                        }
                    }
                }

            }
            if (!isUncertain[constraintIndex]) {
                return TRUE;
            }
        }

        // Select one variable to branch (here we perform a BFS, i.e., to branch first on every variable)

        if (currentVariableIndex >= bernsteinPolys.get(0).variableSize) {

            // Perform BFS on the next level
            int i = 0;
            ArrayList<Polynomial> leftPolys = new ArrayList<Polynomial>();
            for (int constraintIndex = 0; constraintIndex < bernsteinPolys.size(); constraintIndex++) {
                leftPolys.add(expandLeftAssumeGuarantee(bernsteinPolys.get(constraintIndex),
                        i, operator[constraintIndex],
                        propertyValuesUpper[constraintIndex],
                        propertyValuesLower[constraintIndex]));
            }
            int lResult = isPropertyHoldBFSDisjunction(leftPolys, operator, propertyValuesUpper, propertyValuesLower, currentDepth + 1, maxDepth, i);


            if (lResult == FALSE) {
                return FALSE;
            } else {

                ArrayList<Polynomial> rightPolys = new ArrayList<Polynomial>();
                for (int constraintIndex = 0; constraintIndex < bernsteinPolys.size(); constraintIndex++) {
                    rightPolys.add(expandRightAssumeGuarantee(bernsteinPolys.get(constraintIndex),
                            i, operator[constraintIndex],
                            propertyValuesUpper[constraintIndex],
                            propertyValuesLower[constraintIndex]));
                }
                int rResult = isPropertyHoldBFSDisjunction(rightPolys, operator, propertyValuesUpper, propertyValuesLower, currentDepth + 1, maxDepth, i);

                if (rResult == FALSE) {
                    return FALSE;
                } else if (lResult == TRUE && rResult == TRUE) {
                    return TRUE;
                }
            }

        } else {
            // Expand on the current level by choosing the next variable in the index

            ArrayList<Polynomial> leftPolys = new ArrayList<Polynomial>();
            for (int constraintIndex = 0; constraintIndex < bernsteinPolys.size(); constraintIndex++) {
                leftPolys.add(expandLeftAssumeGuarantee(bernsteinPolys.get(constraintIndex),
                        currentVariableIndex, operator[constraintIndex],
                        propertyValuesUpper[constraintIndex],
                        propertyValuesLower[constraintIndex]));
            }
            int lResult = isPropertyHoldBFSDisjunction(leftPolys, operator, propertyValuesUpper, propertyValuesLower, currentDepth, maxDepth, currentVariableIndex + 1);

            if (lResult == FALSE) {
                return FALSE;
            } else {

                ArrayList<Polynomial> rightPolys = new ArrayList<Polynomial>();
                for (int constraintIndex = 0; constraintIndex < bernsteinPolys.size(); constraintIndex++) {
                    rightPolys.add(expandRightAssumeGuarantee(bernsteinPolys.get(constraintIndex),
                            currentVariableIndex, operator[constraintIndex],
                            propertyValuesUpper[constraintIndex],
                            propertyValuesLower[constraintIndex]));
                }
                int rResult = isPropertyHoldBFSDisjunction(rightPolys, operator, propertyValuesUpper, propertyValuesLower, currentDepth, maxDepth, currentVariableIndex + 1);

                if (rResult == FALSE) {
                    return FALSE;
                } else if (lResult == TRUE && rResult == TRUE) {
                    return TRUE;
                }
            }
        }

        return UNKNOWN;

    }

    /**
     * For simple constraints, generate the left polynomial with interval
     * [0, 0.5] for the j-th variable and perform property checking.
     *
     * @param poly original Bernstein polynomial
     * @param j index of the variable that is being split
     * @param operator comparator sign
     * @param propertyValueUpper upperbound of the value due to imprecision
     * @param propertyValueLower lowerbound of the value due to imprecision
     * @return new polynomial that is the left expansion
     */
    final Polynomial expandLeftSimple(final Polynomial poly, final int j,
            final int operator, final double propertyValueUpper,
            final double propertyValueLower) {

        boolean isUnknown = false;
        Polynomial leftHalfBernsteinPoly = new Polynomial(poly);

        for (int i = 0; i < leftHalfBernsteinPoly.variableSize; i++) {
            if (i == j) {
                leftHalfBernsteinPoly.lowerBound[i] = poly.lowerBound[i];
                leftHalfBernsteinPoly.upperBound[i] = (poly.lowerBound[i] + poly.upperBound[i]) / 2;
            } else {
                leftHalfBernsteinPoly.lowerBound[i] = poly.lowerBound[i];
                leftHalfBernsteinPoly.upperBound[i] = poly.upperBound[i];
            }
        }


        for (int k = 0; k < leftHalfBernsteinPoly.allTerms.length; k++) {
            // Generate b_k_left
            double b_k_left = 0;
            for (int r = 0; r <= poly.allTerms[k][j]; r++) {
                b_k_left += poly.parameter[ k + (r - poly.allTerms[k][j]) * shiftIndex[j]] * binomialCoefficient[poly.allTerms[k][j]][r];
            }
            b_k_left = b_k_left / ((double) (1 << poly.allTerms[k][j]));

            if (isNotEndIndex[k] == false
                    && ((operator == GT && b_k_left <= propertyValueLower)
                    || (operator == GE && b_k_left < propertyValueLower)
                    || (operator == LT && b_k_left >= propertyValueUpper)
                    || (operator == LE && b_k_left > propertyValueUpper))) {
                // Check if immediate violation exists
                generateCounterExample(leftHalfBernsteinPoly, leftHalfBernsteinPoly.allTerms[k]);
                leftHalfBernsteinPoly.isEarlyErrorDetected = true;
                return leftHalfBernsteinPoly;

            } else if (isUnknown == false
                    && ((operator == GT && b_k_left <= propertyValueUpper)
                    || (operator == GE && b_k_left < propertyValueUpper)
                    || (operator == LT && b_k_left >= propertyValueLower)
                    || (operator == LE && b_k_left > propertyValueLower))) {
                // The generated Bernstein ploynomial has value unknown.
                leftHalfBernsteinPoly.isEarlyUnknownDetected = true;
                isUnknown = true;
            }
            leftHalfBernsteinPoly.parameter[k] = b_k_left;
        }
        return leftHalfBernsteinPoly;
    }

    /**
     * For assume-guarantee style constraints, generate the left polynomial
     * with interval [0, 0.5] for the j-th variable and perform property
     * checking.
     *
     * @param poly original Bernstein polynomial
     * @param j index of the variable that is being split
     * @param operator comparator sign
     * @param value the specified value under comparison
     *        (imprecision from double is not included).
     * @return left Bernstein polynomial
     */
    final Polynomial expandLeftAssumeGuarantee(final Polynomial poly,
            final int j, final int operator, final double propertyValueUpper,
            final double propertyValueLower) {

        boolean isUnknown = false;
        Polynomial leftHalfBernsteinPoly = new Polynomial(poly);
        leftHalfBernsteinPoly.violationSet = new HashSet<String>();

        for (int i = 0; i < leftHalfBernsteinPoly.variableSize; i++) {
            if (i == j) {
                leftHalfBernsteinPoly.lowerBound[i] = poly.lowerBound[i];
                leftHalfBernsteinPoly.upperBound[i] = (poly.lowerBound[i] + poly.upperBound[i]) / 2;
            } else {
                leftHalfBernsteinPoly.lowerBound[i] = poly.lowerBound[i];
                leftHalfBernsteinPoly.upperBound[i] = poly.upperBound[i];
            }
        }


        for (int k = 0; k < leftHalfBernsteinPoly.allTerms.length; k++) {
            // Generate b_k_left
            double b_k_left = 0;
            for (int r = 0; r <= poly.allTerms[k][j]; r++) {
                b_k_left += poly.parameter[ k + (r - poly.allTerms[k][j]) * shiftIndexPolys[poly.id][j]] * binomialCoefficient[poly.allTerms[k][j]][r];
            }
            b_k_left = b_k_left / ((double) (1 << poly.allTerms[k][j]));

            if (!isNotEndIndexPolys[poly.id][k]
                    && ((operator == GT && b_k_left <= propertyValueLower)
                    || (operator == GE && b_k_left < propertyValueLower)
                    || (operator == LT && b_k_left >= propertyValueUpper)
                    || (operator == LE && b_k_left > propertyValueUpper))) {
                // leftHalfBernsteinPoly.violationSet.add(combineTerm(leftHalfBernsteinPoly.allTerms[k]));
                leftHalfBernsteinPoly.violationSet.add(leftHalfBernsteinPoly.terms.get(k));
                isUnknown = true;
            } else if (!isUnknown
                    && ((operator == GT && b_k_left <= propertyValueUpper)
                    || (operator == GE && b_k_left < propertyValueUpper)
                    || (operator == LT && b_k_left >= propertyValueLower)
                    || (operator == LE && b_k_left > propertyValueLower))) {
                // The generated Bernstein ploynomial has value unknown.
                leftHalfBernsteinPoly.isEarlyUnknownDetected = true;
                isUnknown = true;
            }
            leftHalfBernsteinPoly.parameter[k] = b_k_left;
        }
        return leftHalfBernsteinPoly;
    }

    /**
     * For simple constraints, generate the right polynomial with interval
     * [0.5, 1] for the j-th variable and perform property checking.
     * 
     * @param poly original Bernstein polynomial
     * @param j index of the variable that is being split
     * @param operator comparator sign
     * @param propertyValueUpper upperbound of the value due to imprecision
     * @param propertyValueLower lowerbound of the value due to imprecision
     * @return right polynomial
     */
    final Polynomial expandRightSimple(final Polynomial poly, final int j,
            final int operator, final double propertyValueUpper,
            final double propertyValueLower) {

        boolean isUnknown = false;
        Polynomial rightHalfBernsteinPoly = new Polynomial(poly);

        for (int i = 0; i < rightHalfBernsteinPoly.variableSize; i++) {
            if (i == j) {
                rightHalfBernsteinPoly.lowerBound[i] = (poly.lowerBound[i] + poly.upperBound[i]) / 2;
                rightHalfBernsteinPoly.upperBound[i] = poly.upperBound[i];
            } else {
                rightHalfBernsteinPoly.lowerBound[i] = poly.lowerBound[i];
                rightHalfBernsteinPoly.upperBound[i] = poly.upperBound[i];
            }
        }

        for (int k = 0; k < rightHalfBernsteinPoly.allTerms.length; k++) {
            // Generate b_k_right 
            double b_k_right = 0;
            for (int r = 0; r <= poly.highestOrder[j] - poly.allTerms[k][j]; r++) {
                b_k_right += poly.parameter[ k + (poly.highestOrder[j] - r - poly.allTerms[k][j]) * shiftIndex[j]] * binomialCoefficient[poly.highestOrder[j] - poly.allTerms[k][j]][r];
            }
            b_k_right = b_k_right / ((double) (1 << poly.highestOrder[j] - poly.allTerms[k][j]));

            if (!isNotEndIndex[k]
                    && ((operator == GT && b_k_right <= propertyValueLower)
                    || (operator == GE && b_k_right < propertyValueLower)
                    || (operator == LT && b_k_right >= propertyValueUpper)
                    || (operator == LE && b_k_right > propertyValueUpper))) {

                // Check if immediate violation exists
                generateCounterExample(rightHalfBernsteinPoly, rightHalfBernsteinPoly.allTerms[k]);
                rightHalfBernsteinPoly.isEarlyErrorDetected = true;
                return rightHalfBernsteinPoly;

            } else if (!isUnknown
                    && ((operator == GT && b_k_right <= propertyValueUpper)
                    || (operator == GE && b_k_right < propertyValueUpper)
                    || (operator == LT && b_k_right >= propertyValueLower)
                    || (operator == LE && b_k_right > propertyValueLower))) {

                // The generated Bernstein ploynomial has value unknown.
                rightHalfBernsteinPoly.isEarlyUnknownDetected = true;
                isUnknown = true;

            }

            rightHalfBernsteinPoly.parameter[k] = b_k_right;
        }
        return rightHalfBernsteinPoly;
    }

    /**
     * For assume-guarantee style constraints, generate the left polynomial with
     * interval [0.5, 1] for the j-th variable and perform property checking.
     *
     * @param poly original Bernstein polynomial
     * @param j index of the variable that is being split
     * @param operator comparator sign
     * @param value  the specified value under comparison
     *              (imprecision from double is not used)
     * @return right polynomial
     */
    final Polynomial expandRightAssumeGuarantee(final Polynomial poly,
            final int j, final int operator, final double propertyValueUpper,
            final double propertyValueLower) {

        boolean isUnknown = false;
        Polynomial rightHalfBernsteinPoly = new Polynomial(poly);
        rightHalfBernsteinPoly.violationSet = new HashSet<String>();

        for (int i = 0; i < rightHalfBernsteinPoly.variableSize; i++) {
            if (i == j) {
                rightHalfBernsteinPoly.lowerBound[i] = (poly.lowerBound[i] + poly.upperBound[i]) / 2;
                rightHalfBernsteinPoly.upperBound[i] = poly.upperBound[i];
            } else {
                rightHalfBernsteinPoly.lowerBound[i] = poly.lowerBound[i];
                rightHalfBernsteinPoly.upperBound[i] = poly.upperBound[i];
            }
        }

        for (int k = 0; k < rightHalfBernsteinPoly.allTerms.length; k++) {
            // Generate b_k_right 
            double b_k_right = 0;
            for (int r = 0; r <= poly.highestOrder[j] - poly.allTerms[k][j]; r++) {
                b_k_right += poly.parameter[ k + (poly.highestOrder[j] - r - poly.allTerms[k][j]) * shiftIndexPolys[poly.id][j]] * binomialCoefficient[poly.highestOrder[j] - poly.allTerms[k][j]][r];
            }
            b_k_right = b_k_right / ((double) (1 << poly.highestOrder[j] - poly.allTerms[k][j]));

            if (!isNotEndIndexPolys[poly.id][k]
                    && ((operator == GT && b_k_right <= propertyValueLower)
                    || (operator == GE && b_k_right < propertyValueLower)
                    || (operator == LT && b_k_right >= propertyValueUpper)
                    || (operator == LE && b_k_right > propertyValueUpper))) {
                // rightHalfBernsteinPoly.violationSet.add(combineTerm(rightHalfBernsteinPoly.allTerms[k]));
                rightHalfBernsteinPoly.violationSet.add(rightHalfBernsteinPoly.terms.get(k));
                isUnknown = true;
            } else if (!isUnknown
                    && ((operator == GT && b_k_right <= propertyValueUpper)
                    || (operator == GE && b_k_right < propertyValueUpper)
                    || (operator == LT && b_k_right >= propertyValueLower)
                    || (operator == LE && b_k_right > propertyValueLower))) {
                // The generated Bernstein ploynomial has value unknown.
                rightHalfBernsteinPoly.isEarlyUnknownDetected = true;
                isUnknown = true;
            }

            rightHalfBernsteinPoly.parameter[k] = b_k_right;
        }
        return rightHalfBernsteinPoly;
    }

    /**
     * Compute all binomial coefficients (x,y) for all x <= n and y <=m.
     * @param n max value for candidates
     * @param m max value for picks
     */
    final private void binom(final int n, final int m) {
        int i, j;
        if (n >= 0) {
            if (m > n || m < 0) {
                System.err.println("Illegal m!!\n");
            } else {
                for (i = 0; i <= n; i++) {
                    binomialCoefficient[i][0] = 1;
                }
                for (i = 1; i <= m; i++) {
                    binomialCoefficient[0][i] = 0;
                }
                for (j = 1; j <= m; j++) {
                    for (i = 1; i <= n; i++) {
                        binomialCoefficient[i][j] = binomialCoefficient[i - 1][j - 1] + binomialCoefficient[i - 1][j];
                    }
                }
            }
        } else {
            System.err.println("Negative n!!\n");
        }
    }

    /**
     * Compare if the first vector is greater or equal to the second.
     *
     * @param vec1 first vector
     * @param vec2 second vector
     * @return true when first >= second
     */
    final boolean isVectorGEQ(final int[] vec1, final int[] vec2) {
        for (int i = 0; i < vec1.length; i++) {
            if (vec1[i] < vec2[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare if the first vector is less or equal to the second.
     *
     * @param vec1 first vector
     * @param vec2 second vector
     * @return true when first <= second
     */
    final boolean isVectorLEQ(final int[] vec1, final int[] vec2) {
        for (int i = 0; i < vec1.length; i++) {
            if (vec1[i] > vec2[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Combine vectors to a string.
     *
     * It is used in checking an assume-guarantee style property, as the solver
     * needs to find a common vector to view it as a counter-example.
     *
     * @param vec vector to be converted.
     * @return string representation of the vector.
     */
    final String combineTerm(final int[] vec) {
        StringBuilder s = new StringBuilder("");
        s.append(vec[0] > 0 ? "1" : "0");
        for (int i = 1; i < vec.length; i++) {
            s.append("_").append(vec[i] > 0 ? "1" : "0");
        }
        return s.toString();
    }

    /**
     * Generate a safe estimate on the error that can appear due to double
     * for range-preserving and basis transformation.
     *
     * Notice: This function is experimental.
     *
     * For assumptions how the result is sound, please see the file 
     * ErrorEstimation.txt listed also in the bernstein package.
     *
     * @param poly polynomial under analysis.
     * @param unitPoly unit polynomial after range transformation
     * @param isPreciseCoefficient whether all coefficients in the original
     *        is precisely stored
     * @param numberOfNonzeroCoef Number of non-zero coefficients in the poly
     * @param largestOriginalCoef coefficient with largest quantity
     * @param recursionDepth the max allowed refinement for each variable
     * @return value: allowed error due to imprecision of double 2^{value}
     */
    final int getErrorEstimate(
            final Polynomial poly, final Polynomial unitPoly,
            final boolean isPreciseCoefficient,
            final int numberOfNonzeroCoef,
            final double largestOriginalCoef, final int recursionDepth,
            final int imprecisionFromPolyToBernstein) {

        // The actual largest quantity of any coefficient in a unit polynomial
        double largestQuantityCoefficientUnitPoly = 0;
        for (int i = 0; i < unitPoly.parameter.length; i++) {
            if (Math.abs(unitPoly.parameter[i])
                    > largestQuantityCoefficientUnitPoly) {
                largestQuantityCoefficientUnitPoly = Math.abs(unitPoly.parameter[i]);
            }
        }

        // The estimated largest quantity of any coefficient in a unit
        // polynomial. It is equal to the actual one when the computation is
        // precise.
        double coefficientMaxEstimateUnitPoly = 0;

        // Return true if all lowerbounds, upperbounds, and the largest
        // coefficient are represented with precision
        boolean isPreciseRepresentable = true;
        for (int i = 0; i < poly.variableSize; i++) {
            if (((lowerBound[i] * MAX_DIVISOR) % 1) != 0
                    || ((upperBound[i] * MAX_DIVISOR) % 1) != 0) {
                isPreciseRepresentable = false;
                break;
            }
        }

        if (isPreciseCoefficient && isPreciseRepresentable) {
            // Check if all highest degree is below 2.

            boolean isPolynomialDegreeLessEqualTwo = true;
            int maxTerms = 1;
            int numberOfHalfLowerbound = 0;
            int numberOfHalfInterval = 0;
            for (int i = 0; i < poly.variableSize; i++) {
                if (poly.highestOrder[i] > 2) {
                    isPolynomialDegreeLessEqualTwo = false;
                    break;
                } else {
                    maxTerms = maxTerms * (poly.highestOrder[i] + 1);
                }
                if ((upperBound[i] - lowerBound[i]) % 1 != 0) {
                    numberOfHalfInterval++;
                }
                if (lowerBound[i] % 1 != 0) {
                    numberOfHalfLowerbound++;
                }
            }


            if (isPolynomialDegreeLessEqualTwo) {
                if ((int) Math.ceil(Math.log(largestQuantityCoefficientUnitPoly) / Math.log(2))
                        + (int) Math.ceil(Math.log(maxTerms) / Math.log(2))
                        + numberOfHalfInterval * 2
                        + numberOfHalfInterval * 2
                        + (recursionDepth * poly.variableSize) * 2 < FRACTION_IN_DOUBLE) {
                    // Impossible to have error (here the coefficient to be compared needs to move to the left)
                    System.out.println("Computation of coefficients using double is precise");
                    return MAX_PRECISION;
                } else if ((int) Math.ceil(Math.log(largestQuantityCoefficientUnitPoly) / Math.log(2))
                        + (int) Math.ceil(Math.log(maxTerms) / Math.log(2))
                        < FRACTION_IN_DOUBLE) {
                    // Error only within subspace refinement
                    System.out.println("Error-estimate: 2^{" + DEFAULT_PRECISION + "}");
                    return DEFAULT_PRECISION;
                }
            }
            coefficientMaxEstimateUnitPoly = largestQuantityCoefficientUnitPoly;

        } else {
            // Start constructing the largest possible value (over-approximation) for a generated coefficient
            coefficientMaxEstimateUnitPoly = largestOriginalCoef;
            for (int i = 0; i < poly.variableSize; i++) {
                if (upperBound[i] - lowerBound[i] <= 1) {
                    // FIXME: Here the precision can be much improved when we also partition "upperBound[i] - lowerBound[i]" 
                    // into intervals 0, 0.25, 0.5, 1
                    if (Math.abs(lowerBound[i]) < 1) {
                        if (Math.abs(lowerBound[i]) < 0.25) {
                            coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                    * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2]
                                    * Math.pow(0.25, poly.highestOrder[i]);
                        } else if (Math.abs(lowerBound[i]) < 0.5) {
                            coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                    * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2]
                                    * Math.pow(0.5, poly.highestOrder[i]);
                        } else if (Math.abs(lowerBound[i]) < 0.75) {
                            coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                    * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2]
                                    * Math.pow(0.75, poly.highestOrder[i]);
                        } else {
                            // Both exponentiation equal to 1
                            coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                    * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2];
                        }
                    } else {
                        coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2]
                                * Math.pow(Math.abs(lowerBound[i]), poly.highestOrder[i]);
                    }
                } else {
                    if (Math.abs(lowerBound[i]) < 1) {
                        if (Math.abs(lowerBound[i]) < 0.25) {
                            coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                    * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2]
                                    * Math.pow(upperBound[i] - lowerBound[i], poly.highestOrder[i])
                                    * Math.pow(0.25, poly.highestOrder[i]);
                        } else if (Math.abs(lowerBound[i]) < 0.5) {
                            coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                    * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2]
                                    * Math.pow(upperBound[i] - lowerBound[i], poly.highestOrder[i])
                                    * Math.pow(0.5, poly.highestOrder[i]);
                        } else if (Math.abs(lowerBound[i]) < 0.75) {
                            coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                    * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2]
                                    * Math.pow(upperBound[i] - lowerBound[i], poly.highestOrder[i])
                                    * Math.pow(0.75, poly.highestOrder[i]);
                        } else {
                            coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                    * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2]
                                    * Math.pow(upperBound[i] - lowerBound[i], poly.highestOrder[i]);
                        }
                    } else {
                        coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly
                                * binomialCoefficient[poly.highestOrder[i]][poly.highestOrder[i] / 2]
                                * Math.pow(upperBound[i] - lowerBound[i], poly.highestOrder[i])
                                * Math.pow(Math.abs(lowerBound[i]), poly.highestOrder[i]);
                    }
                }
            }

            // Sum with the maximum number of additions (only non-zero terms)
            coefficientMaxEstimateUnitPoly = coefficientMaxEstimateUnitPoly * numberOfNonzeroCoef;
        }

        // Variable for storing the number of multiplications in generating each term
        int numberOfMulInRangeTransformation = 1;
        for (int i = 0; i < poly.variableSize; i++) {
            numberOfMulInRangeTransformation += (poly.highestOrder[i] + 1);
        }

        // Derive error-estimate = 2^{errorDigits} due to range-preserving
        // transformation
        int errorDigits =
                (int) Math.ceil(Math.log(numberOfNonzeroCoef) / Math.log(2))
                + (int) Math.ceil(Math.log(coefficientMaxEstimateUnitPoly) / Math.log(2))
                - FRACTION_IN_DOUBLE
                + (int) Math.ceil(Math.log(2 * (numberOfMulInRangeTransformation + 1)) / Math.log(2));

        // Derive error-estimate due to transformation from polynomial basis
        // into Bernstein basis
        //
        // shiftIndex[0] stores (Deg[0]+1)...(Deg[m-2]+1),
        // so we need to additionally multiply it with Deg[m-1]+1
        // errorDigits = errorDigits + (int) Math.ceil(Math.log(shiftIndex[0]
        //        * (poly.highestOrder[poly.variableSize - 1] + 1))
        //        / Math.log(2));

        errorDigits = errorDigits + imprecisionFromPolyToBernstein;



        // System.out.println("After range-preserving transformation and "
        //        + "basis transformation,\nthe accumulated error is no larger "
        //        + "than 2^{" + errorDigits + "}");

        // Add 1 digit for division error
        if (errorDigits + 1 < DEFAULT_PRECISION) {
            System.out.println("Error-estimate: 2^{" + DEFAULT_PRECISION + "}");
            return DEFAULT_PRECISION;
        } else {
            System.out.println("Error-estimate: 2^{" + (errorDigits + 1) + "}");
            return errorDigits + 1;
        }

    }

    /**
     * Generate a new polynomial that is range-preserving but has
     * variables with domain [0, 1].
     *
     * @param poly original polynomial
     * @return range-preserving polynomial with domain [0,1]
     */
    final Polynomial generateRangePreservingDomainUnitPolynomial(
            final Polynomial poly) {

        boolean isUnitRange = true;

        Polynomial unitDomainPoly = new Polynomial(poly);
        for (int i = 0; i < unitDomainPoly.variableSize; i++) {
            unitDomainPoly.lowerBound[i] = 0;
            unitDomainPoly.upperBound[i] = 1;
            if (poly.lowerBound[i] != 0 || poly.upperBound[i] != 1) {
                isUnitRange = false;
            }
        }

        if (isUnitRange) {
            unitDomainPoly.parameter = poly.parameter;
        } else {
            for (int k = 0; k < unitDomainPoly.allTerms.length; k++) {
                // Generate r_k 
                double r_k = 0;
                for (int i = 0; i < poly.allTerms.length; i++) {
                    if (poly.parameter[i] != 0 && isVectorGEQ(poly.allTerms[i], unitDomainPoly.allTerms[k])) {
                        double c_i = poly.parameter[i];
                        for (int j = 0; j < poly.variableSize; j++) {
                            c_i = c_i * binomialCoefficient[poly.allTerms[i][j]][unitDomainPoly.allTerms[k][j]]
                                    * Math.pow(poly.upperBound[j] - poly.lowerBound[j], unitDomainPoly.allTerms[k][j])
                                    * Math.pow(poly.lowerBound[j], poly.allTerms[i][j] - unitDomainPoly.allTerms[k][j]);
                        }
                        r_k += c_i;
                    }
                }
                unitDomainPoly.parameter[k] = r_k;
            }
        }

        return unitDomainPoly;
    }

    /**
     * Convert a unit polynomial from a polynomial basis to Bernstein basis,
     * while performing early property checking.
     *
     * @param poly original Bernstein polynomial
     * @param operator comparator sign
     * @param propertyValueUpper upperbound of the value due to imprecision
     * @param propertyValueLower lowerbound of the value due to imprecision
     * @param isSimpleConstraint whether the introduced constraint is
     *                           simple or assume-guarantee style.
     * @return Bernstein polynomial
     */
    final Polynomial generateBernsteinPolynomial(final Polynomial poly,
            final int operator,
            final double propertyValueUpper,
            final double propertyValueLower,
            final boolean isSimpleConstraint) {

        Polynomial bernsteinPoly = new Polynomial(poly);
        bernsteinPoly.violationSet = new HashSet<String>();

        bernsteinPoly.lowerBound = poly.lowerBound;
        bernsteinPoly.upperBound = poly.upperBound;

        for (int k = 0; k < bernsteinPoly.allTerms.length; k++) {
            // Generate b_k
            double b_k = 0;
            for (int i = 0; i < poly.allTerms.length; i++) {
                if (isVectorLEQ(poly.allTerms[i], bernsteinPoly.allTerms[k])) {

                    double c_i = poly.parameter[i];
                    for (int j = 0; j < poly.variableSize; j++) {
                        c_i = c_i * binomialCoefficient[bernsteinPoly.allTerms[k][j]][poly.allTerms[i][j]]
                                / binomialCoefficient[poly.highestOrder[j]][poly.allTerms[i][j]];
                    }
                    b_k = b_k + c_i;


                    // Implementation that first accumulates the value in
                    // multiplication (thus error free). Then only one 
                    // division is used. This ensures the precision of the 
                    // resulting computation
                    /*
                    double value = 1;
                    for (int j = 0; j < poly.variableSize; j++) {
                    value = value * (binomialCoefficient[poly.highestOrder[j]][poly.allTerms[i][j]]
                    / binomialCoefficient[bernsteinPoly.allTerms[k][j]][poly.allTerms[i][j]]);
                    }
                    b_k = b_k + (poly.parameter[i] / value);
                     */
                }
            }

            if (isSimpleConstraint) {
                if (!isNotEndIndex[k]) {
                    // Check if immediate violation exists
                    if ((operator == GT && b_k <= propertyValueLower)
                            || (operator == GE && b_k < propertyValueLower)
                            || (operator == LT && b_k >= propertyValueUpper)
                            || (operator == LE && b_k > propertyValueUpper)) {

                        generateCounterExample(bernsteinPoly,
                                bernsteinPoly.allTerms[k]);
                        bernsteinPoly.isEarlyErrorDetected = true;
                        return bernsteinPoly;

                    }
                }
            } else {
                if (!isNotEndIndexPolys[poly.id][k]) {
                    if ((operator == GT && b_k <= propertyValueLower)
                            || (operator == GE && b_k < propertyValueLower)
                            || (operator == LT && b_k >= propertyValueUpper)
                            || (operator == LE && b_k > propertyValueUpper)) {
                        // bernsteinPoly.violationSet.add(combineTerm(bernsteinPoly.allTerms[k]));
                        bernsteinPoly.violationSet.add(bernsteinPoly.terms.get(k));
                    }
                }
            }

            bernsteinPoly.parameter[k] = b_k;

        }
        return bernsteinPoly;


    }

    /**
     * Inner class for representing the data structure of a polynomial.
     */
    class Polynomial {

        /** number of variables in the polynomial. */
        private int variableSize;
        /** highest order for each variable. */
        private int[] highestOrder;
        /** lowerbound for each variable. */
        private double[] lowerBound;
        /** upperbound for each variable. */
        private double[] upperBound;
        /** The list of basis in a polynomial in string form. */
        private ArrayList<String> terms;
        /** The list of basis in a polynomial in vector form. */
        private int[][] allTerms;
        /** parameters for each index following the order in terms. */
        private double[] parameter;
        /** value is true when error is detected during construction. */
        private boolean isEarlyErrorDetected;
        /** value is true when unknown is detected during construction. */
        private boolean isEarlyUnknownDetected;
        /** A set of endpoints that violate the property
         * (used only in assume-guarantee style).
         */
        private HashSet<String> violationSet;
        /**
         * ID that specifies the constraint index with assume-guarantee rule
         * (used only in assume-guarantee style).
         */
        private int id;

        /**
         * Generate a fresh polynomial, together with all possible terms and
         * corresponding indices.
         *
         * @param paraVariableSize number of variables
         * @param paraHighestOrder highest order for each variable
         */
        Polynomial(final int paraVariableSize, final int[] paraHighestOrder) {
            this.variableSize = paraVariableSize;
            this.highestOrder = paraHighestOrder;
            lowerBound = new double[paraVariableSize];
            upperBound = new double[paraVariableSize];
            terms = new ArrayList<String>();
            for (int i = 0; i <= paraHighestOrder[0]; i++) {
                terms.add(String.valueOf(i));
            }
            if (paraVariableSize > 1) {
                for (int i = 1; i < paraVariableSize; i++) {
                    ArrayList<String> newTerms = new ArrayList<String>();
                    for (String t : terms) {
                        for (int j = 0; j <= paraHighestOrder[i]; j++) {
                            newTerms.add(t + "_" + String.valueOf(j));
                        }
                    }
                    terms = newTerms;
                }
            }

            allTerms = new int[terms.size()][paraVariableSize];
            int i = 0;
            for (String term : terms) {
                String[] s = term.split("_");
                for (int j = 0; j < s.length; j++) {
                    allTerms[i][j] = Integer.parseInt(s[j]);
                }
                i++;
            }
            parameter = new double[allTerms.length];
        }

        /**
         * Generate a new polynomial with fresh upper-bound,
         * lower-bound, and parameters. Other fields are
         * derived from another polynomial.
         *
         * @param poly polynomial
         */
        Polynomial(final Polynomial poly) {
            this.variableSize = poly.variableSize;
            this.highestOrder = poly.highestOrder;
            this.allTerms = poly.allTerms;
            this.terms = poly.terms;
            this.id = poly.id;
            lowerBound = new double[variableSize];
            upperBound = new double[variableSize];
            parameter = new double[allTerms.length];
        }
    }
}
