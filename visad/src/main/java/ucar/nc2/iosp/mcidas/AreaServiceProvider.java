/*
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

    public String getFileTypeId() {
      return "McIDASArea";
    }

    public String getFileTypeDescription() {
      return "McIDAS area file";
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
