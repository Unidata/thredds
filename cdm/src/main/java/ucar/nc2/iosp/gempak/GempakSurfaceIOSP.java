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

import java.io.IOException;

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
        return gemreader.getSurfaceFileType().equals(gemreader.STANDARD);
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
        initTables();
        gemreader.init(raf, true);
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
        /*
        if ((gemreader.getInitFileSize() < raf.length()) && extendIndex) {
            gemreader.init(true);
            GridIndex index = gemreader.getGridIndex();
            // reconstruct the ncfile objects
            ncfile.empty();
            open(index, null);
            return true;
        }
        */
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
        System.out.println("looking for " + v2);
        System.out.println("Section = " + section);
        long  start = System.currentTimeMillis();
        Array array = null;
        if (gemreader.getSurfaceFileType().equals(gemreader.SHIP)) {
            //array = readShipData(v2, section);
        } else if (gemreader.getSurfaceFileType().equals(
                gemreader.STANDARD)) {
            array = readStandardData(v2, section);
        } else {  // climate data
            //array = readClimateData(v2, section);
        }
        //    long took = System.currentTimeMillis() - start;
        //    System.out.println("  read data took=" + took + " msec ");
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

        Array dataArray = Array.factory(DataType.FLOAT, section.getShape());
        Attribute     att           = v2.findAttribute("missing_value");
        float         missing_value = (att == null)
                                      ? -9999.0f
                                      : att.getNumericValue().floatValue();
        IndexIterator ii            = dataArray.getIndexIteratorFast();
        int           rank          = section.getRank();
        Range         stationRange  = section.getRange(0);
        Range         timeRange     = section.getRange(1);

        for (int y = stationRange.first(); y <= stationRange.last();
                y += stationRange.stride()) {
            for (int x = timeRange.first(); x <= timeRange.last();
                    x += timeRange.stride()) {
                GempakFileReader.RealData vals = gemreader.DM_RDTR(x + 1,
                                                     y + 1, "SFDT");
                if (vals == null) {
                    ii.setFloatNext(missing_value);
                } else {
                    ii.setFloatNext(vals.data[0]);
                }
            }
        }

        return dataArray;
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
     * TODO:  generalize this
     * static class for testing
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
        Dimension station = new Dimension("station", stations.size(), true);
        ncfile.addDimension(null, station);
        List<Variable> stationVars = makeStationVars(stations, station);
        // loop through and add to ncfile
        for (Variable stnVar : stationVars) {
            ncfile.addVariable(null, stnVar);
        }


        // Build variable list (var(station,time))
        // time
        List<Date> timeList = gemreader.getDates();
        int        numTimes = timeList.size();
        Dimension  times    = new Dimension("time", numTimes, true);
        ncfile.addDimension(null, times);
        Array varArray = null;
        Variable timeVar = new Variable(ncfile, null, null, "time",
                                        DataType.DOUBLE, "time");
        timeVar.addAttribute(
            new Attribute("units", "seconds since 1970-01-01 00:00:00"));
        timeVar.addAttribute(new Attribute("long_name", "time"));
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
        List<GempakParameter> params = gemreader.getParameters("SFDT");
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
        /*
        Structure sfData = makeStructure("SFDT", stationTime);
        if (sfData == null) {
            return;
        }
        sfData.addAttribute(new Attribute("coordinates",
                                          "time SLAT SLON SELV"));
        ncfile.addVariable(null, sfData);
        */
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
        return sVar;
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
        Dimension           record   = new Dimension("record", numObs, true);
        ncfile.addDimension(null, record);
        List<Dimension> records = new ArrayList(1);
        records.add(record);

        // time
        List<Date> timeList = gemreader.getDates();
        Array      varArray = null;
        Variable timeVar = new Variable(ncfile, null, null, "time",
                                        DataType.DOUBLE, "record");
        timeVar.addAttribute(
            new Attribute("units", "seconds since 1970-01-01 00:00:00"));
        timeVar.addAttribute(new Attribute("long_name", "time"));
        varArray = new ArrayDouble.D1(numObs);
        int i = 0;
        for (Date date : timeList) {
            ((ArrayDouble.D1) varArray).set(i, date.getTime() / 1000.d);
            i++;
        }
        timeVar.setCachedData(varArray, false);
        ncfile.addVariable(null, timeVar);

        List<Variable> stationVars = makeStationVars(stations, record);
        // loop through and add to ncfile
        for (Variable stnVar : stationVars) {
            ncfile.addVariable(null, stnVar);
        }

        /*
        List<GempakParameter> params = gemreader.getParameters("SFDT");
        if (params == null) return;
        for (GempakParameter param : params) {
            Variable var = makeParamVariable(param, records);
            var.addAttribute(new Attribute("coordinates", "time SLAT SLON SELV"));
            ncfile.addVariable(null, var);
        }
        */
        Structure sfData = makeStructure("SFDT", records);
        if (sfData == null) {
            return;
        }
        sfData.addAttribute(new Attribute("coordinates",
                                          "time SLAT SLON SELV"));
        ncfile.addVariable(null, sfData);
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
        ncfile.addAttribute(null, new Attribute("Conventions", "CF-1.5"));
        // dataset discovery
        /*
        ncfile.addAttribute(
            null,
            new Attribute(
                "CF:featureType",
                CF.FeatureType.stationTimeSeries.toString()));
        */
        String fileType = "GEMPAK Surface (" + gemreader.getSurfaceFileType()
                          + ")";
        ncfile.addAttribute(null, new Attribute("file_format", fileType));
        ncfile.addAttribute(null, new Attribute("history", "Direct read of "
                + fileType + " into NetCDF-Java 4.0 API at "
                + dateFormat.toDateTimeStringISO(new Date())));
    }

    /** station variable names */
    private static String[] stnVarNames = {
        GempakStation.STID, GempakStation.STNM, GempakStation.SLAT,
        GempakStation.SLON, GempakStation.SELV, GempakStation.STAT,
        GempakStation.COUN, GempakStation.STD2, GempakStation.SPRI,
        //GempakStation.SWFO, GempakStation.WFO2
    };


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
        List<Variable> vars = new ArrayList<Variable>();
        for (int i = 0; i < stnVarNames.length; i++) {
            String    varName  = stnVarNames[i];
            Variable  v        = makeStationVariable(varName, dim);
            Attribute stIDAttr = new Attribute("standard_name", "station_id");
            if (varName.equals(GempakStation.STID) && useSTID) {
                v.addAttribute(stIDAttr);
            }
            if (varName.equals(GempakStation.STNM) && !useSTID) {
                v.addAttribute(stIDAttr);
            }
            Array varArray;
            if (v.getDataType().equals(DataType.CHAR)) {
                int[] shape = v.getShape();
                //System.out.println("shape = " + shape[0] + "x" + shape[1]);
                varArray = new ArrayChar.D2(shape[0], shape[1]);
            } else {
                varArray = get1DArray(v.getDataType(), numStations);
            }
            int    index   = 0;
            String varname = v.getName();
            for (GempakStation stn : stations) {
                String test = "";
                if (varname.equals(GempakStation.STID)) {
                    test = stn.getSTID();
                } else if (varname.equals(GempakStation.STNM)) {
                    ((ArrayInt.D1) varArray).set(index, stn.getSTNM());
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
            vars.add(v);
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
        dims.add(firstDim);

        if (varname.equals(GempakStation.STID)) {
            longName = "Station identifier";
            Dimension stid_len = new Dimension("stid_len", 4);
            ncfile.addDimension(null, stid_len);
            dims.add(stid_len);
        } else if (varname.equals(GempakStation.STNM)) {
            longName = "WMO number";
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
            Dimension stat_len = new Dimension("stat_len", 2);
            ncfile.addDimension(null, stat_len);
            dims.add(stat_len);
        } else if (varname.equals(GempakStation.COUN)) {
            longName = "country code";
            Dimension coun_len = new Dimension("coun_len", 2);
            ncfile.addDimension(null, coun_len);
            dims.add(coun_len);
        } else if (varname.equals(GempakStation.STD2)) {
            longName = "Extended station id";
            Dimension std2_len = new Dimension("std2_len", 4);
            ncfile.addDimension(null, std2_len);
            dims.add(std2_len);
        } else if (varname.equals(GempakStation.SPRI)) {
            longName = "Station priority";
            type     = DataType.INT;
        } else if (varname.equals(GempakStation.SWFO)) {
            longName = "WFO code";
            Dimension wfo_len = new Dimension("wfo_len", 4);
            ncfile.addDimension(null, wfo_len);
            dims.add(wfo_len);
        } else if (varname.equals(GempakStation.WFO2)) {
            longName = "Second WFO code";
            Dimension wfo2_len = new Dimension("wfo2_len", 4);
            ncfile.addDimension(null, wfo2_len);
            dims.add(wfo2_len);
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
        v.setDimensions(dims);
        return v;
    }

}

