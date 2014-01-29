package ucar.nc2.grib.grib1.tables;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The purpose of this class is to read in the files from ECMWFs gribapi software and
 * create useful grib 1 tables for the CDM. Note that the intent is to process these
 * local concept files as minimally as possible. Only run this from the git directory.
 */
public class EcmwfLocalConcepts {
    // super hash map keys
    private static String SHORTNAME_ID = "shortName";
    private static String DESCRIPTION_ID = "description";
    private static String UNIT_ID = "units";

    // IDs used in localConcept files
    private static String TABLE_VERSION_ID = "table2Version";
    private static String PARAM_NUM_ID = "indicatorOfParameter";

    // input streams for the necessary localConcept files
    private InputStream descriptionsIs = null;
    private InputStream shortNamesIs = null;
    private InputStream unitsIs = null;

    // tableNumber -> paramNumber -> metadata from table
    private HashMap<String, HashMap<String, HashMap<String,String>>> localConcepts = new HashMap<String, HashMap<String, HashMap<String,String>>>();

    // location of the localConcept files
    private String ecmwfLocalConceptsLoc;

    // default constructor
    public EcmwfLocalConcepts() {
        // find path to localConcept files

        String sep = File.separator;
        String classPath = EcmwfLocalConcepts.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String split = "thredds"+sep+"grib";
        String sourcesPath = classPath.split(split)[0];
        ecmwfLocalConceptsLoc = sourcesPath+"thredds"+sep+"grib"+sep+"src"+sep+"main"+sep+"sources"+sep+"ecmwfGribApi"+sep;
        // initialize input streams for reading the localConcept files
        try {
            descriptionsIs = new FileInputStream(ecmwfLocalConceptsLoc + "name.def");
            shortNamesIs = new FileInputStream(ecmwfLocalConceptsLoc + "shortName.def");
            unitsIs = new FileInputStream(ecmwfLocalConceptsLoc + "units.def");
        } catch  (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse the localConcept files needed to create grib1 tables for use by the CDM
     * @throws IOException
     */
    public void parseLocalConcepts() throws IOException {

        addLocalConcept(shortNamesIs, SHORTNAME_ID);
        addLocalConcept(descriptionsIs, DESCRIPTION_ID);
        addLocalConcept(unitsIs, UNIT_ID);
    }

    /**
     * Add the information from a localConcept file to super HashMap localConcepts
     *
     * @param is InputStream of the localConcept file
     * @param conceptName "type" of localConcept being added
     * @throws IOException
     */
    private void addLocalConcept(InputStream is, String conceptName) throws IOException {
        /*
        example entry from name.def:

        #Total precipitation of at least 5 mm
        'Total precipitation of at least 5 mm' = {
             table2Version = 131 ;
             indicatorOfParameter = 61 ;
            }

         */

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();

        while (!line.startsWith("#"))
            line = br.readLine(); // skip

        while (true) {
            HashMap<String, String> items = new HashMap<String,String>();
            line = br.readLine();
            if (line == null) break; // done with the file
            if ((line.length() == 0) || line.startsWith("#")) continue;
            line = cleanLine(line);
            if (line.contains("{")) {
                String paramName = line.split("=")[0].trim();
                line = cleanLine(br.readLine());
                while (line.contains("=")) {
                    String[] kvp = line.split("=");
                    items.put(kvp[0].trim(),kvp[1].trim());
                    line = cleanLine(br.readLine());
                }
                String tableVersion = items.get(TABLE_VERSION_ID);
                String parameterNumber = items.get(PARAM_NUM_ID);

                storeConcept(tableVersion, parameterNumber, conceptName, paramName);
            }
        }
    }

    /**
     * clean the string representation of a line in the localConcept
     * file. Basic removal of tabs, semicolons, single quotes, etc.
     * @param lineIn  line (String) to be cleaned
     * @return cleaned version of lineIn
     */
    private String cleanLine(String lineIn) {
        String lineOut = "";
        lineOut = lineIn.replaceAll("'", "");
        lineOut = lineOut.replaceAll("\t", "");
        lineOut = lineOut.replaceAll(";", "");
        lineOut = lineOut.trim();
        return lineOut;
    }

    /**
     * Store localConcept information in super HashMap localConcepts
     *
     * @param tableVersion  - version of the table to add parameter to
     * @param parameterNumber  - parameter number
     * @param key Type of metadata to be added (shortName, Description, Units)
     * @param value Value of metadata
     */
    private void storeConcept(String tableVersion, String parameterNumber, String key, String value) {

        HashMap<String,HashMap<String, String>> tmpTable = null;
        if (localConcepts.containsKey(tableVersion)) {
            tmpTable = localConcepts.get(tableVersion);
            if (tmpTable.containsKey(parameterNumber)) {
                HashMap tmpParam = (HashMap) tmpTable.get(parameterNumber);
                if (!tmpParam.containsKey(key)) {
                     tmpParam.put(key, value);
                } else {
                    System.out.println("already has key value pair: " + key + ":" + value);
                }
            } else {
                HashMap<String, String> tmpParam = new HashMap<String,String>(4);
                tmpParam.put(key, value);
                tmpTable.put(parameterNumber, tmpParam);
            }
        } else {
            tmpTable = new HashMap<String,HashMap<String, String>>();
            HashMap<String, String> tmpParam = new HashMap<String,String>(4);
            tmpParam.put(key, value);
            tmpTable.put(parameterNumber, tmpParam);
        }
        localConcepts.put(tableVersion, tmpTable);
    }

    /**
     * Write out grib1 tables based on localConcepts files - these are the tables
     * that the CDM will read.
     *
     * @throws IOException
     * @throws ParseException
     */
    private void writeGrib1Tables() throws IOException, ParseException {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
        Calendar cal = Calendar.getInstance();
        String writeDate = dateFormat.format(cal.getTime());
        String grib1Info;
        List<String> tableNums = new ArrayList<String>();
        HashMap<String, String> paramInfo;
        Path dir = Paths.get(ecmwfLocalConceptsLoc.replace("sources/", "resources/resources/grib1/"));
        for (String tableNum : localConcepts.keySet()) {
            tableNums.add(tableNum);
            String fileName = "2.98." + tableNum + ".table";
            System.out.println("Writing: " + fileName);
            Path newFile = dir.resolve(fileName);
            Files.deleteIfExists(newFile);
            Files.createFile(newFile);
            FileWriter writer = new FileWriter(newFile.toFile());
            PrintWriter printWriter = new PrintWriter(writer);
            printWriter.println("# Generated by " + this.getClass().getCanonicalName() + " on " + writeDate);
            for (String paramNum : localConcepts.get(tableNum).keySet()) {

                paramInfo = localConcepts.get(tableNum).get(paramNum);

                String shortName = paramInfo.get(SHORTNAME_ID);
                String description = paramInfo.get(DESCRIPTION_ID);
                String units = paramInfo.get(UNIT_ID);

                grib1Info = paramNum + " " + shortName + " [" + description + "] (" + units + ")";

                printWriter.println(grib1Info);
            }
            printWriter.close();
        }
        writeLookupTableFile(tableNums, dir, writeDate);
    }

    /**
     * Write the lookupTables.txt file, which basically registers all of the new grib1 tables
     * with the CDM
     *
     * @param tableNums List of Table Numbers
     * @param dir Directory where the tables live
     * @param writeDate Date on which the main method of this class was run, resulting in new tables
     *
     * @throws IOException
     */
    private void writeLookupTableFile(List<String> tableNums, Path dir, String writeDate) throws IOException {

        Collections.sort(tableNums);
        Path lookupTableReg = dir.resolve("lookupTables.txt");
        Files.deleteIfExists(lookupTableReg);
        Files.createFile(lookupTableReg);
        FileWriter writer = new FileWriter(lookupTableReg.toFile());
        PrintWriter printWriter = new PrintWriter(writer);
        printWriter.println("# Generated by " + this.getClass().getCanonicalName() + " on " + writeDate);

        for (String tn : tableNums) {
            String tableName = "2.98." + tn + ".table";
            String reg = "98:\t-1:\t" + tn + ":\t" + tableName;
            printWriter.println(reg);
        }
        printWriter.close();
    }

    /**
     * Quick prinout to System.out of the different parameter metadata fields
     */
    private void showLocalConcepts() {
        for (String tableNum : localConcepts.keySet()) {
            for (String paramNum : localConcepts.get(tableNum).keySet()) {
                for (String key :localConcepts.get(tableNum).get(paramNum).keySet()) {
                   System.out.println(key + ":" + localConcepts.get(tableNum).get(paramNum).get(key));
                }
            }
        }
    }

    /**
     * Generate grib1 tables for the CDM based on the localConcept files from ECMWF GRIB-API
     *
     * @param args None
     */
    public static void main(String[] args) {

        EcmwfLocalConcepts ec = new EcmwfLocalConcepts();
        try {
            ec.parseLocalConcepts();
            ec.writeGrib1Tables();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
