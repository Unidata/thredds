/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package thredds.catalog;

import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionUpdater;
import thredds.inventory.MFileCollectionManager;
import thredds.inventory.TimePartitionCollection;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.Grib1Iosp;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Iosp;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implement FeatureCollection GRIB - a collection of Grib1 or Grib2 files that are served as Grids.
 *
 * @author caron
 * @since 4/15/11
 */
@ThreadSafe
public class InvDatasetFcGrib extends InvDatasetFeatureCollection {
  static private final Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFcGrib.class);
  static private final String COLLECTION = "collection";
  static private final String VARIABLES = "?metadata=variableMap";

  /////////////////////////////////////////////////////////////////////////////
  protected class StateGrib extends State {
    TimePartition timePartition;
    GribCollection gribCollection;

    protected StateGrib(StateGrib from) {
      super(from);
      if (from != null) {
        this.timePartition = from.timePartition;
        this.gribCollection = from.gribCollection;
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  private final FeatureCollectionConfig.GribConfig gribConfig;
  private final AtomicBoolean needsUpdate = new AtomicBoolean();
  private final AtomicBoolean needsProto = new AtomicBoolean();
  private boolean first = true;
  private DataFormatType format;

  public InvDatasetFcGrib(InvDatasetImpl parent, String name, String path, FeatureType featureType, FeatureCollectionConfig config) {
    super(parent, name, path, featureType, config);
    this.gribConfig = config.gribConfig;

    Formatter errlog = new Formatter();
    if (config.useIndexOnly)
      this.dcm = TimePartitionCollection.fromExistingIndices(config, errlog);
    else if (config.timePartition != null) {
      this.dcm = TimePartitionCollection.factory(config, errlog);
      this.dcm.setChangeChecker(GribIndex.getChangeChecker());
    } else {
      this.dcm = new MFileCollectionManager(config, errlog);
      this.dcm.setChangeChecker(GribIndex.getChangeChecker());
    }

    // sneak in extra config info
    if (config.gribConfig != null)
      dcm.putAuxInfo(FeatureCollectionConfig.AUX_GRIB_CONFIG, config.gribConfig);

    String errs = errlog.toString();
    if (errs.length() > 0) logger.warn("{}: CollectionManager parse error = {} ", name, errs);

    tmi.setDataType(FeatureType.GRID); // override GRIB
    finish();
  }

  @Override
  public void update(CollectionManager.Force force) { // this may be called from a background thread
    if (first) {
      synchronized (lock) {
        this.format = getDataFormatType(); // why wait until now ??
        firstInit(); // why ??
        first = false;
      }
    }

    if (force == CollectionManager.Force.nocheck) {
      // we need to update the dcm without triggering an index rewrite
      try {
        dcm.scan(false);
      } catch (IOException e) {
        logger.error("Error on scan " + dcm, e);
      }
    }

    // do the update in a local object
    StateGrib localState = new StateGrib((StateGrib) state);
    try {
      updateCollection(localState, force);
    } catch (Throwable e) {
      logger.error("Fail to create/update collection", e);
      return;
    }
    makeTopDatasets(localState);
    localState.lastInvChange = System.currentTimeMillis();

    // switch to live
    synchronized (lock) {
      needsUpdate.set(false);
      needsProto.set(false);
      state = localState;
    }

  }

  @Override
  public void updateProto() {
    needsProto.set(true);

    // no actual work, wait until next call to updateCollection (??)
    // not sure proto is used in GC
  }

  @Override
  protected StateGrib checkState() throws IOException { // this is called from the request thread
    synchronized (lock) {
      if (first) {
        this.format = getDataFormatType(); // for some reason have to wait until first request ??
        firstInit();
        dcm.scanIfNeeded();
        first = false;

      } else {
        if (!dcm.scanIfNeeded())
          return (StateGrib) state;
      }

     // if this is the TDS, and its using the TDM, then your not allowed to update
      boolean tdsUsingTdm = !CollectionUpdater.INSTANCE.isTdm() && config.tdmConfig != null;

      // update local copy of state, then switch all at once
      StateGrib localState = new StateGrib((StateGrib) state);
      updateCollection(localState, tdsUsingTdm ? CollectionManager.Force.nocheck : CollectionManager.Force.test);

      makeTopDatasets(localState);
      localState.lastInvChange = System.currentTimeMillis();
      needsUpdate.set(false);
      needsProto.set(false);

      state = localState;
      return localState;
    }
  }

  private void updateCollection(StateGrib localState, CollectionManager.Force force) throws IOException {
    if (config.timePartition != null) {
      TimePartition previous = localState.timePartition;
      localState.timePartition = TimePartition.factory(format == DataFormatType.GRIB1, (TimePartitionCollection) this.dcm, force, new Formatter());
      localState.gribCollection = null;
      if (previous != null) previous.close(); // LOOK thread safety

    } else {
      GribCollection previous = localState.gribCollection;
      localState.gribCollection = GribCollection.factory(format == DataFormatType.GRIB1, dcm, force, new Formatter());
      localState.timePartition = null;
      if (previous != null) previous.close(); // LOOK thread safety
    }
    logger.debug("{}: Collection was recreated", getName());
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // called by DataRootHandler.makeDynamicCatalog() when the catref is requested
  @Override
  public InvCatalogImpl makeCatalog(String match, String orgPath, URI baseURI) {
    //logger.debug("{}: make catalog for {} {}", name, match, baseURI);
    StateGrib localState = null;
    try {
      localState = (StateGrib) checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    try {
      // top catalog : uses state.datasets previously made in checkState()
      if ((match == null) || (match.length() == 0)) {
        InvCatalogImpl main = makeCatalogTop(baseURI, localState);
        main.addService(virtualService);
        main.getDataset().getLocalMetadataInheritable().setServiceName(virtualService.getName());
        main.finish();
        return main;
      }

      if (localState.timePartition == null) {
        String[] path = match.split("/");
        if (path.length < 2) return null;
        GribCollection.GroupHcs group = localState.gribCollection.findGroupById(path[0]);
        if (group != null) {
          return makeFilesCatalog(localState.gribCollection, group, baseURI, localState);
        }

      } else { // time partitions

        if (match.equals(COLLECTION)) {
          return makeGribCollectionCatalog(localState.timePartition, baseURI, localState);
        }

        TimePartition.Partition tpp = localState.timePartition.getPartitionByName(match);
        if (tpp != null) {
          GribCollection gc = tpp.getGribCollection();
          InvCatalogImpl result = makeGribCollectionCatalog(gc, baseURI, localState);
          gc.close();
          return result;
        }

        // files catalogs
        String[] path = match.split("/");
        if (path.length < 2) return null;

        /* collection level has form <partitionName>/<hcs>/files eg 200808/LatLon-181X360/files
        if (path[0].equals(timePartition.getName())) {
          GribCollection.GroupHcs group = timePartition.findGroup(path[1]);
          if (group == null) return null;
          return makeFilesCatalog(timePartition, group, baseURI, localState);
        } */

        // otherwise of form <partition>/<hcs>/files eg 200808/LatLon-181X360/files
        tpp = localState.timePartition.getPartitionByName(path[0]);
        if (tpp != null) {
          GribCollection gc = tpp.getGribCollection();
          GribCollection.GroupHcs group = gc.findGroupById(path[1]);
          if (group == null) return null;
          InvCatalogImpl result =  makeFilesCatalog(gc, group, baseURI, localState);
          gc.close();
          return result;
        }
      }

    } catch (Exception e) {
      logger.error("Error making catalog for " + path, e);
    }

    return null;
  }

  /////////////////////////////////////////////////////////////////////////

  // datasets of the top catalog are InvCatalogRef pointing to "PartitionCatalogs"
  private void makeTopDatasets(StateGrib localState) {
    List<InvDataset> datasets = new ArrayList<InvDataset>();

    String id = getID();
    if (id == null) id = getPath();

    if (localState.timePartition == null) {

      for (GribCollection.GroupHcs group : localState.gribCollection.getGroups()) {
        String groupId = group.getId();
        InvDatasetImpl ds = new InvDatasetImpl(this, group.getDescription());
        //groupId = StringUtil2.replace(groupId, ' ', "_");
        ds.setUrlPath(this.path + "/" + groupId);
        ds.setID(id + "/" + groupId);
        addFileDatasets(ds, groupId);

        // metadata is specific to each group
        ds.tmi.addVariableMapLink( makeMetadataLink( this.path, groupId, VARIABLES));
        ds.tmi.setGeospatialCoverage(extractGeospatial(group));
        CalendarDateRange cdr = extractCalendarDateRange(group);
        if (cdr != null) ds.tmi.setTimeCoverage(cdr);

        ds.finish();
        datasets.add(ds);
      }

    } else {

      // the entire collection
      InvCatalogRef ds = new InvCatalogRef(this, COLLECTION, getCatalogHref(COLLECTION));
      ds.setUrlPath(this.path + "/" + COLLECTION);
      ds.setID(id + "/" + COLLECTION);
      ds.finish();
      datasets.add(ds);

      for (TimePartition.Partition dc : localState.timePartition.getPartitions()) {
        String dname = dc.getName();
        ds = new InvCatalogRef(this, dname, getCatalogHref(dname));
        dname = StringUtil2.replace(dname, ' ', "_");
        ds.setUrlPath(this.path + "/" + dname);
        ds.setID(id + "/" + dname);
        ds.tmi.addVariableMapLink(makeMetadataLink( this.path, dname, VARIABLES));
        //ThreddsMetadata tm = ds.getLocalMetadata();
        //tm.addDocumentation("summary", "Best time series, taking the data from the most recent run available.");
        //ds.getLocalMetadataInheritable().setServiceName(virtualService.getName());

        //addDatasets(ds, null);
        ds.finish();
        datasets.add(ds);
      }
    }

    localState.datasets = datasets;
    this.datasets = datasets;
    finish();
  }

  private ThreddsMetadata.GeospatialCoverage extractGeospatial(GribCollection.GroupHcs group) {
    GdsHorizCoordSys gdsCoordSys = group.hcs; // .gds.makeHorizCoordSys();
    LatLonRect llbb = GridCoordSys.getLatLonBoundingBox(gdsCoordSys.proj, gdsCoordSys.getStartX(), gdsCoordSys.getStartY(),
            gdsCoordSys.getEndX(), gdsCoordSys.getEndY());

    ThreddsMetadata.GeospatialCoverage gc = new ThreddsMetadata.GeospatialCoverage();
    if (llbb != null)
      gc.setBoundingBox(llbb);

    if (group.hcs.isLatLon()) {
      gc.setLonResolution(gdsCoordSys.dx);
      gc.setLatResolution(gdsCoordSys.dy);
    }

    return gc;
  }

  private CalendarDateRange extractCalendarDateRange(GribCollection.GroupHcs group) {
    TimeCoord max = null;

    for (TimeCoord tc : group.timeCoords) {
      if (!tc.isInterval()) {
        if ((max == null) || (max.getSize() < tc.getSize()))
          max = tc;
      }
    }

    if (max == null) {
      for (TimeCoord tc : group.timeCoords) {
        if (tc.isInterval()) {
          if ((max == null) || (max.getSize() < tc.getSize()))
            max = tc;
        }
      }
    }

    return (max == null) ? null : max.getCalendarRange();
  }

  // each partition gets its own catalog, showing the different groups (horiz coord sys)
  private InvCatalogImpl makeGribCollectionCatalog(GribCollection gribCollection, URI baseURI, State localState) throws IOException {

    String collectionName = gribCollection.getName();
    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    URI myURI = baseURI.resolve(getCatalogHref(collectionName));
    InvCatalogImpl partCatalog = new InvCatalogImpl(getFullName(), parent.getVersion(), myURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(collectionName);

    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    //if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
    //if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    partCatalog.addDataset(top);

    // services need to be local
    partCatalog.addService(virtualService);
    top.getLocalMetadataInheritable().setServiceName(virtualService.getName());

    String id = getID();
    if (id == null) id = getPath();

    for (GribCollection.GroupHcs group : gribCollection.getGroups()) {
      String groupId = group.getId();
      InvDatasetImpl ds = new InvDatasetImpl(this, groupId + "_" + COLLECTION);
      //groupId = StringUtil2.replace(groupId, ' ', "_");
      ds.setUrlPath(this.path + "/" + collectionName + "/" + groupId);
      ds.setID(id + "/" + collectionName + "/" + groupId);

      // metadata is specific to each group
      ds.tmi.addVariableMapLink( makeMetadataLink( this.path + "/" + collectionName, groupId, VARIABLES));
      ds.tmi.setGeospatialCoverage(extractGeospatial(group));
      CalendarDateRange cdr = extractCalendarDateRange(group);
      if (cdr != null) ds.tmi.setTimeCoverage(cdr);

      //ThreddsMetadata tm = ds.getLocalMetadata();
      //tm.addDocumentation("summary", "Best time series, taking the data from the latest file.");
      //tm.setGeospatialCoverage(group.getGeospatialCoverage());
      //tm.setTimeCoverage(group.getTimeCoverage());
      //tm.addVariables(group.getVariables());

      if (!(gribCollection instanceof TimePartition)) // dont add files for collection dataset
        addFileDatasets(ds, collectionName + "/" + groupId);
      ds.finish();
      top.addDataset(ds);
    }

    partCatalog.finish();

    return partCatalog;
  }

  // file datasets of the partition catalog are InvCatalogRef pointing to "FileCatalogs"
  private void addFileDatasets(InvDatasetImpl parent, String prefix) {
    String name = prefix + "/" + FILES;
    InvCatalogRef ds = new InvCatalogRef(this, FILES, getCatalogHref(name));
    // ds.setUrlPath(this.path + "/" + FILES);
    ds.finish();
    parent.addDataset(ds);
  }

  // this catalog lists the individual files comprising a grib collection.
  // cant use InvDatasetScan because we might have multiple hcs
  private InvCatalogImpl makeFilesCatalog(GribCollection gc, GribCollection.GroupHcs group, URI baseURI, State localState) throws IOException {

    String collectionName = gc.getName();
    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    URI myURI = baseURI.resolve(getCatalogHref(collectionName));
    InvCatalogImpl result = new InvCatalogImpl(getFullName(), parent.getVersion(), myURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(FILES);

    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
    //if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    result.addDataset(top);

    // services need to be local
    result.addService(orgService);
    top.getLocalMetadataInheritable().setServiceName(orgService.getName());

    String id = getID();
    if (id == null) id = getPath();

    for (String f : group.getFilenames()) {
      if (!f.startsWith(topDirectory))
        System.out.println("HEY");
      String fname = f.substring(topDirectory.length() + 1);
      String path = FILES + "/" + fname;
      InvDatasetImpl ds = new InvDatasetImpl(this, fname);
      ds.setUrlPath(this.path + "/" + path);
      ds.setID(id + "/" + path);
      ds.tmi.addVariableMapLink( makeMetadataLink( this.path, fname, VARIABLES));
      File file = new File(f);
      ds.tm.setDataSize(file.length());
      ds.finish();
      top.addDataset(ds);
    }

    result.finish();
    return result;
  }

  ///////////////////////////////////////////////////////////////////////////

  private String makeMetadataLink(String path, String dataset, String metadata) {
    return dataset + metadata;
  }

  @Override
  public ucar.nc2.dt.GridDataset getGridDataset(String matchPath) throws IOException {
    StateGrib localState = null;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    if ((matchPath == null) || (matchPath.length() == 0)) return null;
    String[] paths = matchPath.split("/");
    if (paths.length < 1) return null;
    String filename = paths.length == 2 ? paths[1] : null;

    if (localState.timePartition == null) {
      return localState.gribCollection.getGridDataset(paths[0], filename, gribConfig);

    } else {
      if (paths.length < 2) return null;

      if (paths[0].equals(localState.timePartition.getName()))
        return localState.timePartition.getGridDataset(paths[1], null, gribConfig);

      TimePartition.Partition tpp = localState.timePartition.getPartitionByName(paths[0]);
      if (tpp != null) {
        GribCollection gc =  tpp.getGribCollection();
        ucar.nc2.dt.GridDataset result = gc.getGridDataset(paths[1], filename, gribConfig);
        // LOOK WRONG gc.close();
        return result;
      }
    }

    return null;
  }

  @Override
  public NetcdfDataset getNetcdfDataset(String matchPath) throws IOException {
    // handle FILES
    NetcdfDataset result = super.getNetcdfDataset(matchPath);
    if (result != null) return result;

    StateGrib localState = null;
    try {
      localState = checkState();
    } catch (IOException e) {
      logger.error("Error in checkState", e);
      return null;
    }

    if ((matchPath == null) || (matchPath.length() == 0)) return null;
    String[] paths = matchPath.split("/");
    if (paths.length < 1) return null;

    if (localState.timePartition == null) {
      int n = paths.length;
      if (n >= 2) { // files
        String group = paths[n - 2];
        String filename = paths[n - 1];
        return localState.gribCollection.getNetcdfDataset(group, filename, gribConfig);
      } else {
        return localState.gribCollection.getNetcdfDataset(paths[0], null, gribConfig);
      }

    } else {
      if (paths.length < 2) return null;

      if (paths[0].equals(localState.timePartition.getName()))
        return localState.timePartition.getNetcdfDataset(paths[1], null, gribConfig);

      TimePartition.Partition dcm = localState.timePartition.getPartitionByName(paths[0]);
      if (dcm != null) {
        String filename = paths.length > 2 ? paths[2] : null;
        return dcm.getGribCollection().getNetcdfDataset(paths[1], filename, gribConfig);
      }
    }

    return null;
  }

  ///////////////////////////

  private ThreddsMetadata.Variables extractThreddsVariables(GribCollection gribCollection, GribCollection.GroupHcs group) {
    ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables(format.toString());
    for (GribCollection.VariableIndex vindex : group.varIndex)
      vars.addVariable(extractThreddsVariables(gribCollection, vindex));
    vars.sort();
    return vars;
  }

    // we need to cache this by variable to reduce duplication
  private Map<Integer, ThreddsMetadata.Variable> map = null;
  private Grib1Customizer cust1 = null;
  private Grib2Customizer cust2 = null;

  public ThreddsMetadata.Variable extractThreddsVariables(GribCollection gc, GribCollection.VariableIndex vindex) {
    if (map == null) map = new HashMap<Integer, ThreddsMetadata.Variable>(100);

    ThreddsMetadata.Variable tv = map.get(vindex.cdmHash);
    if (tv != null) return tv;
    tv = new ThreddsMetadata.Variable();

    if (gc.isGrib1()) {
      if (cust1 == null) cust1 = Grib1Customizer.factory(gc.getCenter(), gc.getSubcenter(), gc.getLocal(), null);
      tv.setName(Grib1Iosp.makeVariableName(cust1, gc, vindex));
      tv.setDescription(Grib1Iosp.makeVariableLongName(cust1, gc, vindex));
      tv.setUnits(Grib1Iosp.makeVariableUnits(cust1, gc, vindex));
      tv.setVocabularyId("1-" + vindex.discipline + "-" + vindex.category + "-" + vindex.parameter);

      map.put(vindex.cdmHash, tv);
      return tv;

    } else {
      if (cust2 == null) cust2 = Grib2Customizer.factory(gc.getCenter(), gc.getSubcenter(), gc.getMaster(), gc.getLocal());

      tv.setName(Grib2Iosp.makeVariableName(cust2, gc, vindex));
      tv.setDescription(Grib2Iosp.makeVariableLongName(cust2, vindex));
      tv.setUnits(Grib2Iosp.makeVariableUnits(cust2, vindex));
      tv.setVocabularyId("2-" + vindex.discipline + "-" + vindex.category + "-" + vindex.parameter);

      String paramDisc = cust2.getTableValue("0.0", vindex.discipline);
      if (paramDisc == null) paramDisc = "Unknown";
      String paramCategory = cust2.getTableValue("4.1." + vindex.discipline, vindex.category);
      if (paramCategory == null) paramCategory = "Unknown";
      String paramName = cust2.getVariableName(vindex.discipline, vindex.category, vindex.parameter);
      tv.setVocabularyName(paramDisc + " / " + paramCategory + " / " + paramName);
      map.put(vindex.cdmHash, tv);
      return tv;
    }
  }

  private class Vhash {
    int center, subcenter, local, master;
    int discipline, category, param;
  }

}
