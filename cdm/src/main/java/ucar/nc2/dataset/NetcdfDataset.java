// $Id:NetcdfDataset.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.NetcdfFileCache;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.ncml.NcMLGWriter;

// factories for remote access
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * NetcdfDataset extends the netCDF API, adding standard attribute parsing such as
 * scale and offset, and explicit support for Coordinate Systems.
 * A NetcdfDataset either wraps a NetcdfFile, or is defined by an NcML document.
 *
 * <p> Be sure to close the dataset when done, best practice is to enclose in a try/finally block:
  <pre>
    NetcdfDataset ncd = null;
    try {
        ncd = NetcdfDataset.openDataset(fileName);
        ...
    } finally {
        ncd.close();
    }
  </pre>
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 * @see ucar.nc2.NetcdfFile
 */

/* Implementation notes.
 *  1) NetcdfDataset wraps a NetcdfFile.
       orgFile = NetcdfFile
       variables are wrapped by VariableDS, but are not reparented. VariableDS uses original variable for read.
       Groups get reparented.
    2) NcML standard
       NcML location is read in as the NetcdfDataset, then modified by the NcML
       orgFile = null
    3) NcML explicit
       NcML location is read in, then transfered to new NetcdfDataset as needed
       orgFile = file defined by NcML location
    4) NcML new
       NcML location = null
       orgFile = null
       NetcdfDataset defined only by NcML, data is set to FillValue unless explicitly defined
 */

public class NetcdfDataset extends ucar.nc2.NetcdfFile {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfDataset.class);
  static protected boolean useNaNs = true;
  static protected boolean fillValueIsMissing = true, invalidDataIsMissing = true, missingDataIsMissing = true;

  // modes

  /**
   * Set whether to use NaNs for missing values, for efficiency
   */
  static public void setUseNaNs(boolean b) {
    useNaNs = b;
  }

  /**
   * Get whether to use NaNs for missing values, for efficiency
   */
  static public boolean getUseNaNs() {
    return useNaNs;
  }

  /**
   * Set if _FillValue attribute is considered isMissing()
   */
  static public void setFillValueIsMissing(boolean b) {
    fillValueIsMissing = b;
  }

  /**
   * Get if _FillValue attribute is considered isMissing()
   */
  static public boolean getFillValueIsMissing() {
    return fillValueIsMissing;
  }

  /**
   * Set if valid_range attribute is considered isMissing()
   */
  static public void setInvalidDataIsMissing(boolean b) {
    invalidDataIsMissing = b;
  }

   /**
   * Get if valid_range attribute is considered isMissing()
   */
   static public boolean getInvalidDataIsMissing() {
    return invalidDataIsMissing;
  }

  /**
   * Set if missing_data attribute is considered isMissing()
   */
  static public void setMissingDataIsMissing(boolean b) {
    missingDataIsMissing = b;
  }

  /**
   * Get if missing_data attribute is considered isMissing()
   */
  static public boolean getMissingDataIsMissing() {
    return missingDataIsMissing;
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   * @return NetcdfDataset object
   * @see #openDataset(String location, boolean enhance,  ucar.nc2.util.CancelTask cancelTask)
   */
  static public NetcdfDataset openDataset(String location) throws IOException {
    return openDataset(location, true, null);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location   location of file
   * @param enhance    if true, process scale/offset/missing and add Coordinate Systems
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   */
  static public NetcdfDataset openDataset(String location, boolean enhance, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return openDataset(location, enhance, -1, cancelTask, null);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location   location of file
   * @param enhance    if true, process scale/offset/missing and add Coordinate Systems
   * @param  buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param  cancelTask allow task to be cancelled; may be null.
   * @param  spiObject sent to iosp.setSpecial() if not null
   * @return NetcdfDataset object
   */
  static public NetcdfDataset openDataset(String location, boolean enhance, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    NetcdfFile ncfile = openFile(location, buffer_size, cancelTask, spiObject);
    NetcdfDataset ds;
    if (ncfile instanceof NetcdfDataset) {
      ds = (NetcdfDataset) ncfile;
      if (enhance) enhance( ds, cancelTask);
    } else {
      ds = new NetcdfDataset(ncfile, enhance);
    }

    return ds;
  }

  static private void enhance(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    List vars = ds.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      if (v instanceof VariableDS) // LOOK : should StructureDS get enhanced ???
       ((VariableDS)v).enhance();
    }
    ucar.nc2.dataset.CoordSysBuilder.addCoordinateSystems(ds, cancelTask);
    ds.finish(); // recalc the global lists
    ds.isEnhanced = true;
  }

  /**
   * Same as openDataset, but file is acquired through the NetcdfFileCache, and its always enhanced.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   *
   * @see #openDataset for meaning of parameters
   */
  static public NetcdfDataset acquireDataset(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    NetcdfFile ncfile = acquireFile(location, cancelTask);
    NetcdfDataset ds;
    if (ncfile instanceof NetcdfDataset) {
      ds = (NetcdfDataset) ncfile;
      enhance( ds, cancelTask);
    } else {
      ds = new NetcdfDataset(ncfile, true);
    }

    return ds;
  }

  /**
   * Same as openFile, but file is acquired through the NetcdfFileCache.
   * <ol>
   * <li> Regular NetcDF file is acquired through NetcdfFileCache
   * <li> HTTP Netcdf file is acquired through NetcdfFileCache
   * <li> DODS file is acquired through NetcdfFileCache with factory
   * <li> NcML file is acquired through NetcdfFileCache with factory, underlying file is not acquired
   * </ol>
   * @see #openFile
   */
  static public NetcdfFile acquireFile(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    if (location == null)
      throw new IOException("NetcdfDataset.openFile: location is null");

    NetcdfFile ncfile;
    location = location.trim();
    location = StringUtil.replace(location, '\\', "/");

    if (location.startsWith("dods:")) {
      // open through DODS
      ncfile = acquireDODS(location, cancelTask);

    } else if (location.endsWith(".xml") || location.endsWith(".ncml")) { //open as a NetcdfDataset through NcML
      if (!location.startsWith("http:") && !location.startsWith("file:"))
        location = "file:" + location;

      // note that the NcML dataset is not acquired, but the underlying dataset
      // return NcMLReader.readNcML(location, cancelTask);

      // acquire as a NcML file
      ncfile = NetcdfFileCache.acquire(location, -1, cancelTask, null, new NetcdfFileFactory() {
        public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
          return NcMLReader.readNcML(location, cancelTask);
        }
      });

    } else if (location.startsWith("http:")) {

      if (isDODS(location)) {
        ncfile = acquireDODS(location, cancelTask); // try as a dods file
      } else {
        ncfile = NetcdfFileCache.acquire(location, cancelTask); // acquire as an http netcdf3 file
      }

    } else {

      // try it as a NetcdfFile; this handles various local file formats
      ncfile = NetcdfFileCache.acquire(location, cancelTask);
    }

    return ncfile;
  }

  /**
    * Factory method for opening a NetcdfFile through the netCDF API.
    *
    * @param location location of dataset.
    * @param cancelTask use to allow task to be cancelled; may be null.
    * @return NetcdfFile object
    * @throws IOException
    */
   public static NetcdfFile openFile(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
     return openFile(location, -1, cancelTask, null);
   }

  /**
   * Factory method for opening a NetcdfFile through the netCDF API. May be any kind of file that
   * can be read through the netCDF API, including OpenDAP and NcML.
   * <p/>
   * <p> This does not necessarily turn it into a NetcdfDataset (it may), use NetcdfDataset.open()
   * method for that. It definitely does not add coordinate systems
   *
   * @param location location of dataset. This may be a
   *  <ol>
   *    <li>local filename (with a file: prefix or no prefix) for netCDF (version 3), hdf5 files, or any file type
   *        registered with NetcdfFile.registerIOProvider().
   *    <li>OpenDAP dataset URL (with a dods: or http: prefix).
   *    <li>NcML file or URL if the location ends with ".xml" or ".ncml"
   *    <li>NetCDF file through an HTTP server (http: prefix)
   *    <li>thredds dataset, see ThreddsDataFactory.openDataset(String location, ...));
   *  </ol>
   * @param  buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param  cancelTask allow task to be cancelled; may be null.
   * @param  spiObject sent to iosp.setSpecial() if not null
   * @return NetcdfFile object
   * @throws IOException
   */
  public static NetcdfFile openFile(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

     if (location == null)
      throw new IOException("NetcdfDataset.openFile: location is null");

    NetcdfFile ncfile;
    location = location.trim();
    location = StringUtil.replace(location, '\\', "/");

    if (location.startsWith("dods:")) {
      // open through DODS
      ncfile = openDODS(location, cancelTask);

    } else if (location.startsWith("thredds:")) {
      StringBuffer log = new StringBuffer();
      ThreddsDataFactory tdf = new ThreddsDataFactory();
      ncfile = tdf.openDataset(location, false, cancelTask, log); // dont acquire
      if (ncfile == null)
        throw new IOException(log.toString());

    } else if (location.endsWith(".xml") || location.endsWith(".ncml")) { //open as a NetcdfDataset through NcML
      if (!location.startsWith("http:") && !location.startsWith("file:"))
        location = "file:" + location;
      return NcMLReader.readNcML(location, cancelTask);

    } else if (location.startsWith("http:")) {

      if (isDODS(location)) {
        ncfile = openDODS(location, cancelTask); // try as a dods file
      } else {
        ncfile = NetcdfFile.open(location, buffer_size, cancelTask, spiObject); // try as an http netcdf3 file
      }

    } else {

      // try it as a NetcdfFile; this handles various local file formats
      ncfile = NetcdfFile.open(location, buffer_size, cancelTask, spiObject);
    }

    return ncfile;
  }

  /**
   * check for existence before essence
   */
  static private boolean isDODS(String location) throws IOException {
    try {
      URL u = new URL(location + ".dds");
      HttpURLConnection conn = (HttpURLConnection) u.openConnection();
      conn.setRequestMethod("HEAD");
      int code = conn.getResponseCode();

      return (code == 200);

    } catch (Exception e) {
      throw new IOException(location + " is not a valid URL." + e.getMessage());
    }

  }

  static private NetcdfFile acquireDODS(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return NetcdfFileCache.acquire(location, -1, cancelTask, null, new NetcdfFileFactory() {
      public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
        return openDODS(location, cancelTask);
      }
    });
  }

  // // try to open as a dods file
  static private NetcdfFile openDODS(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    String dodsUri = null;
    try {
      dodsUri = DODSNetcdfFile.canonicalURL(location);
      return new DODSNetcdfFile(dodsUri, cancelTask);
    } catch (IOException e) {
      throw new FileNotFoundException("Cant open " + location + " or as DODS " + dodsUri + "\n" + e.getMessage());
    }
  }

  /**
   * Retrieve a "standard" Variable with the specified name, that handles scale, offset, etc.
   * This is for backwards compatibility with 2.1,
   * used in ucar.unidata.geoloc.vertical
   *
   * @param name String which //identifies the desired variable
   * @return the VariableStandardized, or null if not found
   */
  public Variable findStandardVariable(String name) {
    return findVariable(name); // all VariableDS handle scale/offset
  }

  ////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////
  private NetcdfFile orgFile = null;
  private boolean isEnhanced;

  private ArrayList coordSys = new ArrayList(); // type CoordinateSystem
  private ArrayList coordAxes = new ArrayList(); // type CoordinateSystem
  private ArrayList coordTransforms = new ArrayList(); // type CoordinateTransform
  private boolean coordSysWereAdded = false;

  // If its an aggregation
  private ucar.nc2.ncml.Aggregation agg = null; // used to close underlying files

  /** If its an NcML aggregation, it has an Aggregation object associated.
   *  This is public for use by NcmlWriter.
   **/
  public ucar.nc2.ncml.Aggregation getAggregation() { return agg; }

  /** Set the Aggregation object associated with this NcML dataset */
  public void setAggregation(ucar.nc2.ncml.Aggregation agg) {
    this.agg = agg;
  }

  /**
   * Get the list of all CoordinateSystem objects used by this dataset.
   *
   * @return list of type CoordinateSystem; may be empty, not null.
   */
  public List getCoordinateSystems() {
    return coordSys;
  }

  /**
   * Get whether the dataset was enhanced.
   */
  public boolean isEnhanced() {
    return isEnhanced;
  }

  /**
   * Get the list of all CoordinateTransform objects used by this dataset.
   *
   * @return list of type CoordinateTransform; may be empty, not null.
   */
  public List getCoordinateTransforms() {
    return coordTransforms;
  }

  /**
   * Get the list of all CoordinateAxis objects used by this dataset.
   *
   * @return list of type CoordinateAxis; may be empty, not null.
   */
  public List getCoordinateAxes() {
    return coordAxes;
  }

  /**
   * Has Coordinate System metadata been added.
   */
  public boolean getCoordSysWereAdded() {
    return coordSysWereAdded;
  }

  /**
   * Set whether Coordinate System metadata has been added.
   */
  public void setCoordSysWereAdded(boolean coordSysWereAdded) {
    this.coordSysWereAdded = coordSysWereAdded;
  }

  /**
   * Retrieve the CoordinateAxis with the specified name.
   *
   * @param fullName full name of the coordinate axis
   * @return the CoordinateAxis, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(String fullName) {
    if (fullName == null) return null;
    for (int i = 0; i < coordAxes.size(); i++) {
      CoordinateAxis v = (CoordinateAxis) coordAxes.get(i);
      if (fullName.equals(v.getName()))
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateSystem with the specified name.
   *
   * @param name String which identifies the desired CoordinateSystem
   * @return the CoordinateSystem, or null if not found
   */
  public CoordinateSystem findCoordinateSystem(String name) {
    if (name == null) return null;
    for (int i = 0; i < coordSys.size(); i++) {
      CoordinateSystem v = (CoordinateSystem) coordSys.get(i);
      if (name.equals(v.getName()))
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateTransform with the specified name.
   *
   * @param name String which identifies the desired CoordinateSystem
   * @return the CoordinateSystem, or null if not found
   */
  public CoordinateTransform findCoordinateTransform(String name) {
    if (name == null) return null;
    for (int i = 0; i < coordTransforms.size(); i++) {
      CoordinateTransform v = (CoordinateTransform) coordTransforms.get(i);
      if (name.equals(v.getName()))
        return v;
    }
    return null;
  }

     /** Used by NetcdfDatasetCache. */
  protected void setCacheState(int cacheState) { super.setCacheState( cacheState); }

   /** Used by NetcdfDatasetCache.  */
  protected void setCacheName(String cacheName) { super.setCacheName( cacheName); }

  /**
   * Close all resources (files, sockets, etc) associated with this dataset.
   * If the underlying file was acquired, it will be released, otherwise closed.
   */
  public synchronized void close() throws java.io.IOException {
    if (getCacheState() == 3)
      return;
    else if (getCacheState() == 2)
      NetcdfDatasetCache.release(this);
    else if (getCacheState() == 1) {
      if (agg != null) agg.persist();
      NetcdfFileCache.release(this);
    } else {
      if (isClosed) return;
      if (agg != null) agg.close();
      agg = null;
      if (orgFile != null) orgFile.close();
      orgFile = null;
      isClosed = true;
    }

  }

  /** Check if file has changed, and reread metadata if needed.
   *  All previous object references (variables, dimensions, etc) may become invalid - you must re-obtain.
   * @return true if file was changed.
   * @throws IOException
   */
  public boolean sync() throws IOException {
    if (agg != null)
      return agg.sync();

    if (orgFile != null)
      return orgFile.sync();

    return false;
  }

  public boolean syncExtend() throws IOException {
    if (agg != null)
      return agg.syncExtend();

    // synch orgFile if it has an unlimited dimension
    if (orgFile != null) {
      boolean wasExtended = orgFile.syncExtend();

      // propagate changes. LOOK rather ad-hoc
      if (wasExtended)  {
        Dimension ndim = orgFile.getUnlimitedDimension();
        int newLength = ndim.getLength();

        Dimension udim = getUnlimitedDimension();
        udim.setLength(newLength);

        List vars = getVariables();
        for (Iterator i = vars.iterator(); i.hasNext();) {
          Variable v = (Variable) i.next();
          if (v.isUnlimited()) // set it in all of the record variables
            v.setDimensions(v.getDimensions());
        }
        return true;
      }
    }

    return false;
  }

  /**
   * Write the NcML representation.
   *
   * @param os  write to this Output Stream.
   * @param uri use this for the uri attribute; if null use getLocation().
   * @throws IOException
   */
  public void writeNcML(java.io.OutputStream os, String uri) throws IOException {
    new NcMLWriter().writeXML(this, os, uri);
  }

  /**
   * Write the NcML-G representation.
   *
   * @param os         write to this Output Stream.
   * @param showCoords
   * @param uri        use this for the uri attribute; if null use getLocation().
   * @throws IOException
   */
  public void writeNcMLG(java.io.OutputStream os, boolean showCoords, String uri) throws IOException {
    new NcMLGWriter().writeXML(this, os, showCoords, uri);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Transform a NetcdfFile into a NetcdfDataset.
   * You must not use the underlying NetcdfFile after this call.
   *
   * @param ncfile NetcdfFile to transform.
   */
  public NetcdfDataset(NetcdfFile ncfile) throws IOException {
    this(ncfile, true);
  }

  /**
   * Transform a NetcdfFile into a NetcdfDataset, optionally add Coordinates.
   * You must not use the underlying NetcdfFile after this call.
   *
   * @param ncfile  NetcdfFile to transform.
   * @param enhance if true, process scale/offset/missing and add Coordinate Systems
   */
  public NetcdfDataset(NetcdfFile ncfile, boolean enhance) throws IOException {
    super(ncfile);
    this.isEnhanced = enhance;

    this.orgFile = ncfile;
    convertGroup(getRootGroup(), ncfile.getRootGroup());
    finish(); // build global lists

    if (enhance) {
      ucar.nc2.dataset.CoordSysBuilder.addCoordinateSystems(this, null);
      finish(); // rebuild global lists
    }
  }

  private void convertGroup(Group g, Group from) {
    List dims = from.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension d = (Dimension) dims.get(i);
      g.addDimension(d);
    }

    List atts = from.getAttributes();
    for (int i = 0; i < atts.size(); i++) {
      Attribute a = (Attribute) atts.get(i);
      g.addAttribute(a);
    }

    List vars = from.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      g.addVariable(convertVariable(g, v));
    }

    List groups = from.getGroups();
    for (int i = 0; i < groups.size(); i++) {
      Group nested = (Group) groups.get(i);
      Group nnested = new Group(this, g, nested.getShortName());
      g.addGroup(nnested);
      convertGroup(nnested, nested);
    }
  }

  private Variable convertVariable(Group g, Variable v) {
    Variable newVar;
    if (v instanceof Structure) {
      newVar = new StructureDS(g, (Structure) v, false);
      convertStructure(g, (Structure) newVar);
    } else {
      newVar = new VariableDS(g, v, this.isEnhanced);
    }
    return newVar;
  }

  // take all the members in newStructure, and wrap them in a new VariableDS or StructureDS
  // We do this when wrapping a netcdfFile, or when adding the record Structure.
  private void convertStructure(Group g, Structure newStructure) {
    Variable newVar;
    ArrayList newList = new ArrayList();
    for (Iterator iter = newStructure.getVariables().iterator(); iter.hasNext();) {
      Variable v = (Variable) iter.next();

      if (v instanceof Structure) {
        newVar = new StructureDS(g, (Structure) v, false);
        convertStructure(g, (Structure) newVar);
      } else
        newVar = new VariableDS(g, v, this.isEnhanced);

      newList.add(newVar);
    }

    replaceStructureMembers(newStructure, newList);
  }

  //////////////////////////////////////

  public boolean addRecordStructure() {
    if (addedRecordStructure)
      return false;

    if (this.orgFile == null) {
      // this is the case where a NcML file has no underlying orgFile (eg ncgen mode)
      return false; 
    }

    if (null != this.orgFile.getRootGroup().findVariable("record"))
      return false;

    boolean didit = this.orgFile.addRecordStructure();
    if (didit) {
      Group root = getRootGroup();
      Structure record = (Structure) this.orgFile.findVariable("record");
      if (record == null) {
        log.error("record not added to "+getLocation());
        return false;
      }
      // LOOK: should copy attributes from variableDS to the member variables NOW
      Structure recordDS = new StructureDS(root, record, false);
      convertStructure(root, recordDS);
      root.removeVariable("record");
      root.addVariable(recordDS);
      finish();
      addedRecordStructure = true;
    }

    return didit;
  }

  /** CDL-formatted string representation
   public String toString() {
   ByteArrayOutputStream ba = new ByteArrayOutputStream(1000);
   PrintStream out = new PrintStream( ba);
   toStringStart(out);

   out.print(" data:\n");

   // loop over variables
   Iterator iterVar = getVariables().iterator();
   while (iterVar.hasNext()) {
   Variable v = (Variable) iterVar.next();
   if (!(v instanceof Variable)) continue;
   Variable vds = (Variable) v;
   if (!vds.isMetadata()) continue;
   // only the ones with metadata

   out.print("    "+v.getName()+" = ");
   if (vds.isRegular()) {
   out.print(vds.getStart()+" start  "+vds.getIncrement()+" increment  "+vds.getSize()+" npts");
   } else {
   Array a = null;
   try {
   a = v.read();
   } catch (IOException ioe) {
   continue;
   }

   if (a instanceof ArrayChar) {
   //strings
   ArrayChar dataC = (ArrayChar) a;
   out.print( "\""+dataC.getString(0)+"\"");
   for (int i=1; i<dataC.getShape()[0]; i++) {
   out.print(", ");
   out.print( "\""+dataC.getString(i)+"\"");
   }
   } else {
   // numbers
   boolean isRealType = (v.getElementType() == double.class) || (v.getElementType() == float.class);
   IndexIterator iter = a.getIndexIterator();
   out.print(isRealType ? iter.getDoubleNext() : iter.getIntNext());
   while (iter.hasNext()) {
   out.print(", ");
   out.print(isRealType ? iter.getDoubleNext() : iter.getIntNext());
   }
   }
   }
   out.print(";\n");
   }

   toStringEnd(out);
   out.flush();
   return ba.toString();
   } */

  //////////////////////////////////////////////////////////////////////////////////////

  /* protected void writeNcMLVariable( Variable v, PrintStream out, IndentLevel indent, boolean showCoords) throws IOException {
    String elemName = (v instanceof CoordinateAxis) ? "coordinateAxis" : "variable";

    out.print(indent);
    out.print("<"+elemName+" name='"+quote(v.getShortName())+"' type='"+ v.getDataType()+"'");

    // any dimensions (scalers must skip this attribute) ?
    if (v.getRank() > 0) {
      writeNcMLDimension( v, out);
    }

    // any coordinate systems ?
    java.util.List coordSys = v.getCoordinateSystems();
      if (coordSys.size() > 0) {
        out.print("' coordSys='");

        for (int i=0; i<coordSys.size(); i++) {
          CoordinateAxis axis = (CoordinateAxis) coordSys.get(i);
          if (i > 0) out.print(" ");
          out.print(axis.getName());
        }
        out.print("'");
      }

      indent.incr();

      boolean closed = false;

      // any attributes ?
      java.util.List atts = v.getAttributes();
      if (atts.size() > 0) {
        if (!closed) {
           out.print(" >\n");
           closed = true;
        }
        Iterator iter = atts.iterator();
        while (iter.hasNext()) {
          Attribute att = (Attribute) iter.next();
          writeNcMLAtt(att, out, indent);
        }
      }

      // print data ?
      if ((showCoords && v.isCoordinateVariable())) {
        if (!closed) {
           out.print(" >\n");
           closed = true;
        }
        writeNcMLValues(v, out, indent);
      }

      indent.decr();

      // close variable element
      if (!closed)
        out.print(" />\n");
      else {
        out.print(indent);
        out.print("</"+elemName+">\n");
      }

  } */


  /**
   * Sort Variables, CoordAxes by name.
   */
  public void sort() {
    Collections.sort(variables, new VariableComparator());
    Collections.sort(coordAxes, new VariableComparator());
  }

  // sort by coord sys, then name
  private class VariableComparator implements java.util.Comparator {
    public int compare(Object o1, Object o2) {
      VariableEnhanced v1 = (VariableEnhanced) o1;
      VariableEnhanced v2 = (VariableEnhanced) o2;

      List list1 = v1.getCoordinateSystems();
      String cs1 = (list1.size() > 0) ? ((CoordinateSystem) list1.get(0)).getName() : "";
      List list2 = v2.getCoordinateSystems();
      String cs2 = (list2.size() > 0) ? ((CoordinateSystem) list2.get(0)).getName() : "";

      if (cs2.equals(cs1))
        return v1.getName().compareToIgnoreCase(v2.getName());
      else
        return cs1.compareToIgnoreCase(cs2);
    }

    public boolean equals(Object obj) {
      return (this == obj);
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  // used by NcMLReader for NcML without a referenced dataset

  /** No-arg Constructor */
  public NetcdfDataset() {
  }

  /**
   * Often a NetcdfDataset wraps a NetcdfFile.
   * For debugging
   *
   * @return underlying file, or null if none.
   */
  public NetcdfFile getReferencedFile() {
    return orgFile;
  }

  /**
   * Set underlying file. CAUTION - normally only done through the constructor.
   */
  public void setReferencedFile(NetcdfFile ncfile) {
    orgFile = ncfile;
  }

  /* public void setReferencedDatasetUri( String referencedDatasetUri) {
    this.referencedDatasetUri = referencedDatasetUri;
  } */
  /* void setReferencedDataset( NetcdfDataset refds) {
    this.referencedDataset = refds;
    this.useReferencedDataset = true;
  } */

  //String getAggDimensionName( ) { return aggDimName; }
  //void setAggDimensionName( String aggDimName) { this.aggDimName = aggDimName; }
  //String getAggDimensionValue( ) { return aggDimValue; }
  //void setAggDimensionValue( String aggDimValue) { this.aggDimValue = aggDimValue; }

  /* NetcdfDataset openReferencedDataset(CancelTask cancelTask) throws IOException, java.net.MalformedURLException {
    if (referencedDataset == null)
      referencedDataset = NetcdfDataset.openDataset( referencedDatasetUri, false, cancelTask);
    return referencedDataset;
  }

  Variable findReferencedVariable(VariableDS vds) {
    if (vds.referencedVariable == null)
      vds.referencedVariable = referencedDataset.findVariable( vds.getName());
    if (vds.referencedVariable == null)
      throw new IllegalStateException("NcML referenced Variable is Missing="+vds.getName());
    return vds.referencedVariable;
  }

  // if NcML, send I/O to referencedDataset, else to superclass
  public Array readData(ucar.nc2.Variable v, List section) throws IOException, InvalidRangeException  {
    if (useReferencedDataset) {
      openReferencedDataset(null);
      Variable referV = findReferencedVariable((VariableDS) v);
      return referencedDataset.readData( referV, section);
    } else {
      return orgFile.readData(v, section);
    }
  }

    public Array readMemberData(ucar.nc2.Variable v, List section, boolean flatten) throws IOException, InvalidRangeException  {
    if (useReferencedDataset) {
      openReferencedDataset(null);
      Variable referV = findReferencedVariable((VariableDS) v);
      return referencedDataset.readMemberData(referV, section, flatten);
    } else {
      return orgFile.readMemberData(v, section, flatten);
    }
  } */

  public Array readMemberData(ucar.nc2.Variable v, List section, boolean flatten) throws IOException, InvalidRangeException {
    return orgFile.readMemberData(v, section, flatten);
  }

  // if NcML, send I/O to referencedDataset, else to superclass
  public Array readData(ucar.nc2.Variable v, List section) throws IOException, InvalidRangeException {
    return orgFile.readData(v, section);
  }


  protected String toStringDebug(Object o) {
    return "";
  }

  ///////////////////////////////////////////////////////////////////////////////////
  // constructor methods

  /**
   * Add a CoordinateSystem to the dataset.
   */
  public void addCoordinateSystem(CoordinateSystem cs) {
    coordSys.add(cs);
  }

  /**
   * Add a CoordinateTransform to the dataset.
   */
  public void addCoordinateTransform(CoordinateTransform ct) {
    if (!coordTransforms.contains(ct))
      coordTransforms.add(ct);
  }

  /**
   * Add a CoordinateSystem to a variable.
   */
  public void addCoordinateSystem(VariableEnhanced v, CoordinateSystem cs) {
    v.addCoordinateSystem(cs);
  }

  /**
   * Add a CoordinateAxis. Also adds it as a variable. Remove any previous with the same name.
   */
  public CoordinateAxis addCoordinateAxis(VariableDS v) {
    if (v == null) return null;
    CoordinateAxis oldVar = findCoordinateAxis(v.getName());
    if (oldVar != null)
      coordAxes.remove(oldVar);

    CoordinateAxis ca = (v instanceof CoordinateAxis) ? (CoordinateAxis) v : CoordinateAxis.factory(this, v);
    coordAxes.add(ca);

    if (v.isMemberOfStructure()) {
      Structure parentOrg = v.getParentStructure();  // gotta be careful to get the wrapping parent
      Structure parent = (Structure) findVariable( parentOrg.getName());
      parent.replaceMemberVariable( ca);

    }  else {
      removeVariable(v.getParentGroup(), v.getShortName()); // remove by hashCode if it exists
      addVariable(ca.getParentGroup(), ca);
    }

    return ca;
  }

  /**
   * Replace a Dimension in a Variable.
   *
   * @param v replace in this Variable.
   * @param d replace existing dimension of the same name.
   */
  protected void replaceDimension(Variable v, Dimension d) {
    super.replaceDimension(v, d);
  }

  /**
   * Replace the group's list of variables. For copy construction.
   */
  protected void replaceGroupVariables(Group g, ArrayList vlist) {
    super.replaceGroupVariables(g, vlist);
  }

  /**
   * Replace the structure's list of variables. For copy construction.
   */
  protected void replaceStructureMembers(Structure s, ArrayList vlist) {
    super.replaceStructureMembers(s, vlist);
  }

  /** recalc any enhancement info */
  public void enhance() throws IOException {
    enhance( this, null);
  }

  ///////////////////////////////////////////////////////////////////////
  // setting variable data values

  /**
   * Generate the list of values from a starting value and an increment.
   *
   * @param npts  number of values
   * @param start starting value
   * @param incr  increment
   */
  public void setValues(Variable v, int npts, double start, double incr) {
    v.setCachedData(makeArray(v.getDataType(), npts, start, incr), true);
  }

  /**
   * Set the data values from a list of Strings.
   *
   * @param values list of Strings
   * @throws NumberFormatException
   */
  public void setValues(Variable v, ArrayList values) throws NumberFormatException {
    Array data = makeArray(v.getDataType(), values);

    if (v.getRank() != 1)
      data = data.reshape(v.getShape());

    v.setCachedData(data, true);
  }

  /**
   * Make an 1D array from a list of strings.
   *
   * @param dtype        data type of the array.
   * @param stringValues list of strings.
   * @return resulting 1D array.
   */
  static public Array makeArray(DataType dtype, ArrayList stringValues) {
    Array result = Array.factory(dtype.getPrimitiveClassType(), new int[]{stringValues.size()});
    IndexIterator dataI = result.getIndexIterator();
    Iterator iter = stringValues.iterator();

    while (iter.hasNext()) {
      if (dtype == DataType.STRING) {
        dataI.setObjectNext(iter.next());

      } else if (dtype == DataType.LONG) {
        long val = Long.parseLong((String) iter.next());
        dataI.setLongNext(val);

      } else {
        double val = Double.parseDouble((String) iter.next());
        dataI.setDoubleNext(val);
      }
    }
    return result;
  }

  /**
   * Make a 1D array from a start and inccr.
   *
   * @param dtype data type of result. must be convertible to double.
   * @param npts  number of points
   * @param start starting values
   * @param incr  increment
   * @return 1D array
   */
  static public Array makeArray(DataType dtype, int npts, double start, double incr) {
    Array result = Array.factory(dtype.getPrimitiveClassType(), new int[]{npts});
    IndexIterator dataI = result.getIndexIterator();
    for (int i = 0; i < npts; i++) {
      double val = start + i * incr;
      dataI.setDoubleNext(val);
    }
    return result;
  }

  ////////////////////////////////////////////////////////////////////
  // debugging

  private NetcdfDatasetInfo info = null;

  /**
   * Debugging: get the information from parsing
   */
  public NetcdfDatasetInfo getInfo() {
    if (null == info)
      info = new NetcdfDatasetInfo( this);
    return info;
  }

  void dumpClasses(Group g, PrintStream out) {

    out.println("Dimensions:");
    Iterator s = g.getDimensions().iterator();
    while (s.hasNext()) {
      Dimension ds = (Dimension) s.next();
      out.println("  " + ds.getName() + " " + ds.getClass().getName());
    }

    out.println("Atributes:");
    Iterator atts = g.getAttributes().iterator();
    while (atts.hasNext()) {
      Attribute ds = (Attribute) atts.next();
      out.println("  " + ds.getName() + " " + ds.getClass().getName());
    }

    out.println("Variables:");
    dumpVariables( g.getVariables(), out);

    out.println("Groups:");
    Iterator groups = g.getGroups().iterator();
    while (groups.hasNext()) {
      Group nested = (Group) groups.next();
      out.println("  " + nested.getName() + " " + nested.getClass().getName());
      dumpClasses(nested, out);
    }
  }

  private void dumpVariables(List vars, PrintStream out) {
    Iterator iter = vars.iterator();
    while (iter.hasNext()) {
      Variable v = (Variable) iter.next();
      out.print("  " + v.getName() + " " + v.getClass().getName()); // +" "+Integer.toHexString(v.hashCode()));
      if (v instanceof CoordinateAxis)
        out.println("  " + ((CoordinateAxis) v).getAxisType());
      else
        out.println();

      if (v instanceof Structure)
        dumpVariables( ((Structure)v).getVariables(), out);
    }
  }

  /**
   * debugging
   */
  public static void debugDump(PrintStream out, NetcdfDataset ncd) {
    String referencedLocation = ncd.orgFile == null ? "(null)" : ncd.orgFile.getLocation();
    out.println("\nNetcdfDataset dump = " + ncd.getLocation() + " uri= " + referencedLocation + "\n");
    ncd.dumpClasses(ncd.getRootGroup(), out);
  }

  /** debug */
  public static void main(String arg[]) {
    //String urls = "file:///C:/data/buoy/cwindRM.xml";
    //String urls = "C:/data/conventions/wrf/wrf_masscore.nc";
    //String urls = "http://motherlode.ucar.edu/cgi-bin/dods/DODS-3.2.1/nph-dods/dods/model/2004050712_eta_211.nc";
    //String defaultUrl = "R:/testdata/grid/netcdf/atd-radar/rgg.20020411.000000.lel.ll.nc";
    //String defaultUrl = "R:/testdata/grid/grib/grib2/test/NAM_CONUS_12km_20060604_1800.grib2";
    String defaultUrl = "C:/data/grib/nam/conus12/NAM_CONUS_12km_20060604_1800.grib2";

    String filename = (arg.length > 0) ? arg[0] : defaultUrl;

    try {
      NetcdfDataset ncDataset = NetcdfDataset.openDataset(filename, true, null);

      System.out.println("NetcdfDataset = " + filename + "\n" + ncDataset);
      debugDump(System.out, ncDataset);
    } catch (Exception ioe) {
      System.out.println("error = " + filename);
      ioe.printStackTrace();
    }
  }

}