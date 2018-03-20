/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.AreaFile;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import ucar.nc2.iosp.AbstractIOServiceProvider;

import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;


/**
 * IOServiceProvider for McIDAS AREA files
 *
 * @author dmurray
 */
public class AreaServiceProvider extends AbstractIOServiceProvider {
  /**
   * AREA file reader
   */
  protected AreaReader areaReader;

  /**
   * Is this a valid file?
   *
   * @param raf RandomAccessFile to check
   * @return true if a valid McIDAS AREA file
   * @throws IOException problem reading file
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
   *
   * @param raf        file to read from
   * @param ncfile     netCDF file we are writing to (memory)
   * @param cancelTask task for cancelling
   * @throws IOException problem reading file
   */
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    if (areaReader == null)
      areaReader = new AreaReader();

    try {
      areaReader.init(raf.getLocation(), ncfile);

    } catch (Throwable e) {
      close();              // try not to leak files
      throw new IOException(e);

    } finally {
      raf.close(); // avoid leaks
    }

  }

  /**
   * Read the data for the variable
   *
   * @param v2      Variable to read
   * @param section section information
   * @return Array of data
   * @throws IOException           problem reading from file
   * @throws InvalidRangeException invalid Range
   */
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    return areaReader.readVariable(v2, section);
  }

  public void close() throws IOException {
    if (areaReader != null) {
      if (areaReader.af != null)
        areaReader.af.close();
      areaReader = null;
    }
  }

  // release any resources like file handles
  public void release() throws IOException {
    if (areaReader.af != null)
      areaReader.af.close();
  }

  // reacquire any resources like file handles
  public void reacquire() throws IOException {
    try {
      areaReader.af = new AreaFile(location);
    } catch (Throwable e) {
      throw new IOException(e);
    }
  }

}
