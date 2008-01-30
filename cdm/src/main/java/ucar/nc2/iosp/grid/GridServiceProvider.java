/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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


package ucar.nc2.iosp.grid;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.dt.fmr.FmrcCoordSys;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import java.util.List;


/**
 * An IOSP for Gempak Grid data
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public abstract class GridServiceProvider extends AbstractIOServiceProvider {

    /** FMRC coordinate system */
    protected FmrcCoordSys fmrcCoordSys;

    /** The netCDF file */
    protected NetcdfFile ncfile;

    /** the file we are reading */
    protected RandomAccessFile raf;

    /** place to store debug stuff */
    protected StringBuffer parseInfo = new StringBuffer();

    /** debug flags */
    protected static boolean debugOpen           = false,
                   debugMissing        = false,
                   debugMissingDetails = false,
                   debugProj           = false,
                   debugTiming         = false,
                   debugVert           = false;

    /** TODO: flag for whether  to add lat/lon coordinates */
    static public boolean addLatLon = false;  // add lat/lon coordinates for striuct CF compliance

    /** flag for using maximal coordinate system */
    static public boolean useMaximalCoordSys = false;

    /**
     * Set whether to use the maximal coordinate system or not
     *
     * @param b  true to use
     */
    static public void useMaximalCoordSys(boolean b) {
        useMaximalCoordSys = b;
    }

    /**
     * Set the debug flags
     *
     * @param debugFlag debug flags
     */
    static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
        debugOpen           = debugFlag.isSet("Grid/open");
        debugMissing        = debugFlag.isSet("Grid/missing");
        debugMissingDetails = debugFlag.isSet("Grid/missingDetails");
        debugProj           = debugFlag.isSet("Grid/projection");
        debugVert           = debugFlag.isSet("Grid/vertical");
        debugTiming         = debugFlag.isSet("Grid/timing");
    }

    /**
     * Open the index and create the netCDF file from that
     *
     * @param index   GridIndex to use
     * @param cancelTask  cancel task
     *
     * @throws IOException  problem reading the file
     */
    protected abstract void open(GridIndex index, CancelTask cancelTask)
     throws IOException;

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
     * Read nested data
     *
     * @param v2  Variable to read
     * @param section  section info
     *
     * @return Array of data
     *
     * @throws IOException  problem reading file
     * @throws InvalidRangeException  invalid range
    public Array readNestedData(Variable v2, List section)
            throws IOException, InvalidRangeException {
        throw new UnsupportedOperationException(
            "GEMPAK IOSP does not support nested variables");
    }
     */

    /**
     * Send an IOSP message
     *
     * @param special  isn't that special?
     */
    public Object sendIospMessage( Object special) {
        if (special instanceof FmrcCoordSys) {
            fmrcCoordSys = (FmrcCoordSys) special;
        }
        return null;
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
        long start = System.currentTimeMillis();

        Array dataArray = Array.factory(DataType.FLOAT.getClassType(),
                                        section.getShape());
        GridVariable  pv        = (GridVariable) v2.getSPobject();

        int           count     = 0;
        Range         timeRange = section.getRange(count++);
        Range         levRange  = pv.hasVert()
                                  ? section.getRange(count++)
                                  : null;
        Range         yRange    = section.getRange(count++);
        Range         xRange    = section.getRange(count);

        IndexIterator ii        = dataArray.getIndexIteratorFast();

        // loop over time
        for (int timeIdx = timeRange.first(); timeIdx <= timeRange.last();
                timeIdx += timeRange.stride()) {
            if (pv.hasVert()) {
                readLevel(v2, timeIdx, levRange, yRange, xRange, ii);
            } else {
                readXY(v2, timeIdx, 0, yRange, xRange, ii);
            }
        }

        if (debugTiming) {
            long took = System.currentTimeMillis() - start;
            System.out.println("  read data took=" + took + " msec ");
        }

        return dataArray;
    }

    // loop over level

    /**
     * Read a level
     *
     * @param v2    variable to put the data into
     * @param timeIdx  time index
     * @param levelRange level range
     * @param yRange   x range
     * @param xRange   y range
     * @param ii       index iterator
     *
     * @throws IOException   problem reading the file
     * @throws InvalidRangeException  invalid range
     */
    private void readLevel(Variable v2, int timeIdx, Range levelRange,
                           Range yRange, Range xRange, IndexIterator ii)
            throws IOException, InvalidRangeException {
        for (int levIdx = levelRange.first(); levIdx <= levelRange.last();
                levIdx += levelRange.stride()) {
            readXY(v2, timeIdx, levIdx, yRange, xRange, ii);
        }
    }


    /**
     * read one product
     *
     * @param v2    variable to put the data into
     * @param timeIdx  time index
     * @param levIdx   level index
     * @param yRange   x range
     * @param xRange   y range
     * @param ii       index iterator
     *
     * @throws IOException   problem reading the file
     * @throws InvalidRangeException  invalid range
     */
    private void readXY(Variable v2, int timeIdx, int levIdx, Range yRange,
                        Range xRange, IndexIterator ii)
            throws IOException, InvalidRangeException {
        Attribute         att           = v2.findAttribute("missing_value");
        float             missing_value = (att == null)
                                          ? -9999.0f
                                          : att.getNumericValue()
                                              .floatValue();

        GridVariable      pv            = (GridVariable) v2.getSPobject();
        GridHorizCoordSys hsys          = pv.getHorizCoordSys();
        int               nx            = hsys.getNx();

        GridRecord        record        = pv.findRecord(timeIdx, levIdx);
        if (record == null) {
            int xyCount = yRange.length() * xRange.length();
            for (int j = 0; j < xyCount; j++) {
                ii.setFloatNext(missing_value);
            }
            return;
        }

        // otherwise read it
        float[] data;
        try {
            data = _readData(record);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (int y = yRange.first(); y <= yRange.last();
                y += yRange.stride()) {
            for (int x = xRange.first(); x <= xRange.last();
                    x += xRange.stride()) {
                int index = y * nx + x;
                ii.setFloatNext(data[index]);
            }
        }
    }

    /**
     * Is this XY level missing?
     *
     * @param v2   Variable
     * @param timeIdx time index
     * @param levIdx  level index
     *
     * @return true if missing
     *
     * @throws InvalidRangeException  invalid range
     */
    private boolean isMissingXY(Variable v2, int timeIdx, int levIdx)
            throws InvalidRangeException {
        GridVariable pv = (GridVariable) v2.getSPobject();
        if (null == pv) System.out.println("HEY");
        if ((timeIdx < 0) || (timeIdx >= pv.getNTimes())) {
            throw new InvalidRangeException("timeIdx=" + timeIdx);
        }
        if ((levIdx < 0) || (levIdx >= pv.getVertNlevels())) {
            throw new InvalidRangeException("levIdx=" + levIdx);
        }
        return (null == pv.findRecord(timeIdx, levIdx));
    }

    /**
     * Read the data for this GridRecord
     *
     * @param gr   grid identifier
     *
     * @return  the data (or null)
     *
     * @throws IOException  problem reading the data
     */
    protected abstract float[] _readData(GridRecord gr) throws IOException;

}
