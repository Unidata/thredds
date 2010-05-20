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

import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.Misc;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants._Coordinate;

import java.util.*;
import java.io.*;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import thredds.inventory.MFile;
import thredds.inventory.CollectionManager;

/**
 * The data inventory of one GridDataset.
 * Track grids, time, vert, ens coordinates.
 * Grids are grouped by the time coordinated that they use.
 * Provides serialization to/from XML.
 * Uses dense time, vert coordinates - just the ones that are in the file.
 *
 * This replaces the older ucar.nc2.dt.fmrc.ForecastModelRunInventory, gets rid of the definition files.
 *
 * Not sure if the vert coords will ever be different across the time coords.
 *
 * Should be immutable, once the file is finished writing.
 *
 * TODO: staggered grids, other dimensions
 *
 * @author caron
 * @since Jan 11, 2010
 */
public class GridDatasetInv {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridDatasetInv.class);

  public static GridDatasetInv open(CollectionManager cm, MFile mfile, Element ncml) throws IOException {
    // do we already have it ?
    byte[] xmlBytes = cm.getMetadata(mfile, "fmrInv.xml");
    if (xmlBytes != null) {
      if (log.isDebugEnabled()) log.debug(" got xmlFile in cache ="+ mfile.getPath()+ " size = "+xmlBytes.length);
      if (xmlBytes.length < 300) {
        log.warn(" xmlFile in cache only has nbytes ="+ xmlBytes.length+"; will reread");
        // drop through and regenerate
      } else {
        GridDatasetInv inv = readXML(xmlBytes);
        long fileModifiedSecs = mfile.getLastModified() / 1000; // ignore msecs
        long xmlModifiedSecs = inv.getLastModified() / 1000; // ignore msecs
        if (xmlModifiedSecs >= fileModifiedSecs) { // LOOK if fileDate is -1, will always succeed
          return inv;
        } else {
          if (log.isInfoEnabled()) log.info(" cache out of date "+new Date(inv.getLastModified())+" < "+new Date(mfile.getLastModified()));
        }
      }
    }

    // generate it and save it
    GridDataset gds = null;
    try {
      if (ncml == null) {
        gds = GridDataset.open( mfile.getPath());

      } else {
        NetcdfFile nc = NetcdfDataset.acquireFile(mfile.getPath(), null);
        NetcdfDataset ncd = NcMLReader.mergeNcML(nc, ncml); // create new dataset
        ncd.enhance(); // now that the ncml is added, enhance "in place", ie modify the NetcdfDataset
        gds = new GridDataset(ncd);
      }

      // System.out.println("gds dataset= "+ gds.getNetcdfDataset());

      GridDatasetInv inv = new GridDatasetInv(gds, cm.extractRunDate(mfile));
      String xmlString = inv.writeXML( new Date(mfile.getLastModified()));
      cm.putMetadata(mfile, "fmrInv.xml", xmlString.getBytes("UTF-8"));
      if (log.isDebugEnabled()) log.debug(" added xmlFile "+ mfile.getPath()+".fmrInv.xml to cache");
      //System.out.println("new xmlBytes= "+ xmlString);
      return inv;
    } finally {
      if (gds != null) gds.close();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  private String location;
  private final List<TimeCoord> times = new ArrayList<TimeCoord>(); // list of TimeCoord
  private final List<VertCoord> vaxes = new ArrayList<VertCoord>(); // list of VertCoord
  private final List<EnsCoord> eaxes = new ArrayList<EnsCoord>(); // list of EnsCoord
  private Date runDate; // date of the run
  private String runTime; // string representation of the date of the run
  private Date lastModified;

  private GridDatasetInv() {
  }

  private GridDatasetInv(ucar.nc2.dt.GridDataset gds, Date runDate) {
    this.location = gds.getLocationURI();

    NetcdfFile ncfile = gds.getNetcdfFile();
    if (runDate == null) {
      runTime = ncfile.findAttValueIgnoreCase(null, _Coordinate.ModelBaseDate, null);
      if (runTime == null)
        runTime = ncfile.findAttValueIgnoreCase(null, _Coordinate.ModelRunDate, null);
      if (runTime == null) {
        log.error("GridDatasetInv missing rundate in file=" + location);
        throw new IllegalArgumentException("File must have " + _Coordinate.ModelBaseDate + " or " + _Coordinate.ModelRunDate + " attribute ");
      }
      this.runDate = DateUnit.getStandardOrISO(runTime);
      if (this.runDate == null) {
        log.error("GridDatasetInv rundate not ISO date string (%s) file=%s", runTime, location);
        throw new IllegalArgumentException(_Coordinate.ModelRunDate + " must be ISO date string " + runTime);
      }

    } else {

      this.runDate = runDate;
      DateFormatter df = new DateFormatter();
      this.runTime = df.toDateTimeStringISO(runDate);
    }

    // add each variable, collect unique time and vertical axes
    for (GridDatatype gg : gds.getGrids()) {
      GridCoordSystem gcs = gg.getCoordinateSystem();
      Grid grid = new Grid(gg.getName());

      // LOOK: Note this assumes a dense coordinate system
      CoordinateAxis1DTime axis = gcs.getTimeAxis1D();
      if (axis != null) {
        TimeCoord tc = getTimeCoordinate(axis);
        tc.addGridInventory(grid);
        grid.tc = tc;
      }

     CoordinateAxis1D vaxis = gcs.getVerticalAxis();
      if (vaxis != null) {
        grid.vc = getVertCoordinate(vaxis);
      }

     /* not yet
     CoordinateAxis1D eaxis = gcs.getEnsembleAxis();
     if (eaxis != null) {
       grid.ec = getEnsCoordinate(eaxis);
     } */

    }

    // assign sequence number
    int seqno = 0;
    for (TimeCoord tc : times) {
      tc.setId(seqno++);
    }

  }

  public String toString() {
    return location;
  }

  public String getLocation() {
    return location;
  }

  public long getLastModified() {
    return lastModified.getTime();
  }

  /**
   * Get the date of the ForecastModelRun
   *
   * @return the date of the ForecastModelRun
   */
  public Date getRunDate() {
    return runDate;
  }

  /**
   * Get string representation of the date of the ForecastModelRun
   *
   * @return string representation of the date of the ForecastModelRun
   */
  public String getRunDateString() {
    return runTime;
  }

  /**
   * Get a list of unique TimeCoords, which contain the list of variables that all use that TimeCoord.
   *
   * @return list of TimeCoord
   */
  public List<TimeCoord> getTimeCoords() {
    return times;
  }

  /**
   * Get a list of unique VertCoords.
   *
   * @return list of VertCoord
   */
  public List<VertCoord> getVertCoords() {
    return vaxes;
  }

  public Grid findGrid(String name) {
    for (TimeCoord tc : times) {
      List<Grid> grids = tc.getGridInventory();
      for (Grid g : grids) {
        if (g.name.equals(name))
          return g;
      }
    }
    return null;
  }

  /////////////////////////////////////////////////////////////////////////

  private TimeCoord getTimeCoordinate(CoordinateAxis1DTime axis) {
    // check for same axis
    for (TimeCoord tc : times) {
      if (tc.getAxisName().equals(axis.getName()))
        return tc;
    }

    // check for same offsets
    TimeCoord want = new TimeCoord(runDate, axis);
    for (TimeCoord tc : times) {
      if ((tc.equalsData(want)))
        return tc;
    }

    // its a new one
    times.add(want);
    return want;
  }

  Grid makeGrid(String gridName) {
    return new Grid(gridName);
  }

  //////////////////////////////////////////////////////

  /**
   * A Grid variable has a name, timeCoord and optionally a Vertical and Ensemble Coordinate
   */
  public class Grid implements Comparable {
    final String name;
    TimeCoord tc = null; // time coordinates reletive to getRunDate()
    EnsCoord ec = null; // optional
    VertCoord vc = null; // optional

    private Grid(String name) {
      this.name = name;
    }

    public String getName() { return name; }

    public String getLocation() { return location; }

    public String getTimeCoordName() {
      return  (tc == null) ? "" : tc.getName();
    }

    public String getVertCoordName() {
      return (vc == null) ? "" : vc.getName();
    }

    public int compareTo(Object o) {
      Grid other = (Grid) o;
      return name.compareTo(other.name);
    }

    public int countTotal() {
      int ntimes = tc.getOffsetHours().length;
      return ntimes * getVertCoordLength();
    }

    public String toString() { return name; }

    public String showCount() {
      int ntimes = tc.getOffsetHours().length;
      int nverts = getVertCoordLength();
      return countTotal()+" ("+ntimes+" x "+nverts+") " + tc;
    }

    public boolean hasOffset(double want) {
      for (double got : tc.getOffsetHours() ) {
        if (Misc.closeEnough(want, got)) return true;
      }
      return false;
    }

    public int getVertCoordLength() {
      return (vc == null) ? 1 : vc.getValues1().length;
    }

    public TimeCoord getTimeCoord() {
      return tc;
    }

    public GridDatasetInv getFile() { return GridDatasetInv.this; }

    public int countInventory(double hourOffset) {
      int timeIndex = tc.findIndex(hourOffset);
      if (timeIndex < 0)
        return 0;

      return getVertCoordLength();
    }

    /**
     * Get inventory as an array of vert coords, at a particular time coord = hourOffset
     *
     * @param hourOffset : may or may not be in the list of time coords
     * @return array of vert coords. NaN = missing; -0.0 = surface.
     */
    public double[] getVertCoords(double hourOffset) {

      int timeIndex = tc.findIndex(hourOffset);
      if (timeIndex < 0)
        return new double[0]; // if not in list of time coordinates, then entire inventory is missing

      if (vc == null) {
        double[] result = new double[1]; // if 2D return -0.0
        result[0] = -0.0;
        return result;
      }

      return vc.getValues1().clone();
    }
  }

  //////////////////////////////////////////////////////

  private VertCoord getVertCoordinate(int wantId) {
    if (wantId < 0) return null;
    for (VertCoord vc : vaxes) {
      if (vc.getId() == wantId)
        return vc;
    }
    return null;
  }

  private VertCoord getVertCoordinate(CoordinateAxis1D axis) {
    for (VertCoord vc : vaxes) {
      if (vc.getName().equals(axis.getName())) return vc;
    }

    VertCoord want = new VertCoord(axis);
    for (VertCoord vc : vaxes) {
      if ((vc.equalsData(want))) return vc;
    }

    // its a new one
    vaxes.add(want);
    return want;
  }

  //////////////////////////////////////////////////////

  private EnsCoord getEnsCoordinate(int ens_id) {
    if (ens_id < 0) return null;
    for (EnsCoord ec : eaxes) {
      if ((ec.getId() == ens_id))
        return ec;
    }
    return null;
  }

  private EnsCoord getEnsCoordinate(CoordinateAxis1D axis) {
    for (EnsCoord ec : eaxes) {
      if (ec.getName().equals(axis.getName())) return ec;
    }

    EnsCoord want = new EnsCoord(axis, null); // NOT YET
    for (EnsCoord ec : eaxes) {
      if ((ec.equalsData(want))) return ec;
    }

    // its a new one
    eaxes.add(want);
    return want;
  }

  //////////////////////////////////////////////////////////////

  /**
   * Write the XML representation to a local file.
   *
   * @param filename wite to this local file
   * @throws IOException on io error
   *
  public void writeXML(String filename) throws IOException {
    OutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(writeDocument(), out);
    out.close();
  }

  /**
   * Write the XML representaion to an OutputStream.
   *
   * @param out write to this OutputStream
   * @throws IOException on io error
   *
  public void writeXML(OutputStream out) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(writeDocument(), out);
  }

  public void writeXML(File f) throws IOException {
    FileOutputStream out = new FileOutputStream(f);
    writeXML(out);
    out.close();
  }  */

  /**
   * Write the XML representation to a String.
   *
   * @return the XML representation to a String.
   */
  public String writeXML(Date lastModified) {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(writeDocument(lastModified));
  }

  /**
   * Create the XML representation of the GridDatasetInv
   *
   * @return the XML representation as a Document
   */
  Document writeDocument(Date lastModified) {
    Element rootElem = new Element("gridInventory");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("location", location);
    rootElem.setAttribute("runTime", runTime);
    DateFormatter df = new DateFormatter();
    if (lastModified != null)
      rootElem.setAttribute("lastModified", df.toDateTimeString(lastModified));

    // list all the vertical coords
    Collections.sort(vaxes);
    int count = 0;
    for (VertCoord vc : vaxes) {
      vc.setId(count++);
      Element vcElem = new Element("vertCoord");
      rootElem.addContent(vcElem);
      vcElem.setAttribute("id", Integer.toString(vc.getId()));
      vcElem.setAttribute("name", vc.getName());
      if (vc.getUnits() != null)
        vcElem.setAttribute("units", vc.getUnits());

      StringBuilder sbuff = new StringBuilder();
      double[] values1 = vc.getValues1();
      double[] values2 = vc.getValues2();
      for (int j = 0; j < values1.length; j++) {
        if (j > 0) sbuff.append(" ");
        sbuff.append(Double.toString(values1[j]));
        if (values2 != null) {
          sbuff.append(",");
          sbuff.append(Double.toString(values2[j]));
        }
      }
      vcElem.addContent(sbuff.toString());
    }

    // list all the time coords
    count = 0;
    for (TimeCoord tc : times) {
      tc.setId(count++);
      Element offsetElem = new Element("timeCoord");
      rootElem.addContent(offsetElem);
      offsetElem.setAttribute("id", Integer.toString(tc.getId()));
      offsetElem.setAttribute("name", tc.getName());

      double[] offset = tc.getOffsetHours();
      StringBuilder sbuff = new StringBuilder();
      for (int j = 0; j < offset.length; j++) {
        if (j > 0) sbuff.append(" ");
        sbuff.append(Double.toString(offset[j]));
      }
      offsetElem.addContent(sbuff.toString());

      List<GridDatasetInv.Grid> vars = tc.getGridInventory();
      Collections.sort(vars);
      for (Grid grid : vars) {
        Element varElem = new Element("grid");
        offsetElem.addContent(varElem);
        varElem.setAttribute("name", grid.name);
        if (grid.ec != null)
          varElem.setAttribute("ens_id", Integer.toString(grid.ec.getId()));
        if (grid.vc != null)
          varElem.setAttribute("vert_id", Integer.toString(grid.vc.getId()));
      }
    }

    return doc;
  }

  /**
   * Construct a GridDatasetInv from its XML representation
   *
   * @param xmlString the xml string
   * @return ForecastModelRun
   * @throws IOException on io error
   */
  private static GridDatasetInv readXML(byte[] xmlString) throws IOException {
    // if (debug) System.out.println(" read from XML " + xmlLocation);

    InputStream is = new BufferedInputStream(new ByteArrayInputStream(xmlString));
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage() + " reading from XML ");
    }

    Element rootElem = doc.getRootElement();
    GridDatasetInv fmr = new GridDatasetInv();
    fmr.runTime = rootElem.getAttributeValue("runTime");
    fmr.location = rootElem.getAttributeValue("location");
    if (fmr.location == null)
      fmr.location = rootElem.getAttributeValue("name"); // old way
    String lastModifiedS = rootElem.getAttributeValue("lastModified");
    DateFormatter df = new DateFormatter();
    if (lastModifiedS != null)
      fmr.lastModified = df.getISODate(lastModifiedS);

    DateFormatter formatter = new DateFormatter();
    fmr.runDate = formatter.getISODate(fmr.runTime);

    java.util.List<Element> vList = rootElem.getChildren("vertCoord");
    for (Element vertElem : vList) {
      VertCoord vc = new VertCoord();
      fmr.vaxes.add(vc);
      vc.setId( Integer.parseInt(vertElem.getAttributeValue("id")));
      vc.setName(vertElem.getAttributeValue("name"));
      vc.setUnits(vertElem.getAttributeValue("units"));

      // parse the values
      String values = vertElem.getTextNormalize();
      StringTokenizer stoke = new StringTokenizer(values);
      int n = stoke.countTokens();
      double[] values1 = new double[n];
      double[] values2 = null;
      int count = 0;
      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        int pos = toke.indexOf(',');
        if (pos < 0)
          values1[count] = Double.parseDouble(toke);
        else {
          if (values2 == null)
            values2 = new double[n];
          String val1 = toke.substring(0, pos);
          String val2 = toke.substring(pos + 1);
          values1[count] = Double.parseDouble(val1);
          values2[count] = Double.parseDouble(val2);
        }
        count++;
      }
      vc.setValues1(values1);
      vc.setValues2(values2);
    }

    java.util.List<Element> tList = rootElem.getChildren("timeCoord");
    for (Element timeElem : tList) {
      TimeCoord tc = new TimeCoord(fmr.runDate);
      fmr.times.add(tc);
      tc.setId(Integer.parseInt(timeElem.getAttributeValue("id")));

      // parse the values
      String values = timeElem.getTextNormalize();
      String[] value = values.split(" "); 
      int n = value.length;
      double[] offsets = new double[n];
      int count = 0;
      for (String v : value)
        offsets[count++] = Double.parseDouble(v);
      tc.setOffsetHours(offsets);

      //get the variable names
      List<Element> varList = timeElem.getChildren("grid");
      for (Element vElem : varList) {
        Grid grid = fmr.makeGrid(vElem.getAttributeValue("name"));
        if (vElem.getAttributeValue("ens_id") != null)
          grid.ec = fmr.getEnsCoordinate( Integer.parseInt(vElem.getAttributeValue("ens_id")));
        if (vElem.getAttributeValue("vert_id") != null)
          grid.vc = fmr.getVertCoordinate( Integer.parseInt(vElem.getAttributeValue("vert_id")));
        tc.addGridInventory(grid);
        grid.tc = tc;
      }
    }

    return fmr;
  }

}
