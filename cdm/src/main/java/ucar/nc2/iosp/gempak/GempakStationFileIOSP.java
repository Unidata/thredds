/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package ucar.nc2.iosp.gempak;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.iosp.AbstractIOServiceProvider;

import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.StringUtil;

import visad.util.Trace;

import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.PrintStream;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;


/**
 * An IOSP for Gempak Station (SF,SN) data.
 *
 * @author Unidata Java Development Team
 */
public abstract class GempakStationFileIOSP extends AbstractIOServiceProvider {

    /** The netCDF file */
    protected NetcdfFile ncfile;

    /** the file we are reading */
    protected RandomAccessFile raf;

    /** Gempak file reader */
    protected AbstractGempakStationFileReader gemreader;

    /** place to store debug stuff */
    protected StringBuilder parseInfo = new StringBuilder();

    /** data formatter */
    private DateFormatter dateFormat = new DateFormatter();

    /** Float missing attribute */
    protected final static Number RMISS = new Float(GempakConstants.RMISSD);

    /** Integer missing attribute */
    protected final static Number IMISS = new Integer(GempakConstants.IMISSD);

    /** static for shared dimension of length 4 */
    protected final static Dimension DIM_LEN8 = new Dimension("len8", 8,
                                                    true);

    /** static for shared dimension of length 4 */
    protected final static Dimension DIM_LEN4 = new Dimension("len4", 4,
                                                    true);

    /** static for shared dimension of length 2 */
    protected final static Dimension DIM_LEN2 = new Dimension("len2", 2,
                                                    true);

    /** name for the time variable */
    protected final static String TIME_VAR = "time";

    /** name for the time variable */
    protected final static String MISSING_VAR = "_isMissing";

    /** station variable names */
    private static String[] stnVarNames = {
        GempakStation.STID, GempakStation.STNM, GempakStation.SLAT,
        GempakStation.SLON, GempakStation.SELV, GempakStation.STAT,
        GempakStation.COUN, GempakStation.STD2, GempakStation.SPRI,
        GempakStation.SWFO, GempakStation.WFO2
    };


    /** lengths of station variable */
    private static int[] stnVarSizes = {
        8, 4, 4, 4, 4, 2, 2, 4, 4, 4, 4
    };


    /**
     * Is this a valid file?
     *
     * @param raf  RandomAccessFile to check
     *
     * @return true if a valid Gempak grid file
     *
     * @throws IOException  problem reading file
     */
    public boolean isValidFile(RandomAccessFile raf) throws IOException {
        try {
            gemreader = makeStationReader();
            Trace.call1("GEMPAKSIOSP.isValidFile: reader.init");
            gemreader.init(raf, false);
            Trace.call2("GEMPAKSIOSP.isValidFile: reader.init");
        } catch (Exception ioe) {
            return false;
        }
        return true;
    }

    /**
     * Make the appropriate station file reader, subclasses need to implement
     * this
     *
     * @return  the appropriate reader for that subclass
     */
    protected abstract AbstractGempakStationFileReader makeStationReader();

    /**
     * Open the service provider for reading.
     * @param raf  file to read from
     * @param ncfile  netCDF file we are writing to (memory)
     * @param cancelTask  task for cancelling
     *
     * @throws IOException  problem reading file
     */
    public void open(RandomAccessFile raf, NetcdfFile ncfile,
                     CancelTask cancelTask)
            throws IOException {

        //System.out.printf("GempakSurfaceIOSP open %s (%s) %n", raf.getLocation(), Calendar.getInstance().getTime());

        this.raf    = raf;
        this.ncfile = ncfile;
        long start = System.currentTimeMillis();
        if (gemreader == null) {
            gemreader = makeStationReader();
        }
        Trace.call1("GEMPAKStationIOSP.open: initTables");
        initTables();
        Trace.call2("GEMPAKStationIOSP.open: initTables");
        Trace.call1("GEMPAKStationIOSP.open: reader.init");
        gemreader.init(raf, true);
        Trace.call2("GEMPAKStationIOSP.open: reader.init");
        buildNCFile();
    }

    /**
     * Initialize the parameter tables.
     */
    private void initTables() {
        try {
            GempakParameters.addParameters(
                "resources/nj22/tables/gempak/params.tbl");
        } catch (Exception e) {
            System.out.println("unable to init param tables");
        }
    }

    /**
     * Close this IOSP
     *
     * @throws IOException problem closing file
     */
    public void close() throws IOException {
        raf.close();
    }

    /**
     * Sync and extend
     *
     * @return false
     */
    public boolean syncExtend() {
        return false;
    }

    /**
     * Get the detail information
     *
     * @return the detail info
     */
    public String getDetailInfo() {
        return parseInfo.toString();
    }

    /**
     * Sync the file
     *
     * @return  true if needed to sync
     *
     * @throws IOException problem synching the file
     */
    public boolean sync() throws IOException {
        //printStack("***************************** sync ************************", 100);
        //System.out.printf("check sync on %s (%s) %n", raf.getLocation(), Calendar.getInstance().getTime());

        if (gemreader.getInitFileSize() < raf.length()) {
            long start = System.currentTimeMillis();
            Trace.msg("GEMPAKStationIOSP.sync: file " + raf.getLocation()
                      + " is bigger: " + raf.length() + " > "
                      + gemreader.getInitFileSize());
            Trace.call1("GEMPAKStationIOSP.sync: reader.init");
            gemreader.init(raf, true);
            Trace.call2("GEMPAKStationIOSP.sync: reader.init");
            Trace.call1("GEMPAKStationIOSP.sync: buildNCFile");
            // reconstruct the ncfile objects
            buildNCFile();
            Trace.call2("GEMPAKSIOSP.sync: buildNCFile");
            //System.out.printf("sync on %s took %d msecs%n", raf.getLocation(), (System.currentTimeMillis()-start));
            return true;
        }
        return false;
    }


    /**
     * Build the netCDF file
     *
     * @throws IOException   problem reading the file
     */
    protected void buildNCFile() throws IOException {
        Trace.call1("GEMPAKSIOSP: buildNCFile");
        ncfile.empty();
        fillNCFile();
        addGlobalAttributes();
        ncfile.finish();
        Trace.call2("GEMPAKSIOSP: buildNCFile");
        //System.out.println(ncfile);
    }

    /**
     * Fill the contents of the netCDF file.  Assumes that the file has been
     * cleared.
     *
     * @throws IOException   problem reading the file
     */
    protected abstract void fillNCFile() throws IOException;

    /**
     * Make a structure for the part
     *
     * @param partName   partname
     * @param dimensions dimensions for the structure
     * @param includeMissing  true to include the missing variable
     *
     * @return  a Structure
     */
    protected Structure makeStructure(String partName, List dimensions,
                                      boolean includeMissing) {
        List<GempakParameter> params = gemreader.getParameters(partName);
        if (params == null) {
            return null;
        }
        Structure sVar = new Structure(ncfile, null, null, partName);
        sVar.setDimensions(dimensions);
        for (GempakParameter param : params) {
            sVar.addMemberVariable(makeParamVariable(param, null));
        }
        if (includeMissing) {
            sVar.addMemberVariable(makeMissingVariable());
        }
        return sVar;
    }

    /**
     * Make the missing variable
     *
     * @return the missing variable
     */
    protected Variable makeMissingVariable() {
        Variable var = new Variable(ncfile, null, null, MISSING_VAR);
        var.setDataType(DataType.BYTE);
        var.setDimensions((List<Dimension>) null);
        var.addAttribute(
            new Attribute(
                "description",
                "missing flag - 1 means all params are missing"));
        var.addAttribute(new Attribute("missing_value", new Byte((byte) 1)));
        return var;
    }

    /**
     * Make a variable from a GempakParmaeter
     *
     * @param param  GempakParameter
     * @param dims   Variable dimensions
     *
     * @return  the Variable
     */
    protected Variable makeParamVariable(GempakParameter param,
                                         List<Dimension> dims) {
        Variable var = new Variable(ncfile, null, null, param.getName());
        var.setDataType(DataType.FLOAT);
        var.setDimensions(dims);
        var.addAttribute(new Attribute("long_name", param.getDescription()));
        String units = param.getUnit();
        if ((units != null) && !units.equals("")) {
            var.addAttribute(new Attribute("units", units));
        }
        var.addAttribute(new Attribute("missing_value", RMISS));
        return var;
    }

    /**
     * Add on global attributes for all types
     */
    protected void addGlobalAttributes() {
        // global stuff
        ncfile.addAttribute(null,
                            new Attribute("Conventions", getConventions()));
        String fileType = "GEMPAK " + gemreader.getFileType();
        ncfile.addAttribute(null, new Attribute("file_format", fileType));
        ncfile.addAttribute(null,
                            new Attribute("history",
                                          "Direct read of " + fileType
                                          + " into NetCDF-Java 4.1 API"));  //  at " + dateFormat.toDateTimeStringISO(new Date())));
        ncfile.addAttribute(null, new Attribute(CF.featureTypeAtt, getCFFeatureType()));
    }

    /**
     * Get the netCDF conventions identifier.
     * @return the convention name
     */
    public String getConventions() {
        return "GEMPAK/CDM";
    }

    /**
     * Get the CF feature type, subclasses should override
     * @return the feature type
     */
    public String getCFFeatureType() {
        return CF.FeatureType.point.toString();
    }

    /**
     * Get the size of a particular station variable
     *
     * @param name name of the variable (key)
     *
     * @return  size or -1
     */
    protected int getStnVarSize(String name) {
        int size = -1;
        for (int i = 0; i < stnVarNames.length; i++) {
            if (name.equals(stnVarNames[i])) {
                size = stnVarSizes[i];
                break;
            }
        }
        return size;
    }


    /**
     * Make the station variables from a representative station
     *
     * @param stations  list of stations
     * @param dim  station dimension
     *
     * @return  the list of variables
     */
    protected List<Variable> makeStationVars(List<GempakStation> stations,
                                             Dimension dim) {
        int     numStations = stations.size();
        boolean useSTID     = true;
        for (GempakStation station : stations) {
            if (station.getSTID().equals("")) {
                useSTID = false;
                break;
            }
        }
        List<Variable> vars        = new ArrayList<Variable>();
        List<String>   stnKeyNames = gemreader.getStationKeyNames();
        for (String varName : stnKeyNames) {
            Variable v = makeStationVariable(varName, dim);
            // use STNM or STID as the name or description
            Attribute stIDAttr = new Attribute("standard_name", "station_id");
            if (varName.equals(GempakStation.STID) && useSTID) {
                v.addAttribute(stIDAttr);
            }
            if (varName.equals(GempakStation.STNM) && !useSTID) {
                v.addAttribute(stIDAttr);
            }
            vars.add(v);
        }
        // see if we fill these in completely now
        if ((dim != null) && (numStations > 0)) {
            for (Variable v : vars) {
                Array varArray;
                if (v.getDataType().equals(DataType.CHAR)) {
                    int[] shape = v.getShape();
                    varArray = new ArrayChar.D2(shape[0], shape[1]);
                } else {
                    varArray = get1DArray(v.getDataType(), numStations);
                }
                int    index   = 0;
                String varname = v.getName();
                for (GempakStation stn : stations) {
                    String test = "";
                    if (varname.equals(GempakStation.STID)) {
                        test = stn.getName();
                    } else if (varname.equals(GempakStation.STNM)) {
                        ((ArrayInt.D1) varArray).set(index,
                        // (int) (stn.getSTNM() / 10));
                        (int) (stn.getSTNM()));
                    } else if (varname.equals(GempakStation.SLAT)) {
                        ((ArrayFloat.D1) varArray).set(index,
                                (float) stn.getLatitude());
                    } else if (varname.equals(GempakStation.SLON)) {
                        ((ArrayFloat.D1) varArray).set(index,
                                (float) stn.getLongitude());
                    } else if (varname.equals(GempakStation.SELV)) {
                        ((ArrayFloat.D1) varArray).set(index,
                                (float) stn.getAltitude());
                    } else if (varname.equals(GempakStation.STAT)) {
                        test = stn.getSTAT();
                    } else if (varname.equals(GempakStation.COUN)) {
                        test = stn.getCOUN();
                    } else if (varname.equals(GempakStation.STD2)) {
                        test = stn.getSTD2();
                    } else if (varname.equals(GempakStation.SPRI)) {
                        ((ArrayInt.D1) varArray).set(index, stn.getSPRI());
                    } else if (varname.equals(GempakStation.SWFO)) {
                        test = stn.getSWFO();
                    } else if (varname.equals(GempakStation.WFO2)) {
                        test = stn.getWFO2();
                    }
                    if ( !test.equals("")) {
                        ((ArrayChar.D2) varArray).setString(index, test);
                    }
                    index++;
                }
                v.setCachedData(varArray, false);
            }
        }
        return vars;
    }

    /**
     * Get a 1DArray for the type and length
     *
     * @param type  DataType
     * @param len   length
     *
     * @return  the array
     */
    private Array get1DArray(DataType type, int len) {
        Array varArray = null;
        if (type.equals(DataType.FLOAT)) {
            varArray = new ArrayFloat.D1(len);
        } else if (type.equals(DataType.DOUBLE)) {
            varArray = new ArrayDouble.D1(len);
        } else if (type.equals(DataType.INT)) {
            varArray = new ArrayInt.D1(len);
        }
        return varArray;
    }


    /**
     * Make a station variable
     *
     * @param varname  variable name
     * @param firstDim station dimension
     *
     * @return corresponding variable
     */
    protected Variable makeStationVariable(String varname,
                                           Dimension firstDim) {
        String          longName = varname;
        String          unit     = null;
        DataType        type     = DataType.CHAR;
        List<Dimension> dims     = new ArrayList<Dimension>();
        List<Attribute> attrs    = new ArrayList<Attribute>();
        if (firstDim != null) {
            dims.add(firstDim);
        }

        if (varname.equals(GempakStation.STID)) {
            longName = "Station identifier";
            dims.add(DIM_LEN8);
        } else if (varname.equals(GempakStation.STNM)) {
            longName = "WMO station id";
            type     = DataType.INT;
        } else if (varname.equals(GempakStation.SLAT)) {
            longName = "latitude";
            unit     = "degrees_north";
            type     = DataType.FLOAT;
            attrs.add(new Attribute("standard_name", "latitude"));
        } else if (varname.equals(GempakStation.SLON)) {
            longName = "longitude";
            unit     = "degrees_east";
            type     = DataType.FLOAT;
            attrs.add(new Attribute("standard_name", "longitude"));
        } else if (varname.equals(GempakStation.SELV)) {
            longName = "altitude";
            unit     = "meter";
            type     = DataType.FLOAT;
            attrs.add(new Attribute("positive", "up"));
            attrs.add(new Attribute("standard_name", "station_altitude"));
        } else if (varname.equals(GempakStation.STAT)) {
            longName = "state or province";
            dims.add(DIM_LEN2);
        } else if (varname.equals(GempakStation.COUN)) {
            longName = "country code";
            dims.add(DIM_LEN2);
        } else if (varname.equals(GempakStation.STD2)) {
            longName = "Extended station id";
            dims.add(DIM_LEN4);
        } else if (varname.equals(GempakStation.SPRI)) {
            longName = "Station priority";
            type     = DataType.INT;
        } else if (varname.equals(GempakStation.SWFO)) {
            longName = "WFO code";
            dims.add(DIM_LEN4);
        } else if (varname.equals(GempakStation.WFO2)) {
            longName = "Second WFO code";
            dims.add(DIM_LEN4);
        }
        Variable v = new Variable(ncfile, null, null, varname);
        v.setDataType(type);
        v.addAttribute(new Attribute("long_name", longName));
        if (unit != null) {
            v.addAttribute(new Attribute("units", unit));
        }
        if (type.equals(DataType.FLOAT)) {
            v.addAttribute(new Attribute("missing_value", RMISS));
        } else if (type.equals(DataType.INT)) {
            v.addAttribute(new Attribute("missing_value", IMISS));
        }
        if ( !attrs.isEmpty()) {
            for (Attribute attr : attrs) {
                v.addAttribute(attr);
            }
        }
        if ( !dims.isEmpty()) {
            v.setDimensions(dims);
        } else {
            v.setDimensions((String) null);
        }
        return v;
    }

    /**
     * Print the stack trace for a given line of code.
     *
     * @param msg        message to print
     * @param maxLines   number of lines in the stack to print
     */
    protected void printStack(String msg, int maxLines) {
        String trace = getStackTrace();
        if (msg != null) {
            System.out.println(msg);
        }
        StringTokenizer tok    = new StringTokenizer(trace, "\n");
        int             allcnt = 0;
        int             cnt    = 0;
        while (tok.hasMoreTokens()) {
            String line = tok.nextToken();
            allcnt++;
            if (allcnt > 4) {
                System.out.println(line);
                cnt++;
                if (cnt > maxLines) {
                    break;
                }
            }
        }
    }

    /**
     * Return the stack trace of this calling thread
     *
     * @return  The stack trace
     */
    protected String getStackTrace() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new IllegalArgumentException("").printStackTrace(
            new PrintStream(baos));
        return baos.toString();
    }

}

