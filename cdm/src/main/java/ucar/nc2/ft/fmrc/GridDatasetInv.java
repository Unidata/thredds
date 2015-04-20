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

package ucar.nc2.ft.fmrc;

import thredds.inventory.CollectionManagerAbstract;
import thredds.inventory.MCollection;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.nc2.constants._Coordinate;

import java.util.*;
import java.io.*;

import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import thredds.inventory.MFile;

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
 * Maybe will go away
 *
 * @author caron
 * @since Jan 11, 2010
 */
public class GridDatasetInv {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GridDatasetInv.class);
  static private final int REQ_VERSION = 2; // minimum required version, else regenerate XML
  static private final int CURR_VERSION = 2;  // current version
  
  static private boolean debug = false;  // current version

  public static GridDatasetInv open(MCollection cm, MFile mfile, Element ncml) throws IOException {
    // do we already have it ?
    byte[] xmlBytes = ((CollectionManagerAbstract)cm).getMetadata(mfile, "fmrInv.xml");  // LOOK should we keep this functionality ??
    if (xmlBytes != null) {
      if (logger.isDebugEnabled()) logger.debug(" got xmlFile in cache ="+ mfile.getPath()+ " size = "+xmlBytes.length);
      if (xmlBytes.length < 300) {
        logger.warn(" xmlFile in cache only has nbytes ="+ xmlBytes.length+"; will reread");
        // drop through and regenerate
      } else {
        GridDatasetInv inv = readXML(xmlBytes);

        // check if version required regen
        if (inv.version >= REQ_VERSION) {
          // check if file has changed
          long fileModifiedSecs = mfile.getLastModified() / 1000; // ignore msecs
          long xmlModifiedSecs = inv.getLastModified() / 1000; // ignore msecs
          if (xmlModifiedSecs >= fileModifiedSecs) { // LOOK if fileDate is -1, will always succeed
            if (logger.isDebugEnabled()) logger.debug(" cache ok "+new Date(inv.getLastModified())+" >= "+new Date(mfile.getLastModified())+" for " + mfile.getName());
            return inv; // ok, use it
          } else {
            if (logger.isInfoEnabled()) logger.info(" cache out of date "+new Date(inv.getLastModified())+" < "+new Date(mfile.getLastModified())+" for " + mfile.getName());
          }
        } else {
          if (logger.isInfoEnabled()) logger.info(" version needs upgrade "+inv.version+" < "+REQ_VERSION +" for " + mfile.getName());
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

      GridDatasetInv inv = new GridDatasetInv(gds, cm.extractDate(mfile));
      String xmlString = inv.writeXML( new Date(mfile.getLastModified()));
      ((CollectionManagerAbstract)cm).putMetadata(mfile, "fmrInv.xml", xmlString.getBytes(CDM.utf8Charset));
      if (logger.isDebugEnabled()) logger.debug(" added xmlFile "+ mfile.getPath()+".fmrInv.xml to cache");
      if (debug) System.out.printf(" added xmlFile %s.fmrInv.xml to cache%n", mfile.getPath());
      // System.out.println("new xmlBytes= "+ xmlString);
      return inv;
    } finally {
      if (gds != null) gds.close();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////

  private String location;
  private int version;
  private final List<TimeCoord> times = new ArrayList<>(); // list of TimeCoord
  private final List<VertCoord> vaxes = new ArrayList<>(); // list of VertCoord
  private final List<EnsCoord> eaxes = new ArrayList<>(); // list of EnsCoord
  private CalendarDate runDate; // date of the run
  private String runTimeString; // string representation of the date of the run
  private Date lastModified;

  private GridDatasetInv() {
  }

  public GridDatasetInv(ucar.nc2.dt.grid.GridDataset gds, CalendarDate runDate) {
    this.location = gds.getLocationURI();
    this.runDate = runDate;

    NetcdfFile ncfile = gds.getNetcdfFile();
    if (this.runDate == null) {
      runTimeString = ncfile.findAttValueIgnoreCase(null, _Coordinate.ModelBaseDate, null);
      if (runTimeString == null)
        runTimeString = ncfile.findAttValueIgnoreCase(null, _Coordinate.ModelRunDate, null);

      if (runTimeString != null) {
        this.runDate = DateUnit.parseCalendarDate(runTimeString);
         if (this.runDate == null) {
           logger.warn("GridDatasetInv rundate not ISO date string ({}) file={}", runTimeString, location);
           //throw new IllegalArgumentException(_Coordinate.ModelRunDate + " must be ISO date string " + runTime);
         }
      }

      if (this.runDate == null) {
        this.runDate = gds.getCalendarDateStart(); // LOOK not really right
        logger.warn("GridDatasetInv using gds.getStartDate() for run date = {}", runTimeString, location);
        //log.error("GridDatasetInv missing rundate in file=" + location);
        //throw new IllegalArgumentException("File must have " + _Coordinate.ModelBaseDate + " or " + _Coordinate.ModelRunDate + " attribute ");
      }
    }

    if (this.runDate == null) {
      throw new IllegalStateException("No run date");
    }

    this.runTimeString = this.runDate.toString();

    // add each variable, collect unique time and vertical axes
    for (GridDatatype gg : gds.getGrids()) {
      GridCoordSystem gcs = gg.getCoordinateSystem();
      Grid grid = new Grid(gg.getFullName());

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
  public CalendarDate getRunDate() {
    return runDate;
  }

  /**
   * Get string representation of the date of the ForecastModelRun
   *
   * @return string representation of the date of the ForecastModelRun
   */
  public String getRunDateString() {
    return runTimeString;
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
      if (tc.getAxisName().equals(axis.getFullName()))
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
      int ntimes = tc.getNCoords();
      return ntimes * getVertCoordLength();
    }

    public String toString() { return name; }

   /*  public String showCount() {
      int ntimes = tc.getOffsetHours().length;
      int nverts = getVertCoordLength();
      return countTotal()+" ("+ntimes+" x "+nverts+") " + tc;
    }

    public boolean hasOffset(double want) {
      for (double got : tc.getOffsetHours() ) {
        if (Misc.closeEnough(want, got)) return true;
      }
      return false;
    } */

    public int getVertCoordLength() {
      return (vc == null) ? 1 : vc.getValues1().length;
    }

    public TimeCoord getTimeCoord() {
      return tc;
    }

    public GridDatasetInv getFile() { return GridDatasetInv.this; }

   /*  public int countInventory(double hourOffset) {
      int timeIndex = tc.findIndex(hourOffset);
      if (timeIndex < 0)
        return 0;

      return getVertCoordLength();
    } */

    /*
     * Get inventory as an array of vert coords, at a particular time coord = hourOffset
     *
     * @param hourOffset : may or may not be in the list of time coords
     * @return array of vert coords. NaN = missing; -0.0 = surface.
     *
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
    } */
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
      if (vc.getName().equals(axis.getFullName())) return vc;
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
    rootElem.setAttribute("runTime", runTimeString);
    if (lastModified != null) {
      rootElem.setAttribute("lastModified", CalendarDateFormatter.toDateTimeString(lastModified));
    }
    rootElem.setAttribute("version", Integer.toString(CURR_VERSION));

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
        vcElem.setAttribute(CDM.UNITS, vc.getUnits());

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
      Element timeElement = new Element("timeCoord");
      rootElem.addContent(timeElement);
      timeElement.setAttribute("id", Integer.toString(tc.getId()));
      timeElement.setAttribute("name", tc.getName());
      timeElement.setAttribute("isInterval", tc.isInterval() ? "true" : "false");

      Formatter sbuff = new Formatter();
      if (tc.isInterval()) {
        double[] bound1 = tc.getBound1();
        double[] bound2 = tc.getBound2();
        for (int j = 0; j < bound1.length; j++)
          sbuff.format((Locale) null, "%f %f,", bound1[j], bound2[j]);

      } else {
        for (double offset : tc.getOffsetTimes())
          sbuff.format((Locale) null, "%f,", offset);
      }
      timeElement.addContent(sbuff.toString());

      List<GridDatasetInv.Grid> vars = tc.getGridInventory();
      Collections.sort(vars);
      for (Grid grid : vars) {
        Element varElem = new Element("grid");
        timeElement.addContent(varElem);
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
    InputStream is = new BufferedInputStream(new ByteArrayInputStream(xmlString));
    org.jdom2.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage() + " reading from XML ");
    }

    Element rootElem = doc.getRootElement();
    GridDatasetInv fmr = new GridDatasetInv();
    fmr.runTimeString = rootElem.getAttributeValue("runTime");
    fmr.location = rootElem.getAttributeValue("location");
    if (fmr.location == null)
      fmr.location = rootElem.getAttributeValue("name"); // old way
    String lastModifiedS = rootElem.getAttributeValue("lastModified");
    if (lastModifiedS != null)
      fmr.lastModified = CalendarDateFormatter.isoStringToDate(lastModifiedS);
    String version = rootElem.getAttributeValue("version");
    fmr.version = (version == null) ? 0 : Integer.parseInt(version);
    if (fmr.version < REQ_VERSION) return fmr;

    fmr.runDate = DateUnit.parseCalendarDate(fmr.runTimeString);

    java.util.List<Element> vList = rootElem.getChildren("vertCoord");
    for (Element vertElem : vList) {
      VertCoord vc = new VertCoord();
      fmr.vaxes.add(vc);
      vc.setId( Integer.parseInt(vertElem.getAttributeValue("id")));
      vc.setName(vertElem.getAttributeValue("name"));
      vc.setUnits(vertElem.getAttributeValue(CDM.UNITS));

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
      String s = timeElem.getAttributeValue("isInterval");
      boolean isInterval = (s != null) && (s.equals("true"));

      if (isInterval) {
        String boundsAll = timeElem.getTextNormalize();
        String[] bounds = boundsAll.split(",");
        int n = bounds.length;
        double[] bound1 = new double[n];
        double[] bound2 = new double[n];
        int count = 0;
        for (String b : bounds) {
          String[] value = b.split(" ");
          bound1[count] = Double.parseDouble(value[0]);
          bound2[count] = Double.parseDouble(value[1]);
          count++;
        }
        tc.setBounds(bound1, bound2);

      } else {
        String values = timeElem.getTextNormalize();
        String[] value = values.split(",");
        int n = value.length;
        double[] offsets = new double[n];
        int count = 0;
        for (String v : value)
          offsets[count++] = Double.parseDouble(v);
        tc.setOffsetTimes(offsets);
      }

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

  public static void main(String[] args) {
    String values = "1,2,3,4";
    String[] value = values.split("[,]");
    for (String s : value)
      System.out.printf("%s%n", s);
  }

}
