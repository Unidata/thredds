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

import net.jcip.annotations.ThreadSafe;
import org.jdom.Element;
import thredds.inventory.FeatureCollectionConfig;
import ucar.nc2.*;
import ucar.nc2.constants.CF;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.ma2.*;

import java.io.IOException;
import java.util.*;

/**
 * Helper class for Fmrc.
 * The various GridDatasets must be thread-safe.
 * <p/>
 * Time coordinate values come from the FmrcInv, so there is little I/O here.
 * Non-aggregation variables are either cached or have DatasetProxyReaders ser so the file is opened when the variable needs to be read.
 * <p/>
 * The prototype dataset is kept separate, since the common case is that just the time coordinates have changed.
 * <p/>
 * This replaces ucar.nc2.dt.fmrc.FmrcImpl
 *
 *
 * @author caron
 * @since Jan 19, 2010
 */
@ThreadSafe
class FmrcDataset {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FmrcDataset.class);
  static private final boolean debugEnhance = false, debugRead = false;

  private final FeatureCollectionConfig.Config config;
  private final Element ncmlOuter, ncmlInner;

  //private List<String> protoList; // the list of datasets in the proto that have proxy reader, so these need to exist. not implemented yet

  // allow to build a new state while old state can still be safely used
  private class State {
    NetcdfDataset proto; // once built, the proto doesnt change until setInventory is called with forceProto = true
    FmrcInvLite lite; // lightweight version of the FmrcInv

    private State(NetcdfDataset proto, FmrcInvLite lite) {
      this.proto = proto;
      this.lite = lite;
    }
  }
  private State state;
  private Object lock= new Object();

  FmrcDataset(FeatureCollectionConfig.Config config, Element ncmlInner, Element ncmlOuter) {
    this.config = config;
    this.ncmlInner = ncmlInner;
    this.ncmlOuter = ncmlOuter;
  }

  List<Date> getRunDates() {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return localState.lite.getRunDates();
  }

  List<Date> getForecastDates() {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return localState.lite.getForecastDates();
  }

  double[] getForecastOffsets() {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return localState.lite.getForecastOffsets();
  }

  /**
   * Make the 2D dataset
   * @param result     use this empty NetcdfDataset, may be null (used by NcML)
   * @return 2D dataset
   * @throws IOException on read error
   */
  GridDataset getNetcdfDataset2D(NetcdfDataset result) throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset2D(result,  localState.proto, localState.lite);
  }

  GridDataset getBest() throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset1D( localState.proto, localState.lite, localState.lite.makeBestDatasetInventory());
  }

  GridDataset getBest(FeatureCollectionConfig.BestDataset bd) throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset1D( localState.proto, localState.lite, localState.lite.makeBestDatasetInventory(bd));
  }

  GridDataset getRunTimeDataset(Date run) throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset1D(  localState.proto, localState.lite, localState.lite.makeRunTimeDatasetInventory( run));
  }

  GridDataset getConstantForecastDataset(Date run) throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset1D(  localState.proto, localState.lite, localState.lite.getConstantForecastDataset( run));
  }

  GridDataset getConstantOffsetDataset(double offset) throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset1D(  localState.proto, localState.lite, localState.lite.getConstantOffsetDataset( offset));
  }

  /**
   * Set a new FmrcInv and optionally a proto dataset
   *
   * @param fmrcInv    based on this inventory
   * @param forceProto create a new proto, else use existing one
   * @throws IOException on read error
   */
  void setInventory(FmrcInv fmrcInv, boolean forceProto) throws IOException {
    NetcdfDataset protoLocal = null;

    // make new proto if needed
    if (state == null || forceProto) {
      protoLocal = buildProto(fmrcInv, config.protoConfig);

      if (ncmlOuter != null) {
        protoLocal = NcMLReader.mergeNcMLdirect(protoLocal, ncmlOuter);
      }
    }

    // switch to new FmrcInvLite
    FmrcInvLite liteLocal = new FmrcInvLite(fmrcInv);
    synchronized (lock) {
      if (protoLocal == null) protoLocal = state.proto;
      state = new State(protoLocal, liteLocal);
    }
  }

   /////////////////////////////////////////////////////////////////////////////
  // the prototypical dataset

  private NetcdfDataset buildProto(FmrcInv fmrcInv, FeatureCollectionConfig.ProtoConfig protoConfig) throws IOException {
    NetcdfDataset result = new NetcdfDataset(); // empty

    // choose some run in the list
    List<FmrInv> list = fmrcInv.getFmrInv();
    if (list.size() == 0) {
      logger.error("Fmrc collection is empty ="+fmrcInv.getName());
      throw new IllegalStateException("Fmrc collection is empty ="+fmrcInv.getName());
    }

    int protoIdx = 0;
    switch (protoConfig.choice) {
      case First:
        protoIdx = 0;
        break;
      case Random:
        Random r = new Random(System.currentTimeMillis());
        protoIdx = r.nextInt(list.size() - 1);
        break;
      case Penultimate:
        protoIdx = Math.max(list.size() - 2, 0);
        break;
      case Latest:
        protoIdx = Math.max(list.size() - 1, 0);
        break;
    }
    FmrInv proto = list.get(protoIdx); // use this one

    HashMap<String, NetcdfDataset> openFilesProto = new HashMap<String, NetcdfDataset>();

    try {
      // create the union of all objects in that run
      // this covers the case where the variables are split across files
      Set<GridDatasetInv> files = proto.getFiles();
      if (logger.isDebugEnabled())
        logger.debug("FmrcDataset: proto= " + proto.getName() + " " + proto.getRunDate() + " collection= " + fmrcInv.getName());
      for (GridDatasetInv file : files) {
        NetcdfDataset ncfile = open(file.getLocation(), openFilesProto);
        transferGroup(ncfile.getRootGroup(), result.getRootGroup(), result);
        if (logger.isDebugEnabled()) logger.debug("FmrcDataset: proto dataset= " + file.getLocation());
      }

      // some additional global attributes
      Group root = result.getRootGroup();
      root.addAttribute(new Attribute("Conventions", "CF-1.4, " + _Coordinate.Convention));
      root.addAttribute(new Attribute("cdm_data_type", FeatureType.GRID.toString()));
      root.addAttribute(new Attribute("CF:feature_type", FeatureType.GRID.toString()));
      root.addAttribute(new Attribute("location", "Proto "+fmrcInv.getName()));

      // remove some attributes that can cause trouble
      root.remove(root.findAttribute(_Coordinate.ModelRunDate));

      // protoList = new ArrayList<String>();
      // these are the non-agg variables - store data or ProxyReader in proto
      List<Variable> copyList = new ArrayList<Variable>(root.getVariables()); // use copy since we may be removing some variables
      for (Variable v : copyList) {
        // see if its a non-agg variable
        FmrcInv.UberGrid grid = fmrcInv.findUberGrid(v.getName());
        if (grid == null) { // only non-agg vars need to be cached
          Variable orgV = (Variable) v.getSPobject();
          if (orgV.getSize() > 10 * 1000 * 1000)
            logger.info("FMRCDataset build Proto cache >10M var= "+orgV.getName());
          v.setCachedData(orgV.read()); // read from original - store in proto
        }

        v.setSPobject(null); // clear the reference to orgV for all of proto
      }

      result.finish();
      
      // enhance the proto. becomes the template for coordSystems in the derived datasets
      CoordSysBuilderIF builder = result.enhance();
      if (debugEnhance) System.out.printf("proto.enhance() parseInfo = %s%n", builder.getParseInfo());

      // turn it into a GridDataset, so we can add standard metadata to result, not dependent on CoordSysBuilder
      // also see  ucar.nc2.dt.grid.NetcdfCFWriter - common code could be extracted
      Formatter parseInfo = new Formatter();
      GridDataset gds = new ucar.nc2.dt.grid.GridDataset(result, parseInfo); // LOOK not sure coord axes will read ??
      if (debugEnhance) System.out.printf("proto GridDataset parseInfo = %s%n", parseInfo);

      // now make standard CF metadata for gridded data
      for (GridDatatype grid : gds.getGrids()) {
        Variable newV = result.findVariable(grid.getName());
        if (newV == null) {
          logger.warn("FmrcDataset cant find "+grid.getName()+" in proto gds ");
          continue;
        }

        // annotate Variable for CF
        StringBuilder sbuff = new StringBuilder();
        GridCoordSystem gcs = grid.getCoordinateSystem();
        for (CoordinateAxis axis : gcs.getCoordinateAxes()) {
          if ((axis.getAxisType() != AxisType.Time) && (axis.getAxisType() != AxisType.RunTime)) // these are added later
            sbuff.append(axis.getName()).append(" ");
        }
        newV.addAttribute(new Attribute("coordinates", sbuff.toString())); // LOOK what about adding lat/lon variable

        // looking for coordinate transform variables
        for (CoordinateTransform ct : gcs.getCoordinateTransforms()) {
          Variable ctv = result.findVariable(ct.getName());
          if ((ctv != null) && (ct.getTransformType() == TransformType.Projection))
            newV.addAttribute(new Attribute("grid_mapping", ctv.getName()));
        }

        // LOOK is this needed ?
        for (CoordinateAxis axis : gcs.getCoordinateAxes()) {
          Variable coordV = result.findVariable(axis.getName());
          if ((axis.getAxisType() == AxisType.Height) || (axis.getAxisType() == AxisType.Pressure) || (axis.getAxisType() == AxisType.GeoZ)) {
            if (null != axis.getPositive())
              coordV.addAttribute(new Attribute("positive", axis.getPositive()));
          }
          if (axis.getAxisType() == AxisType.Lat) {
            coordV.addAttribute(new Attribute("units", "degrees_north"));
            coordV.addAttribute(new Attribute("standard_name", "latitude"));
          }
          if (axis.getAxisType() == AxisType.Lon) {
            coordV.addAttribute(new Attribute("units", "degrees_east"));
            coordV.addAttribute(new Attribute("standard_name", "longitude"));
          }
          if (axis.getAxisType() == AxisType.GeoX) {
            coordV.addAttribute(new Attribute("standard_name", "projection_x_coordinate"));
          }
          if (axis.getAxisType() == AxisType.GeoY) {
            coordV.addAttribute(new Attribute("standard_name", "projection_y_coordinate"));
          }
          if (axis.getAxisType() == AxisType.Time) {
            Attribute att = axis.findAttribute("bounds");  // LOOK nasty : remove time bounds from proto
            if ((att != null) && att.isString())
              result.removeVariable(null, att.getStringValue());
          }
        }
      }

      // more troublesome attributes, use pure CF
      for (Variable v : result.getVariables()) {
        Attribute att = null;
        if (null != (att = v.findAttribute(_Coordinate.Axes.toString())))
           v.remove(att);
        if (null != (att = v.findAttribute(_Coordinate.Systems.toString())))
           v.remove(att);
        if (null != (att = v.findAttribute(_Coordinate.SystemFor.toString())))
          v.remove(att);
        if (null != (att = v.findAttribute(_Coordinate.Transforms.toString())))
          v.remove(att);
      }

      // apply ncml if it exists
      if (protoConfig.ncml != null)
        NcMLReader.mergeNcMLdirect(result, protoConfig.ncml);

      return result;

    } finally {
      // data is read and cached, can close files now
      closeAll(openFilesProto);
    }
  }

  // transfer the objects in src group to the target group, unless that name already exists
  // Dimensions, Variables, Groups are not transferred, but an equivalent object is created with same metadata
  // Attributes and EnumTypedef are transferred, these are immutable with no references to container

  private void transferGroup(Group srcGroup, Group targetGroup, NetcdfDataset target) throws IOException {
    // group attributes
    DatasetConstructor.transferGroupAttributes(srcGroup, targetGroup);

    // dimensions
    for (Dimension d : srcGroup.getDimensions()) {
      if (null == targetGroup.findDimensionLocal(d.getName())) {
        Dimension newd = new Dimension(d.getName(), d.getLength(), d.isShared(), d.isUnlimited(), d.isVariableLength());
        targetGroup.addDimension(newd);
      }
    }

    // transfer variables - eliminate any references to component files
    for (Variable v : srcGroup.getVariables()) {
      Variable targetV = targetGroup.findVariable(v.getShortName());

      if (null == targetV) { // add it
        if (v instanceof Structure) {
          targetV = new StructureDS(target, targetGroup, null, v.getShortName(), v.getDimensionsString(), v.getUnitsString(), v.getDescription());
          //LOOK - not adding the members here - what to do ??

        } else {
          targetV = new VariableDS(target, targetGroup, null, v.getShortName(), v.getDataType(), v.getDimensionsString(), v.getUnitsString(), v.getDescription());
        }

        DatasetConstructor.transferVariableAttributes(v, targetV);
        VariableDS vds = (VariableDS) v;
        targetV.setSPobject(vds); //temporary, for non-agg variables when proto is made
        if (vds.hasCachedDataRecurse())
          targetV.setCachedData(vds.read()); //
        targetGroup.addVariable(targetV);
      }
    }

    // nested groups - check if target already has it
    for (Group srcNested : srcGroup.getGroups()) {
      Group nested = targetGroup.findGroup(srcNested.getShortName());
      if (null == nested) {
        nested = new Group(target, targetGroup, srcNested.getShortName());
        targetGroup.addGroup(nested);
        for (EnumTypedef et : srcNested.getEnumTypedefs()) {
          targetGroup.addEnumeration(et);
        }
      }
      transferGroup(srcNested, nested, target);
    }
  }

  /////////////////////////////////////////////////////////
  // constructing the dataset

  // private static final String FTIME_PREFIX = "";

  private String getRunDimensionName() {
    return "run";
  }

  private String makeCoordinateList(VariableDS aggVar, String timeCoordName, boolean is2D) {
    String coords = "";
    Attribute att = aggVar.findAttribute(CF.COORDINATES);
    if (att == null)
      att = aggVar.findAttribute(_Coordinate.Axes);    
    if (att != null)
      coords = att.getStringValue();

    if (is2D)
      return getRunDimensionName() + " " + timeCoordName + " " + coords;
    else
      return timeCoordName + "_" +  getRunDimensionName() + " " + timeCoordName + " " + coords;
  }

  private void addAttributeInfo(NetcdfDataset result, String attName, String info) {
    Attribute att = result.findGlobalAttribute(attName);
    if (att == null)
      result.addAttribute(null, new Attribute(attName, info));
    else {
      String oldValue = att.getStringValue();
      result.addAttribute(null, new Attribute(attName, oldValue +" ;\n"+ info));
    }
  }

  /**
   * Build the 2D time dataset, make it immutable so it can be shared across threads
   *
   * @param result     place results in here, if null create a new one. must be threadsafe (immutable)
   * @param proto      current proto dataset
   * @param lite       current inventory
   * @return resulting GridDataset
   * @throws IOException on read error
   */
  private GridDataset buildDataset2D(NetcdfDataset result, NetcdfDataset proto, FmrcInvLite lite) throws IOException {
    // make a copy, so that this object can coexist with previous incarnations
    if (result == null) result = new NetcdfDataset();
    result.setLocation(lite.collectionName);
    transferGroup(proto.getRootGroup(), result.getRootGroup(), result);
    result.finish();
    //CoordSysBuilderIF builder = result.enhance();
    //if (debugEnhance) System.out.printf("buildDataset2D.enhance() parseInfo = %s%n", builder.getParseInfo());

    addAttributeInfo(result, "history", "FMRC 2D Dataset");

    // create runtime aggregation dimension
    double[] runOffset = lite.runOffset;
    String runtimeDimName = getRunDimensionName();
    int nruns = runOffset.length;
    Dimension runDim = new Dimension(runtimeDimName, nruns);
    result.removeDimension(null, runtimeDimName); // remove previous declaration, if any
    result.addDimension(null, runDim);

    // deal with promoteGlobalAttribute
    // promoteGlobalAttributes((AggregationOuterDimension.DatasetOuterDimension) typicalDataset);

    DateFormatter dateFormatter = new DateFormatter();
    ProxyReader2D proxyReader2D = new ProxyReader2D();

    // extract a copy of the runtimes for thread safety
    // List<Date> runTimes = new ArrayList<Date>(fmrcInv.getRunTimes());

    // create runtime aggregation coordinate variable
    DataType coordType = DataType.DOUBLE; // LOOK getCoordinateType();
    VariableDS runtimeCoordVar = new VariableDS(result, null, null, runtimeDimName, coordType, runtimeDimName, null, null);
    runtimeCoordVar.addAttribute(new Attribute("long_name", "Run time for ForecastModelRunCollection"));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute("standard_name", "forecast_reference_time"));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO(lite.base)));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
    result.removeVariable(null, runtimeCoordVar.getShortName());
    result.addVariable(null, runtimeCoordVar);
    if (logger.isDebugEnabled()) logger.debug("FmrcDataset: added runtimeCoordVar " + runtimeCoordVar.getName());

    // make the runtime coordinates
    Array runCoordVals = ArrayDouble.factory(DataType.DOUBLE, new int[] {nruns}, runOffset);
    runtimeCoordVar.setCachedData(runCoordVals);

    // make the time coordinate(s) as 2D
    List<Variable> nonAggVars = result.getVariables();
    for (FmrcInvLite.Gridset gridset : lite.gridSets) {
      Group newGroup = result.getRootGroup(); // can it be different ??

      //int noffsets = runSeq.getNTimeOffsets();
      Dimension timeDim = new Dimension(gridset.gridsetName, gridset.noffsets);
      result.removeDimension(null, gridset.gridsetName); // remove previous declaration, if any
      result.addDimension(null, timeDim);

      DataType dtype = DataType.DOUBLE;
      String dims = getRunDimensionName() + " " + gridset.gridsetName;
      VariableDS timeVar = new VariableDS(result, newGroup, null, gridset.gridsetName, dtype, dims, null, null); // LOOK could just make a CoordinateAxis1D
      timeVar.addAttribute(new Attribute("long_name", "Forecast time for ForecastModelRunCollection"));
      timeVar.addAttribute(new ucar.nc2.Attribute("standard_name", "time"));
      timeVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO(lite.base)));
      timeVar.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));
      timeVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

      // remove the old one if any
      newGroup.removeVariable(gridset.gridsetName);
      newGroup.addVariable(timeVar);

      Array timeCoordVals = Array.factory(DataType.DOUBLE, timeVar.getShape(), gridset.timeOffset);
      timeVar.setCachedData(timeCoordVals);

      // promote all grid variables to agg variables
      for (FmrcInvLite.Gridset.Grid ugrid : gridset.grids) {
        VariableDS aggVar = (VariableDS) result.findVariable(ugrid.name);
        if (aggVar == null) {
          logger.error("cant find ugrid variable "+ugrid.name+" in collection "+lite.collectionName);
          continue; // skip
        }

        // create dimension list
        List<Dimension> dimList = aggVar.getDimensions();
        dimList = dimList.subList(1, dimList.size());  // LOOK assumes time is outer dimension
        dimList.add(0, timeDim);
        dimList.add(0, runDim);

        aggVar.setDimensions(dimList);
        aggVar.setProxyReader(proxyReader2D);
        aggVar.setSPobject(ugrid);
        nonAggVars.remove(aggVar);

        // we need to explicitly list the coordinate axes, because time coord is now 2D
        String coords = makeCoordinateList(aggVar, gridset.gridsetName, true);
        aggVar.removeAttribute(_Coordinate.Axes);
        aggVar.addAttribute(new Attribute(CF.COORDINATES, coords));

        /* transfer Coordinate Systems
        VariableDS protoV = (VariableDS) proto.findVariable(aggVar.getName());
        for (CoordinateSystem protoCs : protoV.getCoordinateSystems()) {
          CoordinateSystem cs = findReplacementCs(protoCs, gridset.gridsetName, result);
          aggVar.addCoordinateSystem(cs);
        } */
      }
    }

    result.finish();  // this puts the new dimensions into the global structures

    //CoordSysBuilderIF builder = result.enhance();
    //if (debugEnhance) System.out.printf("parseInfo = %s%n", builder.getParseInfo());

    // LOOK better not to do this when you only want the NetcdfDataset
    Formatter parseInfo = new Formatter();
    GridDataset gds = new ucar.nc2.dt.grid.GridDataset(result, parseInfo);
    if (debugEnhance) System.out.printf("GridDataset2D parseInfo = %s%n", parseInfo);
    return gds;
  }

  private CoordinateSystem findReplacementCs(CoordinateSystem protoCs, String timeDim, NetcdfDataset result) {
    CoordinateSystem replace = result.findCoordinateSystem(protoCs.getName());
    if (replace != null) return replace;

    List<CoordinateAxis> axes = new ArrayList<CoordinateAxis>();
    for (CoordinateAxis axis : protoCs.getCoordinateAxes()) {
      Variable v = result.findCoordinateAxis(axis.getName());
      CoordinateAxis ra;
      if (v instanceof CoordinateAxis)
        ra = (CoordinateAxis) v;
      else {
        // if not a CoordinateAxis, will turn into one
        ra = result.addCoordinateAxis((VariableDS) v);

        if (axis.getAxisType() != null) {
          ra.setAxisType(axis.getAxisType());
          ra.addAttribute(new Attribute(_Coordinate.AxisType, axis.getAxisType().toString()));
        }
      }
      axes.add(ra);
    }

    // coord transforms are immutable and can be shared
    CoordinateSystem cs = new CoordinateSystem(result, axes, protoCs.getCoordinateTransforms());
    result.addCoordinateSystem(cs);
    return cs;
  }

  /*
  private ArrayDouble.D2 makeTimeCoordinateData2D(Date baseDate, FmrcInv.RunSeq runSeq, VariableDS timeVar) { // throws IOException {
    int[] shape = timeVar.getShape();
    int nruntimes = shape[0];
    int noffsets = shape[1];

    ArrayDouble.D2 timeCoordVals = new ArrayDouble.D2(nruntimes, noffsets);
    Index ima = timeCoordVals.getIndex();
    MAMath.setDouble(timeCoordVals, Double.NaN);  // all default to missing

    int runIdx = 0;
    for (TimeCoord tc : runSeq.getTimes()) {
      double runOffset = FmrcInv.getOffsetInHours(baseDate, tc.getRunDate());
      double[] offset = tc.getOffsetHours();
      for (int j = 0; j < offset.length; j++)
        timeCoordVals.setDouble(ima.set(runIdx, j), offset[j] + runOffset); // offset reletive to base date
      runIdx++;
    }

    return timeCoordVals;
  }

  // keep track of the ugrid and 2D time coords for each variable, put in SPobject

  private class Vstate2D {
    String varName;
    ArrayObject.D2 location;
    ArrayInt.D2 invIndex;
    Date baseDate;

    private Vstate2D(FmrcInv.UberGrid ugrid, Date baseDate, ArrayDouble.D2 timeCoordVals, List<Date> runDates) {
      this.varName = ugrid.getName();
      this.baseDate = baseDate;

      int[] shape = timeCoordVals.getShape();
      int nruns = shape[0]; // this will always equal the complete set of runs
      int ntimes = shape[1];

      this.location = (ArrayObject.D2) Array.factory(DataType.STRING, shape);
      this.invIndex = (ArrayInt.D2) Array.factory(DataType.INT, shape);

      // loop over runDates
      int gridIdx = 0;
      List<FmrInv.GridVariable> grids = ugrid.getRuns(); // must be sorted by rundate

      for (int runIdx=0; runIdx<nruns; runIdx++ ) {
        Date runDate =  runDates.get(runIdx);

        // do we have a grid for this runDate?
        FmrInv.GridVariable grid = grids.get(gridIdx);
        if (!grid.getRunDate().equals(runDate)) continue;
        gridIdx++; // for next loop

        // loop over actual inventory
        for (GridDatasetInv.Grid inv : grid.getInventory()) {
          double invOffset = FmrcInv.getOffsetInHours(baseDate, inv.tc.getRunDate()); // offset reletive to inv
          double[] offsets = inv.tc.getOffsetHours();
          for (int i = 0; i < offsets.length; i++) {
            int timeIdx = findIndex(timeCoordVals, runIdx, ntimes, invOffset + offsets[i]);
            if (timeIdx >= 0) {
              location.set(runIdx, timeIdx, inv.getLocation());
              invIndex.set(runIdx, timeIdx, i);
            }
          }
        }
      }

    }

    // look in the runIdx row of coords to see if a value matches want, return index else -1
    private int findIndex(ArrayDouble.D2 coords, int runIdx, int ntimes, double want) {
      for (int j=0; j<ntimes; j++)
        if (Misc.closeEnough(coords.get(runIdx, j), want)) return j;
      return -1;
    }

    private TimeInstance findInventory(int runIdx, int timeIdx) {
      String loc = (String) location.get(runIdx, timeIdx);
      if (loc == null) return null;
      return new TimeInstance(loc, invIndex.get(runIdx, timeIdx));
    }

  }  */

  private class ProxyReader2D implements ProxyReader {

    @Override
    public Array reallyRead(Variable mainv, CancelTask cancelTask) throws IOException {
      try {
        return reallyRead(mainv, mainv.getShapeAsSection(), cancelTask);

      } catch (InvalidRangeException e) {
        throw new IOException(e);
      }
    }

    // here is where agg variables get read

    @Override
    public Array reallyRead(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      FmrcInvLite.Gridset.Grid gridLite = (FmrcInvLite.Gridset.Grid) mainv.getSPobject();

      // read the original type - if its been promoted to a new type, the conversion happens after this read
      DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();

      Array allData = Array.factory(dtype, section.getShape());
      int destPos = 0;

      // assumes the first two dimensions are runtime and time: LOOK: ensemble ??
      List<Range> ranges = section.getRanges();
      Range runRange = ranges.get(0);
      Range timeRange = ranges.get(1);
      List<Range> innerSection = ranges.subList(2, ranges.size());

      // keep track of open file - must be local variable for thread safety
      HashMap<String, NetcdfDataset> openFilesRead = new HashMap<String, NetcdfDataset>();
      try {

        // iterate over the desired runs
        Range.Iterator runIter = runRange.getIterator();
        while (runIter.hasNext()) {
          int runIdx = runIter.next();
          //Date runDate = vstate.runTimes.get(runIdx);

          // iterate over the desired forecast times
          Range.Iterator timeIter = timeRange.getIterator();
          while (timeIter.hasNext()) {
            int timeIdx = timeIter.next();
            Array result = null;

            // find the inventory for this grid, runtime, and hour
            TimeInventory.Instance timeInv = gridLite.getInstance(runIdx, timeIdx);
            if (timeInv != null) {
              if (debugRead) System.out.printf("HIT %d %d ", runIdx, timeIdx);
              result = read(timeInv, gridLite.name, innerSection, openFilesRead); // may return null
              result = MAMath.convert(result, dtype); // just in case it need to be converted
            }

            // missing data
            if (result == null) {
              int[] shape = new Section(innerSection).getShape();
              result = ((VariableDS) mainv).getMissingDataArray(shape); // fill with missing values
              if (debugRead) System.out.printf("MISS %d %d ", runIdx, timeIdx);
            }

            if (debugRead)
              System.out.printf("%d %d reallyRead %s %d bytes start at %d total size is %d%n", runIdx, timeIdx, mainv.getName(), result.getSize(), destPos, allData.getSize());

            Array.arraycopy(result, 0, allData, destPos, (int) result.getSize());
            destPos += result.getSize();
          }
        }
        return allData;

      } finally {
        // close any files used during this operation
        closeAll(openFilesRead);
      }
    }

  }

 /////////////////////////////////////////////////////////////////////////
  // 1D

  /**
   * Build a dataset with a 1D time coordinate.
   *
   * @param proto      current proto dataset
   * @param lite       current inventory
   * @param timeInv         use this to generate the time coordinates
   * @return resulting GridDataset
   * @throws IOException on read error
   */
  private GridDataset buildDataset1D(NetcdfDataset proto, FmrcInvLite lite, TimeInventory timeInv) throws IOException {
    NetcdfDataset result = new NetcdfDataset(); // make a copy, so that this object can coexist with previous incarnations
    result.setLocation(lite.collectionName);
    transferGroup(proto.getRootGroup(), result.getRootGroup(), result);
    result.finish();
    addAttributeInfo(result, "history", "FMRC "+timeInv.getName()+" Dataset");

    DateFormatter dateFormatter = new DateFormatter();
    ProxyReader1D proxyReader1D = new ProxyReader1D();

    // make the time coordinate(s) for each runSeq
    List<Variable> nonAggVars = result.getVariables();
    for (FmrcInvLite.Gridset gridset : lite.gridSets) {

      Group group = result.getRootGroup(); // can it be different ??
      String timeDimName = gridset.gridsetName;

      // construct the dimension
      int ntimes = timeInv.getTimeLength(gridset);
      if (ntimes == 0) {   // eg a constant offset dataset for variables that dont have that offset
        // remove all variables that are in this gridsset
        for (FmrcInvLite.Gridset.Grid ugrid : gridset.grids)
         result.removeVariable(group, ugrid.name);
        continue; // skip the rest
      }

      Dimension timeDim = new Dimension(timeDimName, ntimes);
      result.removeDimension(group, timeDimName); // remove previous declaration, if any
      result.addDimension(group, timeDim);

      // optional time coordinate
      group.removeVariable(timeDimName);
      double[] timeCoordValues = timeInv.getTimeCoords(gridset);
      if (timeCoordValues != null) {
        VariableDS timeCoord = makeTimeCoordinate(result, group, timeDimName, lite.base, timeCoordValues, dateFormatter);
        group.addVariable(timeCoord);
      }

      // optional runtime coordinate
      group.removeVariable(timeDimName+"_run");
      double[] runtimeCoordValues = timeInv.getRunTimeCoords(gridset);
      if (runtimeCoordValues != null) {
        VariableDS runtimeCoord = makeRunTimeCoordinate(result, group, timeDimName, lite.base, runtimeCoordValues, dateFormatter);
        group.addVariable(runtimeCoord);
      }

      // optional offset coordinate
      group.removeVariable(timeDimName+"_offset");
      double[] offsetCoordValues = timeInv.getOffsetCoords(gridset);
      if (offsetCoordValues != null) {
        VariableDS offsetCoord = makeOffsetCoordinate(result, group, timeDimName, lite.base, offsetCoordValues, dateFormatter);
        group.addVariable(offsetCoord);
      }

      // promote all grid variables to agg variables
      for (FmrcInvLite.Gridset.Grid ugrid : gridset.grids) {
        //BestInventory bestInv = makeBestInventory(bestTimeCoord, ugrid);

        VariableDS aggVar = (VariableDS) result.findVariable(ugrid.name);
        if (aggVar == null) {
          logger.error("cant find ugrid variable "+ugrid.name+" in collection "+lite.collectionName);
          continue; // skip
        }

        // create dimension list
        List<Dimension> dimList = aggVar.getDimensions();
        dimList = dimList.subList(1, dimList.size());  // LOOK assumes time is outer dimension
        dimList.add(0, timeDim);

        aggVar.setDimensions(dimList);
        aggVar.setProxyReader(proxyReader1D);
        aggVar.setSPobject( new Vstate1D(ugrid, timeInv));
        nonAggVars.remove(aggVar);

        // we need to explicitly list the coordinate axes
        String coords = makeCoordinateList(aggVar, timeDimName, false);
        aggVar.removeAttribute(_Coordinate.Axes);
        aggVar.addAttribute(new Attribute(CF.COORDINATES, coords)); // CF

       // if (logger.isDebugEnabled()) logger.debug("FmrcDataset: added grid " + aggVar.getName());
      }
    }

    result.finish();  // this puts the new dimensions into the global structures

    // these are the non-agg variables - get data or ProxyReader from proto
    for (Variable v : nonAggVars) {
      VariableDS protoV = (VariableDS) proto.findVariable(v.getName());
      if (protoV.hasCachedDataRecurse()) {
        v.setCachedData(protoV.read()); // read from original
      } else {
        v.setProxyReader(protoV.getProxyReader());
      }
    }

    CoordSysBuilderIF builder = result.enhance();
    if (debugEnhance) System.out.printf("GridDataset1D parseInfo = %s%n", builder.getParseInfo());

    return new ucar.nc2.dt.grid.GridDataset(result);
  }

  private VariableDS makeTimeCoordinate(NetcdfDataset result, Group group, String dimName, Date base, double[] values, DateFormatter dateFormatter) {
    DataType dtype = DataType.DOUBLE;
    VariableDS timeVar = new VariableDS(result, group, null, dimName, dtype, dimName, null, null); // LOOK could just make a CoordinateAxis1D
    timeVar.addAttribute(new Attribute("long_name", "Forecast time for ForecastModelRunCollection"));
    timeVar.addAttribute(new ucar.nc2.Attribute("standard_name", "time"));
    timeVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO( base)));
    timeVar.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));
    timeVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    // construct the values
    int ntimes = values.length;
    ArrayDouble.D1 timeCoordVals = (ArrayDouble.D1) Array.factory( DataType.DOUBLE, new int[] {ntimes}, values);
    timeVar.setCachedData(timeCoordVals);

    return timeVar;
  }

  private VariableDS makeRunTimeCoordinate(NetcdfDataset result, Group group, String dimName, Date base, double[] values, DateFormatter dateFormatter) {
    DataType dtype = DataType.DOUBLE;
    VariableDS timeVar = new VariableDS(result, group, null, dimName+"_run", dtype, dimName, null, null); // LOOK could just make a CoordinateAxis1D
    timeVar.addAttribute(new Attribute("long_name", "run times for coordinate = " + dimName));
    timeVar.addAttribute(new ucar.nc2.Attribute("standard_name", "forecast_reference_time"));
    timeVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO( base)));
    timeVar.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));
    timeVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));

    // construct the values
    int ntimes = values.length;
    ArrayDouble.D1 timeCoordVals = (ArrayDouble.D1) Array.factory( DataType.DOUBLE, new int[] {ntimes}, values);
    timeVar.setCachedData(timeCoordVals);

    return timeVar;
  }

  private VariableDS makeOffsetCoordinate(NetcdfDataset result, Group group, String dimName, Date base, double[] values, DateFormatter dateFormatter) {
    DataType dtype = DataType.DOUBLE;
    VariableDS timeVar = new VariableDS(result, group, null, dimName+"_offset", dtype, dimName, null, null); // LOOK could just make a CoordinateAxis1D
    timeVar.addAttribute(new Attribute("long_name", "offset hour from start of run for coordinate = " + dimName));
    timeVar.addAttribute(new ucar.nc2.Attribute("standard_name", "forecast_period"));
    timeVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO( base)));
    timeVar.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));

    // construct the values
    int ntimes = values.length;
    ArrayDouble.D1 timeCoordVals = (ArrayDouble.D1) Array.factory( DataType.DOUBLE, new int[] {ntimes}, values);
    timeVar.setCachedData(timeCoordVals);

    return timeVar;
  }

  private class Vstate1D {
    FmrcInvLite.Gridset.Grid gridLite;
    TimeInventory timeInv;

    private Vstate1D(FmrcInvLite.Gridset.Grid gridLite, TimeInventory timeInv) {
      this.gridLite = gridLite;
      this.timeInv = timeInv;
    }
  }

  private class ProxyReader1D implements ProxyReader {

    @Override
    public Array reallyRead(Variable mainv, CancelTask cancelTask) throws IOException {
      try {
        return reallyRead(mainv, mainv.getShapeAsSection(), cancelTask);

      } catch (InvalidRangeException e) {
        throw new IOException(e);
      }
    }

    // here is where 1D agg variables get read
    @Override
    public Array reallyRead(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      Vstate1D vstate = (Vstate1D) mainv.getSPobject();

     // read the original type - if its been promoted to a new type, the conversion happens after this read
     DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();

     Array allData = Array.factory(dtype, section.getShape());
     int destPos = 0;

     // assumes the first dimension is time: LOOK: what about ensemble ??
     List<Range> ranges = section.getRanges();
     Range timeRange = ranges.get(0);
     List<Range> innerSection = ranges.subList(1, ranges.size());

       // keep track of open files - must be local variable for thread safety
      HashMap<String, NetcdfDataset> openFilesRead = new HashMap<String, NetcdfDataset>();

      try {

        // iterate over the desired forecast times
        Range.Iterator timeIter = timeRange.getIterator();
        while (timeIter.hasNext()) {
          int timeIdx = timeIter.next();
          Array result = null;

          // find the inventory for this grid, runtime, and hour
          TimeInventory.Instance timeInv =  vstate.timeInv.getInstance(vstate.gridLite, timeIdx);
          if (timeInv == null)
            logger.error("Missing Inventory timeInx="+timeIdx+ " for "+ mainv.getName()+" in "+state.lite.collectionName);
          
          if (timeInv.getDatasetLocation() != null) {
            if (debugRead) System.out.printf("HIT %d ", timeIdx);
            result = read(timeInv, mainv.getName(), innerSection, openFilesRead); // may return null
            result = MAMath.convert(result, dtype); // just in case it need to be converted
          }

          // may have missing data
          if (result == null) {
            int[] shape = new Section(innerSection).getShape();
            result = ((VariableDS) mainv).getMissingDataArray(shape); // fill with missing values
            if (debugRead) System.out.printf("MISS %d ", timeIdx);
          }

          if (debugRead)
            System.out.printf("%d reallyRead %s %d bytes start at %d total size is %d%n", timeIdx, mainv.getName(), result.getSize(), destPos, allData.getSize());

          Array.arraycopy(result, 0, allData, destPos, (int) result.getSize());
          destPos += result.getSize();
        }
        return allData;

      } finally {
        // close any files used during this operation
        closeAll(openFilesRead);
      }
    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  /* private BestInventory makeBestInventory(TimeCoord.TimeResult bestTimeCoord, FmrcInv.UberGrid ugrid) {
    BestInventory bestInv = new BestInventory(bestTimeCoord, ugrid);

    // each run
    for (FmrInv.GridVariable grid : ugrid.getRuns()) {

      // each file in the run
      for (GridDatasetInv.Grid inv : grid.getInventory()) {
        double invOffset = FmrcInv.getOffsetInHours(bestTimeCoord.base, inv.tc.getRunDate()); // offset reletive to inv

        // set of offsets are relative to inv.tc.getRunDate()
        double[] offsets = inv.tc.getOffsetHours();
        for (int i = 0; i < offsets.length; i++) {
          bestInv.setOffset(offsets[i] + invOffset, inv.getLocation(), i); // later ones override
        }
      }
    }

    return bestInv;
  }

  public static class BestInventory {
    String varName;
    double[] offsets;
    String[] location; // file location
    int[] invIndex; // the index in the file

    BestInventory(TimeCoord.TimeResult tc, FmrcInv.UberGrid ugrid) {
      this.varName = ugrid.getName();
      this.offsets = tc.offsets; // all the TimeCoords possible
      this.location = new String[tc.offsets.length];
      this.invIndex = new int[tc.offsets.length];
    }

    void setOffset(double offsetHour, String location, int invIndex) {
      int offsetIndex = findIndex(offsetHour);
      if (offsetIndex < 0)
        throw new IllegalStateException("BestInventory cant find hour " + offsetHour + " in " + varName);
      this.location[offsetIndex] = location;
      this.invIndex[offsetIndex] = invIndex;
    }

    // linear search - barf
     private int findIndex(double offsetHour) {
      for (int i = 0; i < offsets.length; i++)
        if (Misc.closeEnough(offsets[i], offsetHour))
          return i;
      return -1;
    }

  }  */

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // the general case is to get only one time per read - probably not too inefficient, eg GRIB, except maybe for remote reads

  private Array read(TimeInventory.Instance timeInstance, String varName, List<Range> innerSection, HashMap<String, NetcdfDataset> openFilesRead) throws IOException, InvalidRangeException {
    NetcdfFile ncfile = open(timeInstance.getDatasetLocation(), openFilesRead);
    if (ncfile == null) return null; // file might be deleted ??

    Variable v = ncfile.findVariable(varName);
    if (v == null) return null; // v could be missing, return missing data i think

    // assume time is first dimension LOOK: out of-order; ensemble; section different ??
    Range timeRange = new Range(timeInstance.getDatasetIndex(), timeInstance.getDatasetIndex());
    Section s = new Section(innerSection);
    s.insertRange(0, timeRange);
    return v.read(s);
  }


  /**
   * Open a file, keep track of open files
   * @param location open this location
   * @param openFiles keep track of open files
   * @return file or null if not found
   */
  private NetcdfDataset open(String location, HashMap<String, NetcdfDataset> openFiles)  { // } throws IOException {
    NetcdfDataset ncd = null;

    if (openFiles != null) {
      ncd = openFiles.get(location);
      if (ncd != null) return ncd;
    }

    try {
      if (ncmlInner == null) {
        ncd = NetcdfDataset.acquireDataset(location, null);  // default enhance

      } else {
        NetcdfFile nc = NetcdfDataset.acquireFile(location, null);
        ncd = NcMLReader.mergeNcML(nc, ncmlInner); // create new dataset
        ncd.enhance(); // now that the ncml is added, enhance "in place", ie modify the NetcdfDataset
      }
    } catch (IOException ioe) {
      logger.error("Cant open file ", ioe);  // file was deleted ??
      return null;
    }

    if (openFiles != null && ncd != null) {
      openFiles.put(location, ncd);
    }

    return ncd;
  }

  /*  from Aggregation.Dataset
  public NetcdfFile acquireFile(CancelTask cancelTask) throws IOException {
    if (debugOpenFile) System.out.println(" try to acquire " + cacheLocation);
    long start = System.currentTimeMillis();

    NetcdfFile ncfile = NetcdfDataset.acquireFile(reader, null, cacheLocation, -1, cancelTask, spiObject);

    // must merge NcML before enhancing
    if (mergeNcml != null)
      ncfile = NcMLReader.mergeNcML(ncfile, mergeNcml); // create new dataset
    if (enhance == null || enhance.isEmpty()) {
      if (debugOpenFile) System.out.println(" acquire (no enhance) " + cacheLocation + " took " + (System.currentTimeMillis() - start));
      return ncfile;
    }

    // must enhance
    NetcdfDataset ds;
    if (ncfile instanceof NetcdfDataset) {
      ds = (NetcdfDataset) ncfile;
      ds.enhance(enhance); // enhance "in place", ie modify the NetcdfDataset
    } else {
      ds = new NetcdfDataset(ncfile, enhance); // enhance when wrapping
    }

    if (debugOpenFile) System.out.println(" acquire (enhance) " + cacheLocation + " took " + (System.currentTimeMillis() - start));
    return ds;
  } */

  private void closeAll(HashMap<String, NetcdfDataset> openFiles) throws IOException {
    for (NetcdfDataset ncfile : openFiles.values()) {
      ncfile.close();
    }
    openFiles.clear();
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // all normal (non agg, non cache) variables must use a proxy to acquire the file
  // the openFiles map is null - should be ok since its a non-agg, so file gets opened (and closed) for each read.

  protected class DatasetProxyReader implements ProxyReader {
    String location;

    DatasetProxyReader(String location) {
      this.location = location;
    }

    public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
      NetcdfFile ncfile = null;
      try {
        ncfile = open(location, null);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        Variable proxyV = findVariable(ncfile, client);
        return proxyV.read();
      } finally {
        ncfile.close();
      }
    }

    public Array reallyRead(Variable client, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      NetcdfFile ncfile = null;
      try {
        ncfile = open(location, null);
        Variable proxyV = findVariable(ncfile, client);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        return proxyV.read(section);
      } finally {
        ncfile.close();
      }
    }
  }

  protected Variable findVariable(NetcdfFile ncfile, Variable client) {
    Variable v = ncfile.findVariable(client.getName());
    if (v == null) {  // might be renamed
      VariableEnhanced ve = (VariableEnhanced) client;
      v = ncfile.findVariable(ve.getOriginalName());
    }
    return v;
  }

  public void showDetails(Formatter out) {
    out.format("==========================%nproto=%n%s%n", state.proto); 
  }

}