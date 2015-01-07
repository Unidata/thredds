/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
