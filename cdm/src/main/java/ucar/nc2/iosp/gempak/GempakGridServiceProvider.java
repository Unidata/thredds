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


package ucar.nc2.iosp.gempak;


import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dt.fmr.FmrcCoordSys;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.grid.GridDefRecord;
import ucar.nc2.iosp.grid.GridIndex;
import ucar.nc2.iosp.grid.GridIndexToNC;

import ucar.nc2.iosp.grid.GridRecord;
import ucar.nc2.iosp.grid.GridServiceProvider;
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
public class GempakGridServiceProvider extends GridServiceProvider {

    /** Gempak file reader */
    protected GempakGridReader gemreader;

    /** Reread the file on a sync */
    public static boolean extendIndex = true;  // check if index needs to be extended

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
            gemreader = new GempakGridReader();
            gemreader.init(raf, false);
        } catch (IOException ioe) {
            return false;
        }
        return true;
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
        super.open(raf, ncfile, cancelTask);
        // debugProj = true;
        long start = System.currentTimeMillis();
        if (gemreader == null) {
            gemreader = new GempakGridReader();
        }
        initTables();
        gemreader.init(raf, true);
        GridIndex index = gemreader.getGridIndex();
        open(index, cancelTask);
        if (debugOpen) {
            System.out.println(" GridServiceProvider.open "
                               + ncfile.getLocation() + " took "
                               + (System.currentTimeMillis() - start));
        }
    }

    /**
     * Open the index and create the netCDF file from that
     *
     * @param index   GridIndex to use
     * @param cancelTask  cancel task
     *
     * @throws IOException  problem reading the file
     */
    protected void open(GridIndex index, CancelTask cancelTask)
            throws IOException {
        GempakLookup lookup =
            new GempakLookup(
                (GempakGridRecord) index.getGridRecords().get(0));
        GridIndexToNC delegate = new GridIndexToNC();
        delegate.setUseDescriptionForVariableName(false);
        delegate.open(index, lookup, 4, ncfile, fmrcCoordSys, cancelTask);
        ncfile.finish();
    }

    /**
     * Sync the file
     *
     * @return  true if needed to sync
     *
     * @throws IOException problem synching the file
     */
    public boolean sync() throws IOException {
        if ((gemreader.getInitFileSize() < raf.length()) && extendIndex) {
            gemreader.init(true);
            GridIndex index = gemreader.getGridIndex();
            // reconstruct the ncfile objects
            ncfile.empty();
            open(index, null);
            return true;
        }
        return false;
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
    protected float[] _readData(GridRecord gr) throws IOException {
        return gemreader.readGrid((GempakGridRecord) gr);
    }

    /**
     * Test this.
     *
     * @param args file name
     *
     * @throws IOException  problem reading the file
     */
    public static void main(String[] args) throws IOException {
        IOServiceProvider mciosp = new GempakGridServiceProvider();
        RandomAccessFile  rf     = new RandomAccessFile(args[0], "r", 2048);
        NetcdfFile ncfile = new MakeNetcdfFile(mciosp, rf, args[0], null);
    }

    /**
     * TODO:  generalize this
     * static class for testing
     */
    protected static class MakeNetcdfFile extends NetcdfFile {

        MakeNetcdfFile(IOServiceProvider spi, RandomAccessFile raf,
                       String location, CancelTask cancelTask)
                throws IOException {
            super(spi, raf, location, cancelTask);
        }
    }

    /**
     * Initialize the parameter tables.
     *
     * @throws IOException problem reading files
     */
    private void initTables() {
        try {
            GempakParameterTable.addParameters(
                "resources/nj22/tables/gempak/wmogrib3.tbl");
            GempakParameterTable.addParameters(
                "resources/nj22/tables/gempak/ncepgrib2.tbl");
        } catch (Exception e) {
            System.out.println("unable to init tables");
        }
    }

    /**
     * Extend the list of grid
     * @param b   true to reread the grid on a sync
     */
    public static void setExtendIndex(boolean b) {
        extendIndex = b;
    }
}
