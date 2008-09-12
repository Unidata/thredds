/*
 * 
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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


import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import ucar.nc2.iosp.AbstractIOServiceProvider;

import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;


/**
 * IOServiceProvider for McIDAS AREA files
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class AreaServiceProvider extends AbstractIOServiceProvider {


    /** AREA file reader */
    protected AreaReader areaReader;

    /**
     * Is this a valid file?
     *
     * @param raf  RandomAccessFile to check
     *
     * @return true if a valid McIDAS AREA file
     *
     * @throws IOException  problem reading file
     */
    public boolean isValidFile(RandomAccessFile raf) throws IOException {
        return AreaReader.isValidFile(raf);
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
        long start = System.currentTimeMillis();
        if (areaReader == null) {
            areaReader = new AreaReader();
        }
        try {
            areaReader.init(raf, ncfile);
        } catch (Exception e) {}
        long end = System.currentTimeMillis() - start;
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
        long  start = System.currentTimeMillis();
        Array array = areaReader.readVariable(v2, section);
        long  end   = System.currentTimeMillis() - start;
        return array;
    }


    /**
     * Sync and extend
     *
     * @return false
     */
    public boolean sync() {
        return true;
    }

    // TODO: what do we do here?

    /**
     * Close this IOSP
     *
     * @throws IOException problem closing file
     */
    public void close() throws IOException {
        if (areaReader != null) {
            areaReader = null;
        }
    }

    /**
     * Test this.
     *
     * @param args [0] input file name [0] output file name
     *
     * @throws IOException  problem reading the file
     */
    public static void main(String[] args) throws IOException {
        IOServiceProvider areaiosp = new AreaServiceProvider();
        RandomAccessFile  rf     = new RandomAccessFile(args[0], "r", 2048);
        NetcdfFile ncfile = new MakeNetcdfFile(areaiosp, rf, args[0], null);
        if (args.length > 1) {
            ucar.nc2.FileWriter.writeToFile(ncfile, args[1]);
        }
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

}
