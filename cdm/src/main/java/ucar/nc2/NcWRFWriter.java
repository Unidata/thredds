package ucar.nc2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.writer.CoverageSubsetter2;
import ucar.nc2.util.Optional;

/**
 * Utility to create WRF intermediate files from netCDFfiles using the netCDF subset services.
 *
 * @author hvandam
 * @since 6/1/2019
 */

public class NcWRFWriter {

    private static class CommandLine {
        @Parameter(names = {"-i", "--input"}, description = "Input dataset.", required = true)
        public File inputFile = null;

        @Parameter(names = {"-o", "--output"}, description = "Output file.", required = true)
        public File outputFile = null;

        @Parameter(names = {"-v", "--variables"}, description = "Generate the output from this list of variables from the " +
                "input dataset.", variableArity = true)
        public List<String> vars = new ArrayList<>();

        @Parameter(names = {"-f", "--vfile"}, description = "Generate the output from an xml file containing " +
                "a list of variables (a varset).")
        public File varsetFile = null;

        @Parameter(names = {"-s", "--showvars"}, description = "Display a list of variables from the input dataset.")
        public boolean show = false;

        @Parameter(names = {"-h", "--help"}, description = "You must provide an input dataset name, an output file name, " +
                "and ONE option: -v (--variables), -f (--vfile), or -s(--showvars).", help = true)
        public boolean help = false;


        private static class ParameterDescriptionComparator implements Comparator<ParameterDescription> {
            // Display parameters in this order in the usage information.
            private final List<String> orderedParamNames = Arrays.asList(
                    "--input", "--output", "--variables", "--vfile", "--showvars", "--help");

            @Override
            public int compare(ParameterDescription p0, ParameterDescription p1) {
                int index0 = orderedParamNames.indexOf(p0.getLongestName());
                int index1 = orderedParamNames.indexOf(p1.getLongestName());
                assert index0 >= 0 : "Unexpected parameter name: " + p0.getLongestName();
                assert index1 >= 0 : "Unexpected parameter name: " + p1.getLongestName();

                return Integer.compare(index0, index1);
            }
        }

        private final JCommander jc;

        public CommandLine(String progName, String[] args) throws ParameterException {

            this.jc = new JCommander(this, null,args);  // Parses args and uses them to initialize *this*.
            jc.setProgramName(progName);           // Displayed in the usage information.

            // Set the ordering of of parameters in the usage information.
            jc.setParameterDescriptionComparator(new NcWRFWriter.CommandLine.ParameterDescriptionComparator());
        }

        public void printUsage() {
            jc.usage();
        }


        public void validateParms(){
            if(!inputFile.exists())
                throw new ParameterException("The input file: " + inputFile + " was not found.");

            // optional arguments, only one is allowed, we don't care which one at this point.
            int a = vars.isEmpty() ? 0 : 1;
            int b = varsetFile == null ? 0 : 1;
            int c = show ? 1 : 0;

            int res = a+b+c;
            if( res != 1)
                throw new ParameterException("Incorrect number of optional arguments, use one and only one.");
        }
    }
    // returns a list of vars not found in the input dataset or provided on the command line
    // the requestedVars are subset of the Coverage collection
    private static List<String> validateRequestedVars(CoverageCollection cc, List<String>requestedVars) {
        List<String> notFound = new ArrayList<>();
        for( String s : requestedVars ) {
            System.out.println("requestedVar is: " + s);
            Coverage grid = cc.findCoverage(s);

            if (grid == null) {
                System.out.println("grid is null");
                notFound.add(s);
            }
        }
        return notFound;
    }

    private static List<String> loadVarsetFile(){
        // open the file
        List<String> theVars = new ArrayList<>();

        return theVars;
    }

    private static void displayDatasetVars(CoverageCollection c, String fName){

        System.out.println("The following grids (variables) were found in the" + fName + " dataset:\n");
        for (Coverage var : c.getCoverages())
            System.out.println(var.getName());
    }

    public static void main(String[] args) throws Exception {
        String progName = NcWRFWriter.class.getName();

        try {
            NcWRFWriter.CommandLine cmdLine = new NcWRFWriter.CommandLine(progName, args);

            if (cmdLine.help) {
                cmdLine.printUsage();
                return;
            }

            cmdLine.validateParms();

            String datasetIn = cmdLine.inputFile.getAbsolutePath();

            if (datasetIn != null) {
                Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openCoverageDataset(datasetIn);
                if (!opt.isPresent())
                    throw new FileNotFoundException("Not a Grid Dataset " + datasetIn + " err=" + opt.getErrorMessage());

                CoverageCollection covColl = opt.get().getSingleCoverageCollection();

    //            Optional<CoverageCollection> opt = CoverageSubsetter2.makeCoverageDatasetSubset(gcd, params.getVar(), subsetParams);

                if (cmdLine.show) {

                    displayDatasetVars(covColl,datasetIn);

                }else if (cmdLine.varsetFile != null) {
                    System.out.println("In varsetFile");

                    List<String> vars = loadVarsetFile();
                    System.out.println("Vars.size is: " + vars.size());
                    if( !vars.isEmpty() ) {
                        List<String> l = loadVarsetFile();
                        List<String> er = validateRequestedVars(covColl, l);

                        if (!er.isEmpty()) {
                            System.out.println("into !errors1.empty");
                            StringBuilder message = new StringBuilder();
                            message.append("The following requested varables were not found in the input dataset:\n");
                            for( String e : er )
                                message.append(e + "\n");
                            System.out.println(message);
                        }
                    }
                }
                else {
                    System.out.println("In varset");

                    List<String> errors = validateRequestedVars(covColl, cmdLine.vars);

                    if (!errors.isEmpty()) {
                        StringBuilder message = new StringBuilder();
                        message.append("The following varset varables were not found in the input dataset:\n");
                        for( String e : cmdLine.vars )
                            message.append(e + "\n");
                        System.out.println(message);
                    }
                        // call WRFWriter( Coverage collection, output file)
                }

                // test to ensure the variables in the varsetFile are in the input dataset
                // if so, create a coverage subset with the varsetFile data
                // call the WRF writer
                                                                                                                                                                        } else {

                // test to ensure the variables in the cmdLine.vars list are in the input dataset
                // if so, create a coverage subset with the vars List
                // call the WRF writer

           }  // if datasetIn
        } catch (ParameterException | IOException e) {
            System.err.println(e.getMessage());
            System.err.printf("Try \"%s --help\" for more information.%n", progName);
        }
    }
}
