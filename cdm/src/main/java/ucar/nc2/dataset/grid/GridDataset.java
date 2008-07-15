// $Id: GridDataset.java,v 1.17 2006/05/25 20:15:26 caron Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
package ucar.nc2.dataset.grid;

import ucar.nc2.dataset.*;
import ucar.nc2.FileWriter;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;

import java.util.*;
import java.io.IOException;

/**
 * Make a NetcdfDataset into a collection of GeoGrids with Georeferencing coordinate systems.
 *
 * <p>
 * A variable will be made into a GeoGrid if it has a Georeferencing coordinate system,
 *   using GridCoordSys.isGridCoordSys(), and it has no extra dimensions, ie
 *   GridCoordSys.isComplete( var) is true.
 * If it has multiple Georeferencing coordinate systems, any one that is a product set will be given preference.
 *
 * Example:
 *
 * <pre>
    GridDataset gridDs = GridDataset.factory (uriString);
    List grids = gridDs.getGrids();
    for (int i=0; i&lt;grids.size(); i++) {
      GeoGrid grid = (Geogrid) grids.get(i);
    }
   </pre>
 *
 * @deprecated (use ucar.nc2.dt.grid)
 * @author caron
 * @version $Revision: 1.17 $ $Date: 2006/05/25 20:15:26 $
 */

public class GridDataset {
  private NetcdfDataset ds;
  private ArrayList gcsList = new ArrayList(); // GeoCoordSys
  private ArrayList grids = new ArrayList();  // GeoGrid
  private HashMap gridsetHash = new HashMap();

  /**
   * Open a netcdf dataset, parse Conventions, find all the geoGrids, return a GridDataset.
   *
   * @param netcdfFileURI netcdf dataset to open. May have a dods:, http: or file: prefix,
   *  or just a local filename. If it ends with ".xml", its assumed to be a NetcdfDataset Definition XML file
   * @return GridDataset
   * @throws java.io.IOException
   * @see ucar.nc2.dataset.NetcdfDataset#open
   */
  static public GridDataset open( String netcdfFileURI) throws java.io.IOException {
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDataset.acquireDataset( netcdfFileURI, null);
    return new GridDataset(ds);
  }

  /**
   * Open a netcdf dataset, parse Conventions, find all the geoGrids, return a GridDataset.
   * @deprecated : use GridDataset.open().
   */
  static public GridDataset factory( String netcdfFileURI) throws java.io.IOException {
    return open( netcdfFileURI);
    }

  /**
   * Create a GridDataset from a NetcdfDataset.
   * @param ds underlying NetcdfDataset.
   */
  public GridDataset( NetcdfDataset ds) {
    this.ds = ds;

    // look for geoGrids
    parseInfo.append("GridDataset look for GeoGrids\n");
    List vars  = ds.getVariables();
    for (int i=0; i< vars.size(); i++) {
      VariableEnhanced varDS = (VariableEnhanced) vars.get(i);
      constructCoordinateSystems( ds, varDS);
    }

  }

  private void constructCoordinateSystems(NetcdfDataset ds, VariableEnhanced v) {

      if (v instanceof StructureDS) {
        StructureDS s = (StructureDS) v;
        List members = s.getVariables();
        for (int i = 0; i < members.size(); i++) {
          VariableEnhanced nested =  (VariableEnhanced) members.get(i);
          // LOOK flatten here ??
          constructCoordinateSystems( ds, nested);
        }
      } else {

        // see if it has a GridCS
        // LOOK: should add geogrid it multiple times if there are multiple geoCS ??
        GridCoordSys gcs = null;
        List csys  = v.getCoordinateSystems();
        for (int j=0; j< csys.size(); j++) {
          CoordinateSystem cs = (CoordinateSystem) csys.get(j);
          GridCoordSys gcsTry = GridCoordSys.makeGridCoordSys( parseInfo, cs, v);
          if (gcsTry != null) {
            gcs = gcsTry;
            if (gcsTry.isProductSet()) break;
          }
        }

        if (gcs != null)
          addGeoGrid( (VariableDS) v, gcs);
        }

   }

  /** Close all resources associated with this dataset. */
  public void close() throws java.io.IOException {
    ds.close();
  }


  private void addGeoGrid( VariableDS varDS, GridCoordSys gcs) {
    Gridset gridset;
    if (null == (gridset = (Gridset) gridsetHash.get( gcs.getName()))) {
      gridset = new Gridset( gcs);
      gridsetHash.put( gcs.getName(), gridset);
      parseInfo.append(" -make new GridCoordSys= "+gcs.getName()+"\n");
      gcs.makeVerticalTransform( this, parseInfo); // delayed until now
    }

    GeoGrid geogrid = new GeoGrid( this, varDS, gridset.gcc);
    grids.add( geogrid);
    gridset.add( geogrid);
  }

    /** the name of the dataset */
  public String getName() { return ds.getLocation(); }
    /** the underlying NetcdfDataset */
  public NetcdfDataset getNetcdfDataset() { return ds; }
    /** get the list of GeoGrid objects contained in this dataset. */
  public List getGrids(){ return grids; }

  /**
   * Return GeoGrid objects grouped by GridCoordSys. All GeoGrids in a Gridset
   *   have the same GridCoordSys.
   * @return Collection of type GridDataset.Gridset
   */
  public Collection getGridSets(){ return gridsetHash.values(); }

  /** find the named GeoGrid. */
  public GeoGrid findGridByName( String name) {
    Iterator iter = getGrids().iterator();
    while (iter.hasNext()) {
      GeoGrid ggi = (GeoGrid) iter.next();
      if (name.equals( ggi.getName()))
        return ggi;
    }
    return null;
  }

    /** Show Grids and coordinate systems. */
  public String getInfo() {
    StringBuffer buf = new StringBuffer(20000);
    int countGridset = 0;
    buf.setLength(0);

    Iterator gsets = gridsetHash.values().iterator();
    while (gsets.hasNext()) {
      Gridset gs = (Gridset) gsets.next();

      buf.append("\nGridset "+ countGridset+" coordSys "+gs.getGeoCoordSys()+"\n");
      buf.append("Name___________Unit___________hasMissing_____Description\n");
      Iterator grids = gs.getGrids().iterator();
      while (grids.hasNext()) {
        GeoGrid grid = (GeoGrid) grids.next();
        buf.append(grid.getInfo());
        buf.append("\n");
      }
      countGridset++;
      buf.append("\n");
    }

    buf.append("\nGeoReferencing Coordinate Axes\n");
    buf.append(   "Name___________Len__Unit________________Type___Description\n");
    Iterator iter = ds.getCoordinateAxes().iterator();
    while (iter.hasNext()) {
      CoordinateAxis axis = (CoordinateAxis) iter.next();
      if (axis.getAxisType() == null) continue;
      buf.append( axis.getInfo());
      buf.append( "\n");
    }
    return buf.toString();
  }

  private StringBuilder parseInfo = new StringBuilder(); // debugging
  public StringBuilder getParseInfo() { return parseInfo; }

    /** Get Details about the dataset. */
  public String getDetailInfo() {
    StringBuilder buff = new StringBuilder(5000);
    buff.append( ds.toString());
    buff.append( "\n\n----------------------------------------------------\n");
    buff.append( getInfo());
    buff.append( "\n\n----------------------------------------------------\n");
    buff.append( ds.getInfo().getParseInfo());
    buff.append( "\n\n----------------------------------------------------\n");
    buff.append( parseInfo.toString());

    return buff.toString();
  }


    /** Debugging info about the dataset.
  public abstract String getDebugInfo();


    /** Iterator returns ucar.grid.CoordAxisImpl. CHANGE TO GENERIC
  public Iterator getAxes(){ return coordAxes.iterator(); }

  public String toString() { return name; }

    /// extra services
    /** iterator returns ucar.grid.GeoGridImpl.Gridset.  CHANGE TO GENERIC
  public Iterator getGridsets(){
    if (null == gridsets) makeGridsets();
    return gridsets.values().iterator();
  }

    /** find the named GeoGrid.
  public GeoGridImpl getGridByName( String name) {
    Iterator iter = getGrids();
    while (iter.hasNext()) {
      GeoGridImpl ggi = (GeoGridImpl) iter.next();
      if (name.equals( ggi.getName()))
        return ggi;
    }
    return null;
  }

    /** find the named GeoGrid.
  public GeoGridImpl getGridByStandardName( String name) {
    Iterator iter = getGrids();
    while (iter.hasNext()) {
      GeoGridImpl ggi = (GeoGridImpl) iter.next();
      thredds.catalog.StandardQuantity sq = ggi.getStandardQuantity();
      if ((sq != null) && name.equals( sq.getName()))
        return ggi;
    }
    return null;
  } */

    //// methods used by the data providers to construct the Dataset

  /** Add a GeoGrid. If g is not a GeoGridImpl, it is wrapped by a GeoGridAdapter to
   * make it into one. This allows us to assume GeoGridImpl without loss of generality.
   *
  public void addGrid( GeoGrid g) {
    if (g instanceof GeoGridImpl)
      grids.add(g);
    else
      grids.add( new GeoGridAdapter( g));
  }

    // construct the gridsets : sets of grids with the same coord.sys
  public void makeGridsets() {
    gridsets = new java.util.HashMap();
    coordAxes = new java.util.HashSet();

    Iterator iter = getGrids();
    while (iter.hasNext()) {
      GeoGridImpl ggi = (GeoGridImpl) iter.next();
      GeoCoordSysImpl gcc = ggi.getGeoCoordSysImpl();
      Gridset gset;
      if (null == (gset = (Gridset) gridsets.get( gcc))) { // problem with equals() ?
        gset = new Gridset(gcc);   // create new gridset with this coordsys
        gridsets.put( gcc, gset);
      }
      gset.add(ggi);

      // also track all coord axes: used in toString().
      if (gcc.getXaxis() != null)
        coordAxes.add( gcc.getXaxis());
      if (gcc.getYaxis() != null)
        coordAxes.add( gcc.getYaxis());
      if (gcc.getZaxis() != null)
        coordAxes.add( gcc.getZaxis());
      if (gcc.getTaxis() != null)
        coordAxes.add( gcc.getTaxis());
    }
  } */



  /**
   * This is a set of GeoGrids with the same GeoCoordSys.
   */
  public class Gridset {

    private GridCoordSys gcc;
    private ArrayList grids = new ArrayList();

    private Gridset ( GridCoordSys gcc) { this.gcc = gcc; }
    private void add( GeoGrid grid) { grids.add( grid); }

    /** Get list of GeoGrid objects */
    public List getGrids() { return grids; }

    /** all GeoGrids point to this GeoCoordSysImpl */
    public GridCoordSys getGeoCoordSys() { return gcc; }
  }

  /** testing */
  public static void main( String arg[]) {
    String defaultFilename = "R:/testdata/grid/netcdf/cf/mississippi.nc";
    String filename = (arg.length > 0) ? arg[0] : defaultFilename;
    try {
      GridDataset gridDs = GridDataset.open (filename);
      //System.out.println(gridDs.getDetailInfo());

      String outFilename = "C:/data/writeGrid.nc";
      GeoGrid gg = gridDs.findGridByName("cape_sfc");
      gg.writeFile(outFilename);

      gridDs = GridDataset.open (outFilename);
      System.out.println(gridDs.getDetailInfo());
    } catch (Exception ioe) {
      ioe.printStackTrace();
    }
  }

}