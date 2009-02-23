/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
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


import ucar.unidata.io.RandomAccessFile;

import java.io.*;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Read a Gempak grid file
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GempakSurfaceFileReader extends GempakFileReader {

    /** Surface Text identifier */
    public static final String SFTX = "SFTX";

    /** Surface Data identifier */
    public static final String SFDT = "SFDT";

    /** Surface Data identifier */
    public static final String SFSP = "SFSP";

    /** date key identifier */
    public static final String DATE = "DATE";

    /** time key identifier */
    public static final String TIME = "TIME";

    /** standard surface file id */
    public static final String STANDARD = "standard";

    /** climate surface file id */
    public static final String CLIMATE = "climate";

    /** ship surface file id */
    public static final String SHIP = "ship";

    /** Surface obs date format */
    private static final String DATE_FORMAT = "yyMMdd'/'HHmm";

    /** date/time keys */
    private List<Key> dateTimeKeys;

    /** station keys */
    private List<Key> stationKeys;

    /** unique list of dates as Strings */
    private List<String> dateList;

    /** unique list of dates as Dates */
    private List<Date> dates;

    /** list of stations */
    private List<GempakStation> stations;

    /** list of parameters */
    private List<GempakParameter> params;

    /** list of parameters */
    private Map<String, List<GempakParameter>> partParamMap =
        new HashMap<String, List<GempakParameter>>();

    /** number of parameters */
    private int numParams = 0;

    /** flag for standard file */
    private String fileType = null;

    /** data formatter */
    private SimpleDateFormat dateFmt = new SimpleDateFormat(DATE_FORMAT);

    /**
     * Default ctor
     */
    GempakSurfaceFileReader() {}

    /**
     * Initialize the file, read in all the metadata (ala DM_OPEN)
     *
     * @param raf   RandomAccessFile to read.
     * @param fullCheck  if true, check entire structure
     *
     * @return A GempakSurfaceFileReader
     * @throws IOException   problem reading file
     */
    public static GempakSurfaceFileReader getInstance(RandomAccessFile raf,
            boolean fullCheck)
            throws IOException {
        GempakSurfaceFileReader gsfr = new GempakSurfaceFileReader();
        gsfr.init(raf, fullCheck);
        return gsfr;
    }

    /**
     * Initialize this reader.  Read all the metadata
     *
     * @return true if successful
     *
     * @throws IOException  problem reading the data
     */
    protected boolean init() throws IOException {
        return init(true);
    }

    /**
     * Initialize this reader.  Get the Grid specific info
     *
     * @param fullCheck  check to make sure there are grids we can handle
     * @return true if successful
     *
     * @throws IOException  problem reading the data
     */
    protected boolean init(boolean fullCheck) throws IOException {

        if ( !super.init(fullCheck)) {
            return false;
        }

        // Modeled after SF_OFIL
        if (dmLabel.kftype != MFSF) {
            logError("not a surface data file ");
            return false;
        }


        numParams = 0;
        String partType = ((dmLabel.kfsrce == 100) && (dmLabel.kprt == 1))
                          ? SFTX
                          : SFDT;

        DMPart part     = getPart(partType);

        if (part == null) {
            logError("No part named " + partType + " found");
            return false;
        } else {
            numParams = part.kparms;
        }

        for (DMPart apart : parts) {
            List<GempakParameter> params = makeParams(apart);
            partParamMap.put(apart.kprtnm, params);
        }

        // get the date/time keys
        dateTimeKeys = getDateTimeKeys();
        if ((dateTimeKeys == null) || dateTimeKeys.isEmpty()) {
            return false;
        }

        // get the station info
        stationKeys = getStationKeys();
        if ((stationKeys == null) || stationKeys.isEmpty()) {
            return false;
        }

        // determine file type
        String latType = findKey(GempakStation.SLAT).type;
        if ( !(findKey(DATE).type.equals(latType))) {
            if (latType.equals(ROW)) {
                fileType = CLIMATE;
            } else {
                fileType = STANDARD;
            }
        } else {
            fileType = SHIP;
        }

        dateList = getDateList(fileType != SHIP);
        stations = getStationList();

        return true;

    }

    /**
     * Get the date/time information
     *
     * @return the list of date/time keys
     */
    private List<Key> getDateTimeKeys() {
        Key date = findKey(DATE);
        Key time = findKey(TIME);
        if ((date == null) || (time == null)
                || !date.type.equals(time.type)) {
            return null;
        }
        List<Key> dt = new ArrayList<Key>(2);
        dt.add(date);
        dt.add(time);
        return dt;
    }

    /**
     * Get the list of dates
     *
     * @param unique true for unique list
     *
     * @return the list of dates
     */
    private List<String> getDateList(boolean unique) {
        Key         date = dateTimeKeys.get(0);
        Key         time = dateTimeKeys.get(1);
        List<int[]> toCheck;
        if (date.type.equals(ROW)) {
            toCheck = headers.rowHeaders;
        } else {
            toCheck = headers.colHeaders;
        }
        List<String> fileDates = new ArrayList<String>();
        for (int[] header : toCheck) {
            if (header[0] != IMISSD) {
                // convert to GEMPAK date/time
                int idate = header[date.loc + 1];
                int itime = header[time.loc + 1];
                // TODO: Add in the century
                String dateTime = GempakUtil.TI_CDTM(idate, itime);
                fileDates.add(dateTime);
            }
        }
        if (unique && !fileDates.isEmpty()) {
            SortedSet<String> uniqueTimes =
                Collections.synchronizedSortedSet(new TreeSet<String>());
            uniqueTimes.addAll(fileDates);
            fileDates.clear();
            fileDates.addAll(uniqueTimes);
        }

        return fileDates;
    }

    /**
     * Make GempakParameters from the list of
     *
     * @param part  the part to use
     *
     * @return  parameters from the info in that part.
     */
    private List<GempakParameter> makeParams(DMPart part) {
        List<GempakParameter> gemparms =
            new ArrayList<GempakParameter>(part.kparms);
        for (DMParam param : part.params) {
            String          name = param.kprmnm;
            GempakParameter parm = GempakParameters.getParameter(name);
            if (parm == null) {
                System.out.println("couldn't find " + name
                                   + " in params table");
                parm = new GempakParameter(1, name, name, "", 0);
            }
            gemparms.add(parm);
        }
        return gemparms;
    }

    /**
     * Get the list of parameters for the part
     *
     * @param partName name of the part
     * @return  list of parameters
     */
    public List<GempakParameter> getParameters(String partName) {
        return partParamMap.get(partName);
    }

    /**
     * Get the station list
     *
     * @return the list of stations
     */
    private List<GempakStation> getStationList() {
        Key slat = findKey(GempakStation.SLAT);
        if (slat == null) {
            return null;
        }
        List<int[]> toCheck;
        if (slat.type.equals(ROW)) {
            toCheck = headers.rowHeaders;
        } else {
            toCheck = headers.colHeaders;
        }
        List<GempakStation> fileStations = new ArrayList<GempakStation>();
        int                 i            = 0;
        for (int[] header : toCheck) {
            if (header[0] != IMISSD) {
                GempakStation station = makeStation(header);
                if (station != null) {
                    station.setIndex(i);
                    fileStations.add(station);
                }
            }
            i++;
        }
        return fileStations;
    }

    /**
     * Make a station from the header info
     *
     * @param header  the station header
     *
     * @return  the corresponding station
     */
    private GempakStation makeStation(int[] header) {
        if ((stationKeys == null) || stationKeys.isEmpty()) {
            return null;
        }
        GempakStation newStation = new GempakStation();
        for (Key key : stationKeys) {
            int loc = key.loc + 1;
            if (key.name.equals(GempakStation.STID)) {
                newStation.setSTID(GempakUtil.ST_ITOC(header[loc]).trim());
            } else if (key.name.equals(GempakStation.STNM)) {
                newStation.setSTNM(header[loc]);
            } else if (key.name.equals(GempakStation.SLAT)) {
                newStation.setSLAT(header[loc]);
            } else if (key.name.equals(GempakStation.SLON)) {
                newStation.setSLON(header[loc]);
            } else if (key.name.equals(GempakStation.SELV)) {
                newStation.setSELV(header[loc]);
            } else if (key.name.equals(GempakStation.SPRI)) {
                newStation.setSPRI(header[loc]);
            } else if (key.name.equals(GempakStation.STAT)) {
                newStation.setSTAT(GempakUtil.ST_ITOC(header[loc]).trim());
            } else if (key.name.equals(GempakStation.COUN)) {
                newStation.setCOUN(GempakUtil.ST_ITOC(header[loc]).trim());
            } else if (key.name.equals(GempakStation.SWFO)) {
                newStation.setSWFO(GempakUtil.ST_ITOC(header[loc]).trim());
            } else if (key.name.equals(GempakStation.WFO2)) {
                newStation.setWFO2(GempakUtil.ST_ITOC(header[loc]).trim());
            } else if (key.name.equals(GempakStation.STD2)) {
                newStation.setSTD2(GempakUtil.ST_ITOC(header[loc]).trim());
            }
        }
        return newStation;
    }

    /**
     * Get the station keys
     *
     * @return the list of station keys
     */
    private List<Key> getStationKeys() {
        Key stid = findKey(GempakStation.STID);
        Key stnm = findKey(GempakStation.STNM);
        Key slat = findKey(GempakStation.SLAT);
        Key slon = findKey(GempakStation.SLON);
        Key selv = findKey(GempakStation.SELV);
        Key stat = findKey(GempakStation.STAT);
        Key coun = findKey(GempakStation.COUN);
        Key std2 = findKey(GempakStation.STD2);
        Key spri = findKey(GempakStation.SPRI);
        Key swfo = findKey(GempakStation.SWFO);
        Key wfo2 = findKey(GempakStation.WFO2);
        if ((slat == null) || (slon == null)
                || !slat.type.equals(slon.type)) {
            return null;
        }
        String tslat = slat.type;
        // check to make sure they are all in the same set of keys
        List<Key> stKeys = new ArrayList<Key>();
        stKeys.add(slat);
        stKeys.add(slon);
        if ((stid != null) && !stid.type.equals(tslat)) {
            return null;
        } else if ((stnm != null) && !stnm.type.equals(tslat)) {
            return null;
        } else if ((selv != null) && !selv.type.equals(tslat)) {
            return null;
        } else if ((stat != null) && !stat.type.equals(tslat)) {
            return null;
        } else if ((coun != null) && !coun.type.equals(tslat)) {
            return null;
        } else if ((std2 != null) && !std2.type.equals(tslat)) {
            return null;
        } else if ((spri != null) && !spri.type.equals(tslat)) {
            return null;
        } else if ((swfo != null) && !swfo.type.equals(tslat)) {
            return null;
        } else if ((wfo2 != null) && !wfo2.type.equals(tslat)) {
            return null;
        }
        if (stid != null) {
            stKeys.add(stid);
        }
        if (stnm != null) {
            stKeys.add(stnm);
        }
        if (selv != null) {
            stKeys.add(selv);
        }
        if (stat != null) {
            stKeys.add(stat);
        }
        if (coun != null) {
            stKeys.add(coun);
        }
        if (std2 != null) {
            stKeys.add(std2);
        }
        if (spri != null) {
            stKeys.add(spri);
        }
        if (swfo != null) {
            stKeys.add(swfo);
        }
        if (wfo2 != null) {
            stKeys.add(wfo2);
        }
        return stKeys;
    }

    /**
     * Get the list of stations in this file.
     * @return list of stations.
     */
    public List<GempakStation> getStations() {
        return stations;
    }

    /**
     * Get the list of stations in this file.
     * @return list of stations.
     */
    public List<Date> getDates() {
        if ((dates == null) || (dates.isEmpty() && !dateList.isEmpty())) {
            dates = new ArrayList<Date>(dateList.size());
            for (String dateString : dateList) {
                Date d = dateFmt.parse(dateString, new ParsePosition(0));
                //DateFromString.getDateUsingSimpleDateFormat(dateString,
                //    DATE_FORMAT);
                dates.add(d);
            }
        }
        return dates;
    }

    /**
     * Print the list of dates in the file
     */
    public void printDates() {
        StringBuilder builder = new StringBuilder();
        builder.append("\nDates:\n");
        for (String date : dateList) {
            builder.append("\t");
            builder.append(date);
            builder.append("\n");
        }
        System.out.println(builder.toString());
    }

    /**
     * Print the list of dates in the file
     * @param list  true to list each station, false to list summary
     */
    public void printStations(boolean list) {
        StringBuilder builder = new StringBuilder();
        builder.append("\nStations:\n");
        if (list) {
            for (GempakStation station : stations) {
                builder.append(station);
                builder.append("\n");
            }
        } else {
            builder.append("\t");
            builder.append(getStations().size());
        }
        System.out.println(builder.toString());
    }

    /**
     * Get the type for this file
     * @return file type (CLIMATE, STANDARD, SHIP)
     */
    public String getSurfaceFileType() {
        return fileType;
    }

    /**
     * Run the program
     *
     * @param args  [0] filename (required),
     *              [1] variable name (X for default),
     *              [2] X to not list grids
     *
     * @throws IOException problem reading the file
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("need to supply a GEMPAK surface file name");
            System.exit(1);
        }
        try {
            GempakParameters.addParameters(
                "resources/nj22/tables/gempak/params.tbl");
        } catch (Exception e) {
            System.out.println("unable to init param tables");
        }

        GempakSurfaceFileReader gsfr = getInstance(getFile(args[0]), true);
        System.out.println("Type = " + gsfr.getSurfaceFileType());
        gsfr.printFileLabel();
        gsfr.printKeys();
        gsfr.printHeaders();
        gsfr.printParts();
        gsfr.printDates();
        gsfr.printStations(false);
    }

}

