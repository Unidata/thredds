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
import thredds.inventory.CollectionManager;
import thredds.inventory.MFileCollectionManager;
import thredds.inventory.FeatureCollectionConfig;
import thredds.inventory.TimePartitionCollection;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.Grib2Iosp;
import ucar.nc2.grib.grib2.table.Grib2Tables;
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
    if (config.gribConfig != null) {
      if (config.gribConfig.gdsHash != null)
        dcm.putAuxInfo(FeatureCollectionConfig.AUX_GDSHASH, config.gribConfig.gdsHash);
      if (config.gribConfig.gdsName != null)
        dcm.putAuxInfo(FeatureCollectionConfig.AUX_GROUP_NAME, config.gribConfig.gdsName);
      if (config.gribConfig.intervalMerge)
        dcm.putAuxInfo(FeatureCollectionConfig.AUX_INTERVAL_MERGE, Boolean.TRUE);
    }

    String errs = errlog.toString();
    if (errs.length() > 0) logger.warn("{}: CollectionManager parse error = {} ", name, errs);

    tmi.setDataType(FeatureType.GRID); // override GRIB
    finish();
  }

  @Override
  public void update(CollectionManager.Force force) { // this is called from a background thread
    if (first) {
      synchronized (lock) {
        this.format = getDataFormatType(); // why wait until now ??
        firstInit(); // why ??
        first  = false;
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
    updateCollection(localState, force);
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
        first  = false;

      } else {
        if (!dcm.scanIfNeeded())
          return (StateGrib) state;
      }

      // update local copy of state, then switch all at once
      // i think this is "copy on write"
      StateGrib localState = new StateGrib((StateGrib) state);
      updateCollection(localState, CollectionManager.Force.test);

      makeTopDatasets(localState);
      localState.lastInvChange = System.currentTimeMillis();
      needsUpdate.set(false);
      needsProto.set(false);

      state = localState;
      return localState;
    }
  }

  private void updateCollection(StateGrib localState, CollectionManager.Force force) {
    try {
      if (config.timePartition != null) {
        TimePartition previous = localState.timePartition;
        localState.timePartition = TimePartition.factory(format == DataFormatType.GRIB1, (TimePartitionCollection) this.dcm, force, new Formatter());
        localState.gribCollection = null;
        if (previous != null) previous.close(); // LOOK thread safety

      } else { // WTF? open and close every time (!)
        GribCollection previous = localState.gribCollection;
        localState.gribCollection = GribCollection.factory(format == DataFormatType.GRIB1, dcm, force, new Formatter());
        localState.timePartition = null;
        if (previous != null) previous.close(); // LOOK thread safety
      }
    } catch (IOException e) {
      logger.error("Cant updateCollection " + dcm, e);
    }
    logger.debug("{}: Collection was recreated", getName());
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // called by DataRootHandler.makeDynamicCatalog() when the catref is requested
  // LOOK maybe we should ehcache some or all of this ??
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
        GribCollection.GroupHcs group = localState.gribCollection.findGroup(path[0]);
        if (group != null) {
          return makeFilesCatalog(localState.gribCollection, group, baseURI, localState);
        }

      } else {

        if (match.equals(COLLECTION)) {
          return makeGribCollectionCatalog(localState.timePartition, baseURI, localState);
        }

        TimePartition.Partition dc = localState.timePartition.getPartitionByName(match);
        if (dc != null) {
          return makeGribCollectionCatalog(dc.getGribCollection(), baseURI, localState);
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
        dc = localState.timePartition.getPartitionByName(path[0]);
        if (dc != null) {
          GribCollection gc = dc.getGribCollection();
          GribCollection.GroupHcs group = gc.findGroup(path[1]);
          if (group == null) return null;
          return makeFilesCatalog(gc, group, baseURI, localState);
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
        String name = group.getGroupName();
        InvDatasetImpl ds = new InvDatasetImpl(this, name);
        name = StringUtil2.replace(name, ' ', "_");
        ds.setUrlPath(this.path + "/" + name);
        ds.setID(id + "/" + name);
        addFileDatasets(ds, name);

        // metadata is specific to each group
        ds.tmi.addVariables(extractThreddsVariables(localState.gribCollection, group));
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

  private ThreddsMetadata.Variables extractThreddsVariables(GribCollection gribCollection, GribCollection.GroupHcs group) {
    ThreddsMetadata.Variables vars = new ThreddsMetadata.Variables(format.toString());
    for (GribCollection.VariableIndex vindex : group.varIndex) {
      ThreddsMetadata.Variable tv = new ThreddsMetadata.Variable();
      VertCoord vc = (vindex.vertIdx < 0) ? null : group.vertCoords.get(vindex.vertIdx);

      if (format == DataFormatType.GRIB2) {
        //GribTables tables = group.getGribCollection().getTables(); // LOOK
        Grib2Tables tables = Grib2Tables.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), gribCollection.getLocal());

        tv.setName(Grib2Iosp.makeVariableName(tables, gribCollection, vindex));
        tv.setDescription(Grib2Iosp.makeVariableLongName(tables, vindex));
        tv.setUnits(Grib2Iosp.makeVariableUnits(tables, vindex));

        tv.setVocabularyId("2-" + vindex.discipline + "-" + vindex.category + "-" + vindex.parameter);

        String paramDisc = tables.getTableValue("0.0", vindex.discipline);
        if (paramDisc == null) paramDisc = "Unknown";
        String paramCategory = tables.getTableValue("4.1." + vindex.discipline, vindex.category);
        if (paramCategory == null) paramCategory = "Unknown";
        String paramName = tables.getVariableName(vindex.discipline, vindex.category, vindex.parameter);
        tv.setVocabularyName(paramDisc + " / " + paramCategory + " / " + paramName);
        vars.addVariable(tv);

      } else {  // LOOK
      }

    }
    vars.sort();
    return vars;
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
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
    //if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    partCatalog.addDataset(top);

    // services need to be local
    partCatalog.addService(virtualService);
    top.getLocalMetadataInheritable().setServiceName(virtualService.getName());

    String id = getID();
    if (id == null) id = getPath();

    for (GribCollection.GroupHcs group : gribCollection.getGroups()) {
      String name = group.getGroupName();
      InvDatasetImpl ds = new InvDatasetImpl(this, name + "_" + COLLECTION);
      name = StringUtil2.replace(name, ' ', "_");
      ds.setUrlPath(this.path + "/" + collectionName + "/" + name);
      ds.setID(id + "/" + collectionName + "/" + name);

      // metadata is specific to each group
      ds.tmi.addVariables(extractThreddsVariables(gribCollection, group));
      ds.tmi.setGeospatialCoverage(extractGeospatial(group));
      CalendarDateRange cdr = extractCalendarDateRange(group);
      if (cdr != null) ds.tmi.setTimeCoverage(cdr);

      //ThreddsMetadata tm = ds.getLocalMetadata();
      //tm.addDocumentation("summary", "Best time series, taking the data from the latest file.");
      //tm.setGeospatialCoverage(group.getGeospatialCoverage());
      //tm.setTimeCoverage(group.getTimeCoverage());
      //tm.addVariables(group.getVariables());

      if (!(gribCollection instanceof TimePartition)) // dont add files for collection dataset
        addFileDatasets(ds, collectionName + "/" + name);
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
  // cat use InvDatasetScan because we might have multiple hcs
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
    if (localState.vars != null) tmi.addVariables(localState.vars);
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
      String fname = f.substring(topDirectory.length()+1);
      String path = FILES + "/" + fname;
      InvDatasetImpl ds = new InvDatasetImpl(this, fname);
      ds.setUrlPath(this.path + "/" + path);
      ds.setID(id + "/" + path);
      File file = new File(f);
      ds.tm.setDataSize(file.length());
      ds.finish();
      top.addDataset(ds);
    }

    result.finish();
    return result;
  }

  ///////////////////////////////////////////////////////////////////////////

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
      return localState.gribCollection.getGridDataset(paths[0], filename);

    } else {
      if (paths.length < 2) return null;

      if (paths[0].equals(localState.timePartition.getName()))
        return localState.timePartition.getGridDataset(paths[1], null);

      TimePartition.Partition dcm = localState.timePartition.getPartitionByName(paths[0]);
      if (dcm != null) {
        return dcm.getGribCollection().getGridDataset(paths[1], filename);
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
        return localState.gribCollection.getNetcdfDataset(group, filename);
      } else {
        return localState.gribCollection.getNetcdfDataset(paths[0], null);
      }

    } else {
      if (paths.length < 2) return null;

      if (paths[0].equals(localState.timePartition.getName()))
        return localState.timePartition.getNetcdfDataset(paths[1], null);

      TimePartition.Partition dcm = localState.timePartition.getPartitionByName(paths[0]);
      if (dcm != null) {
        String filename = paths.length > 2 ? paths[2] : null;
        return dcm.getGribCollection().getNetcdfDataset(paths[1], filename);
      }
    }

    return null;
  }

}
