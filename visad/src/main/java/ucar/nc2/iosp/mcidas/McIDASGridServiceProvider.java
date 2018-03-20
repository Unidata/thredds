/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.mcidas;

import ucar.nc2.*;

import ucar.nc2.iosp.grid.*;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;


/**
 * An IOSP for McIDAS Grid data
 *
 * @author dmurray
 */
public class McIDASGridServiceProvider extends GridServiceProvider {

  /**
   * McIDAS file reader
   */
  protected McIDASGridReader mcGridReader;

  /**
   * Is this a valid file?
   *
   * @param raf RandomAccessFile to check
   * @return true if a valid McIDAS grid file
   * @throws IOException problem reading file
   */
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    mcGridReader = new McIDASGridReader();
    return mcGridReader.init(raf, false);
  }

  /**
   * Get the file type id
   *
   * @return the file type id
   */
  public String getFileTypeId() {
    return "McIDASGrid";
  }

  /**
   * Get the file type description
   *
   * @return the file type description
   */
  public String getFileTypeDescription() {
    return "McIDAS Grid file";
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
    //debugProj = true;
    super.open(raf, ncfile, cancelTask);
    long start = System.currentTimeMillis();
    if (mcGridReader == null) {
      mcGridReader = new McIDASGridReader();
    }
    mcGridReader.init(raf);
    GridIndex index = mcGridReader.getGridIndex();
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
  protected void open(GridIndex index, CancelTask cancelTask)
          throws IOException {
    McIDASLookup lookup = new McIDASLookup((McIDASGridRecord) index.getGridRecords().get(0));
    GridIndexToNC delegate = new GridIndexToNC(index.filename);
    //delegate.setUseDescriptionForVariableName(false);
    delegate.open(index, lookup, 4, ncfile, cancelTask);
    ncfile.finish();
  }

  /**
   * Sync and extend
   *
   * @return false
   */
  public boolean sync() {
    try {
      if (!mcGridReader.init()) {
        return false;
      }
      GridIndex index = mcGridReader.getGridIndex();
      // reconstruct the ncfile objects
      ncfile.empty();
      open(index, null);
      return true;

    } catch (IOException ioe) {
      return false;
    }
  }

  /**
   * Read the data for this GridRecord
   *
   * @param gr grid identifier
   * @return the data (or null)
   * @throws IOException problem reading the data
   */
  protected float[] _readData(GridRecord gr) throws IOException {
    return mcGridReader.readGrid((McIDASGridRecord) gr);
  }

}

