/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.iosp.grads;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Class to hold information from a GrADS Data Descriptor File
 *
 * @see http://www.iges.org/grads/gadoc/descriptorfile.html
 * 
 * @author         Don Murray - CU/CIRES
 */
public class GradsDataDescriptorFile {

    /** logger */
    private static org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(GradsDataDescriptorFile.class);


    /** DSET identifier */
    public static final String DSET = "DSET";

    /** DTYPE identifier */
    public static final String DTYPE = "DTYPE";

    /** INDEX identifier */
    public static final String INDEX = "INDEX";

    /** TITLE identifier */
    public static final String TITLE = "TITLE";

    /** UNDEF identifier */
    public static final String UNDEF = "UNDEF";

    /** UNPACK identifier */
    public static final String UNPACK = "UNPACK";

    /** FILEHEADER identifier */
    public static final String FILEHEADER = "FILEHEADER";

    /** XYHEADER identifier */
    public static final String XYHEADER = "XYHEADER";

    /** THEADER identifier */
    public static final String THEADER = "THEADER";

    /** OPTIONS identifier */
    public static final String OPTIONS = "OPTIONS";

    /** XDEF identifier */
    public static final String XDEF = "XDEF";

    /** YDEF identifier */
    public static final String YDEF = "YDEF";

    /** ZDEF identifier */
    public static final String ZDEF = "ZDEF";

    /** TDEF identifier */
    public static final String TDEF = "TDEF";

    /** EDEF identifier */
    public static final String EDEF = "EDEF";

    /** EDEF identifier */
    public static final String PDEF = "PDEF";

    /** ENDEDEF identifier */
    public static final String ENDEDEF = "ENDEDEF";

    /** VARS identifier */
    public static final String VARS = "VARS";

    /** ENDVARS identifier */
    public static final String ENDVARS = "ENDVARS";

    /** y revesed identifier */
    private static final String YREV = "YREV";

    /** template identifier */
    private static final String TEMPLATE = "TEMPLATE";

    /** big endian identifier */
    private static final String BIG_ENDIAN = "BIG_ENDIAN";

    /** little endian identifier */
    private static final String LITTLE_ENDIAN = "LITTLE_ENDIAN";

    /** sequential identifier */
    private static final String SEQUENTIAL = "SEQUENTIAL";

    /** the file that this relates to */
    private String ddFile;

    /** the data file that this points to */
    private String dataFile;

    /** Is this a big endian file? */
    boolean bigEndian = false;

    /** missing data value */
    private double missingData = Double.NaN;

    /** number of xy header bytes */
    private int xyHeaderBytes = 0;

    /** number of file header bytes */
    private int fileHeaderBytes = 0;

    /** data type */
    private String dataType = null;

    /** The list of variables */
    List<GradsVariable> variableList;

    /** The list of dimensions */
    List<GradsDimension> dimList;

    /** The list of dimensions */
    List<GradsAttribute> attrList;

    /** The XDEF dimension */
    private GradsDimension xDim;

    /** The YDEF dimension */
    private GradsDimension yDim;

    /** The ZDEF dimension */
    private GradsDimension zDim;

    /** The TDEF dimension */
    private GradsTimeDimension tDim;

    /** The EDEF dimension */
    private GradsEnsembleDimension eDim;

    /** The title */
    private String title = null;

    /** grids per timestep */
    private int gridsPerTimeStep = 0;

    /** is this a template file */
    private boolean isTemplate = false;

    /** is this a sequential file */
    private boolean isSequential = false;

    /** is y reversed */
    private boolean yReversed = false;

    /** the list of filenames that this ctl points to */
    List<String> fileNames;
    
    /** defines a projection */
    private boolean hasProjection = false;

    /**
     * Create a GradsDataDescriptorFile from the file
     *
     * @param filename  the name of the file
     */
    public GradsDataDescriptorFile(String filename) {
        ddFile = filename;
        try {
            parseDDF();
        } catch (Exception e) {
            System.err.println("couldn't parse file: " + e.getMessage());
        }
    }

    /**
     * Parse the file
     *
     * @throws Exception problem parsing the file
     */
    private void parseDDF() throws Exception {

        BufferedReader r = new BufferedReader(new FileReader(ddFile));

        //System.err.println("parsing " + ddFile);

        variableList = new ArrayList<GradsVariable>();
        dimList      = new ArrayList<GradsDimension>();
        attrList     = new ArrayList<GradsAttribute>();


        // begin parsing
        try {
            boolean        inVarSection = false;
            boolean        inEnsSection = false;
            String         line;
            String         original;
            GradsDimension curDim = null;

            while ((original = r.readLine()) != null) {

                original = original.trim();
                if (original.isEmpty()) {
                    continue;
                }
                line = original.toLowerCase();

                if (line.startsWith("@ ")) {
                    attrList.add(GradsAttribute.parseAttribute(original));

                    continue;
                }

                // ignore attribute metadata and comments 
                //if (line.startsWith("@") || line.startsWith("*")) {
                if (line.startsWith("*")) {
                    continue;
                }

                if (inEnsSection) {
                    if (line.startsWith(ENDEDEF.toLowerCase())) {
                        inEnsSection = false;

                        continue;  // done skipping ensemble definitions
                    }
                    // parse the ensemble info
                }
                if (inVarSection) {
                    if (line.startsWith(ENDVARS.toLowerCase())) {
                        inVarSection = false;

                        continue;  // done parsing variables
                    }
                    GradsVariable var       = new GradsVariable(original);
                    int           numLevels = var.getNumLevels();
                    if (numLevels == 0) {
                        numLevels = 1;
                    }
                    gridsPerTimeStep += numLevels;

                    // parse a variable
                    variableList.add(var);

                } else {
                    // not in var section or edef section, look for general metadata
                    StringTokenizer st    = new StringTokenizer(original);

                    String          label = st.nextToken();

                    // TODO: Handle other options
                    if (label.equalsIgnoreCase(OPTIONS)) {
                        curDim = null;
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken();
                            if (token.equalsIgnoreCase(BIG_ENDIAN)) {
                                bigEndian = true;
                            } else if (token.equalsIgnoreCase(
                                    LITTLE_ENDIAN)) {
                                bigEndian = false;
                            } else if (token.equalsIgnoreCase(YREV)) {
                                yReversed = true;
                            } else if (token.equalsIgnoreCase(TEMPLATE)) {
                                isTemplate = true;
                            } else if (token.equalsIgnoreCase(SEQUENTIAL)) {
                                isSequential = true;
                            }
                        }
                    } else if (label.equalsIgnoreCase(DSET)) {
                        curDim   = null;
                        dataFile = st.nextToken();
                        dataFile = getFullPath(dataFile, getDDFPath());
                    } else if (label.equalsIgnoreCase(UNDEF)) {
                        curDim      = null;
                        missingData = Double.parseDouble(st.nextToken());
                    } else if (label.equalsIgnoreCase(XYHEADER)) {
                        curDim        = null;
                        xyHeaderBytes = Integer.parseInt(st.nextToken());
                    } else if (label.equalsIgnoreCase(FILEHEADER)) {
                        curDim          = null;
                        fileHeaderBytes = Integer.parseInt(st.nextToken());
                    } else if (label.equalsIgnoreCase(XDEF)) {
                        int xSize =
                            Integer.valueOf(st.nextToken()).intValue();
                        String xMapping = st.nextToken();
                        xDim   = new GradsDimension(label, xSize, xMapping);
                        curDim = xDim;
                        dimList.add(xDim);
                    } else if (label.equalsIgnoreCase(YDEF)) {
                        int ySize =
                            Integer.valueOf(st.nextToken()).intValue();
                        String yMapping = st.nextToken();
                        yDim   = new GradsDimension(label, ySize, yMapping);
                        curDim = yDim;
                        dimList.add(yDim);
                    } else if (label.equalsIgnoreCase(ZDEF)) {
                        int zSize =
                            Integer.valueOf(st.nextToken()).intValue();
                        String zMapping = st.nextToken();
                        zDim   = new GradsDimension(label, zSize, zMapping);
                        curDim = zDim;
                        dimList.add(zDim);
                    } else if (label.equalsIgnoreCase(TDEF)) {
                        int tSize =
                            Integer.valueOf(st.nextToken()).intValue();
                        // we can read the following directly
                        // since tdef never uses "levels"
                        String tMapping = st.nextToken();
                        tDim = new GradsTimeDimension(label, tSize, tMapping);
                        curDim = tDim;
                        dimList.add(tDim);
                    } else if (label.equalsIgnoreCase(EDEF)) {
                        int eSize =
                            Integer.valueOf(st.nextToken()).intValue();
                        // Check if EDEF entry is the short or extended version 
                        if (st.nextToken().equalsIgnoreCase(
                                GradsEnsembleDimension.NAMES)) {
                            inEnsSection = false;
                            String eMapping = GradsEnsembleDimension.NAMES;
                            eDim = new GradsEnsembleDimension(label, eSize,
                                    eMapping);
                            curDim = eDim;
                            dimList.add(curDim);
                        } else {
                            // TODO: handle list of ensembles
                            curDim       = null;
                            inEnsSection = true;
                        }
                    } else if (label.equalsIgnoreCase(PDEF)) {
                    	curDim = null;
                    	hasProjection = true;
                    } else if (label.equalsIgnoreCase(VARS)) {
                        curDim       = null;
                        inVarSection = true;
                    } else if (label.equalsIgnoreCase(DTYPE)) {
                        curDim   = null;
                        dataType = st.nextToken();
                    } else if (label.equalsIgnoreCase(TITLE)) {
                        curDim = null;
                        title =
                            original.substring(original.indexOf(" ")).trim();
                    } else if (curDim != null) {
                        curDim.addLevel(label);
                    }
                    // get the rest of the tokens
                    if (curDim != null) {
                        while (st.hasMoreTokens()) {
                            curDim.addLevel(st.nextToken());
                        }
                    }
                }

            }  // end parsing loop

        } catch (IOException ioe) {
            log.error("Error parsing metadata for " + ddFile);
            throw new IOException("error parsing metadata for " + ddFile);
        } finally {
            try {
                r.close();
            } catch (IOException ioe) {}
        }
    }

    /**
     * Get the dimensions
     *
     * @return  the dimensions
     */
    public List<GradsDimension> getDimensions() {
        return dimList;
    }

    /**
     * Get the variables
     *
     * @return  the variables
     */
    public List<GradsVariable> getVariables() {
        return variableList;
    }

    /**
     * Get the attributes
     *
     * @return the attributes
     */
    public List<GradsAttribute> getAttributes() {
        return attrList;
    }

    /**
     * Get the ensemble dimension
     *
     * @return the ensemble dimension
     */
    public GradsEnsembleDimension getEnsembleDimension() {
        return eDim;
    }

    /**
     * Get the time dimension
     *
     * @return the time dimension
     */
    public GradsTimeDimension getTimeDimension() {
        return tDim;
    }

    /**
     * Get the Z dimension
     *
     * @return the Z dimension
     */
    public GradsDimension getZDimension() {
        return zDim;
    }

    /**
     * Get the Y dimension
     *
     * @return the Y dimension
     */
    public GradsDimension getYDimension() {
        return yDim;
    }

    /**
     * Get the X dimension
     *
     * @return the X dimension
     */
    public GradsDimension getXDimension() {
        return xDim;
    }

    /**
     * Get the data file path
     *
     * @return  the data file path
     */
    public String getDataFile() {
        return dataFile;
    }

    /**
     * Get the data descriptor file path
     *
     * @return  the data descriptor file path
     */
    public String getDataDescriptorFile() {
        return ddFile;
    }

    /**
     * Get the missing value
     *
     * @return the missing value
     */
    public double getMissingValue() {
        return missingData;
    }

    /**
     * Get the number of grids per timestep
     *
     * @return the number of grids per timestep
     */
    public int getGridsPerTimeStep() {
        return gridsPerTimeStep;
    }

    /**
     * Get whether this is using a template or not
     *
     * @return whether this is using a template or not
     */
    public boolean isTemplate() {
        return isTemplate;
    }

    /**
     * Get whether this is using a projection or no
     *
     * @return whether this is using a projection or not
     */
    public boolean hasProjection() {
        return hasProjection;
    }

    /**
     * Get whether this is a sequentially written file
     *
     * @return true if this is sequential
     */
    public boolean isSequential() {
        return isSequential;
    }

    /**
     * Get whether y axis is reversed or not
     *
     * @return whether y axis is reversed or not
     */
    public boolean isYReversed() {
        return yReversed;
    }

    /**
     * Get the number of xy header bytes
     *
     * @return the number of xy header bytes
     */
    public int getXYHeaderBytes() {
        return xyHeaderBytes;
    }

    /**
     * Get the number of file header bytes
     *
     * @return the number of file header bytes
     */
    public int getFileHeaderBytes() {
        return fileHeaderBytes;
    }


    /**
     * Is this a big endian file
     *
     * @return  true if big endian
     */
    public boolean isBigEndian() {
        return bigEndian;
    }

    /**
     * Get the title
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the data type.  Only support raw binary
     *
     * @return  type or null
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Return a String representation of this object
     *
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Parsed: ");
        buf.append(ddFile);
        buf.append("\n");
        buf.append("Data file: ");
        buf.append(dataFile);
        buf.append("\n");
        for (GradsDimension dim : dimList) {
            buf.append(dim.toString());
        }
        for (GradsVariable var : variableList) {
            buf.append(var.toString());
        }

        return buf.toString();
    }

    /**
     * For testing
     *
     * @param args  the filename
     */
    public static void main(String[] args) {
        GradsDataDescriptorFile gdd = new GradsDataDescriptorFile(args[0]);
        System.out.println(gdd);
    }

    /**
     * Get the list of filenames
     *
     * @return the filenames
     *
     * @throws IOException _more_
     */
    public List<String> getFileNames() throws IOException {
        if (fileNames == null) {
            fileNames = new ArrayList<String>();
            String curFile = null;
            String path    = getDDFPath();
            if ( !isTemplate()) {
                fileNames.add(getFullPath(dataFile, path));
            } else if (dataFile.indexOf("%e") >= 0) {
                List<String> ensNames = eDim.getEnsembleNames();
                for (String ename : ensNames) {
                    curFile = getFullPath(dataFile.replace("%e", ename),
                                          path);
                    fileNames.add(curFile);
                }
            }
            // now make sure they exist
            for (String file : fileNames) {
                File f = new File(file);
                if ( !f.exists()) {
                    log.error("File: " + f + " does not exist");
                    throw new IOException("File: " + f + " does not exist");
                }
            }
        }
        return fileNames;
    }

    /**
     * Get the path to the Data Descriptor File
     *
     * @return the path to the Data Descriptor File
     */
    private String getDDFPath() {
        int lastSlash = ddFile.lastIndexOf("/");
        if (lastSlash < 0) {
            lastSlash = ddFile.lastIndexOf(File.separator);
        }
        String path = (lastSlash < 0)
                      ? ""
                      : ddFile.substring(0, lastSlash + 1);

        return path;
    }

    /**
     * Get the full path for a given filename
     *
     * @param filename   the raw filename
     * @param ddfPath  the path to the DDF
     *
     * @return  the full filename
     */
    private String getFullPath(String filename, String ddfPath) {

        String file = filename;
        if (filename.startsWith("^")) {
            file = filename.replace("^", "");
            file = ddfPath + file;
        } else {
            File f = new File(filename);
            if ( !f.isAbsolute()) {
                file = ddfPath + filename;
            } else {
                file = filename;
            }
        }

        return file;
    }

}

