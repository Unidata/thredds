/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.grid;


import java.util.*;

/**
 * An "in memory" index for 2D grid files.
 * Has list of GridRecord (Parameter), GridDefRecord (GDS) and attributes.
 */
public final class GridIndex {

  /**
   * used to check versions of already created indexes.
   */
  static public final String current_index_version = "8.0";

  public final String filename;

  /**
   * Contains GridRecords
   */
  private final List<GridRecord> index = new ArrayList<GridRecord>();

  /**
   * Contains list of grid definitions (mostly projection info)
   */
  private final List<GridDefRecord> gcs = new ArrayList<GridDefRecord>();

  /**
   * contains global attributes of the Index.
   */
  private final Map<String, String> atts = new HashMap<String, String>();

  /**
   * Constructor for creating an Index from the Grid file.
   * Use the addXXX() methods.
   * @param filename name of data file, for debugging
   */
  public GridIndex(String filename) {
    this.filename = filename;
  }

  /**
   * GlobalAttributes of index.
   *
   * @return HashMap of type GlobalAttributes.
   */
  public final Map<String, String> getGlobalAttributes() {
    return atts;
  }

  /**
   * Grib records of index, one for each parameter.
   *
   * @return list of type GridRecord.
   */
  public final List<GridRecord> getGridRecords() {
    return index;
  }

  /**
   * GridDefs of the index.
   *
   * @return list of type GridDef.
   */
  public final List<GridDefRecord> getHorizCoordSys() {
    return gcs;
  }

  /**
   * adds a GridRecord to the index.
   *
   * @param gr GridRecord
   */
  public final void addGridRecord(GridRecord gr) {
    index.add(gr);
  }

  /**
   * adds a GridDefRecord to the index.
   *
   * @param gds GdsRecord
   */
  public final void addHorizCoordSys(GridDefRecord gds) {
    gcs.add(gds);
  }

  /**
   * adds a GlobalAttribute to the index.
   *
   * @param name  GlobalAttribute
   * @param value String
   */
  public final void addGlobalAttribute(String name, String value) {
    atts.put(name, value);
  }

  /**
   * Get the count of GridRecords
   *
   * @return the count of GridRecords
   */
  public int getGridCount() {
    return index.size();
  }

  /**
   * compares GDS for duplicates
   */
  public void finish() {
    if (gcs.size() == 1)
      return;
    if (gcs.size() == 2) {
      List hcs = getHorizCoordSys();
      GridDefRecord.compare((GridDefRecord) hcs.get(0), (GridDefRecord) hcs.get(1));
    }
  }

}

