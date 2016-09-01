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

/**
 *
 */
public class MATLABNonLinear implements IOptimisationTechnique {

    private static String TEMPLATE_DIRECTORY = "/Users/skin/repository/xadd-inference-1/src/xadd/optimization/templates";

    private static String DEFAULT_MATLAB_LOWER_BOUND = "0.0"; //"realmin";
    private static String DEFAULT_MATLAB_UPPER_BOUND = "500.0"; //"realmax";
    private static String DEFAULT_MATLAB_INITIAL_VALUE = "0";

    private static String OBJECTIVE_FILE_NAME = "xadd_obj";
    private static String CONSTRAINTS_FILE_NAME = "xadd_constraints";

    private static MatlabProxyFactory matlabProxyFactory;
    private Configuration matlabConfig;
    private MatlabProxy matlabProxy;

    /**
     * Get the default MATLAB ProxyFactory
     * @return
     */
    private static MatlabProxyFactory GetDefaultMATLABProxyFactory() {

        if(MATLABNonLinear.matlabProxyFactory == null) {

            MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
                    .setHidden(true)
                    .setProxyTimeout(30000L)
                    .build();

            MATLABNonLinear.matlabProxyFactory = new MatlabProxyFactory(options);
        }

        return MATLABNonLinear.matlabProxyFactory;
    }

    /**
     * Get the default MATLAB configuration
     *
     * @param templateDirectory
     * @return  Configuration
     */
    private static Configuration GetDefaultMATLABConfiguration(File templateDirectory) {
        Configuration config = new Configuration(Configuration.VERSION_2_3_25);

        // Load the template directory
        try {
            config.setDirectoryForTemplateLoading(templateDirectory);
            config.setOutputFormat(PlainTextOutputFormat.INSTANCE);
            config.setDefaultEncoding("UTF-8");
            config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            config.setLogTemplateExceptions(false);
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
            System.exit(1);
        }

        return config;
    }

    /**
     *
     */
    public MATLABNonLinear() {

        this.matlabConfig = MATLABNonLinear.GetDefaultMATLABConfiguration(new File(MATLABNonLinear.TEMPLATE_DIRECTORY));

        try {
            // Initialise a MatlabProxy
            this.matlabProxy = MATLABNonLinear.GetDefaultMATLABProxyFactory().getProxy();
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
    public OptimisationResult run(OptimisationDirection maxMin, String objective, Set<String> variables, Collection<String> constraints,
                                  Collection<String> lowerBounds, Collection<String> upperBounds) {

        double maxValue = 0.0;
        Double argMax = null;
        Double cpuTime = null;

        // Convert the variables Set into a HashMap
        ArrayList<HashMap<String, Object>> variableList = new ArrayList<HashMap<String, Object>>();

        Integer varCount = 1;
        for(String var : variables) {
            HashMap<String, Object> tmpMap = new HashMap<String, Object>();
            tmpMap.put("symbol", var);
            tmpMap.put("index", varCount++);

            variableList.add(tmpMap);
        }

        // Create the objective and constraints *.m files
        this.createObjectiveFile(maxMin, objective, variableList);
        this.createConstraintsFile(constraints, variableList, lowerBounds, upperBounds);

        // Run MATLAB's fmincon function with the objective and constraint files
        try {

            String functionCall = this.buildMATLABFunctionCall(variableList.size());
//            System.out.println(functionCall);

            // Change to the domain file directory
            // TODO: Pass in the directory as a parameter
            this.matlabProxy.eval("cd('/Users/skin/repository/xadd-inference-1/src/cmomdp/')");
            this.matlabProxy.eval("clear all;");

            // Execute the functionCall and time its execution
            this.matlabProxy.eval("start_time = cputime;");
            this.matlabProxy.eval(functionCall);
            this.matlabProxy.eval("exec_time = cputime - start_time;");

            // Returns the value of the objective function FUN at the solution X.
            argMax = ((double[]) this.matlabProxy.getVariable("res"))[0];
            maxValue = ((double[]) this.matlabProxy.getVariable("fval"))[0];

            if(maxMin == OptimisationDirection.MAXIMISE) {
                maxValue = -1 * maxValue;
            }

            // The CPU time taken to calculate the result
            cpuTime = ((double[]) this.matlabProxy.getVariable("exec_time"))[0];

        } catch (MatlabInvocationException e) {
            e.printStackTrace();
        }

        return new OptimisationResult(maxValue, argMax, cpuTime);
    }

    /**
     * Returns the MATLAB function call to fmincon
     *
     * @param numObjectiveVariables
     * @return
     */
    private String buildMATLABFunctionCall(Integer numObjectiveVariables) {
        String initialValues = "";
        String minValues = "";
        String maxValues = "";

        for(Integer i = 0; i < numObjectiveVariables; i++) {
            initialValues += MATLABNonLinear.DEFAULT_MATLAB_INITIAL_VALUE + " ";
            minValues += MATLABNonLinear.DEFAULT_MATLAB_LOWER_BOUND + " ";
            maxValues += MATLABNonLinear.DEFAULT_MATLAB_UPPER_BOUND + " ";
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

            objectiveTemp = this.matlabConfig.getTemplate(templateFileName);
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
     * @param maxMin
     * @param objective
     * @param variableList
     * @return  File name of the objective file
     */
    private void createObjectiveFile(OptimisationDirection maxMin, String objective,
                                     ArrayList<HashMap<String, Object>> variableList) {

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("objective", objective);
        data.put("variables", variableList);

        if(maxMin == OptimisationDirection.MAXIMISE) {
            data.put("direction", "-1");
        } else {
            data.put("direction", "1");
        }

//        ArrayList<HashMap<String, Integer>> constantList = new ArrayList<HashMap<String, Integer>>();
//        data.put("constants", constantList);

        this.writeDataToTemplateFile(data, "MATLAB_objective.ftlh", MATLABNonLinear.OBJECTIVE_FILE_NAME + ".m");
    }

    /**
     *
     * @param constraints
     * @param variableList
     * @param lowerBounds
     * @param upperBounds
     * @return
     */
    private void createConstraintsFile(Collection<String> constraints, ArrayList<HashMap<String, Object>> variableList,
                                       Collection<String> lowerBounds,
                                       Collection<String> upperBounds) {

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
