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


package ucar.nc2.iosp.mcidas;


import ucar.ma2.*;

import ucar.nc2.*;


import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.grid.*;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;


/**
 * An IOSP for McIDAS Grid data
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class McIDASGridServiceProvider extends GridServiceProvider {

    /** McIDAS file reader */
    protected McIDASGridReader mcGridReader;

    /**
     * Is this a valid file?
     *
     * @param raf  RandomAccessFile to check
     *
     * @return true if a valid McIDAS grid file
     *
     * @throws IOException  problem reading file
     */
    public boolean isValidFile(RandomAccessFile raf) throws IOException {
        try {
            mcGridReader = new McIDASGridReader(raf);
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
        //debugProj = true;
        super.open(raf, ncfile, cancelTask);
        long start = System.currentTimeMillis();
        if (mcGridReader == null) {
            mcGridReader = new McIDASGridReader();
        }
        mcGridReader.init(raf);
        GridIndex index = ((McIDASGridReader) mcGridReader).getGridIndex();
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
        McIDASLookup lookup =
            new McIDASLookup(
                (McIDASGridRecord) index.getGridRecords().get(0));
        GridIndexToNC delegate = new GridIndexToNC();
        delegate.setUseDescriptionForVariableName(false);
        delegate.open(index, lookup, 4, ncfile, fmrcCoordSys, cancelTask);
        ncfile.finish();
    }

    /**
     * Sync and extend
     *
     * @return false
     */
    public boolean sync() {
        try {
            mcGridReader.init();
            GridIndex index =
                ((McIDASGridReader) mcGridReader).getGridIndex();
            // reconstruct the ncfile objects
            ncfile.empty();
            open(index, null);
        } catch (IOException ioe) {
            return false;
        }
        return true;
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
        return mcGridReader.readGrid((McIDASGridRecord) gr);
    }

    /**
     * Test this
     *
     * @param args filename
     *
     * @throws IOException  problem reading file
     */
    public static void main(String[] args) throws IOException {
        IOServiceProvider mciosp = new McIDASGridServiceProvider();
        RandomAccessFile  rf     = new RandomAccessFile(args[0], "r", 2048);
        NetcdfFile ncfile = new MakeNetcdfFile(mciosp, rf, args[0], null);
    }

    /**
     * Not sure why we need this
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    static class MakeNetcdfFile extends NetcdfFile {

        /**
         * wrapper for a netCDF file
         *
         * @param spi  iosp
         * @param raf  random access file
         * @param location    location of the file
         * @param cancelTask  cancel task
         *
         * @throws IOException  problem reading file
         */
        MakeNetcdfFile(IOServiceProvider spi, RandomAccessFile raf,
                       String location, CancelTask cancelTask)
                throws IOException {
            super(spi, raf, location, cancelTask);
        }
    }
}

