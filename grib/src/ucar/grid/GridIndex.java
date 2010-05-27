/*
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


package ucar.grid;


import java.util.*;

/**
 * An "in memory" index for 2D grid files.
 *
 */
public final class GridIndex {

   /**
   * used to check versions of already created indexes.
   */
    static public final String current_index_version = "8.0";

    /**
     * Contains GridRecords
     */
    private final List<GridRecord> index = new ArrayList<GridRecord>();

    /**
     * Contains list of grid definitions (mostly projection info)
     */
    private final List<GridDefRecord> gcs = new ArrayList<GridDefRecord>();

    /**
     *
     * contains global attributes of the Index.
     */
    private final Map<String, String> atts = new HashMap<String, String>();

    /**
     * Constructor for creating an Index from the Grid file.
     * Use the addXXX() methods.
     */
    public GridIndex() {}

    /**
     * GlobalAttributes of index.
     * @return HashMap of type GlobalAttributes.
     */
    public final Map<String,String> getGlobalAttributes() {
        return atts;
    }

    /**
     * Grib records of index, one for each parameter.
     * @return list of type GridRecord.
     */
    public final List<GridRecord> getGridRecords() {
        return index;
    }

    /**
     * GridDefs of the index.
     * @return list of type GridDef.
     */
    public final List<GridDefRecord> getHorizCoordSys() {
        return gcs;
    }

    /**
     * adds a GridRecord to the index.
     * @param gr GridRecord
     */
    public final void addGridRecord(GridRecord gr) {
        index.add(gr);
    }

    /**
     * adds a GridDefRecord to the index.
     * @param gds GdsRecord
     */
    public final void addHorizCoordSys(GridDefRecord gds) {
        gcs.add(gds);
    }

    /**
     * adds a GlobalAttribute to the index.
     * @param name GlobalAttribute
     * @param value String
     */
    public final void addGlobalAttribute(String name, String value) {
        atts.put(name, value);
    }

    /**
     * Get the grid count
     * @return the count
     */
    public int getGridCount() {
        return index.size();
    }

  /**
   * compares GDS for duplicates
   *
   */
  public void finish() {
    if( gcs.size() == 1 )
      return;
    if( gcs.size() == 2 ) {
      List hcs = getHorizCoordSys();
      GridDefRecord.compare( (GridDefRecord) hcs.get(0), (GridDefRecord)hcs.get(1));
    }
  }

}

