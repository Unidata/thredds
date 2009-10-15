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

package ucar.nc2.dt.fmrc;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.*;

import ucar.ma2.InvalidRangeException;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.constants._Coordinate;

import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.iosp.grid.GridServiceProvider;
import ucar.nc2.iosp.grid.GridVariable;
import ucar.nc2.iosp.grid.GridEnsembleCoord;
import ucar.nc2.Variable;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.IO;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.fmr.FmrcCoordSys;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

/**
 * This reads and writes XML files to summarize the inventory for a single ForecastModelRun.
 * The underlying dataset is a GridDataset.
 * <p/>
 * Tracks unique TimeCoords (aka "valid times" aka "forecast times" aka "offset hours"), and tracks the list of
 * variables (aka grids) that use that TimeCoord.
 * <p/>
 * Tracks unique VertCoords; grids have a reference to one if they are 3D.
 * <p/>
 * <pre>
 * Data Structures
 *  List VertCoord
 *    double[] values
 * <p/>
 *  List TimeCoord
 *    double[] offsetHour
 *    List Grid
 *      VertCoord (optional)
 *      List Misssing
 * </pre>
 *
 * @author caron
 */
public class ForecastModelRunInventory {
  public static final int OPEN_NORMAL = 1; // try to open XML, if fail, open dataset and write XML
  public static final int OPEN_FORCE_NEW = 2;  // only open dataset and write XML new
  public static final int OPEN_XML_ONLY = 3; // only open XML, if not exist, return

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ForecastModelRunInventory.class);

  private String name;
  private List<TimeCoord> times = new ArrayList<TimeCoord>(); // list of TimeCoord
  private List<VertCoord> vaxes = new ArrayList<VertCoord>(); // list of VertCoord
  private List<EnsCoord> eaxes = new ArrayList<EnsCoord>(); // list of EnsCoord
  private Date runDate; // date of the run
  private String runTime; // string representation of the date of the run
  private GridDataset gds; // underlying dataset - may be null if read from XML
  private LatLonRect bb;

  private boolean debugMissing = false;

  private ForecastModelRunInventory() {
  }

  private ForecastModelRunInventory(ucar.nc2.dt.GridDataset gds, Date runDate) {

    this.gds = gds;
    name = gds.getTitle();

    NetcdfFile ncfile = gds.getNetcdfFile();
    if (runDate == null) {
      runTime = ncfile.findAttValueIgnoreCase(null, _Coordinate.ModelBaseDate, null);
      if (runTime == null)
        runTime = ncfile.findAttValueIgnoreCase(null, _Coordinate.ModelRunDate, null);
      if (runTime == null)
        throw new IllegalArgumentException("File must have " + _Coordinate.ModelBaseDate + " or " +
                _Coordinate.ModelRunDate + " attribute ");
      this.runDate = DateUnit.getStandardOrISO(runTime);
      if (this.runDate == null)
        throw new IllegalArgumentException(_Coordinate.ModelRunDate + " must be ISO date string " + runTime);
    } else {
      this.runDate = runDate;
      DateFormatter df = new DateFormatter();
      this.runTime = df.toDateTimeStringISO(runDate);
    }
    getIosp();

    // add each variable
    for (GridDatatype gg : gds.getGrids()) {
      GridCoordSystem gcs = gg.getCoordinateSystem();
      Grid grid = new Grid(gg.getName());
      VariableEnhanced ve = gg.getVariable();
      Variable v = ve.getOriginalVariable();   // LOOK why original variable ??
      addMissing(v, gcs, grid);

      // LOOK: Note this assumes a dense coordinate system
      CoordinateAxis1D axis = gcs.getTimeAxis1D();
      if (axis != null) {
        TimeCoord tc = getTimeCoordinate(axis);
        tc.vars.add(grid);
        grid.parent = tc;
      }

      CoordinateAxis1D eaxis = gcs.getEnsembleAxis();
      if (eaxis != null) {
        int[] einfo = getEnsInfo(  v );
        grid.ec = getEnsCoordinate(eaxis, einfo );
      }

      CoordinateAxis1D vaxis = gcs.getVerticalAxis();
      if (vaxis != null) {
        grid.vc = getVertCoordinate(vaxis);
      }

      LatLonRect rect = gcs.getLatLonBoundingBox();
      if (null == bb)
        bb = rect;
      else if (!bb.equals(rect))
        bb.extend(rect);
    }
  }

  public void close() throws IOException {
    if (null != gds) gds.close();
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * Get the date of the ForecastModelRun
   * @return the date of the ForecastModelRun
   */
  public Date getRunDate() {
    return runDate;
  }

  /**
   * Get string representation of the date of the ForecastModelRun
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

  public LatLonRect getBB() {
    return bb;
  }

  /**
   * Release and close the dataset, and allow CG.
   *
   * @throws IOException on io error
   */
  public void releaseDataset() throws IOException {
    if (gds == null)
      return;

    gds.close();
    for (TimeCoord tc : times) {
      tc.axis = null;  // allow GC
    }

    for (VertCoord vc : vaxes) {
      vc.axis = null;  // allow GC
    }

  }

  public Grid findGrid(String name) {
    for (TimeCoord tc : times) {
      List<Grid> grids = tc.getGrids();
      for (Grid g : grids) {
        if (g.name.equals(name))
          return g;
      }
    }
    return null;
  }

  //////////////////////////////////////////////////////////

  // Grib files are collections of 2D horizontal arrays.
  // LOOK: breaking encapsolation !!!

  private void getIosp() {
    NetcdfDataset ncd = (NetcdfDataset) gds.getNetcdfFile();
    NetcdfFile ncfile = ncd.getReferencedFile();
    while (ncfile instanceof NetcdfDataset) {
      ncd = (NetcdfDataset) ncfile;
      ncfile = ncd.getReferencedFile();
    }
    if (ncfile == null) return;
    IOServiceProvider iosp = ncfile.getIosp();
    if (iosp == null) return;
    if (!(iosp instanceof GridServiceProvider)) return;
    gribIosp = (GridServiceProvider) iosp;
  }

  private GridServiceProvider gribIosp;

  private void addMissing(Variable v, GridCoordSystem gcs, Grid grid) {
    if (gribIosp == null) return;
    if (gcs.getVerticalAxis() == null && gcs.getEnsembleAxis() == null) return;
    int ntimes = (int) gcs.getTimeAxis().getSize();
    int nverts = 1;
    if (gcs.getVerticalAxis() != null )
      nverts = (int) gcs.getVerticalAxis().getSize();
    int nens = 1;
    if (gcs.getEnsembleAxis() != null )
      nens = (int) gcs.getEnsembleAxis().getSize();
    //int total = ntimes * nverts;
    int total = ntimes * nens * nverts;


    List<Missing> missing = new ArrayList<Missing>();
    for (int timeIndex = 0; timeIndex < ntimes; timeIndex++) {
      for (int ensIndex = 0; ensIndex < nens; ensIndex++) {
        for (int vertIndex = 0; vertIndex < nverts; vertIndex++)
          try {
            if (gribIosp.isMissingXY(v, timeIndex, ensIndex, vertIndex))
              missing.add(new Missing(timeIndex, ensIndex, vertIndex));
          } catch (InvalidRangeException e) {
            e.printStackTrace();
          }
      }
    }
    if (missing.size() > 0) {
      grid.missing = missing;
      if (debugMissing)
        System.out.println("Missing " + gds.getTitle() + " " + v.getName() + " # =" + missing.size() + "/" + total);
    } else if (debugMissing)
      System.out.println(" None missing for " + gds.getTitle() + " " + v.getName() + " total = " + total);
  }

  private int[] getEnsInfo( Variable v ) {
    if (gribIosp == null) return null;
    int[] info = gribIosp.ensembleInfo(v);
    return info;
  }

  /////////////////////////////////////////////////////////////////////////

  private TimeCoord getTimeCoordinate(CoordinateAxis1D axis) {
    for (TimeCoord tc : times) {
      if ((tc.axis != null) && (tc.axis == axis))
        return tc;
    }

    TimeCoord want = new TimeCoord(runDate, axis);
    for (TimeCoord tc : times) {
      if ((tc.equalsData(want)))
        return tc;
    }

    // its a new one
    times.add(want);
    want.setId(Integer.toString(tc_seqno));
    tc_seqno++;
    return want;
  }

  private int tc_seqno = 0;

  /**
   * Represents a list of valid times.
   * Tracks a list of variables that all have the same list of valid times.
   */
  public static class TimeCoord implements FmrcCoordSys.TimeCoord, Comparable {
    private CoordinateAxis1D axis; // is null when read from XML
    private List<Grid> vars = new ArrayList<Grid>();  // list of Grid
    private String id; // unique id
    private double[] offset; // hours since runTime

    TimeCoord() {
    }

    TimeCoord(int num, TimeCoord from) {
      this.id = Integer.toString(num);
      this.offset = from.offset;
    }

    TimeCoord(Date runDate, CoordinateAxis1D axis) {
      this.axis = axis;

      DateUnit unit = null;
      try {
        unit = new DateUnit(axis.getUnitsString());
      } catch (Exception e) {
        throw new IllegalArgumentException("Not a unit of time "+axis.getUnitsString());
      }

      int n = (int) axis.getSize();
      offset = new double[n];
      for (int i = 0; i < axis.getSize(); i++) {
        Date d = unit.makeDate(axis.getCoordValue(i));
        offset[i] = getOffsetInHours(runDate, d);
      }
    }

    /**
     * The list of Grid that use this TimeCoord
     * @return list of Grid that use this TimeCoord
     */
    public List<Grid> getGrids() {
      return vars;
    }

    /**
     * A unique id for this TimeCoord
     * @return unique id for this TimeCoord
     */
    public String getId() {
      return id;
    }

    /**
     * Set the unique id for this TimeCoord
     * @param id id for this TimeCoord
     */
    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return id.equals("0") ? "time" : "time" + id;
    }

    /**
     * The list of valid times, in units of hours since the run time
     */
    public double[] getOffsetHours() {
      return offset;
    }

    public void setOffsetHours(double[] offset) {
      this.offset = offset;
    }

    /**
     * Instances that have the same offsetHours are equal
     * @param tother compare this TomCoord's data
     * @return true if data is equal
     */
    public boolean equalsData(TimeCoord tother) {
      if (offset.length != tother.offset.length)
        return false;
      for (int i = 0; i < offset.length; i++) {
        if (!ucar.nc2.util.Misc.closeEnough(offset[i], tother.offset[i]))
          return false;
      }
      return true;
    }

    int findIndex(double offsetHour) {
      for (int i = 0; i < offset.length; i++)
        if (offset[i] == offsetHour)
          return i;
      return -1;
    }

    /* Overrride hashcode to correspond to equals()
    public int hashCode() {
      if (hashcode != 0) return hashcode;

      int result = 17;
      for (int i = 0; i < offset.length ; i++) {
        long temp = Double.doubleToLongBits( offset[i]);
        result = 37*result + (int) (temp ^ (temp >>>32));
      }
      hashcode = result;
      return hashcode;
    }
    private int hashcode = 0; */

    public int compareTo(Object o) {
      TimeCoord ot = (TimeCoord) o;
      return id.compareTo(ot.id);
    }
  }

  //////////////////////////////////////////////////////

  /**
   * A Grid variable has a name, timeCoord and optionally a Vertical Coordinate, and list of Missing.
   * The inventory is represented as:
   * 1) if 2D, the timeCoord represents the inventory
   * 2) if 3D, inventory = timeCoord * vertCoord - Missing
   */
  public static class Grid implements Comparable {
    String name; // , sname;
    TimeCoord parent = null;
    EnsCoord ec = null; // optional
    VertCoord vc = null; // optional
    List<Missing> missing;

    Grid(String name) {
      this.name = name;
    }

    public int compareTo(Object o) {
      Grid other = (Grid) o;
      return name.compareTo(other.name);
    }

    public int countInventory() {
      return countTotal() - countMissing();
    }

    public int countTotal() {
      int ntimes = parent.getOffsetHours().length;
      return ntimes * getVertCoordLength();
    }

    public int countMissing() {
      return (missing == null) ? 0 : missing.size();
    }

    int getVertCoordLength() {
      return (vc == null) ? 1 : vc.getValues1().length;
    }

    public int countInventory(double hourOffset) {
      int timeIndex = parent.findIndex(hourOffset);
      if (timeIndex < 0)
        return 0;

      // otherwise, count the Missing with this time index
      if (missing == null)
        return getVertCoordLength();

      int count = 0;
      for (Missing m : missing) {
        if (m.timeIndex == timeIndex)
          count++;
      }
      return getVertCoordLength() - count;
    }

    /**
     * Get inventory as an array of vert coords, at a particular time coord = hourOffset
     *
     * @param hourOffset : may or may not be in the list of time coords
     * @return array of vert coords. NaN = missing; -0.0 = surface.
     */
    public double[] getVertCoords(double hourOffset) {

      int timeIndex = parent.findIndex(hourOffset);
      if (timeIndex < 0)
        return new double[0]; // if not in list of time coordinates, then entire inventory is missing

      if (vc == null) {
        double[] result = new double[1]; // if 2D return -0.0
        result[0] = -0.0;
        return result;
      }

      double[] result = vc.getValues1().clone();
      if (null != missing) {
        for (Missing m : missing) {
          if (m.timeIndex == timeIndex)
            result[m.vertIndex] = Double.NaN;
        }
      }
      return result;
    }
  }

  public static class Missing {
    int timeIndex, ensIndex, vertIndex;

    Missing(int timeIndex, int ensIndex, int vertIndex) {
      this.timeIndex = timeIndex;
      this.ensIndex = ensIndex;
      this.vertIndex = vertIndex;
    }
  }

  //////////////////////////////////////////////////////

  private VertCoord getVertCoordinate(String vert_id) {
    if (vert_id == null)
      return null;
    for (VertCoord vc : vaxes) {
      if ((vc.id.equals(vert_id)))
        return vc;
    }
    return null;
  }

  private VertCoord getVertCoordinate(CoordinateAxis1D axis) {
    for (VertCoord vc : vaxes) {
      if ((vc.axis != null) && (vc.axis == axis)) return vc;
    }

    VertCoord want = new VertCoord(axis);
    for (VertCoord vc : vaxes) {
      if ((vc.equalsData(want))) return vc;
    }

    // its a new one
    vaxes.add(want);
    want.setId(Integer.toString(vc_seqno));
    vc_seqno++;
    return want;
  }

  private int vc_seqno = 0;



  /**
   * Represents a vertical coordinate.
   * Tracks a list of variables that all have the same list of valid times.
   */
  public static class VertCoord implements FmrcCoordSys.VertCoord, Comparable {
    CoordinateAxis1D axis; // is null when read from XML
    private String name, units;
    private String id; // unique id
    double[] values1, values2;

    VertCoord() {
    }

    VertCoord(CoordinateAxis1D axis) {
      this.axis = axis;
      this.name = axis.getName();
      this.units = axis.getUnitsString();

      int n = (int) axis.getSize();
      if (axis.isLayer()) {
        values1 = axis.getBound1();
        values2 = axis.getBound2();
      } else {
        values1 = new double[n];
        for (int i = 0; i < axis.getSize(); i++)
          values1[i] = axis.getCoordValue(i);
      }
    }

    // copy constructor
    VertCoord(VertCoord vc) {
      this.name = vc.getName();
      this.units = vc.getUnits();
      this.id = vc.getId();
      this.values1 = vc.getValues1().clone();
      this.values2 = (vc.getValues2() == null) ? null : vc.getValues2().clone();
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getUnits() {
      return units;
    }

    public void setUnits(String units) {
      this.units = units;
    }

    public double[] getValues1() {
      return values1;
    }

    public void setValues1(double[] values) {
      this.values1 = values;
    }

    public double[] getValues2() {
      return values2;
    }

    public void setValues2(double[] values) {
      this.values2 = values;
    }

    public int getSize() {
      return values1.length;
    }

    public boolean equalsData(VertCoord other) {
      if (values1.length != other.values1.length)
        return false;

      for (int i = 0; i < values1.length; i++) {
        if (!ucar.nc2.util.Misc.closeEnough(values1[i], other.values1[i]))
          return false;
      }

      if ((values2 == null) && (other.values2 == null))
        return true;

      if ((values2 == null) || (other.values2 == null))
        return false;

      if (values2.length != other.values2.length)
        return false;
      for (int i = 0; i < values2.length; i++) {
        if (!ucar.nc2.util.Misc.closeEnough(values2[i], other.values2[i]))
          return false;
      }

      return true;
    }

    public int compareTo(Object o) {
      VertCoord other = (VertCoord) o;
      return name.compareTo(other.name);
    }
  }


  //////////////////////////////////////////////////////

  private EnsCoord getEnsCoordinate(String ens_id) {
    if (ens_id == null)
      return null;
    for (EnsCoord ec : eaxes) {
      if ((ec.id.equals(ens_id)))
        return ec;
    }
    return null;
  }

  private EnsCoord getEnsCoordinate(CoordinateAxis1D axis, int[] einfo ) {
    for (EnsCoord ec : eaxes) {
      if ((ec.axis != null) && (ec.axis == axis)) return ec;
    }

    EnsCoord want = new EnsCoord(axis, einfo );
    for (EnsCoord ec : eaxes) {
      if ((ec.equalsData(want))) return ec;
    }

    // its a new one
    eaxes.add(want);
    want.setId(Integer.toString(ec_seqno));
    ec_seqno++;
    return want;
  }

  private int ec_seqno = 0;

  /**
   * Represents a ensemble coordinate.
   * Tracks a list of variables that all have the same list of ensembles.
   */
  public static class EnsCoord implements FmrcCoordSys.EnsCoord, Comparable {
    CoordinateAxis1D axis; // is null when read from XML
    private String name; //, units;
    private String id; // unique id
    private int ensembles;
    private int pdn;
    private int[] ensTypes;

    EnsCoord() {
    }

    EnsCoord(CoordinateAxis1D axis, int[] einfo) {
      this.axis = axis;
      this.name = axis.getName();
      this.ensembles = einfo[ 0 ];
      this.pdn = einfo[ 1 ];
      this.ensTypes = new int[ this.ensembles ];
      System.arraycopy( einfo, 2, ensTypes, 0, ensembles);
    }

    // copy constructor
    EnsCoord(EnsCoord ec) {
      this.name = ec.getName();
      this.id = ec.getId();
      this.ensembles = ec.getNEnsembles();
      this.pdn = ec.getPDN();
      this.ensTypes = ec.getEnsTypes().clone();
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getNEnsembles() {
      return ensembles;
    }

    public void setNEnsembles(int ensembles) {
      this.ensembles = ensembles;
    }

    public int getPDN() {
      return pdn;
    }

    public void setPDN(int pdn ) {
      this.pdn = pdn;
    }

    public int[] getEnsTypes() {
      return ensTypes;
    }

    public void setEnsTypes(int[] ensTypes ) {
      this.ensTypes = ensTypes;
    }

    public int getSize() {
      return ensembles;
    }

    public boolean equalsData(EnsCoord other) {


      if (ensembles != other.ensembles)
        return false;

      if (pdn != other.pdn)
        return false;

      for (int i = 0; i < ensTypes.length; i++) {
        if ( ensTypes[i] != other.ensTypes[i])
          return false;
      }

      return true;
    }

    public int compareTo(Object o) {
      EnsCoord other = (EnsCoord) o;
      return name.compareTo(other.name);
    }
  }

  static public double getOffsetInHours(Date origin, Date date) {
    double secs = date.getTime() / 1000;
    double origin_secs = origin.getTime() / 1000;
    double diff = secs - origin_secs;

    return diff / 3600.0;
  }

  //////////////////////////////////////////////////////////////

  /**
   * Write the XML representation to a local file.
   *
   * @param filename wite to this local file
   * @throws IOException on io error
   */
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
   */
  public void writeXML(OutputStream out) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(writeDocument(), out);
  }

  /**
   * Write the XML representation to a String.
   * @return the XML representation to a String.
   */
  public String writeXML() {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(writeDocument());
  }

  /**
   * Create the XML representation
   * @return the XML representation as a Document
   */
  public Document writeDocument() {
    Element rootElem = new Element("forecastModelRun");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("name", getName());
    rootElem.setAttribute("runTime", runTime);

    // list all the ensemble coords
    Collections.sort(eaxes);
    for (EnsCoord ec : eaxes) {
      Element ecElem = new Element("ensCoord");
      rootElem.addContent(ecElem);
      ecElem.setAttribute("id", ec.id);
      ecElem.setAttribute("name", ec.name);
      ecElem.setAttribute("product_definition", Integer.toString(ec.pdn));
      //if (ec.units != null)
      //  ecElem.setAttribute("units", ec.units);

      StringBuilder sbuff = new StringBuilder();
      for (int j = 0; j < ec.ensTypes.length; j++) {
        if (j > 0) sbuff.append(" ");
        sbuff.append(Integer.toString(ec.ensTypes[j]));

      }
      ecElem.addContent(sbuff.toString());
    }

    // list all the vertical coords
    Collections.sort(vaxes);
    for (VertCoord vc : vaxes) {
      Element vcElem = new Element("vertCoord");
      rootElem.addContent(vcElem);
      vcElem.setAttribute("id", vc.id);
      vcElem.setAttribute("name", vc.name);
      if (vc.units != null)
        vcElem.setAttribute("units", vc.units);

      StringBuilder sbuff = new StringBuilder();
      for (int j = 0; j < vc.values1.length; j++) {
        if (j > 0) sbuff.append(" ");
        sbuff.append(Double.toString(vc.values1[j]));
        if (vc.values2 != null) {
          sbuff.append(",");
          sbuff.append(Double.toString(vc.values2[j]));
        }
      }
      vcElem.addContent(sbuff.toString());
    }

    // list all the offset hours
    for (TimeCoord tc : times) {
      Element offsetElem = new Element("offsetHours");
      rootElem.addContent(offsetElem);
      offsetElem.setAttribute("id", tc.id);

      StringBuilder sbuff = new StringBuilder();
      for (int j = 0; j < tc.offset.length; j++) {
        if (j > 0) sbuff.append(" ");
        sbuff.append(Double.toString(tc.offset[j]));
      }
      offsetElem.addContent(sbuff.toString());

      Collections.sort(tc.vars);
      for (Grid grid : tc.vars) {
        Element varElem = new Element("variable");
        offsetElem.addContent(varElem);
        varElem.setAttribute("name", grid.name);
        if (grid.ec != null)
          varElem.setAttribute("ens_id", grid.ec.id);
        if (grid.vc != null)
          varElem.setAttribute("vert_id", grid.vc.id);

        if ((grid.missing != null) && (grid.missing.size() > 0)) {
          Element missingElem = new Element("missing");
          varElem.addContent(missingElem);
          sbuff.setLength(0);
          for (int k = 0; k < grid.missing.size(); k++) {
            Missing m = grid.missing.get(k);
            if (k > 0) sbuff.append(" ");
            sbuff.append(m.timeIndex);
            if ( grid.ec != null ) {
              sbuff.append(",");
              sbuff.append(m.ensIndex);
            }
            if ( grid.vc != null ) {
              sbuff.append(",");
              sbuff.append(m.vertIndex);
            }
          }
          missingElem.addContent(sbuff.toString());
        }
      }

      // add lat/lon bounding box
      if (bb != null) {
        Element bbElem = new Element("horizBB");
        rootElem.addContent(bbElem);
        LatLonPoint llpt = bb.getLowerLeftPoint();
        LatLonPoint urpt = bb.getUpperRightPoint();
        bbElem.setAttribute("west", ucar.unidata.util.Format.dfrac(llpt.getLongitude(), 3));
        bbElem.setAttribute("east", ucar.unidata.util.Format.dfrac(urpt.getLongitude(), 3));
        bbElem.setAttribute("south", ucar.unidata.util.Format.dfrac(llpt.getLatitude(), 3));
        bbElem.setAttribute("north", ucar.unidata.util.Format.dfrac(urpt.getLatitude(), 3));
      }
    }

    return doc;
  }

  /**
   * Construct a ForecastModelRun from its XML representation
   *
   * @param xmlLocation location of xml - assumed to be a local file.
   * @return ForecastModelRun
   * @throws IOException on io error
   */
  public static ForecastModelRunInventory readXML(String xmlLocation) throws IOException {
    if (debug) System.out.println(" read from XML " + xmlLocation);

    InputStream is = new BufferedInputStream(new FileInputStream(xmlLocation));
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder();
      doc = builder.build(is);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage() + " reading from XML " + xmlLocation);
    }

    Element rootElem = doc.getRootElement();
    ForecastModelRunInventory fmr = new ForecastModelRunInventory();
    fmr.runTime = rootElem.getAttributeValue("runTime");

    DateFormatter formatter = new DateFormatter();
    fmr.runDate = formatter.getISODate(fmr.runTime);

    java.util.List<Element> eList = rootElem.getChildren("ensCoord");
    for (Element ensElem : eList) {
      EnsCoord ec = new EnsCoord();
      fmr.eaxes.add(ec);
      ec.id = ensElem.getAttributeValue("id");
      ec.name = ensElem.getAttributeValue("name");
      ec.pdn = Integer.parseInt( ensElem.getAttributeValue("product_definition"));

      // parse the values
      String values = ensElem.getText();
      StringTokenizer stoke = new StringTokenizer(values);
      ec.ensembles = stoke.countTokens();
      ec.ensTypes = new int[ ec.ensembles ];
      int count = 0;
      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        int pos = toke.indexOf(',');
        if (pos < 0)
          ec.ensTypes[count] = Integer.parseInt(toke);
//        else {
//          String val1 = toke.substring(0, pos);
//          String val2 = toke.substring(pos + 1);
//          vc.values1[count] = Double.parseDouble(val1);
//          vc.values2[count] = Double.parseDouble(val2);
//        }
        count++;
      }
    }

    java.util.List<Element> vList = rootElem.getChildren("vertCoord");
    for (Element vertElem : vList) {
      VertCoord vc = new VertCoord();
      fmr.vaxes.add(vc);
      vc.id = vertElem.getAttributeValue("id");
      vc.name = vertElem.getAttributeValue("name");
      vc.units = vertElem.getAttributeValue("units");

      // parse the values
      String values = vertElem.getText();
      StringTokenizer stoke = new StringTokenizer(values);
      int n = stoke.countTokens();
      vc.values1 = new double[n];
      int count = 0;
      while (stoke.hasMoreTokens()) {
        String toke = stoke.nextToken();
        int pos = toke.indexOf(',');
        if (pos < 0)
          vc.values1[count] = Double.parseDouble(toke);
        else {
          if (vc.values2 == null)
            vc.values2 = new double[n];
          String val1 = toke.substring(0, pos);
          String val2 = toke.substring(pos + 1);
          vc.values1[count] = Double.parseDouble(val1);
          vc.values2[count] = Double.parseDouble(val2);
        }
        count++;
      }
    }

    java.util.List<Element> tList = rootElem.getChildren("offsetHours");
    for (Element timeElem : tList) {
      TimeCoord tc = new TimeCoord();
      fmr.times.add(tc);
      tc.id = timeElem.getAttributeValue("id");

      // parse the values
      String values = timeElem.getText();
      StringTokenizer stoke = new StringTokenizer(values);
      int n = stoke.countTokens();
      tc.offset = new double[n];
      int count = 0;
      while (stoke.hasMoreTokens()) {
        tc.offset[count++] = Double.parseDouble(stoke.nextToken());
      }

      //get the variable names
      List<Element> varList = timeElem.getChildren("variable");
      for (Element vElem : varList) {
        Grid grid = new Grid(vElem.getAttributeValue("name"));
        grid.ec = fmr.getEnsCoordinate(vElem.getAttributeValue("ens_id"));
        grid.vc = fmr.getVertCoordinate(vElem.getAttributeValue("vert_id"));
        tc.vars.add(grid);
        grid.parent = tc;

        List<Element> mList = vElem.getChildren("missing");
        for (Element mElem : mList) {
          grid.missing = new ArrayList<Missing>();

          // parse the values
          values = mElem.getText();
          stoke = new StringTokenizer(values, " ,");
          while (stoke.hasMoreTokens()) {
            int timeIdx = Integer.parseInt(stoke.nextToken());
            int ensIdx = 0;
            if (grid.ec != null )
               ensIdx = Integer.parseInt(stoke.nextToken());
            int vertIdx = 0;
            if (grid.vc != null )
              vertIdx = Integer.parseInt(stoke.nextToken());
            grid.missing.add(new Missing(timeIdx, ensIdx, vertIdx));
          }
        }
      }
    }

    // add lat/lon bounding box
    Element bbElem = rootElem.getChild("horizBB");
    if (bbElem != null) {
      double west = Double.parseDouble(bbElem.getAttributeValue("west"));
      double east = Double.parseDouble(bbElem.getAttributeValue("east"));
      double north = Double.parseDouble(bbElem.getAttributeValue("north"));
      double south = Double.parseDouble(bbElem.getAttributeValue("south"));
      fmr.bb = new LatLonRect(new LatLonPointImpl(south, west), new LatLonPointImpl(north, east));
    }

    return fmr;
  }


  /**
   * Open a GridDataset and construct a ForecastModelRun.
   * The information is serialized into am XML file at ncfileLocation.fmrInv.xml, and used if it exists.
   *
   * @param cache          use this cache to look for fmrInv.xml files (may be null)
   * @param ncfileLocation location of the grid dataset.
   * @param mode           one of OPEN_NORMAL, OPEN_FORCE_NEW, OPEN_XML_ONLY constants
   * @param isFile         if its a file: new File( ncfileLocation) makes sense, so we can check if its changed
   * @return ForecastModelRun
   * @throws IOException on io error
   */
  public static ForecastModelRunInventory open(ucar.nc2.util.DiskCache2 cache, String ncfileLocation, int mode, boolean isFile) throws IOException {
    boolean force = (mode == OPEN_FORCE_NEW); // always write a new one
    boolean xml_only = (mode == OPEN_XML_ONLY); // never write a new one

    // do we already have a fmrInv file?
    String summaryFileLocation = ncfileLocation + ".fmrInv.xml";
    File summaryFile = new File(summaryFileLocation);
    if (!summaryFile.exists()) {
      if (null != cache) { // look for it in the  cache
        summaryFile = cache.getCacheFile(summaryFileLocation);
        summaryFileLocation = summaryFile.getPath();
      }
    }
    boolean haveOne = (summaryFile != null) && (summaryFile.exists());

    if (xml_only && !haveOne) return null;

    // use it if it exists
    if (!force && haveOne) {

      if (isFile) { // see if its changed
        File ncdFile = new File(ncfileLocation);
        if (!ncdFile.exists())
          throw new IllegalArgumentException("Data File must exist = " + ncfileLocation);

        if (xml_only || (summaryFile.lastModified() >= ncdFile.lastModified())) {
          try {  // hasnt changed - use it
            return readXML(summaryFileLocation);
          } catch (Exception ee) {
            log.error("Failed to read FmrcInventory " + summaryFileLocation, ee);
            // fall through to recreating it
          }
        }
        // fall through to recreating it

      } else {  // not a file, just use it

        try {
          return readXML(summaryFileLocation);
        } catch (Exception ee) {
          log.error("Failed to read FmrcInventory " + summaryFileLocation, ee);
          // fall through to recreating it
        }
      }
    }

    // otherwise, try to make it

    /* try {
      if (null != cache) {
        summaryFile = cache.getCacheFile(summaryFileLocation);
        summaryFileLocation = summaryFile.getPath();
      } else {
        summaryFile = new File(summaryFileLocation);
        if (summaryFile.createNewFile()) {
          summaryFile.delete();
        } else {
          summaryFile = null;
        }
      }
    } catch (Throwable t) {
      summaryFileLocation = null;
    } */

    if (debug) System.out.println(" read from dataset " + ncfileLocation + " write to XML " + summaryFileLocation);
    ucar.nc2.dt.grid.GridDataset gds = null;
    ForecastModelRunInventory fmr = null;
    try {
      gds = ucar.nc2.dt.grid.GridDataset.open(ncfileLocation);
      fmr = new ForecastModelRunInventory(gds, null);
    } catch (IOException ioe) {
      if (debug)
        ioe.printStackTrace();
       return null;
    }  finally {
      if (gds != null) gds.close();
    }

    // try to write it for future reference
    if (summaryFileLocation != null) {
      try {
        fmr.writeXML(summaryFileLocation);
      } catch (Throwable t) {
        log.error("Failed to write FmrcInventory to " + summaryFileLocation, t);
      }
    }

    if (showXML)
      IO.copyFile(summaryFileLocation, System.out);

    fmr.releaseDataset();
    return fmr;
  }

  public static ForecastModelRunInventory open(ucar.nc2.dt.GridDataset gds, Date runDate) {
    return new ForecastModelRunInventory(gds, runDate);
  }


  private static boolean debug = false, showXML = false;

  public static void main2(String args[]) throws Exception {
    //String def = "C:/data/grib/nam/c20s/NAM_CONUS_20km_surface_20060316_1800.grib1";
    // String def = "C:/data/radarMosaic/RADAR_10km_mosaic_20060807_2220.grib1";
    String def = "R:/testdata/motherlode/grid/NAM_CONUS_80km_20060728_1200.grib1";
    String datasetName = (args.length < 1) ? def : args[0];
    // ucar.nc2.util.DiskCache2 cache = new ucar.nc2.util.DiskCache2("C:/data/grib", false, -1, -1);
    // cache.setCachePathPolicy(DiskCache2.CACHEPATH_POLICY_NESTED_TRUNCATE, "RUC");
    ForecastModelRunInventory fmr = open(null, datasetName, OPEN_FORCE_NEW, true);
    fmr.writeXML(System.out);
  }

  public static void main(String args[]) throws IOException {
    if (args.length == 1) {
      ForecastModelRunInventory.open(null, args[0], ForecastModelRunInventory.OPEN_FORCE_NEW, true);
      ForecastModelRunInventory.readXML( args[0] +".fmrInv.xml" );
      return;
    }
    DiskCache2 cache =  new DiskCache2("fmrcInventory/", true, 5 * 24 * 3600, 3600);
    String url = "http://motherlode.ucar.edu:9080/thredds/dodsC/fmrc/NCEP/NAM/CONUS_12km/files/NAM_CONUS_12km_20070419_1800.grib2";
    ForecastModelRunInventory fmr = ForecastModelRunInventory.open(cache, url, ForecastModelRunInventory.OPEN_NORMAL, false);
    fmr.writeXML(System.out);
  }

}
