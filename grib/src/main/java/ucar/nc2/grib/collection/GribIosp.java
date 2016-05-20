/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import org.jdom2.Element;

import thredds.client.catalog.Catalog;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.coord.Coordinate;
import ucar.coord.CoordinateEns;
import ucar.coord.CoordinateRuntime;
import ucar.coord.CoordinateTime;
import ucar.coord.CoordinateTime2D;
import ucar.coord.CoordinateTimeAbstract;
import ucar.coord.CoordinateTimeIntv;
import ucar.coord.CoordinateVert;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.grib.EnsCoord;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribStatType;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.time.Calendar;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.projection.RotatedPole;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

/**
 * Grib Collection IOSP, version 2.
 * Handles both collections and single GRIB files.
 * Immutable after open() is called.
 *
 * @author caron
 * @since 4/6/11
 */
public abstract class GribIosp extends AbstractIOServiceProvider {
  static public final String VARIABLE_ID_ATTNAME = "Grib_Variable_Id";
  static public final String GRIB_VALID_TIME = "GRIB forecast or observation time";

  // do not use
  static public boolean debugRead = false;
  static public int debugIndexOnlyCount = 0;  // count number of data accesses
  static boolean debugIndexOnlyShow = false;  // debugIndexOnly must be true; show record fetch
  static boolean debugIndexOnly = false;      // we are running with only ncx index files, no data
  static public boolean debugGbxIndexOnly = false;  // we are running with only ncx and gbx index files, no data

  static private final boolean debug = false, debugTime = false, debugName = false;

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugRead = debugFlag.isSet("Grib/showRead");
    debugIndexOnly = debugFlag.isSet("Grib/indexOnly");
    debugIndexOnlyShow = debugFlag.isSet("Grib/indexOnlyShow");
    debugGbxIndexOnly = debugFlag.isSet("Grib/debugGbxIndexOnly");
  }

  ///////////////////////////////////////////////////////////////////////////////////

  // store custom tables in here
  protected FeatureCollectionConfig config = new FeatureCollectionConfig();

  public void setParamTable(Element paramTable) {
    config.gribConfig.paramTable = paramTable;
  }

  public void setLookupTablePath(String lookupTablePath) {
    config.gribConfig.lookupTablePath = lookupTablePath;
  }

  public void setParamTablePath(String paramTablePath) {
    config.gribConfig.paramTablePath = paramTablePath;
  }

  @Override
  public Object sendIospMessage(Object special) {
    if (special instanceof String) {
      String s = (String) special;
      if (s.startsWith("gribParameterTableLookup")) {
        int pos = s.indexOf("=");
        if (pos > 0)
          config.gribConfig.lookupTablePath = s.substring(pos + 1).trim();

      } else if (s.startsWith("gribParameterTable")) {
        int pos = s.indexOf("=");
        if (pos > 0)
          config.gribConfig.paramTablePath = s.substring(pos + 1).trim();
      }

      if (debug) System.out.printf("GRIB got IOSP message=%s%n", special);
      return null;
    }

    if (special instanceof org.jdom2.Element) {  // the root element will be <iospParam>
      Element root = (org.jdom2.Element) special;
      config.gribConfig.configFromXml(root, Catalog.ncmlNS);
      return null;
    }

    return super.sendIospMessage(special);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected final boolean isGrib1;
  protected final org.slf4j.Logger logger;

  protected GribCollectionImmutable gribCollection;
  protected GribCollectionImmutable.GroupGC gHcs;
  protected GribCollectionImmutable.Type gtype; // only used if gHcs was set
  protected boolean isPartitioned;
  protected boolean owned; // if Iosp is owned by GribCollection; affects close() LOOK get rid of this
  protected ucar.nc2.grib.GribTables gribTable;

  public GribIosp(boolean isGrib1, org.slf4j.Logger logger) {
    this.isGrib1 = isGrib1;
    this.logger = logger;
  }

  protected abstract ucar.nc2.grib.GribTables createCustomizer() throws IOException;

  protected abstract String makeVariableName(GribCollectionImmutable.VariableIndex vindex);

  protected abstract String makeVariableLongName(GribCollectionImmutable.VariableIndex vindex);

  protected abstract String makeVariableNameFromRecord(GribCollectionImmutable.VariableIndex vindex);

  protected abstract String makeVariableUnits(GribCollectionImmutable.VariableIndex vindex);

  protected abstract String getVerticalCoordDesc(int vc_code);

  protected abstract GribTables.Parameter getParameter(GribCollectionImmutable.VariableIndex vindex);

  protected abstract void addVariableAttributes(Variable v, GribCollectionImmutable.VariableIndex vindex);

  protected abstract void show(RandomAccessFile rafData, long pos) throws IOException;

  protected abstract float[] readData(RandomAccessFile rafData, DataRecord dr) throws IOException;

  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    if (gHcs != null) { // just use the one group that was set in the constructor
      this.gribCollection = gHcs.getGribCollection();
      if (this.gribCollection instanceof PartitionCollectionImmutable)
        isPartitioned = true;
      gribTable = createCustomizer();

      addGroup(ncfile, ncfile.getRootGroup(), gHcs, gtype, false);

    } else if (gribCollection == null) { // may have been set in the constructor

      this.gribCollection = GribCdmIndex.openGribCollectionFromRaf(raf, config, CollectionUpdateType.testIndexOnly, logger);
      if (gribCollection == null)
        throw new IllegalStateException("Not a GRIB data file or index file " + raf.getLocation());

      isPartitioned = (this.gribCollection instanceof PartitionCollectionImmutable);
      gribTable = createCustomizer();

      boolean useDatasetGroup = gribCollection.getDatasets().size() > 1;
      for (GribCollectionImmutable.Dataset ds : gribCollection.getDatasets()) {
        Group topGroup;
        if (useDatasetGroup) {
          topGroup = new Group(ncfile, null, ds.getType().toString());
          ncfile.addGroup(null, topGroup);
        } else {
          topGroup = ncfile.getRootGroup();
        }

        Iterable<GribCollectionImmutable.GroupGC> groups = ds.getGroups();
        boolean useGroups = ds.getGroupsSize() > 1;
        for (GribCollectionImmutable.GroupGC g : groups)
          addGroup(ncfile, topGroup, g, ds.getType(), useGroups);
      }
    }

    for (Attribute att : gribCollection.getGlobalAttributes())
      ncfile.addAttribute(null, att);
  }

  private void addGroup(NetcdfFile ncfile, Group parent, GribCollectionImmutable.GroupGC group, GribCollectionImmutable.Type gctype, boolean useGroups) {

    Group g;
    if (useGroups) {
      g = new Group(ncfile, parent, group.getId());
      g.addAttribute(new Attribute(CDM.LONG_NAME, group.getDescription()));
      try {
        ncfile.addGroup(parent, g);
      } catch (Exception e) {
        logger.warn("Duplicate Group - skipping");
        return;
      }
    } else {
      g = parent;
    }

    makeGroup(ncfile, g, group, gctype);
  }

  private void makeGroup(NetcdfFile ncfile, Group g, GribCollectionImmutable.GroupGC group, GribCollectionImmutable.Type gctype) {
    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();
    String grid_mapping = hcs.getName() + "_Projection";

    String horizDims;

    boolean isRotatedLatLon = !isGrib1 && hcs.proj instanceof RotatedPole;
    boolean isLatLon2D = !isGrib1 && Grib2Utils.isLatLon2D(hcs.template, gribCollection.getCenter());
    boolean isLatLon = isGrib1 ? hcs.isLatLon() : Grib2Utils.isLatLon(hcs.template, gribCollection.getCenter());

    if (isRotatedLatLon) {
      Variable hcsV = ncfile.addVariable(g, new Variable(ncfile, g, null, grid_mapping, DataType.INT, ""));
      hcsV.setCachedData(Array.factory(DataType.INT, new int[0], new int[] { 0 }));
      for (Parameter p : hcs.proj.getProjectionParameters()) {
        hcsV.addAttribute(new Attribute(p));
      }
      horizDims = "rlat rlon";
      ncfile.addDimension(g, new Dimension("rlat", hcs.ny));
      ncfile.addDimension(g, new Dimension("rlon", hcs.nx));
      Variable rlat = ncfile.addVariable(g, new Variable(ncfile, g, null, "rlat", DataType.FLOAT, "rlat"));
      rlat.addAttribute(new Attribute(CF.STANDARD_NAME, CF.GRID_LATITUDE));
      rlat.addAttribute(new Attribute(CDM.UNITS, CDM.RLATLON_UNITS));
      rlat.setCachedData(Array.makeArray(DataType.FLOAT, hcs.ny, hcs.starty, hcs.dy));
      Variable rlon = ncfile.addVariable(g, new Variable(ncfile, g, null, "rlon", DataType.FLOAT, "rlon"));
      rlon.addAttribute(new Attribute(CF.STANDARD_NAME, CF.GRID_LONGITUDE));
      rlon.addAttribute(new Attribute(CDM.UNITS, CDM.RLATLON_UNITS));
      rlon.setCachedData(Array.makeArray(DataType.FLOAT, hcs.nx, hcs.startx, hcs.dx));
    } else if (isLatLon2D) { // CurvilinearOrthogonal - lat and lon fields must be present in the file
      horizDims = "lat lon";

      // LOOK - assume same number of points for all grids
      ncfile.addDimension(g, new Dimension("lon", hcs.nx));
      ncfile.addDimension(g, new Dimension("lat", hcs.ny));

    } else if (isLatLon) {
      // make horiz coordsys coordinate variable
      Variable hcsV = ncfile.addVariable(g, new Variable(ncfile, g, null, grid_mapping, DataType.INT, ""));
      hcsV.setCachedData(Array.factory(DataType.INT, new int[0], new int[]{0}));
      for (Parameter p : hcs.proj.getProjectionParameters())
        hcsV.addAttribute(new Attribute(p));

      horizDims = "lat lon";
      ncfile.addDimension(g, new Dimension("lon", hcs.nx));
      ncfile.addDimension(g, new Dimension("lat", hcs.ny));

      Variable cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "lat", DataType.FLOAT, "lat"));
      cv.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
      if (hcs.getGaussianLats() != null)
        cv.setCachedData(hcs.getGaussianLats());
      else
        cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.ny, hcs.starty, hcs.dy));

      cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "lon", DataType.FLOAT, "lon"));
      cv.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.nx, hcs.startx, hcs.dx));

    } else {
      // make horiz coordsys coordinate variable
      Variable hcsV = ncfile.addVariable(g, new Variable(ncfile, g, null, grid_mapping, DataType.INT, ""));
      hcsV.setCachedData(Array.factory(DataType.INT, new int[0], new int[]{0}));
      for (Parameter p : hcs.proj.getProjectionParameters())
        hcsV.addAttribute(new Attribute(p));

      horizDims = "y x";
      ncfile.addDimension(g, new Dimension("x", hcs.nx));
      ncfile.addDimension(g, new Dimension("y", hcs.ny));

      Variable cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "x", DataType.FLOAT, "x"));
      cv.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_X_COORDINATE));
      cv.addAttribute(new Attribute(CDM.UNITS, "km"));
      cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.nx, hcs.startx, hcs.dx));

      cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "y", DataType.FLOAT, "y"));
      cv.addAttribute(new Attribute(CF.STANDARD_NAME, CF.PROJECTION_Y_COORDINATE));
      cv.addAttribute(new Attribute(CDM.UNITS, "km"));
      cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.ny, hcs.starty, hcs.dy));
    }

    boolean is1Dtime = (gctype == GribCollectionImmutable.Type.MRSTC) || (gctype == GribCollectionImmutable.Type.TP);
    for (Coordinate coord : group.coords) {
      Coordinate.Type ctype = coord.getType();
      switch (ctype) {
        case runtime:
          if (!is1Dtime)
            makeRuntimeCoordinate(ncfile, g, (CoordinateRuntime) coord);
          break;
        case timeIntv:
          //if (is2Dtime) makeTimeCoordinate2D(ncfile, g, coord, runtime);
          makeTimeCoordinate1D(ncfile, g, (CoordinateTimeIntv) coord);
          break;
        case time:
          //if (is2Dtime) makeTimeCoordinate2D(ncfile, g, coord, runtime);
          makeTimeCoordinate1D(ncfile, g, (CoordinateTime) coord);
          break;
        case vert:
          makeVerticalCoordinate(ncfile, g, (CoordinateVert) coord);
          break;
        case ens:
          makeEnsembleCoordinate(ncfile, g, (CoordinateEns) coord);
          break;
        case time2D:
          makeTimeCoordinate2D(ncfile, g, (CoordinateTime2D) coord, gctype);
          break;
      }
    }

    /* int ccount = 0;
    for (EnsCoord ec : gHcs.ensCoords) {
      int n = ec.getSize();
      String ecName = "ens" + ccount;
      ncfile.addDimension(g, new Dimension(ecName, n));
      Variable v = new Variable(ncfile, g, null, ecName, DataType.INT, ecName);
      ncfile.addVariable(g, v);
      ccount++;
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Ensemble.toString()));

      int[] data = new int[n];
      int count = 0;
      for (EnsCoord.Coord ecc : ec.getCoords())
        data[count++] = ecc.getEnsMember();
      v.setCachedData(Array.factory(DataType.INT, new int[]{n}, data));
    } */

    for (GribCollectionImmutable.VariableIndex vindex : group.variList) {
      Formatter dimNames = new Formatter();
      Formatter coordinateAtt = new Formatter();

      // do the times first
      Coordinate run = vindex.getCoordinate(Coordinate.Type.runtime);
      Coordinate time = vindex.getCoordinateTime();
      boolean isRunScaler = (run != null) && run.getSize() == 1;

      switch (gctype) {
        case GC:
        case SRC:     // GC: Single Runtime Collection                          [ntimes]           (run, 2D)  scalar runtime
          assert isRunScaler;
          dimNames.format("%s ", time.getName());
          coordinateAtt.format("%s %s ", run.getName(), time.getName());
          break;

        case MRSTC:             // GC: Multiple Runtime Single Time Collection  [nruns, 1]
        case TP:                // PC: Multiple Runtime Single Time Partition   [nruns, 1]         (run, 2D)  ignore the run, its generated from the 2D in
          dimNames.format("%s ", time.getName());
          coordinateAtt.format("ref%s %s ", time.getName(), time.getName());
          break;

        case MRC:               // GC: Multiple Runtime Collection              [nruns, ntimes]    (run, 2D) use Both
        case TwoD:              // PC: TwoD time partition                      [nruns, ntimes]
          assert run != null : "GRIB MRC or TWOD does not have run coordinate";
          if (isRunScaler)
            dimNames.format("%s ", time.getName());
          else
            dimNames.format("%s %s ", run.getName(), time.getName());
          coordinateAtt.format("%s %s ", run.getName(), time.getName());
          break;

        case Best:              // PC: Best time partition                      [ntimes]          (time)   reftime is generated in makeTimeAuxReference()
        case BestComplete:      // PC: Best complete time partition             [ntimes]
          dimNames.format("%s ", time.getName());
          coordinateAtt.format("ref%s %s ", time.getName(), time.getName());
          break;

        default:
          throw new IllegalStateException("Uknown GribCollection TYpe = "+gctype);
      }

      // do other (vert, ens) coordinates
      for (Coordinate coord : vindex.getCoordinates()) {
        if (coord instanceof CoordinateTimeAbstract || coord instanceof CoordinateRuntime) continue;
        String name = coord.getName().toLowerCase();
        dimNames.format("%s ", name);
        coordinateAtt.format("%s ", name);
      }
      // do horiz coordinates
      dimNames.format("%s", horizDims);
      coordinateAtt.format("%s ", horizDims);

      String vname = makeVariableName(vindex);
      Variable v = new Variable(ncfile, g, null, vname, DataType.FLOAT, dimNames.toString());
      ncfile.addVariable(g, v);
      if (debugName) System.out.printf("added %s%n", vname);

      String desc = makeVariableLongName(vindex);
      v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
      v.addAttribute(new Attribute(CDM.UNITS, makeVariableUnits(vindex)));

      GribTables.Parameter gp = getParameter(vindex);
      if (gp != null) {
        if (gp.getDescription() != null)
          v.addAttribute(new Attribute(CDM.DESCRIPTION, gp.getDescription()));
        if (gp.getAbbrev() != null)
          v.addAttribute(new Attribute(CDM.ABBREV, gp.getAbbrev()));
        v.addAttribute(new Attribute(CDM.MISSING_VALUE, gp.getMissing()));
        if (gp.getFill() != null)
          v.addAttribute(new Attribute(CDM.FILL_VALUE, gp.getFill()));
      } else {
        v.addAttribute(new Attribute(CDM.MISSING_VALUE, Float.NaN));
      }

      // horiz coord system
      if (isLatLon2D) {
        String s = searchCoord(Grib2Utils.getLatLon2DcoordType(desc), group.variList);
        if (s == null) { // its a lat/lon coordinate
          v.setDimensions(horizDims); // LOOK make this 2D and munge the units
          String units = desc.contains("Latitude of") ? CDM.LAT_UNITS : CDM.LON_UNITS;
          v.addAttribute(new Attribute(CDM.UNITS, units));

        } else {
          coordinateAtt.format("%s ", s);
        }
      } else {
        v.addAttribute(new Attribute(CF.GRID_MAPPING, grid_mapping));
      }
      v.addAttribute(new Attribute(CF.COORDINATES, coordinateAtt.toString()));

      // statistical interval type
      if (vindex.getIntvType() >= 0) {
        GribStatType statType = gribTable.getStatType(vindex.getIntvType());   // LOOK find the time coordinate
        if (statType != null) {
          v.addAttribute(new Attribute("Grib_Statistical_Interval_Type", statType.toString()));
          CF.CellMethods cm = GribStatType.getCFCellMethod(statType);
          Coordinate timeCoord = vindex.getCoordinate(Coordinate.Type.timeIntv);
          if (cm != null && timeCoord != null)
            v.addAttribute(new Attribute(CF.CELL_METHODS, timeCoord.getName() + ": " + cm.toString()));
        } else {
          v.addAttribute(new Attribute("Grib_Statistical_Interval_Type", vindex.getIntvType()));
        }
      }

      addVariableAttributes(v, vindex);

      v.setSPobject(vindex);
    }
  }

  private void makeRuntimeCoordinate(NetcdfFile ncfile, Group g, CoordinateRuntime rtc) {
    int n = rtc.getSize();
    boolean isScalar = (n == 1);      // this is the case of runtime[1]
    String tcName = rtc.getName();
    String dims = isScalar ? null : rtc.getName();  // null means scalar
    if (!isScalar)
      ncfile.addDimension(g, new Dimension(tcName, n));

    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    v.addAttribute(new Attribute(CDM.UNITS, rtc.getUnit()));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    v.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    /* String vsName = tcName + "_ISO";
    Variable vs = ncfile.addVariable(g, new Variable(ncfile, g, null, vsName, DataType.STRING, dims));
    vs.addAttribute(new Attribute(CDM.UNITS, "ISO8601"));
    v.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    // coordinate values
    String[] dataS = new String[n];
    int count = 0;
    for (CalendarDate val : rtc.getRuntimesSorted()) {
      dataS[count++] = val.toString();
    }
    vs.setCachedData(Array.factory(DataType.STRING, isScalar ? new int[0] : new int[]{n}, dataS)); */

    // lazy eval
    v.setSPobject(new Time2Dinfo(Time2DinfoType.reftime, null, rtc));

  }

  static private enum  Time2DinfoType {off, intv, bounds, is1Dtime, reftime, timeAuxRef}
  static private class Time2Dinfo {
    Time2DinfoType which;
    CoordinateTime2D time2D;
    Coordinate time1D;

    private Time2Dinfo(Time2DinfoType which, CoordinateTime2D time2D, Coordinate time1D) {
      this.which = which;
      this.time2D = time2D;
      this.time1D = time1D;
    }
  }

  /*
  1) time(1, ntimes) -> time(ntimes) with scalar reftime coordinate
  2) time(nruns, 1)  -> time(nruns) with reftime(nruns) auxilary coordinate
  3) time(nruns, ntimes) with reftime(nruns)
   */
  private void makeTimeCoordinate2D(NetcdfFile ncfile, Group g, CoordinateTime2D time2D, GribCollectionImmutable.Type gctype) {
    CoordinateRuntime runtime = time2D.getRuntimeCoordinate();

    int nruns = time2D.getNruns();
    int ntimes = time2D.getNtimes();
    String tcName = time2D.getName();
    boolean is1Dtime = (gctype == GribCollectionImmutable.Type.MRSTC) || (gctype == GribCollectionImmutable.Type.TP);
    boolean hasOneRun = (nruns == 1);  // dont use reftime dimension if time(1, ntimes) or time(nruns, 1))
    String dims = hasOneRun || is1Dtime ? tcName : runtime.getName() + " " + tcName;
    int dimLength = is1Dtime ? nruns : ntimes;

    ncfile.addDimension(g, new Dimension(tcName, dimLength));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = runtime.getUnit(); // + " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    // the data is not generated until asked for to same space
    if (!time2D.isTimeInterval()) {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.off, time2D, null));
    } else {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.intv, time2D, null));
      // bounds for intervals
      String bounds_name = tcName + "_bounds";
      Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, dims + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
      bounds.addAttribute(new Attribute(CDM.UNITS, units));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));
      bounds.setSPobject(new Time2Dinfo(Time2DinfoType.bounds, time2D, null));
    }

    // for this case we have to generate a separate reftime, because have to use the same dimension
    if (is1Dtime) {
      String refName = "ref"+tcName;
      Variable vref = ncfile.addVariable(g, new Variable(ncfile, g, null, refName, DataType.DOUBLE, tcName));
      vref.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
      vref.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));
      vref.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));
      vref.addAttribute(new Attribute(CDM.UNITS, units));
      vref.setSPobject(new Time2Dinfo(Time2DinfoType.is1Dtime, time2D, null));
    }
  }

  private Array makeLazyCoordinateData(Variable v2, Time2Dinfo info) {
    if (info.time2D != null)
      return makeLazyTime2Darray(v2, info);
    else
      return makeLazyTime1Darray(v2, info);
  }

    // only for the 2d times
  private Array makeLazyTime1Darray(Variable v2, Time2Dinfo info) {
    int length =  info.time1D.getSize();
    double[] data = new double[length];
    for (int i = 0; i < length; i++) data[i] = Double.NaN;

    // coordinate values
    switch (info.which) {
      case reftime:
        CoordinateRuntime rtc = (CoordinateRuntime) info.time1D;
        int count = 0;
        for (double val : rtc.getOffsetsInTimeUnits())
          data[count++] = val;
        return Array.factory(DataType.DOUBLE, v2.getShape(), data);

      case timeAuxRef:
        CoordinateTimeAbstract time = (CoordinateTimeAbstract) info.time1D;
        count = 0;
        List<Double> masterOffsets = gribCollection.getMasterRuntime().getOffsetsInTimeUnits();
        for (int masterIdx : time.getTime2runtime()) {
          data[count++] = masterOffsets.get(masterIdx-1);
        }
        return Array.factory(DataType.DOUBLE, v2.getShape(), data);
    }
    return null;
  }


  // only for the 2d times
  private Array makeLazyTime2Darray(Variable v2, Time2Dinfo info) {
    CoordinateTime2D time2D = info.time2D;

    int nruns = time2D.getNruns();
    int ntimes = time2D.getNtimes();
    int length =  nruns * ntimes;
    if (info.which == Time2DinfoType.bounds) length *= 2;

    double[] data = new double[length];
    for (int i = 0; i < length; i++) data[i] = Double.NaN;

    // coordinate values
    switch (info.which) {
      case off:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTime coordTime = (CoordinateTime) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (int val : coordTime.getOffsetSorted()) {
            data[runIdx * ntimes + timeIdx] = val + time2D.getOffset(runIdx);
            timeIdx++;
          }
        }
        return Array.factory(DataType.DOUBLE, v2.getShape(), data);

      case intv:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (TimeCoord.Tinv tinv : timeIntv.getTimeIntervals()) {
            data[runIdx * ntimes + timeIdx] = tinv.getBounds2() + time2D.getOffset(runIdx); // use upper bounds for coord value
            timeIdx++;
          }
        }
        return Array.factory(DataType.DOUBLE,  v2.getShape(), data);

      case is1Dtime:
        CoordinateRuntime runtime = time2D.getRuntimeCoordinate();
        int count = 0;
        for (double val : runtime.getOffsetsInTimeUnits()) { // convert to udunits
          data[count++] = val;
        }
        return Array.factory(DataType.DOUBLE,  v2.getShape(), data);

      case bounds:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (TimeCoord.Tinv tinv : timeIntv.getTimeIntervals()) {
            data[runIdx * ntimes * 2 + timeIdx] = tinv.getBounds1() + time2D.getOffset(runIdx);
            data[runIdx * ntimes * 2 + timeIdx + 1] = tinv.getBounds2() + time2D.getOffset(runIdx);
            timeIdx += 2;
          }
        }
        return Array.factory(DataType.DOUBLE, v2.getShape(), data);

      default:
        throw new IllegalStateException();
    }
  }

  /* private void makeTimeCoordinate2D(NetcdfFile ncfile, Group g, Coordinate tc, CoordinateRuntime runtime) {
    int nruns = runtime.getSize();
    int ntimes = tc.getSize();
    String tcName = tc.getName();
    String dims = runtime.getName() + " " + tc.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = tc.getUnit() + " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    double[] data = new double[nruns * ntimes];
    int count = 0;

    // coordinate values
    if (tc instanceof CoordinateTime) {
      CoordinateTime coordTime = (CoordinateTime) tc;
      CalendarPeriod period = coordTime.getPeriod();
      for (CalendarDate cd : runtime.getRuntimesSorted()) {
        double offset = period.getOffset(runtime.getFirstDate(), cd);
        for (int val : coordTime.getOffsetSorted())
          data[count++] = offset + val;
      }
      v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{nruns, ntimes}, data));

    } else if (tc instanceof CoordinateTimeIntv) {
      CoordinateTimeIntv coordTime = (CoordinateTimeIntv) tc;
      CalendarPeriod period = coordTime.getPeriod();

      // use upper bounds for coord value
      for (CalendarDate cd : runtime.getRuntimesSorted()) {
        double offset = period.getOffset(runtime.getFirstDate(), cd);
        for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals())
          data[count++] = offset + tinv.getBounds2();
      }
      v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{nruns, ntimes}, data));

      // bounds
      /* String intvName = getIntervalName(tc.getCode());
      if (intvName != null)
        v.addAttribute(new Attribute(CDM.LONG_NAME, intvName));
      String bounds_name = tcName + "_bounds";

      Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, dims + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
      bounds.addAttribute(new Attribute(CDM.UNITS, units));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

      data = new double[nruns * ntimes * 2];
      count = 0;
      for (CalendarDate cd : runtime.getRuntimesSorted()) {
        double offset = period.getOffset(runtime.getFirstDate(), cd);
        for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals()) {
          data[count++] = offset + tinv.getBounds1();
          data[count++] = offset + tinv.getBounds2();
        }
      }
      bounds.setCachedData(Array.factory(DataType.DOUBLE, new int[]{nruns, ntimes, 2}, data));
    }
  }  */

  private void makeTimeCoordinate1D(NetcdfFile ncfile, Group g, CoordinateTime coordTime) { //}, CoordinateRuntime runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = coordTime.getUnit() + " since " + coordTime.getRefDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    double[] data = new double[ntimes];
    int count = 0;

    // coordinate values
    for (int val : coordTime.getOffsetSorted())
      data[count++] = val;
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes}, data));

    makeTimeAuxReference(ncfile, g, tcName, units, coordTime);
  }

  private void makeTimeAuxReference(NetcdfFile ncfile, Group g, String timeName, String units, CoordinateTimeAbstract time) {
    if (time.getTime2runtime() == null) return;
    String tcName = "ref"+timeName;
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, timeName));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    v.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));
    v.addAttribute(new Attribute(CDM.UNITS, units));

    // lazy evaluation
    v.setSPobject(new Time2Dinfo(Time2DinfoType.timeAuxRef, null, time));
  }

  private void makeTimeCoordinate1D(NetcdfFile ncfile, Group g, CoordinateTimeIntv coordTime) { // }, CoordinateRuntime runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = coordTime.getUnit() + " since " + coordTime.getRefDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    double[] data = new double[ntimes];
    int count = 0;

    // CalendarPeriod period = coordTime.getPeriod();

    // use upper bounds for coord value
    for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals())
      data[count++] = tinv.getBounds2();
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes}, data));

    // bounds
    /* String intvName = getIntervalName(coordTime.getCode());
    if (intvName != null)
      v.addAttribute(new Attribute(CDM.LONG_NAME, intvName));  */
    String bounds_name = tcName + "_bounds";
    Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, dims + " 2"));
    v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
    bounds.addAttribute(new Attribute(CDM.UNITS, units));
    bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

    data = new double[ntimes * 2];
    count = 0;
    for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals()) {
      data[count++] = tinv.getBounds1();
      data[count++] = tinv.getBounds2();
    }
    bounds.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes, 2}, data));

    makeTimeAuxReference(ncfile, g, tcName, units, coordTime);
  }

  private void makeVerticalCoordinate(NetcdfFile ncfile, Group g, CoordinateVert vc) {
    int n = vc.getSize();
    String vcName = vc.getName().toLowerCase();

    ncfile.addDimension(g, new Dimension(vcName, n));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, vcName, DataType.FLOAT, vcName));
    if (vc.getUnit() != null) {
      v.addAttribute(new Attribute(CDM.UNITS, vc.getUnit()));
      String desc = getVerticalCoordDesc(vc.getCode());
      if (desc != null) v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
      v.addAttribute(new Attribute(CF.POSITIVE, vc.isPositiveUp() ? CF.POSITIVE_UP : CF.POSITIVE_DOWN));
    }

    v.addAttribute(new Attribute("Grib_level_type", vc.getCode()));
    VertCoord.VertUnit vu = vc.getVertUnit();
    if (vu != null) {
      if (vu.getDatum() != null)
        v.addAttribute(new Attribute("datum", vu.getDatum()));
    }

    if (vc.isLayer()) {
      float[] data = new float[n];
      int count = 0;
      for (VertCoord.Level val : vc.getLevelSorted())
        data[count++] = (float) (val.getValue1() + val.getValue2()) / 2;
      v.setCachedData(Array.factory(DataType.FLOAT, new int[]{n}, data));

      Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, vcName + "_bounds", DataType.FLOAT, vcName + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, vcName + "_bounds"));
      String vcUnit = vc.getUnit();
      if (vcUnit != null)
        bounds.addAttribute(new Attribute(CDM.UNITS, vcUnit));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + vcName));

      data = new float[2 * n];
      count = 0;
      for (VertCoord.Level level : vc.getLevelSorted()) {
        data[count++] = (float) level.getValue1();
        data[count++] = (float) level.getValue2();
      }
      bounds.setCachedData(Array.factory(DataType.FLOAT, new int[]{n, 2}, data));

    } else {
      float[] data = new float[n];
      int count = 0;
      for (VertCoord.Level val : vc.getLevelSorted())
        data[count++] = (float) val.getValue1();
      v.setCachedData(Array.factory(DataType.FLOAT, new int[]{n}, data));
    }
  }

  private void makeEnsembleCoordinate(NetcdfFile ncfile, Group g, CoordinateEns ec) {
    int n = ec.getSize();
    String ecName = ec.getName().toLowerCase();
    ncfile.addDimension(g, new Dimension(ecName, n));

    Variable v = new Variable(ncfile, g, null, ecName, DataType.INT, ecName);
    ncfile.addVariable(g, v);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Ensemble.toString()));

    int[] data = new int[n];
    int count = 0;
    for (EnsCoord.Coord ecc : ec.getEnsSorted())
      data[count++] = ecc.getEnsMember();
    v.setCachedData(Array.factory(DataType.INT, new int[]{n}, data));
  }


  private String searchCoord(Grib2Utils.LatLonCoordType type, List<GribCollectionImmutable.VariableIndex> list) {
    if (type == null) return null;

    GribCollectionImmutable.VariableIndex lat, lon;
    switch (type) {
      case U:
        lat = searchCoord(list, 198);
        lon = searchCoord(list, 199);
        return (lat != null && lon != null) ? makeVariableName(lat) + " " + makeVariableName(lon) : null;
      case V:
        lat = searchCoord(list, 200);
        lon = searchCoord(list, 201);
        return (lat != null && lon != null) ? makeVariableName(lat) + " " + makeVariableName(lon) : null;
      case P:
        lat = searchCoord(list, 202);
        lon = searchCoord(list, 203);
        return (lat != null && lon != null) ? makeVariableName(lat) + "  " + makeVariableName(lon) : null;
    }
    return null;
  }

  private GribCollectionImmutable.VariableIndex searchCoord(List<GribCollectionImmutable.VariableIndex> list, int p) {
    for (GribCollectionImmutable.VariableIndex vindex : list) {
      if ((vindex.getDiscipline() == 0) && (vindex.getCategory() == 2) && (vindex.getParameter() == p))
        return vindex;
    }
    return null;
  }

  @Override
  public void close() throws java.io.IOException {
    if (!owned && gribCollection != null) // LOOK klugerino
      gribCollection.close();
    gribCollection = null;
    super.close();
  }

  @Override
  public String getDetailInfo() {
    Formatter f = new Formatter();
    f.format("%s", super.getDetailInfo());
    if (gribCollection != null)
      gribCollection.showIndex(f);
    return f.toString();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    long start = System.currentTimeMillis();

    // see if its time2D - then generate data on the fly
    if (v2.getSPobject() instanceof Time2Dinfo) {
      Time2Dinfo info = (Time2Dinfo) v2.getSPobject();
      Array data = makeLazyCoordinateData(v2, info);
      assert data != null;
      Section sectionFilled = Section.fill(section, v2.getShape());
      return data.sectionNoReduce(sectionFilled.getRanges());
    }

    try {
      Array result;
      if (isPartitioned)
        result = readDataFromPartition(v2, section, null);
      else
        result = readDataFromCollection(v2, section, null);

      long took = System.currentTimeMillis() - start;
      if (debugTime) System.out.println("  read data took=" + took + " msec ");
      return result;

    } catch (IOException ioe) {
      logger.error("Failed to readData ", ioe);
      throw ioe;
    }
  }

  // LOOK this is by Variable - might want to do over variables, so only touch a file once, if multiple variables in a file
  @Override
  public long streamToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
          throws java.io.IOException, ucar.ma2.InvalidRangeException {

    long start = System.currentTimeMillis();

    /* if (isPartitioned)
      streamDataFromPartition(v2, section, channel);
    else */
    readDataFromCollection(v2, section, channel);

    long took = System.currentTimeMillis() - start;
    if (debugTime) System.out.println("  read data took=" + took + " msec ");
    return 0;
  }

///////////////////////////////////////////////////////

  private Array readDataFromCollection(Variable v, Section section, WritableByteChannel channel) throws IOException, InvalidRangeException {
    GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) v.getSPobject();

    // first time, read records and keep in memory
    vindex.readRecords();

    int sectionLen = section.getRank();
    Range yRange = section.getRange(sectionLen - 2);
    Range xRange = section.getRange(sectionLen - 1);

    Section sectionWanted = section.subSection(0, sectionLen - 2); // all but x, y
    Section.Iterator iterWanted = sectionWanted.getIterator(v.getShape());
    int[] indexWanted = new int[sectionLen - 2];                              // place to put the iterator result

    // collect all the records that need to be read
    DataReader dataReader = new DataReader(vindex);
    int count = 0;
    while (iterWanted.hasNext()) {
      int sourceIndex = iterWanted.next(indexWanted);
      dataReader.addRecord(sourceIndex, count++);
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver = channel == null ? new DataReceiver(section, yRange, xRange) : new ChannelReceiver(channel, yRange, xRange);
    dataReader.read(dataReceiver);
    return dataReceiver.getArray();
  }

  static class DataRecord implements Comparable<DataRecord> {
    int resultIndex; // index into the result array
    int fileno;
    long dataPos;  // grib1 - start of GRIB record; grib2 - start of drs (data record)
    long bmsPos;  // if non zero, use alternate bms
    int scanMode;
    GdsHorizCoordSys hcs;

    DataRecord(int resultIndex, int fileno, long dataPos, long bmsPos, int scanMode, GdsHorizCoordSys hcs) {
      this.resultIndex = resultIndex;
      this.fileno = fileno;
      this.dataPos = dataPos; // (dataPos == 0) && !isGrib1 ? GribCollection.MISSING_RECORD : dataPos; // 0 also means missing in Grib2
      this.bmsPos = bmsPos;
      this.scanMode = scanMode;
      this.hcs = hcs;
    }

    @Override
    public int compareTo(DataRecord o) {
      int r = Misc.compare(fileno, o.fileno);
      if (r != 0) return r;
      return Misc.compare(dataPos, o.dataPos);
    }

    // debugging
    public void show(GribCollectionImmutable gribCollection) throws IOException {
      String dataFilename = gribCollection.getFilename( fileno);
      System.out.printf(" fileno=%d filename=%s datapos=%d%n", fileno, dataFilename, dataPos);
    }
  }

  protected class DataReader {
    GribCollectionImmutable.VariableIndex vindex;
    List<DataRecord> records = new ArrayList<>();

    private DataReader(GribCollectionImmutable.VariableIndex vindex) {
      this.vindex = vindex;
    }

    void addRecord(int sourceIndex, int resultIndex) {
      GribCollectionImmutable.Record record = vindex.getRecordAt(sourceIndex);
      if (debugRead) {
        System.out.printf("GribIosp debugRead sourceIndex=%d resultIndex=%d record is null=%s%n", sourceIndex, resultIndex, record == null);
      }
      if (record != null)   // LOOK why not just store the Record ??
        records.add(new DataRecord(resultIndex, record.fileno, record.pos, record.bmsPos, record.scanMode, vindex.group.getGdsHorizCoordSys()));
    }

    void read(DataReceiverIF dataReceiver) throws IOException {
      Collections.sort(records);

      int currFile = -1;
      RandomAccessFile rafData = null;
      try {
        for (DataRecord dr : records) {
          if (debugIndexOnly || debugGbxIndexOnly) {
            debugIndexOnlyCount++;
            // if (debugIndexOnlyShow) dr.show(gribCollection);
            dataReceiver.setDataToZero();
            continue;
          }

          if (dr.fileno != currFile) {
            if (rafData != null) rafData.close();
            rafData = gribCollection.getDataRaf(dr.fileno);
            currFile = dr.fileno;
          }

          if (dr.dataPos == GribCollectionMutable.MISSING_RECORD) continue;

          if (debugRead && rafData != null) { // for validation
            show(rafData, dr.dataPos);
          }

          float[] data = readData(rafData, dr);
          GdsHorizCoordSys hcs = vindex.group.getGdsHorizCoordSys();
          dataReceiver.addData(data, dr.resultIndex, hcs.nx);
        }
      } finally {
        if (rafData != null) rafData.close();  // make sure its closed even on exception
      }
    }
  }

  static private interface DataReceiverIF {
    void addData(float[] data, int resultIndex, int nx) throws IOException;
    void setDataToZero();
    Array getArray();
  }

  static private class DataReceiver implements DataReceiverIF {
    private Array dataArray;
    private Range yRange, xRange;
    private int horizSize;

    DataReceiver(Section section, Range yRange, Range xRange) {
      // prefill primitive array efficiently
      int len = (int) section.computeSize();
      float[] data = new float[len];
      for (int i = 0; i < len; i++)
        data[i] = Float.NaN;

      dataArray = Array.factory(DataType.FLOAT, section.getShape(), data);
      this.yRange = yRange;
      this.xRange = xRange;
      this.horizSize = yRange.length() * xRange.length();
    }

    @Override
    public void addData(float[] data, int resultIndex, int nx) throws IOException {
      int start = resultIndex * horizSize;
      int count = 0;
      for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
          int dataIdx = y * nx + x;
          dataArray.setFloat(start + count, data[dataIdx]);
          count++;
        }
      }
    }

    // optimization
    @Override
    public void setDataToZero() {
      float[] data = (float []) dataArray.get1DJavaArray(dataArray.getElementType());
      for (int i = 0; i < data.length; i++)
        data[i] = 0;
    }


    @Override
    public Array getArray() {
      return dataArray;
    }
  }

  private static class ChannelReceiver implements DataReceiverIF {
    private WritableByteChannel channel;
    private DataOutputStream outStream;
    private Range yRange, xRange;

    ChannelReceiver(WritableByteChannel channel, Range yRange, Range xRange) {
      this.channel = channel;
      this.outStream = new DataOutputStream(Channels.newOutputStream(channel));
      this.yRange = yRange;
      this.xRange = xRange;
    }

    @Override
    public void addData(float[] data, int resultIndex, int nx) throws IOException {
      // LOOK: write some ncstream header
      // outStream.write(header);

      // now write the data
      for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
          int dataIdx = y * nx + x;
          outStream.writeFloat(data[dataIdx]);
        }
      }
    }

        // optimization
    @Override
    public void setDataToZero() { }

    @Override
    public Array getArray() {
      return null;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  private Array readDataFromPartition(Variable v, Section section, WritableByteChannel channel) throws IOException, InvalidRangeException {
    PartitionCollectionImmutable.VariableIndexPartitioned vindexP = (PartitionCollectionImmutable.VariableIndexPartitioned) v.getSPobject(); // the variable in the partition collection

    int sectionLen = section.getRank();
    Range yRange = section.getRange(sectionLen - 2);  // last 2
    Range xRange = section.getRange(sectionLen - 1);
    Section sectionWanted = section.subSection(0, sectionLen - 2); // all but x, y
    Section.Iterator iterWanted = sectionWanted.getIterator(v.getShape());  // iterator over wanted indices in vindexP
    int[] indexWanted = new int[sectionLen - 2];                              // place to put the iterator result
    int[] useIndex = indexWanted;

    // collect all the records that need to be read
    DataReaderPartitioned dataReader = new DataReaderPartitioned();
    int resultPos = 0;
    while (iterWanted.hasNext()) {
      iterWanted.next(indexWanted);   // returns the vindexP index in indexWanted array

      // for 1D TP, second index is implictly 0
      if (vindexP.getType() == GribCollectionImmutable.Type.TP) {
        int[] indexReallyWanted = new int[indexWanted.length+1];
        indexReallyWanted[0] = indexWanted[0];
        indexReallyWanted[1] = 0;
        System.arraycopy(indexWanted, 1, indexReallyWanted, 2, indexWanted.length-1);
        useIndex = indexReallyWanted;
      }

      PartitionCollectionImmutable.DataRecord record = vindexP.getDataRecord(useIndex);
      if (record == null) {
        if (debug || debugRead) System.out.printf("readDataFromPartition missing data%n");
        // vindexP.getDataRecord(indexWanted); // debug
        resultPos++;
        continue;
      }
      record.resultIndex = resultPos;
      dataReader.addRecord(record);
      resultPos++;
    }

    // sort by file and position, then read
    DataReceiverIF dataReceiver = channel == null ? new DataReceiver(section, yRange, xRange) : new ChannelReceiver(channel, yRange, xRange);
    dataReader.read(dataReceiver);

    return dataReceiver.getArray();
  }

  private class DataReaderPartitioned {
    List<PartitionCollectionImmutable.DataRecord> records = new ArrayList<>();

    void addRecord(PartitionCollectionImmutable.DataRecord dr) {
      if (dr != null) records.add(dr);
    }

    void read(DataReceiverIF dataReceiver) throws IOException {
      Collections.sort(records);

      PartitionCollectionImmutable.DataRecord lastRecord = null;
      RandomAccessFile rafData = null;
      try {

        for (PartitionCollectionImmutable.DataRecord dr : records) {
          if (debugIndexOnly || debugGbxIndexOnly) {
            debugIndexOnlyCount++;
            if (debugIndexOnlyShow) dr.show();
            dataReceiver.setDataToZero();
            continue;
          }

          if ((rafData == null) || !dr.usesSameFile(lastRecord)) {
            if (rafData != null) rafData.close();
            rafData = dr.usePartition.getRaf(dr.partno, dr.fileno);
          }
          lastRecord = dr;

          if (dr.dataPos == GribCollectionMutable.MISSING_RECORD) continue;

          if (debugRead) { // for validation
            show(rafData, dr.dataPos);
          }

          float[] data = readData(rafData, dr);
          GdsHorizCoordSys hcs = dr.hcs;
          dataReceiver.addData(data, dr.resultIndex, hcs.nx);
        }

      } finally {
        if (rafData != null) rafData.close();  // make sure its closed even on exception
      }
    }
  }

  ///////////////////////////////////////
  // debugging back door
  abstract public Object getLastRecordRead();
  abstract public void clearLastRecordRead();
  abstract public Object getGribCustomizer();

}
