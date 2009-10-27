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


import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil;

import visad.util.Trace;

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
import java.util.TimeZone;
import java.util.TreeSet;


/**
 * Class to hold common methods for reading surface and sounding files
 *
 * @author IDV Development Team
 */
public abstract class AbstractGempakStationFileReader extends GempakFileReader {

    /** date key identifier */
    public static final String DATE = "DATE";

    /** time key identifier */
    public static final String TIME = "TIME";

    /** obs date format */
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

    /** data formatter */
    private SimpleDateFormat dateFmt = new SimpleDateFormat(DATE_FORMAT);

    /** The file subtype */
    protected String subType = "";

    /**
     * Default ctor
     */
    AbstractGempakStationFileReader() {}

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
        //Trace.startTrace();

        if ( !super.init(fullCheck)) {
            return false;
        }

        if ((dmLabel.kftype != MFSN) && (dmLabel.kftype != MFSF)) {
            logError("not a point data file ");
            return false;
        }
        return true;

    }

    /**
     * Read in the stations and times.  Subclasses should
     * call this during init()
     * @param uniqueTimes  make a set of unique times
     * @return true if stations and times were read okay.
     */
    protected boolean readStationsAndTimes(boolean uniqueTimes) {

        Trace.call1("GEMPAK: making params");
        for (DMPart apart : parts) {
            List<GempakParameter> params = makeParams(apart);
            partParamMap.put(apart.kprtnm, params);
        }
        Trace.call2("GEMPAK: making params");

        // get the date/time keys
        dateTimeKeys = getDateTimeKeys();
        if ((dateTimeKeys == null) || dateTimeKeys.isEmpty()) {
            return false;
        }

        // get the station info
        stationKeys = findStationKeys();
        if ((stationKeys == null) || stationKeys.isEmpty()) {
            return false;
        }
        Trace.call1("GEMPAK: get station list");
        stations = getStationList();
        Trace.call2("GEMPAK: get station list");
        makeFileSubType();

        Trace.call1("GEMPAK: get date list");
        // null out the old cached list
        dates = null;
        dateList = makeDateList(uniqueTimes);
        Trace.call2("GEMPAK: get date list");

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
    protected List<String> makeDateList(boolean unique) {
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
                //System.out.println("couldn't find " + name
                //                   + " in params table");
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
                    station.setIndex(i + 1);
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
     * Get the station key names
     *
     * @return the list of station key names
     */
    public List<String> getStationKeyNames() {
        List<String> keys = new ArrayList<String>();
        if ((stationKeys != null) && !stationKeys.isEmpty()) {
            for (Key key : stationKeys) {
                keys.add(key.name);
            }
        }
        return keys;
    }

    /**
     * Get the station keys
     *
     * @return the list of station keys
     */
    private List<Key> findStationKeys() {
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
     * Get the list of dates in this file.
     * @return list of dates.
     */
    public List<Date> getDates() {
        if ((dates == null || dates.isEmpty()) && !dateList.isEmpty()) {
            dates = new ArrayList<Date>(dateList.size());
            dateFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
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
     * Get the date string at the index
     *
     * @param index index (row or column)
     *
     * @return the date at that index
     */
    protected String getDateString(int index) {
        return dateList.get(index);
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
            for (GempakStation station : getStations()) {
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
     * Find the station index for the specified station id.
     * @param id station id (case sensitive)
     * @return index or -1 if not found.
     */
    public int findStationIndex(String id) {
        for (GempakStation station : getStations()) {
            if (station.getSTID().equals(id)) {
                return station.getIndex();
            }
        }
        return -1;
    }

    /**
     * Get the type for this file
     * @return file type
     */
    public String getFileType() {
        String type = "Unknown";
        switch (dmLabel.kftype) {

          case MFSN :
              type = "Sounding";
              break;

          case MFSF :
              type = "Surface";
              break;

          default :
        }
        if ( !subType.equals("")) {
            type = type + " (" + subType + ")";
        }
        return type;
    }

    /**
     * Subclasses need to set the subtype. This is an abstract
     * method so users will implement something to set the type.
     */
    protected abstract void makeFileSubType();

    /**
     * Get the file sub type
     * @return the subtype.
     */
    protected String getFileSubType() {
        return subType;
    }

}

