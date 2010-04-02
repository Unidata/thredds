/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.fmrc;

import java.util.*;

/**
 * Inventory for a Forecast Model Run - one runtime.
 * Track inventory by coordinate value, not index.
 * Composed of one or more GridDatasets, each described by a GridDatasetInv.
 * For each Grid, the vert, time and ens coordinates are created as the union of the components. 
 * We make sure we are sharing coordinates across grids where they are equivilent.
 * We are thus making a rectangular array var(time, ens, level).
 * So obviously we have to tolerate missing data.
 * <p/>
 * seems to be immutable after finish() is called.
 * 
 * @author caron
 * @since Jan 11, 2010
 */
public class FmrInv implements Comparable<FmrInv> {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FmrInv.class);

  private final List<TimeCoord> timeCoords = new ArrayList<TimeCoord>(); // list of unique TimeCoord
  private final List<EnsCoord> ensCoords = new ArrayList<EnsCoord>(); // list of unique EnsCoord
  private final List<VertCoord> vertCoords = new ArrayList<VertCoord>(); // list of unique VertCoord
  private final Map<String, GridVariable> uvHash = new HashMap<String, GridVariable>(); // hash of FmrInv.Grid
  private List<GridVariable> gridList;              // sorted list of FmrInv.Grid

  public List<TimeCoord> getTimeCoords() {
    return timeCoords;
  }

  public List<EnsCoord> getEnsCoords() {
    return ensCoords;
  }

  public List<VertCoord> getVertCoords() {
    return vertCoords;
  }

  public List<GridVariable> getGrids() {
    return gridList;
  }

  public List<GridDatasetInv> getInventoryList() {
    return invList;
  }

  public Date getRunDate() {
    return runtime;
  }

  public String getName() {
    return "";
  }

  public TimeCoord findTimeCoord(String name) {
    for (TimeCoord tc : timeCoords) {
      if (tc.getName().equals(name)) return tc;
    }
    return null;
  }


  ////////////////////////////////////////////////////////////////////////////////////

  private final List<GridDatasetInv> invList = new ArrayList<GridDatasetInv>();
  private final Date runtime;

  FmrInv(Date runtime) {
    this.runtime = runtime;
  }

  void addDataset(GridDatasetInv inv, Formatter debug) {
    invList.add(inv);

    if (debug != null) {
      debug.format(" Fmr add GridDatasetInv %s = ", inv.getLocation());
      for (TimeCoord tc : inv.getTimeCoords()) {
        debug.format("  %s offsets = ", tc.getName());
        for (double off : tc.getOffsetHours())
          debug.format("%f,", off);
        debug.format("%n");
      }
    }

    // invert tc -> grid
    for (TimeCoord tc : inv.getTimeCoords()) {
      for (GridDatasetInv.Grid grid : tc.getGridInventory()) {
        GridVariable uv = uvHash.get(grid.getName());
        if (uv == null) {
          uv = new GridVariable(grid.getName());
          uvHash.put(grid.getName(), uv);
        }
        uv.addGridDatasetInv(grid);
      }
    }
  }

  // call after adding all runs
  void finish() {
    gridList = new ArrayList<GridVariable>(uvHash.values());
    Collections.sort(gridList);

    // find the common coordinates
    for (GridVariable grid : gridList) {
      grid.finish();
    }

    // assign sequence number
    int seqno = 0;
    for (TimeCoord tc : timeCoords) {
      tc.setId(seqno++);
    }
  }

  @Override
  public int compareTo(FmrInv fmr) {
    return runtime.compareTo(fmr.getRunDate());
  }

  /**
   * A grid variable for an fmr (one run)
   * A collection of GridDatasetInv.Grid, one for each seperate dataset. All have the same runDate.
   * The time and vert coord of the GridVariable is the union of the GridDatasetInv.Grid time and vert coords.
   * 
   * @author caron
   * @since Jan 12, 2010
   */
  public class GridVariable implements Comparable {
    private final String name;
    private final List<GridDatasetInv.Grid> gridList = new ArrayList<GridDatasetInv.Grid>();
    VertCoord vertCoordUnion = null; // union of vert coords
    EnsCoord ensCoordUnion = null; // union of ens coords NOT USED YET
    TimeCoord timeCoordUnion = null; // union of time coords
    TimeCoord timeExpected = null; // expected time coords
    private int countInv, countExpected;

    GridVariable(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public Date getRunDate( ) {
      return FmrInv.this.getRunDate();
    }

    void addGridDatasetInv(GridDatasetInv.Grid grid) {
      gridList.add(grid);
    }

    public List<GridDatasetInv.Grid> getInventory() { return gridList; }

    public TimeCoord getTimeExpected() { return timeExpected; }

    public TimeCoord getTimeCoord() { return timeCoordUnion; }

    public int compareTo(Object o) {
      GridVariable uv = (GridVariable) o;
      return name.compareTo(uv.name);
    }

    public int getNVerts() {
      return (vertCoordUnion == null) ? 1 : vertCoordUnion.getSize();
    }

    public int countTotal() {
      int total = 0;
      for (GridDatasetInv.Grid grid : gridList)
        total += grid.countTotal();
      return total;
    }

    void finish() {
      if (gridList.size() == 1) {
        GridDatasetInv.Grid grid = gridList.get(0);
        ensCoordUnion = EnsCoord.findEnsCoord(getEnsCoords(), grid.ec);
        vertCoordUnion = VertCoord.findVertCoord(getVertCoords(), grid.vc);
        timeCoordUnion = TimeCoord.findTimeCoord(getTimeCoords(), grid.tc);
        return;
      }

      // run over all ensCoords and construct the union
      List<EnsCoord> ensList = new ArrayList<EnsCoord>();
      EnsCoord ec_union = null;
      for (GridDatasetInv.Grid grid : gridList) {
        EnsCoord ec = grid.ec;
        if (ec == null) continue;
        if (ec_union == null)
          ec_union = new EnsCoord(ec);
        else if (!ec_union.equalsData(ec))
          ensList.add(ec);
      }
      if (ec_union != null) {
        if (ensList.size() > 0) EnsCoord.normalize(ec_union, ensList); // add the other coords
        ensCoordUnion = EnsCoord.findEnsCoord(getEnsCoords(), ec_union);  // find unique within collection
      }

      // run over all vertCoords and construct the union
      List<VertCoord> vertList = new ArrayList<VertCoord>();
      VertCoord vc_union = null;
      for (GridDatasetInv.Grid grid : gridList) {
        VertCoord vc = grid.vc;
        if (vc == null) continue;
        if (vc_union == null)
          vc_union = new VertCoord(vc);
        else if (!vc_union.equalsData(vc)) {
          System.out.printf("GridVariable %s has different vert coords in file %s %n", grid.getName(), grid.getFile());
          vertList.add(vc);
        }
      }
      if (vc_union != null) {
        if (vertList.size() > 0) VertCoord.normalize(vc_union, vertList); // add the other coords
        vertCoordUnion = VertCoord.findVertCoord(getVertCoords(), vc_union); // now find unique within collection
      }

      // run over all timeCoords and construct the union
      List<TimeCoord> timeList = new ArrayList<TimeCoord>();
      for (GridDatasetInv.Grid grid : gridList) {
        TimeCoord tc = grid.tc;
        timeList.add(tc);
      }
      // all time coordinates have the same run date
      TimeCoord tc_union = TimeCoord.makeUnion(timeList, getRunDate()); // add the other coords
      timeCoordUnion = TimeCoord.findTimeCoord(getTimeCoords(), tc_union); // now find unique within collection
    }

  }

  public Set<GridDatasetInv> getFiles() {
    HashSet<GridDatasetInv> fileSet = new HashSet<GridDatasetInv>();
    for (FmrInv.GridVariable grid :getGrids()) {
      for (GridDatasetInv.Grid inv : grid.getInventory())  {
        fileSet.add(inv.getFile());  
      }
    }
    return fileSet;
  }
}
