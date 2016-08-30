package xadd.optimization;

import freemarker.core.PlainTextOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import matlabcontrol.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

// TODO: 1. Singleton for the MatlabProxy

public class MATLABNonLinear implements IOptimisationTechnique {

    private static String TEMPLATE_DIRECTORY = "/Users/skin/repository/xadd-inference-1/src/xadd/optimization/templates";

    private static String MATLAB_LOWER_BOUND = "realmin";
    private static String MATLAB_UPPER_BOUND = "realmax";
    private static String MATLAB_INITIAL_VALUE = "0";

    private static String OBJECTIVE_FILE_NAME = "xadd_obj";
    private static String CONSTRAINTS_FILE_NAME = "xadd_constraints";

    private static MatlabProxyFactory factory;
    private Configuration cfg;
    private MatlabProxy proxy;

    /**
     *
     */
    public MATLABNonLinear() {

        // TODO: Should we just use an instance of the Optimise class instead of registering this with the entire class?
        //Optimise.RegisterOptimisationMethod(this);

        this.cfg = new Configuration(Configuration.VERSION_2_3_25);

        // Load the template directory
        try {
            this.cfg.setDirectoryForTemplateLoading(new File(MATLABNonLinear.TEMPLATE_DIRECTORY));
            this.cfg.setOutputFormat(PlainTextOutputFormat.INSTANCE);
            this.cfg.setDefaultEncoding("UTF-8");
            this.cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            this.cfg.setLogTemplateExceptions(false);
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
            System.exit(1);
        }

        // Create a MatlabProxyFactory instance
        if(MATLABNonLinear.factory == null) {
            MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
                    .setHidden(true)
                    .setProxyTimeout(30000L)
                    .build();
            MATLABNonLinear.factory = new MatlabProxyFactory(options);
        }

        // Get a MatlabProxy
        try {
            this.proxy = MATLABNonLinear.factory.getProxy();
        } catch (MatlabConnectionException e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param objective
     * @param variables
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     * @return
     */
    @Override
    public OptimisationResult run(String objective, Set<String> variables, Collection<String> constraints,
                                  Collection<String> lowerBounds, Collection<String> upperBounds) {

        double maxValue = 0.0;
        Double argMax = null;
        Double cpuTime = null;

        OptimisationResult result = null;

        // convert the variables Set into a HashMap
        ArrayList<HashMap<String, Object>> variableList = new ArrayList<HashMap<String, Object>>();

        Integer varCount = 1;
        for(String var : variables) {
            HashMap<String, Object> tmpMap = new HashMap<String, Object>();
            tmpMap.put("symbol", var);
            tmpMap.put("index", varCount++);

            variableList.add(tmpMap);
        }

        // Create the objective and constraints *.m files
        this.createObjectiveFile(objective, variableList);
        this.createConstraintsFile(constraints, lowerBounds, upperBounds, variableList);

        // Run MATLAB's fmincon function with the objective and constraint files
        try {

            this.proxy.eval("cd('/Users/skin/repository/xadd-inference-1/src/cmomdp/')");

            this.proxy.eval("clear all");
            this.proxy.eval("start_time = cputime");

            String functionCall = this.buildMATLABFunctionCall(variableList.size());
            this.proxy.eval(functionCall);
            this.proxy.eval("exec_time = cputime - start_time;");

            // Returns the value of the objective function FUN at the solution X.
            argMax = ((double[]) proxy.getVariable("res"))[0];
            maxValue = ((double[]) proxy.getVariable("fval"))[0];

            // The CPU time taken to calculate the result
            cpuTime = ((double[]) proxy.getVariable("exec_time"))[0];

        } catch (MatlabInvocationException e) {
            e.printStackTrace();
        }

        return new OptimisationResult(maxValue, argMax, cpuTime);
    }

    /**
     * Returns the MATLAB function call
     *
     * @param numObjectiveVariables
     * @return
     */
    private String buildMATLABFunctionCall(Integer numObjectiveVariables) {
        String initialValues = "";
        String minValues = "";
        String maxValues = "";

        for(Integer i = 0; i < numObjectiveVariables; i++) {
            initialValues += MATLABNonLinear.MATLAB_INITIAL_VALUE + " ";
            minValues += MATLABNonLinear.MATLAB_LOWER_BOUND + " ";
            maxValues += MATLABNonLinear.MATLAB_UPPER_BOUND + " ";
        }

        String functionCall = String.format("[res, fval] = fmincon(@%s, [%s], [], [], [], [], [%s], [%s], @%s);",
                MATLABNonLinear.OBJECTIVE_FILE_NAME, initialValues.trim(), minValues.trim(), maxValues.trim(),
                MATLABNonLinear.CONSTRAINTS_FILE_NAME);

        return functionCall;
    }

    /**
     *  Write data to a templated file
     *
     * @param data
     * @param templateFileName
     * @param outFileName
     */
    private void writeDataToTemplateFile(HashMap<String, Object> data, String templateFileName, String outFileName) {

        Writer outFile;
        Template objectiveTemp;

        try {

            objectiveTemp = this.cfg.getTemplate(templateFileName);
            outFile = new FileWriter (new File("/Users/skin/repository/xadd-inference-1/src/cmomdp/" + outFileName));

            objectiveTemp.process(data, outFile);

            outFile.flush();
            outFile.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (TemplateException e) {
            e.printStackTrace();
            System.err.println("Unable to write data to " + templateFileName);
            System.exit(1);
        }

    }

    /**
     *
     * @param objective
     * @param variableList
     * @return  File name of the objective file
     */
    private void createObjectiveFile(String objective, ArrayList<HashMap<String, Object>> variableList) {

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("objective", objective);
        data.put("variables", variableList);

//        ArrayList<HashMap<String, Integer>> constantList = new ArrayList<HashMap<String, Integer>>();
//        data.put("constants", constantList);

        this.writeDataToTemplateFile(data, "MATLAB_objective.ftlh", MATLABNonLinear.OBJECTIVE_FILE_NAME + ".m");
    }

    /**
     *
     * @param constraints
     * @param lowerBounds
     * @param upperBounds
     * @param variableList
     * @return  File name of the constraints file
     */
    private void createConstraintsFile(Collection<String> constraints, Collection<String> lowerBounds,
                                         Collection<String> upperBounds, ArrayList<HashMap<String, Object>> variableList) {

        // Nonlinear equality constraints have to be in the form c(x) <= 0
        // Nonlinear equality constraints are of the form ceq(x) = 0.

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("variables", variableList);

        if(!constraints.isEmpty()) {
            // Constraints
            ArrayList<HashMap<String, Object>> constraintList = new ArrayList<HashMap<String, Object>>();
            Integer constraintCounter = 1;

            for(String constraint : constraints) {
                HashMap<String, Object> tmpMap = new HashMap<String, Object>();

                tmpMap.put("index", constraintCounter);
                tmpMap.put("function", "-1 * (" + constraint + ")");

                constraintList.add(tmpMap);
                constraintCounter++;
            }

            data.put("constraints", constraintList);
        }

//        lowerBounds.addAll(upperBounds);
//
//        for(String bound : lowerBounds) {
//            HashMap<String, Object> tmpMap = new HashMap<String, Object>();
//
//            tmpMap.put("index", constraintCounter);
//            tmpMap.put("function", "-1 * (" + bound + ")");
//
//            constraintList.add(tmpMap);
//            constraintCounter++;
//        }

        this.writeDataToTemplateFile(data, "MATLAB_constraints.ftlh", MATLABNonLinear.CONSTRAINTS_FILE_NAME + ".m");
    }
}
