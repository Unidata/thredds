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

import org.jdom2.Element;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.coord.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.time.CalendarPeriod;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.Formatter;

/**
 * Grib-2 Collection IOSP, ver2.
 * Handles both collections and single GRIB files.
 *
 * @author caron
 * @since 4/6/11
 */
public abstract class GribIosp extends AbstractIOServiceProvider {
  static public final String VARIABLE_ID_ATTNAME = "Grib_Variable_Id";
  static public final String GRIB_VALID_TIME = "GRIB forecast or observation time";
  static private final boolean debugTime = false, debugRead = false, debugName = false;

  private static final boolean debug = false;

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
          config.gribConfig.lookupTablePath = s.substring(pos+1).trim();

      } else if (s.startsWith("gribParameterTable")) {
        int pos = s.indexOf("=");
        if (pos > 0)
          config.gribConfig.paramTablePath = s.substring(pos+1).trim();
      }

      if (debug) System.out.printf("GRIB got IOSP message=%s%n", special);
      return null;
    }

    if (special instanceof org.jdom2.Element) {  // the root element will be <iospParam>
      Element root = (org.jdom2.Element) special;
      config.gribConfig.configFromXml(root, NcMLReader.ncNS);
      return null;
    }

    return super.sendIospMessage(special);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected final boolean isGrib1;
  protected final org.slf4j.Logger logger;

  protected GribCollection gribCollection;
  protected GribCollection.GroupGC gHcs;
  protected GribCollection.Type gtype; // only used if gHcs was set
  protected boolean isPartitioned;
  protected boolean owned; // if Iosp is owned by GribCollection; affects close()
  protected ucar.nc2.grib.GribTables gribTable;

  public GribIosp(boolean isGrib1, org.slf4j.Logger logger) {
    this.isGrib1 = isGrib1;
    this.logger = logger;
  }

  protected abstract ucar.nc2.grib.GribTables createCustomizer() throws IOException;
  protected abstract String makeVariableName(GribCollection.VariableIndex vindex);
  protected abstract String makeVariableNameFromRecord(GribCollection.VariableIndex vindex);
  protected abstract String makeVariableLongName(GribCollection.VariableIndex vindex);
  protected abstract String makeVariableUnits(GribCollection.VariableIndex vindex);

  protected abstract String getVerticalCoordDesc(int vc_code);
  protected abstract GribTables.Parameter getParameter(GribCollection.VariableIndex vindex);

  protected abstract void addGlobalAttributes(NetcdfFile ncfile);
  protected abstract void addVariableAttributes(Variable v, GribCollection.VariableIndex vindex);
  protected abstract void show(RandomAccessFile rafData, long pos) throws IOException;

  protected abstract float[] readData(RandomAccessFile rafData, DataRecord dr) throws IOException;

  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    if (gHcs != null) { // just use the one group that was set in the constructor
      this.gribCollection = gHcs.getGribCollection();
      if (this.gribCollection instanceof PartitionCollection)
        isPartitioned = true;
      gribTable = createCustomizer();

      addGroup(ncfile, ncfile.getRootGroup(), gHcs, gtype, false);

    } else if (gribCollection == null) { // may have been set in the constructor

      this.gribCollection = GribCdmIndex.makeGribCollectionFromRaf(raf, config, CollectionUpdateType.test, logger);
      if (gribCollection == null)
        throw new IllegalStateException("Not a GRIB data file or ncx2 file "+raf.getLocation());

      isPartitioned = (this.gribCollection instanceof PartitionCollection);
      gribTable = createCustomizer();

      boolean useDatasetGroup = gribCollection.getDatasets().size() > 1;
      for (GribCollection.Dataset ds : gribCollection.getDatasets()) {
        Group topGroup;
        if (useDatasetGroup) {
          topGroup = new Group(ncfile, null, ds.getType().toString());
          ncfile.addGroup(null, topGroup);
        } else {
          topGroup = ncfile.getRootGroup();
        }

        Iterable<GribCollection.GroupGC> groups = ds.getGroups();
        // Collections.sort(groups);
        boolean useGroups = ds.getGroupsSize() > 1;
        for (GribCollection.GroupGC g : groups)
          addGroup(ncfile, topGroup, g, ds.getType(), useGroups);
      }
    }

    String val = CommonCodeTable.getCenterName(gribCollection.getCenter(), 2);
    ncfile.addAttribute(null, new Attribute(GribUtils.CENTER, val == null ? Integer.toString(gribCollection.getCenter()) : val));
    val = gribTable.getSubCenterName(gribCollection.getCenter(), gribCollection.getSubcenter());
    ncfile.addAttribute(null, new Attribute(GribUtils.SUBCENTER, val == null ? Integer.toString(gribCollection.getSubcenter()) : val));
    ncfile.addAttribute(null, new Attribute(GribUtils.TABLE_VERSION, gribCollection.getMaster() + "," + gribCollection.getLocal())); // LOOK

    addGlobalAttributes(ncfile);

    ncfile.addAttribute(null, new Attribute(CDM.CONVENTIONS, "CF-1.6"));
    ncfile.addAttribute(null, new Attribute(CDM.HISTORY, "Read using CDM IOSP GribCollection v2"));
    ncfile.addAttribute(null, new Attribute(CF.FEATURE_TYPE, FeatureType.GRID.name()));
    ncfile.addAttribute(null, new Attribute(CDM.FILE_FORMAT, getFileTypeId()));

    if (gribCollection.getParams() != null)
      for (Parameter p : gribCollection.getParams())
        ncfile.addAttribute(null, new Attribute(p));
  }

  private void addGroup(NetcdfFile ncfile, Group parent, GribCollection.GroupGC gHcs, GribCollection.Type gctype, boolean useGroups) {

    Group g;
    if (useGroups) {
      g = new Group(ncfile, parent, gHcs.getId());
      g.addAttribute(new Attribute(CDM.LONG_NAME, gHcs.getDescription()));
      try {
        ncfile.addGroup(parent, g);
      } catch (Exception e) {
        logger.warn("Duplicate Group - skipping");
        return;
      }
    } else {
      g = parent;
    }

    makeGroup(ncfile, g, gHcs, gctype);
  }

  private void makeGroup(NetcdfFile ncfile, Group g, GribCollection.GroupGC gHcs, GribCollection.Type gctype) {
    GdsHorizCoordSys hcs = gHcs.getGdsHorizCoordSys();
    String grid_mapping = hcs.getName()+"_Projection";

    String horizDims;

    boolean isLatLon2D = !isGrib1 && Grib2Utils.isLatLon2D(hcs.template, gribCollection.getCenter());
    boolean isLatLon = isGrib1 ? hcs.isLatLon() : Grib2Utils.isLatLon(hcs.template, gribCollection.getCenter());

    if (isLatLon2D) { // CurvilinearOrthogonal - lat and lon fields must be present in the file
      horizDims = "lat lon";  // LOOK: orthogonal curvilinear

      // LOOK - assume same number of points for all grids
      ncfile.addDimension(g, new Dimension("lon", hcs.nx));
      ncfile.addDimension(g, new Dimension("lat", hcs.ny));

    } else if (isLatLon) {
      horizDims = "lat lon";
      ncfile.addDimension(g, new Dimension("lon", hcs.nx));
      ncfile.addDimension(g, new Dimension("lat", hcs.ny));

      Variable cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "lat", DataType.FLOAT, "lat"));
      cv.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
      if (hcs.gaussLats != null)
        cv.setCachedData(hcs.gaussLats);
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

    boolean is2Dtime = (gctype == GribCollection.Type.TwoD);
    CoordinateRuntime runtime = null;
    for (Coordinate coord : gHcs.coords) {
      Coordinate.Type ctype = coord.getType();
      switch (ctype) {
        case runtime:
          runtime = (CoordinateRuntime) coord;
          makeRuntimeCoordinate(ncfile, g, (CoordinateRuntime) coord);
          break;
        case timeIntv:
          if (is2Dtime) makeTimeCoordinate2D(ncfile, g, coord, runtime);
          else makeTimeCoordinate1D(ncfile, g, (CoordinateTimeIntv) coord, runtime);
          break;
        case time:
          if (is2Dtime) makeTimeCoordinate2D(ncfile, g, coord, runtime);
          else makeTimeCoordinate1D(ncfile, g, (CoordinateTime)  coord, runtime);
          break;
        case vert:
          makeVerticalCoordinate(ncfile, g, (CoordinateVert) coord);
          break;
        case ens:
          makeEnsembleCoordinate(ncfile, g, (CoordinateEns) coord);
          break;
        case time2D:
          makeTime2D(ncfile, g, (CoordinateTime2D) coord, is2Dtime);
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

    for (GribCollection.VariableIndex vindex : gHcs.variList) {

      Formatter dims = new Formatter();
      Formatter coords = new Formatter();

      for (Coordinate coord : vindex.getCoordinates()) {
        if (!is2Dtime && coord.getType() == Coordinate.Type.runtime) continue; // skip reference time
        String coordName = (coord.getType() == Coordinate.Type.vert) ? coord.getName().toLowerCase() : coord.getName();
        dims.format("%s ", coordName);
        coords.format("%s ", coordName);
      }
      dims.format("%s", horizDims);

      String vname = makeVariableName(vindex);
      Variable v = new Variable(ncfile, g, null, vname, DataType.FLOAT, dims.toString());
      ncfile.addVariable(g, v);
      if (debugName) System.out.printf("added %s%n",vname);

      String desc = makeVariableLongName(vindex);
      v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
      v.addAttribute(new Attribute(CDM.UNITS, makeVariableUnits(vindex)));
      v.addAttribute(new Attribute(CDM.MISSING_VALUE, Float.NaN));

      GribTables.Parameter gp = getParameter(vindex);
      if (gp != null) {
        if (gp.getDescription() != null)
          v.addAttribute(new Attribute(CDM.DESCRIPTION, gp.getDescription()));
        if (gp.getAbbrev() != null)
          v.addAttribute(new Attribute(CDM.ABBREV, gp.getAbbrev()));
      }

      if (isLatLon2D) {
        String s = searchCoord( Grib2Utils.getLatLon2DcoordType(desc), gHcs.variList);
        if (s == null) { // its a lat/lon coordinate
          v.setDimensions(horizDims); // LOOK make this 2D and munge the units
          String units = desc.contains("Latitude of") ? CDM.LAT_UNITS : CDM.LON_UNITS;
          v.addAttribute(new Attribute(CDM.UNITS, units));

        } else {
          coords.format("%s ", s);
        }
      } else {
        if (!hcs.isLatLon()) v.addAttribute(new Attribute(CF.GRID_MAPPING, grid_mapping));
      }
      v.addAttribute(new Attribute(CF.COORDINATES, coords.toString()));

      if (vindex.intvType >= 0) {
        GribStatType statType = gribTable.getStatType(vindex.intvType);   // LOOK find the time coordinate
        if (statType != null) {
          v.addAttribute(new Attribute("Grib_Statistical_Interval_Type", statType.toString()));
          CF.CellMethods cm = GribStatType.getCFCellMethod(statType);
          Coordinate timeCoord = vindex.getCoordinate(Coordinate.Type.timeIntv);
          if (cm != null && timeCoord != null)
            v.addAttribute(new Attribute(CF.CELL_METHODS, timeCoord.getName() + ": " + cm.toString()));
        } else {
          v.addAttribute(new Attribute("Grib_Statistical_Interval_Type", vindex.intvType));
        }
      }

      addVariableAttributes(v, vindex);

      v.setSPobject(vindex);
    }
  }

  private void makeRuntimeCoordinate(NetcdfFile ncfile, Group g, CoordinateRuntime rtc) {
    int n = rtc.getSize();
    String tcName = rtc.getName();
    ncfile.addDimension(g, new Dimension(tcName, n));

    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, tcName));
    v.addAttribute(new Attribute(CDM.UNITS, rtc.getUnit()));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    v.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));

    String vsName = tcName + "_ISO";
    Variable vs = ncfile.addVariable(g, new Variable(ncfile, g, null, vsName, DataType.STRING, tcName));
    vs.addAttribute(new Attribute(CDM.UNITS, "ISO8601"));
    v.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));

    // coordinate values
    String[] dataS = new String[n];
    int count = 0;
    for (CalendarDate val : rtc.getRuntimesSorted()) {
      dataS[count++] = val.toString();
    }
    vs.setCachedData(Array.factory(DataType.STRING, new int[]{n}, dataS));

    // now convert to udunits
    double[] data = new double[n];
    count = 0;
    for (double val : rtc.getOffsetsInHours()) {
      data[count++] = val;
    }
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{n}, data));
  }

  private void makeTime2D(NetcdfFile ncfile, Group g, CoordinateTime2D time2D, boolean is2Dtime) {
     CoordinateRuntime runtime = time2D.getRuntimeCoordinate();
     //makeRuntimeCoordinate(ncfile, g, runtime);

     int nruns = runtime.getSize();
     int ntimes = time2D.getNtimes();
     String tcName = time2D.getName();
     String dims = is2Dtime ? runtime.getName()+" "+tcName : tcName;
     ncfile.addDimension(g, new Dimension(tcName, ntimes));
     Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
     String units = runtime.getUnit(); // + " since " + runtime.getFirstDate();
     v.addAttribute(new Attribute(CDM.UNITS, units));
     v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
     v.addAttribute(new Attribute(CDM.LONG_NAME, GRIB_VALID_TIME));

     double[] data = new double[nruns * ntimes];
     for (int i=0; i<nruns * ntimes; i++) data[i] = Double.NaN;

     // coordinate values
     List<Coordinate> times = time2D.getTimes();
     if (!time2D.isTimeInterval()) {
       int runIdx = 0;
       for (Coordinate time : times) {
         CoordinateTime coordTime = (CoordinateTime) time;
         int timeIdx = 0;
         for (int val : coordTime.getOffsetSorted()) {
           data[runIdx*ntimes + timeIdx] = val + time2D.getOffset(runIdx);
           timeIdx++;
         }
         runIdx++;
       }
       int[] shape = is2Dtime ?  new int[]{nruns, ntimes} :  new int[]{ntimes};
       v.setCachedData(Array.factory(DataType.DOUBLE, shape, data));

     } else {

       int runIdx = 0;
       for (Coordinate time : times) {
         CoordinateTimeIntv coordTime = (CoordinateTimeIntv) time;
         int timeIdx = 0;
         for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals()) {
           data[runIdx*ntimes + timeIdx] = tinv.getBounds2() + time2D.getOffset(runIdx); // use upper bounds for coord value
           timeIdx++;
         }
         runIdx++;
       }
       int[] shape = is2Dtime ?  new int[]{nruns, ntimes} :  new int[]{ntimes};
       v.setCachedData(Array.factory(DataType.DOUBLE, shape, data));

       // bounds
       /*String intvName = getIntervalName(time2D.get());
       if (intvName != null)
         v.addAttribute(new Attribute(CDM.LONG_NAME, intvName)); */
       String bounds_name = tcName + "_bounds";

       Variable bounds = ncfile.addVariable(g, new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, dims + " 2"));
       v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
       bounds.addAttribute(new Attribute(CDM.UNITS, units));
       bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

       data = new double[nruns * ntimes * 2];
       for (int i=0; i<nruns * ntimes * 2; i++) data[i] = Double.NaN;

       runIdx = 0;
       for (Coordinate time : times) {
         CoordinateTimeIntv coordTime = (CoordinateTimeIntv) time;
         int timeIdx = 0;
         for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals()) {
           data[runIdx*ntimes + timeIdx] = tinv.getBounds1() + time2D.getOffset(runIdx);
           data[runIdx*ntimes + timeIdx+1] = tinv.getBounds2() + time2D.getOffset(runIdx);
           timeIdx+=2;
         }
         runIdx++;
       }
       int[] shapeb = is2Dtime ? new int[]{nruns, ntimes, 2} : new int[]{ntimes, 2};
       bounds.setCachedData(Array.factory(DataType.DOUBLE, shapeb, data));
     }
   }

  private void makeTimeCoordinate2D(NetcdfFile ncfile, Group g, Coordinate tc, CoordinateRuntime runtime) {
    int nruns = runtime.getSize();
    int ntimes = tc.getSize();
    String tcName = tc.getName();
    String dims = runtime.getName()+" "+tc.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = tc.getUnit()+ " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, GRIB_VALID_TIME));

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
        v.addAttribute(new Attribute(CDM.LONG_NAME, intvName));  */
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
  }

  private void makeTimeCoordinate1D(NetcdfFile ncfile, Group g, CoordinateTime coordTime, CoordinateRuntime runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = coordTime.getUnit()+ " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, GRIB_VALID_TIME));

    double[] data = new double[ntimes];
    int count = 0;

    // coordinate values
    for (int val : coordTime.getOffsetSorted())
      data[count++] = val;
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes}, data));
  }

  private void makeTimeCoordinate1D(NetcdfFile ncfile, Group g, CoordinateTimeIntv coordTime, CoordinateRuntime runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile.addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = coordTime.getUnit()+ " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, GRIB_VALID_TIME));

    double[] data = new double[ntimes];
    int count = 0;

    CalendarPeriod period = coordTime.getPeriod();

  // use upper bounds for coord value
    for (TimeCoord.Tinv tinv : coordTime.getTimeIntervals())
      data[count++] = tinv.getBounds2();
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes}, data));

    // bounds
    /* String intvName = getIntervalName(coordTime.getCode());
    if (intvName != null)
      v.addAttribute(new Attribute(CDM.LONG_NAME, intvName));*/
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
  }

  private void makeVerticalCoordinate(NetcdfFile ncfile, Group g, CoordinateVert vc) {
    int n = vc.getSize();
    String vcName = vc.getName().toLowerCase();
    if (vcName.startsWith("DEGY"))
      System.out.println("HEY");

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


  private String searchCoord(Grib2Utils.LatLonCoordType type, List<GribCollection.VariableIndex> list) {
    if (type == null) return null;

    GribCollection.VariableIndex lat, lon;
    switch (type) {
      case U:
        lat = searchCoord(list, 198);
        lon = searchCoord(list, 199);
        return (lat != null && lon != null) ? makeVariableName(lat) + " "+ makeVariableName(lon) : null;
      case V:
        lat = searchCoord(list, 200);
        lon = searchCoord(list, 201);
        return (lat != null && lon != null) ? makeVariableName(lat) + " "+ makeVariableName(lon) : null;
      case P:
        lat = searchCoord(list, 202);
        lon = searchCoord(list, 203);
        return (lat != null && lon != null) ? makeVariableName(lat) + "  "+ makeVariableName(lon) : null;
    }
    return null;
  }

  private GribCollection.VariableIndex searchCoord(List<GribCollection.VariableIndex> list, int p) {
    for (GribCollection.VariableIndex vindex : list) {
      if ((vindex.discipline == 0) && (vindex.category == 2) && (vindex.parameter == p))
        return vindex;
    }
    return null;
  }

  @Override
  public void close() throws java.io.IOException {
    if (!owned && gribCollection != null) // klugerino
      gribCollection.close();
    gribCollection = null;
    super.close();
  }

  @Override
  public String getDetailInfo() {
    Formatter f = new Formatter();
    f.format("%s",super.getDetailInfo());
    if (gribCollection != null)
      gribCollection.showIndex(f);
    return f.toString();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    long start = System.currentTimeMillis();

    Array result;
    if (isPartitioned)
      result = readDataFromPartition(v2, section, null);
    else
      result = readDataFromCollection(v2, section, null);

    long took = System.currentTimeMillis() - start;
    if (debugTime) System.out.println("  read data took=" + took + " msec ");
    return result;
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
    GribCollection.VariableIndex vindex = (GribCollection.VariableIndex) v.getSPobject();

    // first time, read records and keep in memory
    vindex.readRecords();

    int sectionLen = section.getRank();
    Range yRange = section.getRange(sectionLen - 2);
    Range xRange = section.getRange(sectionLen-1);

    //int[] otherShape = new int[sectionLen-2];
    //System.arraycopy(vindex.getSparseArray().getShape(), 0, otherShape, 0, sectionLen-2);  // LOOK WRONG
    Section sectionWanted = section.subSection(0, sectionLen-2); // all but x, y
    Section.Iterator iterWanted = sectionWanted.getIterator(v.getShape());
    int[] indexWanted = new int[sectionLen-2];                              // place to put the iterator result

     // collect all the records that need to be read
    DataReader dataReader = new DataReader(vindex);
    int count = 0;
    while (iterWanted.hasNext()) {
      int sourceIndex = iterWanted.next(indexWanted);
      dataReader.addRecord(sourceIndex, count++);
    }

    /*
    DataReaderPartitioned dataReader = new DataReaderPartitioned();
    int resultPos = 0;
    while (iterWanted.hasNext()) {
      iterWanted.next(indexWanted);   // returns the vindexP index in indexWanted aaray

      PartitionCollection.DataRecord record = vindexP.getDataRecord(indexWanted);
      if (record == null) {
        vindexP.getDataRecord(indexWanted); // debug
        resultPos++;
        continue;
      }
      record.resultIndex = resultPos;
      dataReader.addRecord(record);
      resultPos++;
    }
    */

    // sort by file and position, then read
    DataReceiverIF dataReceiver = channel == null ? new DataReceiver(section, yRange, xRange) : new ChannelReceiver(channel, yRange, xRange);
    dataReader.read(dataReceiver);
    return dataReceiver.getArray();
  }

  static class DataRecord implements Comparable<DataRecord> {
    int resultIndex; // index in the ens / time / vert array
    int fileno;
    long dataPos;  // grib1 - start of GRIB record; grib2 - start of drs (data record)
    long bmsPos;  // if non zero, use alternate bms
    int scanMode;
    GdsHorizCoordSys hcs;

    DataRecord(int resultIndex, int fileno, long dataPos, long bmsPos, int scanMode, GdsHorizCoordSys hcs) {
      this.resultIndex = resultIndex;
      this.fileno = fileno;
      this.dataPos = (dataPos == 0) ? GribCollection.MISSING_RECORD : dataPos; // 0 also means missing in Grib2
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
  }

  protected class DataReader {
    GribCollection.VariableIndex vindex;
    List<DataRecord> records = new ArrayList<>();

    private DataReader(GribCollection.VariableIndex vindex) {
      this.vindex = vindex;
    }

    void addRecord(int sourceIndex, int resultIndex) {
      GribCollection.Record record = vindex.getSparseArray().getContent(sourceIndex);
      if (record != null)
        records.add(new DataRecord(resultIndex, record.fileno, record.pos, record.bmsPos, record.scanMode, vindex.group.getGdsHorizCoordSys()));
    }

    void read(DataReceiverIF dataReceiver) throws IOException {
      Collections.sort(records);

      int currFile = -1;
      RandomAccessFile rafData = null;
      for (DataRecord dr : records) {
        if (dr.fileno != currFile) {
          if (rafData != null) rafData.close();
          rafData = gribCollection.getDataRaf(dr.fileno);
          currFile = dr.fileno;
        }

        if (dr.dataPos == GribCollection.MISSING_RECORD) continue;

        if (debugRead) { // for validation
          show(rafData, dr.dataPos);
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = vindex.group.getGdsHorizCoordSys();
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }
      if (rafData != null) rafData.close();
    }
  }

  private interface DataReceiverIF {
    void addData(float[] data, int resultIndex, int nx) throws IOException;
    Array getArray();
  }

  private class DataReceiver implements DataReceiverIF {
    private Array dataArray;
    private Range yRange, xRange;
    private int horizSize;

    DataReceiver(Section section, Range yRange, Range xRange) {
      dataArray = Array.factory(DataType.FLOAT, section.getShape());
      this.yRange = yRange;
      this.xRange = xRange;
      this.horizSize = yRange.length() * xRange.length();

      // prefill with NaNs, to deal with missing data
      IndexIterator iter = dataArray.getIndexIterator();
      while (iter.hasNext())
        iter.setFloatNext(Float.NaN);
    }

    public void addData(float[] data, int resultIndex, int nx) throws IOException {
      int start = resultIndex * horizSize;
      int count = 0;
      for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
        for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
          int dataIdx = y * nx + x;
          if (dataIdx >= data.length)
            System.out.println("HEY");
          dataArray.setFloat(start + count, data[dataIdx]);
          count++;
        }
      }
    }

    public Array getArray() {
      return dataArray;
    }
  }

  private class ChannelReceiver implements DataReceiverIF {
    private WritableByteChannel channel;
    private DataOutputStream outStream;
    private Range yRange, xRange;

    ChannelReceiver(WritableByteChannel channel, Range yRange, Range xRange) {
      this.channel = channel;
      this.outStream = new DataOutputStream(Channels.newOutputStream(channel));
      this.yRange = yRange;
      this.xRange = xRange;
    }

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

    public Array getArray() {
      return null;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  private Array readDataFromPartition(Variable v, Section section, WritableByteChannel channel) throws IOException, InvalidRangeException {
    PartitionCollection.VariableIndexPartitioned vindexP = (PartitionCollection.VariableIndexPartitioned) v.getSPobject(); // the variable in the partition collection

    //boolean hasRuntime = vindexP.isTwod;

    int sectionLen = section.getRank();
    Range yRange = section.getRange(sectionLen - 2);  // last 2
    Range xRange = section.getRange(sectionLen-1);
    Section sectionWanted = section.subSection(0, sectionLen - 2); // all but x, y
    Section.Iterator iterWanted = sectionWanted.getIterator(v.getShape());  // iterator over wanted indices in vindexP
    int[] indexWanted = new int[sectionLen-2];                              // place to put the iterator result

        // collect all the records that need to be read
    DataReaderPartitioned dataReader = new DataReaderPartitioned();
    int resultPos = 0;
    while (iterWanted.hasNext()) {
      iterWanted.next(indexWanted);   // returns the vindexP index in indexWanted aaray

      PartitionCollection.DataRecord record = vindexP.getDataRecord(indexWanted);
      if (record == null) {
        vindexP.getDataRecord(indexWanted); // debug
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

    // close partitions as needed
    vindexP.cleanup();

    return dataReceiver.getArray();
  }

  private class DataReaderPartitioned {
    List<PartitionCollection.DataRecord> records = new ArrayList<>();

    void addRecord(PartitionCollection.DataRecord dr) {
      if (dr != null) records.add(dr);
    }

    void read(DataReceiverIF dataReceiver) throws IOException {
      Collections.sort(records);

      PartitionCollection.DataRecord lastRecord = null;
      RandomAccessFile rafData = null;

      for (PartitionCollection.DataRecord dr : records) {
        if ((rafData == null) || !dr.usesSameFile(lastRecord)) {
          if (rafData != null) rafData.close();
          rafData = dr.usePartition.getRaf(dr.partno, dr.fileno);
        }
        lastRecord = dr;

        if (dr.dataPos == GribCollection.MISSING_RECORD) continue;

        if (debugRead) { // for validation
          show( rafData, dr.dataPos);
        }

        float[] data = readData(rafData, dr);
        GdsHorizCoordSys hcs = dr.hcs;
        dataReceiver.addData(data, dr.resultIndex, hcs.nx);
      }
      if (rafData != null) rafData.close();
    }
  }


}
