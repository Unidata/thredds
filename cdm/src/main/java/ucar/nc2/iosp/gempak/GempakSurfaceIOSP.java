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

import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * An IOSP for Gempak Surface data.  Warning:  This is a work in progress
 * and does not give readable files yet!
 *
 * @author Unidata Java Development Team
 */
public class GempakSurfaceIOSP extends AbstractIOServiceProvider {

    /** The netCDF file */
    protected NetcdfFile ncfile;

    /** the file we are reading */
    protected RandomAccessFile raf;

    /** Gempak file reader */
    protected GempakSurfaceFileReader gemreader;

    /** place to store debug stuff */
    protected StringBuilder parseInfo = new StringBuilder();

    /** data formatter */
    private DateFormatter dateFormat = new DateFormatter();

    /** Float missing attribute */
    private final static Number RMISS = new Float(GempakConstants.RMISSD);

    /** Integer missing attribute */
    private final static Number IMISS = new Integer(GempakConstants.IMISSD);

    /** static for shared dimension of length 4 */
    private final static Dimension DIM_LEN8 = new Dimension("len8", 8, true);

    /** static for shared dimension of length 4 */
    private final static Dimension DIM_LEN4 = new Dimension("len4", 4, true);

    /** static for shared dimension of length 2 */
    private final static Dimension DIM_LEN2 = new Dimension("len2", 2, true);

    /** name for the time variable */
    private final static String TIME_VAR = "time";

    /** name for the time variable */
    private final static String MISSING_VAR = "_isMissing";

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
            gemreader = new GempakSurfaceFileReader();
            gemreader.init(raf, false);
        } catch (Exception ioe) {
            return false;
        }
        // TODO:  handle other types of surface files 
        return gemreader.getSurfaceFileType().equals(gemreader.STANDARD)
               || gemreader.getSurfaceFileType().equals(gemreader.SHIP);
    }

    /**
     * Get the file type id
     *
     * @return the file type id
     */
    public String getFileTypeId() {
        return "GempakSurface";
    }

    /**
     * Get the file type description
     *
     * @return the file type description
     */
    public String getFileTypeDescription() {
        return "GEMPAK Surface Obs Data";
    }

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
        this.raf    = raf;
        this.ncfile = ncfile;
        long start = System.currentTimeMillis();
        if (gemreader == null) {
            gemreader = new GempakSurfaceFileReader();
        }
        Trace.call1("GEMPAK: open:initTables");
        initTables();
        Trace.call2("GEMPAK: open:initTables");
        Trace.call1("GEMPAK: reader.init");
        gemreader.init(raf, true);
        Trace.call2("GEMPAK: reader.init");
        Trace.call1("GEMPAK: buildNCFile");
        buildNCFile();
        Trace.call2("GEMPAK: buildNCFile");
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

        if (gemreader.getInitFileSize() < raf.length()) {
            Trace.msg("GEMPAK: file is bigger");
            Trace.call1("GEMPAK: reader.init");
            gemreader.init(true);
            Trace.call2("GEMPAK: reader.init");
            Trace.call1("GEMPAK: buildNCFile");
            // reconstruct the ncfile objects
            ncfile.empty();
            buildNCFile();
            ncfile.finish();
            Trace.call2("GEMPAK: buildNCFile");
            return true;
        }
        
        return false;
    }

    /**
     * Read the data for the variable
     * @param v2  Variable to read
     * @param section   section infomation
     * @return Array of data
     *
     * @throws IOException problem reading from file
     * @throws InvalidRangeException  invalid Range
     */
    public Array readData(Variable v2, Section section)
            throws IOException, InvalidRangeException {
        if (gemreader == null) {
            return null;
        }
        //System.out.println("looking for " + v2);
        //System.out.println("Section = " + section);
        //Trace.call1("GEMPAK: readData");
        Array array = null;
        if (gemreader.getSurfaceFileType().equals(gemreader.SHIP)) {
            array = readShipData(v2, section);
        } else if (gemreader.getSurfaceFileType().equals(
                gemreader.STANDARD)) {
            array = readStandardData(v2, section);
        } else {  // climate data
            //array = readClimateData(v2, section);
        }
        //  long took = System.currentTimeMillis() - start;
        //  System.out.println("  read data took=" + took + " msec ");
        //Trace.call2("GEMPAK: readData");
        return array;
    }

    /**
     * Read in the data for the variable.  In this case, it should be
     * a Structure.  The section should be rank 2 (station, time).
     *
     * @param v2  variable to read
     * @param section  section of the variable
     *
     * @return the array of data
     *
     * @throws IOException  problem reading the file
     */
    private Array readStandardData(Variable v2, Section section)
            throws IOException {

        Array array = null;
        if (v2 instanceof Structure) {
            List<GempakParameter> params =
                gemreader.getParameters(gemreader.SFDT);
            Structure                     pdata         = (Structure) v2;
            StructureMembers members = pdata.makeStructureMembers();
            List<StructureMembers.Member> mbers         =
                members.getMembers();
            int                           i             = 0;
            int                           numBytes      = 0;
            int                           totalNumBytes = 0;
            for (StructureMembers.Member member : mbers) {
                member.setDataParam(4 * i++);
                numBytes      = member.getDataType().getSize();
                totalNumBytes += numBytes;
            }
            // one member is a byte
            members.setStructureSize(totalNumBytes);
            float[] missing = new float[mbers.size()];
            int     missnum = 0;
            for (Variable v : pdata.getVariables()) {
                Attribute att = v.findAttribute("missing_value");
                missing[missnum++] = (att == null)
                                     ? GempakConstants.RMISSD
                                     : att.getNumericValue().floatValue();
            }


            int   num          = 0;
            Range stationRange = section.getRange(0);
            Range timeRange    = section.getRange(1);
            int   size         = stationRange.length() * timeRange.length();
            // Create a ByteBuffer using a byte array
            byte[]     bytes = new byte[totalNumBytes * size];
            ByteBuffer buf   = ByteBuffer.wrap(bytes);
            array = new ArrayStructureBB(members, new int[] { size }, buf, 0);

            //Trace.call1("GEMPAK: readStandardData", section.toString());
            for (int y = stationRange.first(); y <= stationRange.last();
                    y += stationRange.stride()) {
                for (int x = timeRange.first(); x <= timeRange.last();
                        x += timeRange.stride()) {
                    GempakFileReader.RData vals = gemreader.DM_RDTR(x + 1,
                                                      y + 1, gemreader.SFDT);
                    if (vals == null) {
                        int k = 0;
                        for (StructureMembers.Member member : mbers) {
                            if (member.getDataType().equals(DataType.FLOAT)) {
                                buf.putFloat(missing[k]);
                            } else {
                                buf.put((byte) 1);
                            }
                            k++;
                        }
                    } else {
                        float[] reals = vals.data;
                        int     var   = 0;
                        for (GempakParameter param : params) {
                            if (members.findMember(param.getName()) != null) {
                                buf.putFloat(reals[var]);
                            }
                            var++;
                        }
                        // always add the missing flag
                        buf.put((byte) 0);
                    }
                }
            }
            //Trace.call2("GEMPAK: readStandardData");
        }
        return array;
    }

    /**
     * Read in the data for the record variable.  In this case, it should be
     * a Structure of record dimension.  We can handle a subset of the
     * variables in a structure.
     *
     * @param v2  variable to read
     * @param section  section of the variable
     *
     * @return the array of data
     *
     * @throws IOException  problem reading the file
     */
    private Array readShipData(Variable v2, Section section)
            throws IOException {

        Array array = null;
        if (v2 instanceof Structure) {
            List<GempakParameter> params =
                gemreader.getParameters(gemreader.SFDT);
            Structure                     pdata     = (Structure) v2;
            StructureMembers members = pdata.makeStructureMembers();
            List<StructureMembers.Member> mbers     = members.getMembers();
            int                           ssize     = 0;
            int                           stnVarNum = 0;
            List<String> stnKeyNames = gemreader.getStationKeyNames();
            for (StructureMembers.Member member : mbers) {
                if (stnKeyNames.contains(member.getName())) {
                    int varSize = getStnVarSize(member.getName());
                    member.setDataParam(ssize);
                    ssize += varSize;
                } else if (member.getName().equals(TIME_VAR)) {
                    member.setDataParam(ssize);
                    ssize += 8;
                } else if (member.getName().equals(MISSING_VAR)) {
                    member.setDataParam(ssize);
                    ssize += 1;
                } else {
                    member.setDataParam(ssize);
                    ssize += 4;
                }
            }
            members.setStructureSize(ssize);

            // TODO:  figure out how to get the missing value for data
            //float[] missing = new float[mbers.size()];
            //int     missnum = 0;
            //for (Variable v : pdata.getVariables()) {
            //    Attribute att = v.findAttribute("missing_value");
            //    missing[missnum++] = (att == null)
            //                         ? GempakConstants.RMISSD
            //                         : att.getNumericValue().floatValue();
            //}


            Range recordRange = section.getRange(0);
            int   size        = recordRange.length();
            // Create a ByteBuffer using a byte array
            byte[]     bytes = new byte[ssize * size];
            ByteBuffer buf   = ByteBuffer.wrap(bytes);
            array = new ArrayStructureBB(members, new int[] { size }, buf, 0);
            List<GempakStation> stationList    = gemreader.getStations();
            List<Date>          dateList       = gemreader.getDates();
            boolean             needToReadData = !pdata.isSubset();
            if ( !needToReadData) {  // subset, see if we need some param data
                for (GempakParameter param : params) {
                    if (members.findMember(param.getName()) != null) {
                        needToReadData = true;
                        break;
                    }
                }
            }
            boolean hasTime = (members.findMember(TIME_VAR) != null);

            //Trace.call1("GEMPAK: readShipData", section.toString());
            // fill out the station information
            for (int x = recordRange.first(); x <= recordRange.last();
                    x += recordRange.stride()) {
                GempakStation stn = stationList.get(x);
                for (String varname : stnKeyNames) {
                    if (members.findMember(varname) == null) {
                        continue;
                    }
                    String temp = null;
                    if (varname.equals(GempakStation.STID)) {
                        temp = StringUtil.padRight(stn.getName(), 8);
                    } else if (varname.equals(GempakStation.STNM)) {
                        buf.putInt((int) (stn.getSTNM()));
                    } else if (varname.equals(GempakStation.SLAT)) {
                        buf.putFloat((float) stn.getLatitude());
                    } else if (varname.equals(GempakStation.SLON)) {
                        buf.putFloat((float) stn.getLongitude());
                    } else if (varname.equals(GempakStation.SELV)) {
                        buf.putFloat((float) stn.getAltitude());
                    } else if (varname.equals(GempakStation.STAT)) {
                        temp = StringUtil.padRight(stn.getSTAT(), 2);
                    } else if (varname.equals(GempakStation.COUN)) {
                        temp = StringUtil.padRight(stn.getCOUN(), 2);
                    } else if (varname.equals(GempakStation.STD2)) {
                        temp = StringUtil.padRight(stn.getSTD2(), 4);
                    } else if (varname.equals(GempakStation.SPRI)) {
                        buf.putInt(stn.getSPRI());
                    } else if (varname.equals(GempakStation.SWFO)) {
                        temp = StringUtil.padRight(stn.getSWFO(), 4);
                    } else if (varname.equals(GempakStation.WFO2)) {
                        temp = StringUtil.padRight(stn.getWFO2(), 4);
                    }
                    if (temp != null) {
                        buf.put(temp.getBytes());
                    }
                }
                if (members.findMember(TIME_VAR) != null) {
                    // put in the time
                    Date time = dateList.get(x);
                    buf.putDouble(time.getTime() / 1000.d);
                }

                if (needToReadData) {
                    int column = stn.getIndex();
                    GempakFileReader.RData vals = gemreader.DM_RDTR(1,
                                                      column, gemreader.SFDT);
                    if (vals == null) {
                        for (GempakParameter param : params) {
                            if (members.findMember(param.getName()) != null) {
                                buf.putFloat(GempakConstants.RMISSD);
                            }
                        }
                        buf.put((byte) 1);
                    } else {
                        float[] reals = vals.data;
                        int     var   = 0;
                        for (GempakParameter param : params) {
                            if (members.findMember(param.getName()) != null) {
                                buf.putFloat(reals[var]);
                            }
                            var++;
                        }
                        buf.put((byte) 0);
                    }
                }
            }
            //Trace.call2("GEMPAK: readShipData");
        }
        return array;
    }

    /**
     * Test this.
     *
     * @param args file name
     *
     * @throws IOException  problem reading the file
     */
    public static void main(String[] args) throws IOException {
        IOServiceProvider mciosp = new GempakSurfaceIOSP();
        RandomAccessFile  rf     = new RandomAccessFile(args[0], "r", 2048);
        NetcdfFile ncfile = new MakeNetcdfFile(mciosp, rf, args[0], null);
        if (args.length > 1) {
            ucar.nc2.FileWriter.writeToFile(ncfile, args[1]);
        } else {
            System.out.println(ncfile);
        }
    }

    /**
     *   TODO:  generalize this
     *   static class for testing
     */
    protected static class MakeNetcdfFile extends NetcdfFile {

        /**
         * Ctor
         *
         * @param spi IOServiceProvider
         * @param raf RandomAccessFile
         * @param location   location of file?
         * @param cancelTask CancelTask
         *
         * @throws IOException problem opening the file
         */
        MakeNetcdfFile(IOServiceProvider spi, RandomAccessFile raf,
                       String location, CancelTask cancelTask)
                throws IOException {
            super(spi, raf, location, cancelTask);
        }
    }

    /**
     * Build the netCDF file
     *
     * @throws IOException   problem reading the file
     */
    private void buildNCFile() throws IOException {
        String fileType = gemreader.getSurfaceFileType();
        if (fileType.equals(gemreader.STANDARD)) {
            buildStandardFile();
        } else if (fileType.equals(gemreader.SHIP)) {
            buildShipFile();
        } else {
            buildClimateFile();
        }
        addGlobalAttributes();
    }

    /**
     * Build a standard station structure
     */
    private void buildStandardFile() {
        // Build station list
        List<GempakStation> stations = gemreader.getStations();
        Trace.msg("GEMPAK: now have " + stations.size() + " stations");
        Dimension station = new Dimension("station", stations.size(), true);
        ncfile.addDimension(null, station);
        ncfile.addDimension(null, DIM_LEN4);
        ncfile.addDimension(null, DIM_LEN2);
        List<Variable> stationVars = makeStationVars(stations, station);
        // loop through and add to ncfile
        for (Variable stnVar : stationVars) {
            ncfile.addVariable(null, stnVar);
        }


        // Build variable list (var(station,time))
        // time
        List<Date> timeList = gemreader.getDates();
        int        numTimes = timeList.size();
        Dimension  times    = new Dimension(TIME_VAR, numTimes, true);
        ncfile.addDimension(null, times);
        Array varArray = null;
        Variable timeVar = new Variable(ncfile, null, null, TIME_VAR,
                                        DataType.DOUBLE, TIME_VAR);
        timeVar.addAttribute(
            new Attribute("units", "seconds since 1970-01-01 00:00:00"));
        timeVar.addAttribute(new Attribute("long_name", TIME_VAR));
        varArray = new ArrayDouble.D1(numTimes);
        int i = 0;
        for (Date date : timeList) {
            ((ArrayDouble.D1) varArray).set(i, date.getTime() / 1000.d);
            i++;
        }
        timeVar.setCachedData(varArray, false);
        ncfile.addVariable(null, timeVar);


        List<Dimension> stationTime = new ArrayList<Dimension>();
        stationTime.add(station);
        stationTime.add(times);
        // TODO: handle other parts
        /*
        List<GempakParameter> params = gemreader.getParameters(gemreader.SFDT);
        if (params == null) {
            return;
        }
        int j = 0;
        for (GempakParameter param : params) {
            if (j > 0) {
                break;
            }
            Variable v = makeParamVariable(param, stationTime);
            v.addAttribute(new Attribute("coordinates",
                                         "time SLAT SLON SELV"));
            ncfile.addVariable(null, v);
            j++;
        }
        */
        Structure sfData = makeStructure(gemreader.SFDT, stationTime);
        if (sfData == null) {
            return;
        }
        sfData.addAttribute(new Attribute("coordinates",
                                          "time SLAT SLON SELV"));
        ncfile.addVariable(null, sfData);
        ncfile.addAttribute(
            null,
            new Attribute(
                "CF:featureType",
                CF.FeatureType.stationTimeSeries.toString()));
    }

    /**
     * Make a structure for the part
     *
     * @param partName   partname
     * @param dimensions dimensions for the structure
     *
     * @return  a Structure
     */
    private Structure makeStructure(String partName, List dimensions) {
        List<GempakParameter> params = gemreader.getParameters(partName);
        if (params == null) {
            return null;
        }
        Structure sVar = new Structure(ncfile, null, null, partName);
        sVar.setDimensions(dimensions);
        for (GempakParameter param : params) {
            sVar.addMemberVariable(makeParamVariable(param, null));
        }
        sVar.addMemberVariable(makeMissingVariable());
        return sVar;
    }

    /**
     * Make the missing variable
     *
     * @return the missing variable
     */
    private Variable makeMissingVariable() {
        Variable var = new Variable(ncfile, null, null, MISSING_VAR);
        var.setDataType(DataType.BYTE);
        var.setDimensions((List<Dimension>) null);
        var.addAttribute(
            new Attribute(
                "description",
                "missing flag - 1 means all params are missing"));
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
    private Variable makeParamVariable(GempakParameter param,
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
     * Build a ship station structure.  Here the columns are the
     * stations/reports and the rows (1) are the reports.
     */
    private void buildShipFile() {
        // Build variable list (var(station,time))
        List<GempakStation> stations = gemreader.getStations();
        int                 numObs   = stations.size();
        Trace.msg("GEMPAK: now have " + numObs + " stations");
        Dimension record = new Dimension("record", numObs, true);
        ncfile.addDimension(null, record);
        List<Dimension> records = new ArrayList(1);
        records.add(record);

        // time
        Variable timeVar = new Variable(ncfile, null, null, TIME_VAR,
                                        DataType.DOUBLE, null);
        timeVar.addAttribute(
            new Attribute("units", "seconds since 1970-01-01 00:00:00"));
        timeVar.addAttribute(new Attribute("long_name", TIME_VAR));

        ncfile.addDimension(null, DIM_LEN4);
        ncfile.addDimension(null, DIM_LEN2);
        List<Variable>        stationVars = makeStationVars(stations, null);

        List<GempakParameter> params =
            gemreader.getParameters(gemreader.SFDT);
        if (params == null) {
            return;
        }
        Structure sVar = new Structure(ncfile, null, null, "Obs");
        sVar.setDimensions(records);
        // loop through and add to ncfile
        for (Variable stnVar : stationVars) {
            sVar.addMemberVariable(stnVar);
        }
        sVar.addMemberVariable(timeVar);

        for (GempakParameter param : params) {
            Variable var = makeParamVariable(param, null);
            sVar.addMemberVariable(var);
        }
        sVar.addMemberVariable(makeMissingVariable());
        sVar.addAttribute(
            new Attribute(
                "coordinates", "Obs.time Obs.SLAT Obs.SLON Obs.SELV"));
        ncfile.addVariable(null, sVar);
        ncfile.addAttribute(null,
                            new Attribute("CF:featureType",
                                          CF.FeatureType.point.toString()));
    }

    /**
     * Build a ship station structure.  Here the columns are the
     * times and the rows are the stations.
     */
    private void buildClimateFile() {}

    /**
     * Add on global attributes for all types
     */
    private void addGlobalAttributes() {
        // global stuff
        ncfile.addAttribute(null, new Attribute("Conventions", "GEMPAK/CDM"));
        String fileType = "GEMPAK Surface (" + gemreader.getSurfaceFileType()
                          + ")";
        ncfile.addAttribute(null, new Attribute("file_format", fileType));
        ncfile.addAttribute(null,
                            new Attribute("history",
                                          "Direct read of " + fileType
                                          + " into NetCDF-Java 4.0 API"));  //  at " + dateFormat.toDateTimeStringISO(new Date())));
    }

    /**
     * Get the size of a particular station variable
     *
     * @param name name of the variable (key)
     *
     * @return  size or -1
     */
    private int getStnVarSize(String name) {
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
    private List<Variable> makeStationVars(List<GempakStation> stations,
                                           Dimension dim) {
        int           numStations = stations.size();
        GempakStation sample      = stations.get(0);
        boolean       useSTID     = true;
        for (GempakStation station : stations) {
            if (station.getSTID().equals("")) {
                useSTID = false;
                break;
            }
        }
        List<Variable> vars        = new ArrayList<Variable>();
        List<String>   stnKeyNames = gemreader.getStationKeyNames();
        for (String varName : stnKeyNames) {
            Variable  v        = makeStationVariable(varName, dim);
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
        if (dim != null) {
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
                                (int) (stn.getSTNM() / 10));
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
    private Variable makeStationVariable(String varname, Dimension firstDim) {
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

}

