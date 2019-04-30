/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nullable;
import org.jdom2.Element;
import thredds.client.catalog.Catalog;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.grib.*;
import ucar.nc2.grib.coord.Coordinate;
import ucar.nc2.grib.coord.CoordinateEns;
import ucar.nc2.grib.coord.CoordinateRuntime;
import ucar.nc2.grib.coord.CoordinateTime;
import ucar.nc2.grib.coord.CoordinateTime2D;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.grib.coord.CoordinateTimeIntv;
import ucar.nc2.grib.coord.CoordinateVert;
import ucar.nc2.grib.coord.EnsCoordValue;
import ucar.nc2.grib.coord.TimeCoordIntvValue;
import ucar.nc2.grib.coord.VertCoordType;
import ucar.nc2.grib.coord.VertCoordValue;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.projection.RotatedPole;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Grib Collection IOSP, version 2. Handles both collections and single GRIB files. Immutable after
 * open() is called.
 *
 * @author caron
 * @since 4/6/11
 */
public abstract class GribIosp extends AbstractIOServiceProvider {
  public static int debugIndexOnlyCount = 0;  // count number of data accesses

  // store custom tables in here
  protected final FeatureCollectionConfig config = new FeatureCollectionConfig();

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
  @Nullable
  public Object sendIospMessage(Object special) {
    if (special instanceof String) {
      String s = (String) special;
      if (s.startsWith("gribParameterTableLookup")) {
        int pos = s.indexOf("=");
        if (pos > 0) {
          config.gribConfig.lookupTablePath = s.substring(pos + 1).trim();
        }

      } else if (s.startsWith("gribParameterTable")) {
        int pos = s.indexOf("=");
        if (pos > 0) {
          config.gribConfig.paramTablePath = s.substring(pos + 1).trim();
        }
      }
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

  protected abstract String makeVariableUnits(GribCollectionImmutable.VariableIndex vindex);

  protected abstract String getVerticalCoordDesc(int vc_code);

  protected abstract GribTables.Parameter getParameter(
      GribCollectionImmutable.VariableIndex vindex);

  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask)
      throws IOException {
    super.open(raf, ncfile, cancelTask);

    if (gHcs != null) { // just use the one group that was set in the constructor
      this.gribCollection = gHcs.getGribCollection();
      if (this.gribCollection instanceof PartitionCollectionImmutable) {
        isPartitioned = true;
      }
      gribTable = createCustomizer();

      addGroup(ncfile, ncfile.getRootGroup(), gHcs, gtype, false);

    } else if (gribCollection == null) { // may have been set in the constructor

      this.gribCollection = GribCdmIndex
          .openGribCollectionFromRaf(raf, config, CollectionUpdateType.testIndexOnly, logger);
      if (gribCollection == null) {
        throw new IllegalStateException("Not a GRIB data file or index file " + raf.getLocation());
      }

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
        for (GribCollectionImmutable.GroupGC g : groups) {
          addGroup(ncfile, topGroup, g, ds.getType(), useGroups);
        }
      }
    }

    for (Attribute att : gribCollection.getGlobalAttributes().getAttributes()) {
      ncfile.addAttribute(null, att);
    }
  }

  private void addGroup(NetcdfFile ncfile, Group parent, GribCollectionImmutable.GroupGC group,
      GribCollectionImmutable.Type gctype, boolean useGroups) {

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

  private void makeGroup(NetcdfFile ncfile, Group g, GribCollectionImmutable.GroupGC group,
      GribCollectionImmutable.Type gctype) {
    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();
    String grid_mapping = hcs.getName() + "_Projection";

    String horizDims;

    boolean isRotatedLatLon = !isGrib1 && hcs.proj instanceof RotatedPole;
    boolean isLatLon2D =
        !isGrib1 && Grib2Utils.isCurvilinearOrthogonal(hcs.template, gribCollection.getCenter());
    boolean isLatLon =
        isGrib1 ? hcs.isLatLon() : Grib2Utils.isLatLon(hcs.template, gribCollection.getCenter());

    if (isRotatedLatLon) {
      Variable hcsV = ncfile
          .addVariable(g, new Variable(ncfile, g, null, grid_mapping, DataType.INT, ""));
      hcsV.setCachedData(Array.factory(DataType.INT, new int[0], new int[]{0}));
      for (Parameter p : hcs.proj.getProjectionParameters()) {
        hcsV.addAttribute(new Attribute(p));
      }
      horizDims = "rlat rlon";
      ncfile.addDimension(g, new Dimension("rlat", hcs.ny));
      ncfile.addDimension(g, new Dimension("rlon", hcs.nx));
      Variable rlat = ncfile
          .addVariable(g, new Variable(ncfile, g, null, "rlat", DataType.FLOAT, "rlat"));
      rlat.addAttribute(new Attribute(CF.STANDARD_NAME, CF.GRID_LATITUDE));
      rlat.addAttribute(new Attribute(CDM.UNITS, CDM.RLATLON_UNITS));
      rlat.setCachedData(Array.makeArray(DataType.FLOAT, hcs.ny, hcs.starty, hcs.dy));
      Variable rlon = ncfile
          .addVariable(g, new Variable(ncfile, g, null, "rlon", DataType.FLOAT, "rlon"));
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
      Variable hcsV = ncfile
          .addVariable(g, new Variable(ncfile, g, null, grid_mapping, DataType.INT, ""));
      hcsV.setCachedData(Array.factory(DataType.INT, new int[0], new int[]{0}));
      for (Parameter p : hcs.proj.getProjectionParameters()) {
        hcsV.addAttribute(new Attribute(p));
      }

      horizDims = "lat lon";
      ncfile.addDimension(g, new Dimension("lon", hcs.nx));
      ncfile.addDimension(g, new Dimension("lat", hcs.ny));

      Variable cv = ncfile
          .addVariable(g, new Variable(ncfile, g, null, "lat", DataType.FLOAT, "lat"));
      cv.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
      if (hcs.getGaussianLats() != null) {
        cv.setCachedData(hcs.getGaussianLats());
        cv.addAttribute(new Attribute(CDM.GAUSSIAN, "true"));
      } else {
        cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.ny, hcs.starty, hcs.dy));
      }

      cv = ncfile.addVariable(g, new Variable(ncfile, g, null, "lon", DataType.FLOAT, "lon"));
      cv.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      cv.setCachedData(Array.makeArray(DataType.FLOAT, hcs.nx, hcs.startx, hcs.dx));

    } else {
      // make horiz coordsys coordinate variable
      Variable hcsV = ncfile
          .addVariable(g, new Variable(ncfile, g, null, grid_mapping, DataType.INT, ""));
      hcsV.setCachedData(Array.factory(DataType.INT, new int[0], new int[]{0}));
      for (Parameter p : hcs.proj.getProjectionParameters()) {
        hcsV.addAttribute(new Attribute(p));
      }

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

    boolean singleRuntimeWasMade = false;

    for (Coordinate coord : group.coords) {
      Coordinate.Type ctype = coord.getType();
      switch (ctype) {
        case runtime:
          if (gctype.isTwoD() || coord.getNCoords() == 1) {
            makeRuntimeCoordinate(ncfile, g, (CoordinateRuntime) coord);
          }
          break;
        case timeIntv:
          makeTimeCoordinate1D(ncfile, g, (CoordinateTimeIntv) coord);
          break;
        case time:
          makeTimeCoordinate1D(ncfile, g, (CoordinateTime) coord);
          break;
        case vert:
          makeVerticalCoordinate(ncfile, g, (CoordinateVert) coord);
          break;
        case ens:
          makeEnsembleCoordinate(ncfile, g, (CoordinateEns) coord);
          break;
        case time2D:
          if (gctype.isUniqueTime()) {
            makeUniqueTimeCoordinate2D(ncfile, g, (CoordinateTime2D) coord);
          } else {
            makeTimeCoordinate2D(ncfile, g, (CoordinateTime2D) coord, gctype);
          }
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
      try (Formatter dimNames = new Formatter();
          Formatter coordinateAtt = new Formatter()) {

        // do the times first
        Coordinate run = vindex.getCoordinate(Coordinate.Type.runtime);
        Coordinate time = vindex.getCoordinateTime();
        if (time == null) {
          throw new IllegalStateException("No time coordinate = " + vindex);
        }

        boolean isRunScaler = (run != null) && run.getSize() == 1;

        switch (gctype) {
          case SRC:     // GC: Single Runtime Collection                          [ntimes]           (run, 2D)  scalar runtime
            assert isRunScaler;
            dimNames.format("%s ", time.getName());
            coordinateAtt.format("%s %s ", run.getName(), time.getName());
            break;

          case MRUTP:             // PC: Multiple Runtime Unique Time Partition  [ntimes]
          case MRUTC:             // GC: Multiple Runtime Unique Time Collection  [ntimes]
            // case MRSTC:             // GC: Multiple Runtime Single Time Collection  [nruns, 1]
            // case MRSTP:             // PC: Multiple Runtime Single Time Partition   [nruns, 1]         (run, 2D)  ignore the run, its generated from the 2D in
            dimNames.format("%s ", time.getName());
            coordinateAtt.format("ref%s %s ", time.getName(), time.getName());
            break;

          case MRC:               // GC: Multiple Runtime Collection              [nruns, ntimes]    (run, 2D) use Both
          case TwoD:              // PC: TwoD time partition                      [nruns, ntimes]
            assert run != null : "GRIB MRC or TWOD does not have run coordinate";
            if (isRunScaler) {
              dimNames.format("%s ", time.getName());
            } else {
              dimNames.format("%s %s ", run.getName(), time.getName());
            }
            coordinateAtt.format("%s %s ", run.getName(), time.getName());
            break;

          case Best:              // PC: Best time partition                      [ntimes]          (time)   reftime is generated in makeTimeAuxReference()
          case BestComplete:      // PC: Best complete time partition             [ntimes]
            dimNames.format("%s ", time.getName());
            coordinateAtt.format("ref%s %s ", time.getName(), time.getName());
            break;

          default:
            throw new IllegalStateException("Uknown GribCollection TYpe = " + gctype);
        }

        // do other (vert, ens) coordinates
        for (Coordinate coord : vindex.getCoordinates()) {
          if (coord instanceof CoordinateTimeAbstract || coord instanceof CoordinateRuntime) {
            continue;
          }
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

        String desc = makeVariableLongName(vindex);
        v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
        v.addAttribute(new Attribute(CDM.UNITS, makeVariableUnits(vindex)));

        GribTables.Parameter gp = getParameter(vindex);
        if (gp != null) {
          if (gp.getDescription() != null) {
            v.addAttribute(new Attribute(CDM.DESCRIPTION, gp.getDescription()));
          }
          if (gp.getAbbrev() != null) {
            v.addAttribute(new Attribute(CDM.ABBREV, gp.getAbbrev()));
          }
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, gp.getMissing()));
          if (gp.getFill() != null) {
            v.addAttribute(new Attribute(CDM.FILL_VALUE, gp.getFill()));
          }
        } else {
          v.addAttribute(new Attribute(CDM.MISSING_VALUE, Float.NaN));
        }

        // horiz coord system
        if (isLatLon2D) { // special case of "LatLon Orthogononal"
          String s = searchCoord(Grib2Utils.getLatLon2DcoordType(desc), group.variList);
          if (s == null) { // its a 2D lat/lon coordinate
            v.setDimensions(horizDims); // LOOK make this 2D and munge the units
            String units = desc.contains("Latitude of") ? CDM.LAT_UNITS : CDM.LON_UNITS;
            v.addAttribute(new Attribute(CDM.UNITS, units));

          } else { // its a variable using the coordinates described by s
            coordinateAtt.format("%s ", s);
          }
        } else {
          v.addAttribute(new Attribute(CF.GRID_MAPPING, grid_mapping));
        }
        v.addAttribute(new Attribute(CF.COORDINATES, coordinateAtt.toString()));

        // statistical interval type
        if (vindex.getIntvType() >= 0) {
          // LOOK find the time coordinate
          GribStatType statType = gribTable.getStatType(vindex.getIntvType());
          if (statType != null) {
            v.addAttribute(new Attribute(Grib.GRIB_STAT_TYPE, statType.toString()));
            CF.CellMethods cm = GribStatType.getCFCellMethod(statType);
            Coordinate timeCoord = vindex.getCoordinate(Coordinate.Type.timeIntv);
            if (cm != null && timeCoord != null) {
              v.addAttribute(
                  new Attribute(CF.CELL_METHODS, timeCoord.getName() + ": " + cm.toString()));
            }
          } else {
            v.addAttribute(new Attribute(Grib.GRIB_STAT_TYPE, vindex.getIntvType()));
          }
        }

        gribCollection.addVariableAttributes(v, vindex);
        v.setSPobject(vindex);
      }
    }
  }

  private void makeRuntimeCoordinate(NetcdfFile ncfile, Group g, CoordinateRuntime rtc) {
    int n = rtc.getSize();
    boolean isScalar = (n == 1);      // this is the case of runtime[1]
    String tcName = rtc.getName();
    String dims = isScalar ? null : rtc.getName();  // null means scalar
    if (!isScalar) {
      ncfile.addDimension(g, new Dimension(tcName, n));
    }

    Variable v = ncfile
        .addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    v.addAttribute(new Attribute(CDM.UNITS, rtc.getUnit()));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
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

  private enum Time2DinfoType {off, offU, intv, intvU, bounds, boundsU, is1Dtime, isUniqueRuntime, reftime, timeAuxRef}

  private static class Time2Dinfo {

    final Time2DinfoType which;
    final CoordinateTime2D time2D;
    final Coordinate time1D;

    private Time2Dinfo(Time2DinfoType which, CoordinateTime2D time2D, Coordinate time1D) {
      this.which = which;
      this.time2D = time2D;
      this.time1D = time1D;
    }
  }

  // time coordinates are unique
  // time(nruns, ntimes) -> time(ntimes) with dependent reftime(ntime) coordinate
  private void makeUniqueTimeCoordinate2D(NetcdfFile ncfile, Group g, CoordinateTime2D time2D) {
    CoordinateRuntime runtime = time2D.getRuntimeCoordinate();

    int countU = 0;
    for (int run = 0; run < time2D.getNruns(); run++) {
      CoordinateTimeAbstract timeCoord = time2D.getTimeCoordinate(run);
      countU += timeCoord.getSize();
    }
    int ntimes = countU;
    String tcName = time2D.getName();

    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile
        .addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, tcName));
    String units = runtime.getUnit(); // + " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    // the data is not generated until asked for to save space
    if (!time2D.isTimeInterval()) {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.offU, time2D, null));
    } else {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.intvU, time2D, null));
      // bounds for intervals
      String bounds_name = tcName + "_bounds";
      Variable bounds = ncfile.addVariable(g,
          new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, tcName + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
      bounds.addAttribute(new Attribute(CDM.UNITS, units));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));
      bounds.setSPobject(new Time2Dinfo(Time2DinfoType.boundsU, time2D, null));
    }

    if (runtime.getNCoords() != 1) {
      // for this case we have to generate a separate reftime, because have to use the same dimension
      String refName = "ref" + tcName;
      if (g.findVariable(refName) == null) {
        Variable vref = ncfile
            .addVariable(g, new Variable(ncfile, g, null, refName, DataType.DOUBLE, tcName));
        vref.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
        vref.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
        vref.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));
        vref.addAttribute(new Attribute(CDM.UNITS, units));
        vref.setSPobject(new Time2Dinfo(Time2DinfoType.isUniqueRuntime, time2D, null));
      }
    }
  }

  /* non unique time case
  3) time(nruns, ntimes) with reftime(nruns)
   */
  private void makeTimeCoordinate2D(NetcdfFile ncfile, Group g, CoordinateTime2D time2D,
      GribCollectionImmutable.Type gctype) {
    CoordinateRuntime runtime = time2D.getRuntimeCoordinate();

    int ntimes = time2D.getNtimes();
    String tcName = time2D.getName();
    String dims = runtime.getName() + " " + tcName;
    int dimLength = ntimes;

    ncfile.addDimension(g, new Dimension(tcName, dimLength));
    Variable v = ncfile
        .addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = runtime.getUnit(); // + " since " + runtime.getFirstDate();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    // the data is not generated until asked for to save space
    if (!time2D.isTimeInterval()) {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.off, time2D, null));
    } else {
      v.setSPobject(new Time2Dinfo(Time2DinfoType.intv, time2D, null));
      // bounds for intervals
      String bounds_name = tcName + "_bounds";
      Variable bounds = ncfile
          .addVariable(g, new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, dims + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
      bounds.addAttribute(new Attribute(CDM.UNITS, units));
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));
      bounds.setSPobject(new Time2Dinfo(Time2DinfoType.bounds, time2D, null));
    }

  }

  private Array makeLazyCoordinateData(Variable v2, Time2Dinfo info) {
    if (info.time2D != null) {
      return makeLazyTime2Darray(v2, info);
    } else {
      return makeLazyTime1Darray(v2, info);
    }
  }

  // only for the 2d times
  private Array makeLazyTime1Darray(Variable v2, Time2Dinfo info) {
    int length = info.time1D.getSize();
    double[] data = new double[length];
    for (int i = 0; i < length; i++) {
      data[i] = Double.NaN;
    }

    // coordinate values
    switch (info.which) {
      case reftime:
        CoordinateRuntime rtc = (CoordinateRuntime) info.time1D;
        int count = 0;
        for (double val : rtc.getOffsetsInTimeUnits()) {
          data[count++] = val;
        }
        return Array.factory(DataType.DOUBLE, v2.getShape(), data);

      case timeAuxRef:
        CoordinateTimeAbstract time = (CoordinateTimeAbstract) info.time1D;
        count = 0;
        List<Double> masterOffsets = gribCollection.getMasterRuntime().getOffsetsInTimeUnits();
        for (int masterIdx : time.getTime2runtime()) {
          data[count++] = masterOffsets.get(masterIdx - 1);
        }
        return Array.factory(DataType.DOUBLE, v2.getShape(), data);

      default:
        throw new IllegalStateException("makeLazyTime1Darray must be reftime or timeAuxRef");
    }
  }

  // only for the 2d times
  private Array makeLazyTime2Darray(Variable coord, Time2Dinfo info) {
    CoordinateTime2D time2D = info.time2D;
    CalendarPeriod timeUnit = time2D.getTimeUnit();

    int nruns = time2D.getNruns();
    int ntimes = time2D.getNtimes();

    int length = (int) coord.getSize();
    if (info.which == Time2DinfoType.bounds) {
      length *= 2;
    }

    double[] data = new double[length];
    for (int i = 0; i < length; i++) {
      data[i] = Double.NaN;
    }
    int count;

    // coordinate values
    switch (info.which) {
      case off:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTime coordTime = (CoordinateTime) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (int val : coordTime.getOffsetSorted()) {
            data[runIdx * ntimes + timeIdx] = timeUnit.getValue() * val + time2D.getOffset(runIdx);
            timeIdx++;
          }
        }
        break;

      case offU:
        count = 0;
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTime coordTime = (CoordinateTime) time2D.getTimeCoordinate(runIdx);
          for (int val : coordTime.getOffsetSorted()) {
            data[count++] = timeUnit.getValue() * val + time2D.getOffset(runIdx);
          }
        }
        break;

      case intv:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[runIdx * ntimes + timeIdx] = timeUnit.getValue() * tinv.getBounds2() + time2D
                .getOffset(runIdx); // use upper bounds for coord value
            timeIdx++;
          }
        }
        break;

      case intvU:
        count = 0;
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[count++] = timeUnit.getValue() * tinv.getBounds2() + time2D
                .getOffset(runIdx); // use upper bounds for coord value
          }
        }
        break;

      case is1Dtime:
        CoordinateRuntime runtime = time2D.getRuntimeCoordinate();
        count = 0;
        for (double val : runtime.getOffsetsInTimeUnits()) { // convert to udunits
          data[count++] = val;
        }
        break;

      case isUniqueRuntime:  // the aux runtime coordinate
        CoordinateRuntime runtimeU = time2D.getRuntimeCoordinate();
        List<Double> runOffsets = runtimeU.getOffsetsInTimeUnits();
        count = 0;
        for (int run = 0; run < time2D.getNruns(); run++) {
          CoordinateTimeAbstract timeCoord = time2D.getTimeCoordinate(run);
          for (int time = 0; time < timeCoord.getNCoords(); time++) {
            data[count++] = runOffsets.get(run);
          }
        }
        break;

      case bounds:
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          int timeIdx = 0;
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[runIdx * ntimes * 2 + timeIdx] =
                timeUnit.getValue() * tinv.getBounds1() + time2D.getOffset(runIdx);
            data[runIdx * ntimes * 2 + timeIdx + 1] =
                timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx);
            timeIdx += 2;
          }
        }
        break;

      case boundsU:
        count = 0;
        for (int runIdx = 0; runIdx < nruns; runIdx++) {
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
          for (TimeCoordIntvValue tinv : timeIntv.getTimeIntervals()) {
            data[count++] = timeUnit.getValue() * tinv.getBounds1() + time2D.getOffset(runIdx);
            data[count++] = timeUnit.getValue() * tinv.getBounds2() + time2D.getOffset(runIdx);
          }
        }
        break;

      default:
        throw new IllegalStateException();
    }

    return Array.factory(DataType.DOUBLE, coord.getShape(), data);
  }

  private void makeTimeCoordinate1D(NetcdfFile ncfile, Group g,
      CoordinateTime coordTime) { //}, CoordinateRuntime runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile
        .addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = coordTime.getTimeUdUnit();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    double[] data = new double[ntimes];
    int count = 0;

    // coordinate values
    for (int val : coordTime.getOffsetSorted()) {
      data[count++] = val;
    }
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes}, data));

    makeTimeAuxReference(ncfile, g, tcName, units, coordTime);
  }

  private void makeTimeAuxReference(NetcdfFile ncfile, Group g, String timeName, String units,
      CoordinateTimeAbstract time) {
    if (time.getTime2runtime() == null) {
      return;
    }
    String tcName = "ref" + timeName;
    Variable v = ncfile
        .addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, timeName));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));
    v.addAttribute(new Attribute(CDM.UNITS, units));

    // lazy evaluation
    v.setSPobject(new Time2Dinfo(Time2DinfoType.timeAuxRef, null, time));
  }

  private void makeTimeCoordinate1D(NetcdfFile ncfile, Group g,
      CoordinateTimeIntv coordTime) { // }, CoordinateRuntime runtime) {
    int ntimes = coordTime.getSize();
    String tcName = coordTime.getName();
    String dims = coordTime.getName();
    ncfile.addDimension(g, new Dimension(tcName, ntimes));
    Variable v = ncfile
        .addVariable(g, new Variable(ncfile, g, null, tcName, DataType.DOUBLE, dims));
    String units = coordTime.getTimeUdUnit();
    v.addAttribute(new Attribute(CDM.UNITS, units));
    v.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    v.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    v.addAttribute(new Attribute(CF.CALENDAR, Calendar.proleptic_gregorian.toString()));

    double[] data = new double[ntimes];
    int count = 0;

    // use upper bounds for coord value
    for (TimeCoordIntvValue tinv : coordTime.getTimeIntervals()) {
      data[count++] = tinv.getBounds2();
    }
    v.setCachedData(Array.factory(DataType.DOUBLE, new int[]{ntimes}, data));

    // bounds
    String bounds_name = tcName + "_bounds";
    Variable bounds = ncfile
        .addVariable(g, new Variable(ncfile, g, null, bounds_name, DataType.DOUBLE, dims + " 2"));
    v.addAttribute(new Attribute(CF.BOUNDS, bounds_name));
    bounds.addAttribute(new Attribute(CDM.UNITS, units));
    bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + tcName));

    data = new double[ntimes * 2];
    count = 0;
    for (TimeCoordIntvValue tinv : coordTime.getTimeIntervals()) {
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
    Variable v = ncfile
        .addVariable(g, new Variable(ncfile, g, null, vcName, DataType.FLOAT, vcName));
    if (vc.getUnit() != null) {
      v.addAttribute(new Attribute(CDM.UNITS, vc.getUnit()));
      String desc = getVerticalCoordDesc(vc.getCode());
      if (desc != null) {
        v.addAttribute(new Attribute(CDM.LONG_NAME, desc));
      }
      v.addAttribute(
          new Attribute(CF.POSITIVE, vc.isPositiveUp() ? CF.POSITIVE_UP : CF.POSITIVE_DOWN));
    }

    v.addAttribute(new Attribute("Grib_level_type", vc.getCode()));
    VertCoordType vu = vc.getVertUnit();
    if (vu != null) {
      if (vu.getDatum() != null) {
        v.addAttribute(new Attribute("datum", vu.getDatum()));
      }
    }

    if (vc.isLayer()) {
      float[] data = new float[n];
      int count = 0;
      for (VertCoordValue val : vc.getLevelSorted()) {
        data[count++] = (float) (val.getValue1() + val.getValue2()) / 2;
      }
      v.setCachedData(Array.factory(DataType.FLOAT, new int[]{n}, data));

      Variable bounds = ncfile.addVariable(g,
          new Variable(ncfile, g, null, vcName + "_bounds", DataType.FLOAT, vcName + " 2"));
      v.addAttribute(new Attribute(CF.BOUNDS, vcName + "_bounds"));
      String vcUnit = vc.getUnit();
      if (vcUnit != null) {
        bounds.addAttribute(new Attribute(CDM.UNITS, vcUnit));
      }
      bounds.addAttribute(new Attribute(CDM.LONG_NAME, "bounds for " + vcName));

      data = new float[2 * n];
      count = 0;
      for (VertCoordValue level : vc.getLevelSorted()) {
        data[count++] = (float) level.getValue1();
        data[count++] = (float) level.getValue2();
      }
      bounds.setCachedData(Array.factory(DataType.FLOAT, new int[]{n, 2}, data));

    } else {
      float[] data = new float[n];
      int count = 0;
      for (VertCoordValue val : vc.getLevelSorted()) {
        data[count++] = (float) val.getValue1();
      }
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
    for (EnsCoordValue ecc : ec.getEnsSorted()) {
      data[count++] = ecc.getEnsMember();
    }
    v.setCachedData(Array.factory(DataType.INT, new int[]{n}, data));
  }

  @Nullable
  private String searchCoord(Grib2Utils.LatLonCoordType type, List<GribCollectionImmutable.VariableIndex> list) {
    if (type == null) {
      return null;
    }

    GribCollectionImmutable.VariableIndex lat, lon;
    switch (type) {
      case U:
        lat = findParameter(list, 198);
        lon = findParameter(list, 199);
        return (lat != null && lon != null) ? makeVariableName(lat) + " " + makeVariableName(lon)
            : null;
      case V:
        lat = findParameter(list, 200);
        lon = findParameter(list, 201);
        return (lat != null && lon != null) ? makeVariableName(lat) + " " + makeVariableName(lon)
            : null;
      case P:
        lat = findParameter(list, 202);
        lon = findParameter(list, 203);
        return (lat != null && lon != null) ? makeVariableName(lat) + "  " + makeVariableName(lon)
            : null;
    }
    return null;
  }

  @Nullable
  private GribCollectionImmutable.VariableIndex findParameter(List<GribCollectionImmutable.VariableIndex> list, int p) {
    for (GribCollectionImmutable.VariableIndex vindex : list) {
      if ((vindex.getDiscipline() == 0) && (vindex.getCategory() == 2) && (vindex.getParameter() == p)) {
        return vindex;
      }
    }
    return null;
  }

  @Override
  public void close() throws java.io.IOException {
    if (!owned && gribCollection != null) // LOOK klugerino
    {
      gribCollection.close();
    }
    gribCollection = null;
    super.close();
  }

  @Override
  public String getDetailInfo() {
    Formatter f = new Formatter();
    f.format("%s", super.getDetailInfo());
    if (gribCollection != null) {
      gribCollection.showIndex(f);
    }
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
      Section sectionFilled = Section.fill(section, v2.getShape());
      return data.sectionNoReduce(sectionFilled.getRanges());
    }

    try {
      Array result;
      GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) v2
          .getSPobject();
      GribDataReader dataReader = GribDataReader.factory(gribCollection, vindex);
      SectionIterable sectionIter = new SectionIterable(section, v2.getShape());
      result = dataReader.readData(sectionIter);

      long took = System.currentTimeMillis() - start;
      return result;

    } catch (IOException ioe) {
      logger.error("Failed to readData ", ioe);
      throw ioe;
    }
  }

  /* private Array readDataFromCollectionNew(Variable v, Section section) throws IOException, InvalidRangeException {
    GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) v.getSPobject();
    GribDataReader dataReader = GribDataReader.factory(gribCollection, vindex);
    return dataReader.readDataFromCollection(vindex, section, v.getShape());
  }


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
    // this assumes that the coordinates reflect the vindex.sparseArray exactly
    GribDataReader dataReader = GribDataReader.factory(gribCollection, vindex);
    int count = 0;
    while (iterWanted.hasNext()) {
      int sourceIndex = iterWanted.next(indexWanted);
      dataReader.addRecord(sourceIndex, count++);
    }

    // sort by file and position, then read
    GribDataReader.DataReceiverIF dataReceiver = (channel == null) ? new GribDataReader.DataReceiver(section) : new GribDataReader.ChannelReceiver(channel, yRange, xRange);
    dataReader.read(dataReceiver);
    return dataReceiver.getArray();
  }

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
    GribDataReader dataReader = GribDataReader.factory(gribCollection, vindexP);
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
        // vindexP.getDataRecord(indexWanted); // debug
        resultPos++;
        continue;
      }
      record.resultIndex = resultPos;
      dataReader.addPartitionedRecord(record);
      resultPos++;
    }

    // sort by file and position, then read
    GribDataReader.DataReceiverIF dataReceiver = (channel == null) ? new GribDataReader.DataReceiver(section) : new GribDataReader.ChannelReceiver(channel, yRange, xRange);
    dataReader.readPartitioned(dataReceiver);

    return dataReceiver.getArray();
  }


  /* LOOK this is by Variable - might want to do over variables, so only touch a file once, if multiple variables in a file
  @Override
  public long streamToByteChannel(ucar.nc2.Variable v2, Section section, WritableByteChannel channel)
          throws java.io.IOException, ucar.ma2.InvalidRangeException {

    long start = System.currentTimeMillis();

    /* if (isPartitioned)
      streamDataFromPartition(v2, section, channel);
    else
    readDataFromCollection(v2, section, channel);

    long took = System.currentTimeMillis() - start;
    return 0;
  } */

  ///////////////////////////////////////
  // debugging back door
  public abstract Object getLastRecordRead();

  public abstract void clearLastRecordRead();

  public abstract Object getGribCustomizer();
}
