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


package ucar.nc2.iosp.gempak;


import ucar.nc2.iosp.grid.*;

import ucar.nc2.*;
import ucar.nc2.iosp.grid.GridIndexToNC;
import ucar.nc2.iosp.grid.GridServiceProvider;

import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;


/**
 * An IOSP for GEMPAK Grid data
 *
 * @author Don Murray
 */
public class GempakGridServiceProvider extends GridServiceProvider {

  /**
   * GEMPAK file reader
   */
  protected GempakGridReader gemreader;

  /**
   * Reread the file on a sync
   */
  public static boolean extendIndex = true;  // check if index needs to be extended

  /**
   * Is this a valid file?
   *
   * @param raf RandomAccessFile to check
   * @return true if a valid GEMPAK grid file
   * @throws IOException problem reading file
   */
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    try {
      gemreader = new GempakGridReader(raf.getLocation());
      return gemreader.init(raf, false);
    } catch (Exception ioe) {
      return false;
    }
  }

  public String getFileTypeId() {
    return "GempakGrid";
  }

  public String getFileTypeDescription() {
    return "GEMPAK Gridded Data";
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
    // debugProj = true;
    long start = System.currentTimeMillis();
    if (gemreader == null) {
      gemreader = new GempakGridReader(raf.getLocation());
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
   * @param index      GridIndex to use
   * @param cancelTask cancel task
   * @throws IOException problem reading the file
   */
  protected void open(GridIndex index, CancelTask cancelTask) throws IOException {
    GempakLookup lookup = new GempakLookup((GempakGridRecord) index.getGridRecords().get(0));
    GridIndexToNC delegate = new GridIndexToNC(index.filename);
    //delegate.setUseDescriptionForVariableName(false);
    delegate.open(index, lookup, 4, ncfile, cancelTask);
    ncfile.finish();
  }

  /**
   * Sync the file
   *
   * @return true if needed to sync
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
   * @param gr grid identifier
   * @return the data (or null)
   * @throws IOException problem reading the data
   */
  protected float[] _readData(GridRecord gr) throws IOException {
    return gemreader.readGrid( gr);
  }

  /**
   * Initialize the parameter tables.
   */
  private void initTables() {
    try {
      GempakGridParameterTable.addParameters(
              "resources/nj22/tables/gempak/wmogrib3.tbl");
      GempakGridParameterTable.addParameters(
              "resources/nj22/tables/gempak/ncepgrib2.tbl");
    } catch (Exception e) {
      System.out.println("unable to init tables");
    }
  }

  /**
   * Extend the list of grid
   *
   * @param b true to reread the grid on a sync
   */
  public static void setExtendIndex(boolean b) {
    extendIndex = b;
  }
}

