/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog.tools;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import thredds.client.catalog.Access;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Property;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.nc2.Attribute;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.remote.CdmrFeatureDataset;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.util.Optional;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nonnull;

/**
 * DataFactory for THREDDS client catalogs
 *
 * @author caron
 * @since 1/8/2015
 */
public class DataFactory {
  static public final String PROTOCOL = "thredds";
  static public final String SCHEME = PROTOCOL + ":";

  static private ServiceType[] preferAccess;

  static public void setPreferCdm(boolean prefer) {
    preferAccess = prefer ? new ServiceType[] {ServiceType.CdmRemote} : null;
  }
  static public void setPreferAccess(ServiceType... prefer) {
    preferAccess = prefer;
  }

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("thredds/debugOpen");
    debugTypeOpen = debugFlag.isSet("thredds/openDatatype");
  }

  static private boolean debugOpen = false;
  static private boolean debugTypeOpen = false;

  /**
   * The result of trying to open a THREDDS dataset.
   * If fatalError is true, the operation failed, errLog should indicate why.
   * Otherwise, the FeatureType and FeatureDataset is valid.
   * There may still be warning or diagnostic errors in errLog.
   */
  public static class Result implements Closeable {
    public boolean fatalError;
    public Formatter errLog = new Formatter();

    public FeatureType featureType;
    public FeatureDataset featureDataset;
    public String imageURL;

    public String location;
    public Access accessUsed;

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("Result");
      sb.append("{fatalError=").append(fatalError);
      sb.append(", errLog=").append(errLog);
      sb.append(", featureType=").append(featureType);
      sb.append(", featureDataset=").append(featureDataset);
      sb.append(", imageURL='").append(imageURL).append('\'');
      sb.append(", location='").append(location).append('\'');
      sb.append(", accessUsed=").append(accessUsed);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public void close() throws IOException {
      if (featureDataset != null) featureDataset.close();
    }
  }

  /**
   * Open a FeatureDataset from a URL location string. Example URLS: <ul>
   * <li>http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds:file:c:/test/data/catalog/addeStationDataset.xml#AddeSurfaceData (absolute file)
   * <li>thredds:resolve:resolveURL
   * </ul>
   *
   * @param urlString [thredds:]catalog.xml#datasetId
   * @param task      may be null
   * @return ThreddsDataFactory.Result check fatalError for validity
   * @throws java.io.IOException on read error
   */
  public DataFactory.Result openFeatureDataset(String urlString, ucar.nc2.util.CancelTask task) throws IOException {

    DataFactory.Result result = new DataFactory.Result();
    Dataset dataset = openCatalogFromLocation(urlString, task, result);
    if (result.fatalError || dataset == null)
      return result;

    return openFeatureDataset(null, dataset, task, result);
  }

  /**
   * Open a FeatureDataset from a URL location string, and a desired type (may by NONE or null).
   *
   * @param wantFeatureType desired feature type, may be NONE or null
   * @param urlString       [thredds:]catalog.xml#datasetId
   * @param task            may be null
   * @return ThreddsDataFactory.Result check fatalError for validity
   * @throws java.io.IOException on read error
   */
  public DataFactory.Result openFeatureDataset(FeatureType wantFeatureType, String urlString, ucar.nc2.util.CancelTask task)
          throws IOException {
    DataFactory.Result result = new DataFactory.Result();
    Dataset ds = openCatalogFromLocation(urlString, task, result);
    if (result.fatalError || ds == null)
      return result;

    return openFeatureDataset(wantFeatureType, ds, task, result);
  }

  private Dataset openCatalogFromLocation(String location, ucar.nc2.util.CancelTask task, Result result) throws IOException {
    location = location.trim();
    location = StringUtil2.replace(location, '\\', "/");

    if (location.startsWith(SCHEME))
      location = location.substring(8);

    if (location.startsWith("resolve:")) {
      location = location.substring(8);
      return openResolver(location, task, result);
    }

    if (!location.startsWith("http:") && !location.startsWith("file:"))   // LOOK whats this for??
      location = "http:" + location;

    Catalog catalog;
    String datasetId;

    int pos = location.indexOf('#');
    if (pos < 0) {
      result.fatalError = true;
      result.errLog.format("Must have the form catalog.xml#datasetId%n");
      return null;
    }

    CatalogBuilder catFactory = new CatalogBuilder();
    String catalogLocation = location.substring(0, pos);
    catalog = catFactory.buildFromLocation(catalogLocation, null);
    if (catalog == null) {
      result.errLog.format("Invalid catalog from Resolver <%s>%n%s%n", catalogLocation, catFactory.getErrorMessage());
      result.fatalError = true;
      return null;
    }

    datasetId = location.substring(pos + 1);
    Dataset ds = catalog.findDatasetByID(datasetId);
    if (ds == null) {
      result.fatalError = true;
      result.errLog.format("Could not find dataset %s in %s %n", datasetId, catalogLocation);
      return null;
    }

    return ds;
  }

  /**
   * Open a FeatureDataset from an Dataset object, deciding on which Access to use.
   *
   * @param Dataset use this to figure out what type, how to open, etc
   * @param task       allow user to cancel; may be null
   * @return ThreddsDataFactory.Result check fatalError for validity
   * @throws IOException on read error
   */
  @Nonnull
  public DataFactory.Result openFeatureDataset(Dataset Dataset, ucar.nc2.util.CancelTask task) throws IOException {
    return openFeatureDataset(null, Dataset, task, new Result());
  }

  @Nonnull
  public DataFactory.Result openFeatureDataset(FeatureType wantFeatureType, Dataset ds, ucar.nc2.util.CancelTask task, Result result)
          throws IOException {

    // deal with RESOLVER type
    Access resolverAccess = findAccessByServiceType(ds.getAccess(), ServiceType.Resolver);
    if (resolverAccess != null) {
      Dataset rds = openResolver(resolverAccess.getStandardUrlName(), task, result);
      if (rds != null) ds = rds;
    }

    try {
      result.featureType = ds.getFeatureType();
      if (result.featureType == null)
        result.featureType = wantFeatureType;

      // first choice would be remote FeatureDataset
      Access access = findAccessByServiceType(ds.getAccess(), ServiceType.CdmrFeature);
      if (access != null)
        return openFeatureDataset(result.featureType, access, task, result);

      // special handling for images
      if (result.featureType == FeatureType.IMAGE) {
        access = getImageAccess(ds, task, result);
        if (access != null) {
          return openFeatureDataset(result.featureType, access, task, result);
        } else
          result.fatalError = true;
        return result;
      }

      // otherwise, open as a remote NetcdfDataset (cdmremote or opendap)
      NetcdfDataset ncd = openDataset(ds, true, task, result);
      if (null != ncd)
        result.featureDataset = FeatureDatasetFactoryManager.wrap(result.featureType, ncd, task, result.errLog);

      if (null == result.featureDataset)
        result.fatalError = true;
      else {
        result.location = result.featureDataset.getLocation();
        if (result.featureType == null)
          result.featureType = result.featureDataset.getFeatureType();
      }

      return result;

    } catch (Throwable t) {
      result.close();
      if (t instanceof IOException) throw (IOException) t;
      throw new RuntimeException(t);
    }
  }

  /**
   * Open a FeatureDataset from an Access object.
   *
   * @param access use this Access.
   * @param task   may be null
   * @return ThreddsDataFactory.Result check fatalError for validity
   * @throws IOException on read error
   */
  public DataFactory.Result openFeatureDataset(Access access, ucar.nc2.util.CancelTask task) throws IOException {
    Dataset ds = access.getDataset();
    DataFactory.Result result = new Result();
    if (ds.getFeatureType() == null) {
      result.errLog.format("InvDatasert must specify a FeatureType%n");
      result.fatalError = true;
      return result;
    }

    return openFeatureDataset(ds.getFeatureType(), access, task, result);
  }

  private DataFactory.Result openFeatureDataset(FeatureType wantFeatureType, Access access, ucar.nc2.util.CancelTask task, Result result)
          throws IOException {
    result.featureType = wantFeatureType;
    result.accessUsed = access;

    // special handling for IMAGE
    if (result.featureType == FeatureType.IMAGE) {
      result.imageURL = access.getStandardUrlName();
      result.location = result.imageURL;
      return result;
    }

    if (access.getService().getType() == ServiceType.CdmrFeature) {
      Optional<FeatureDataset> opt = CdmrFeatureDataset.factory(wantFeatureType, access.getStandardUrlName());
      if (opt.isPresent())
        result.featureDataset = opt.get();
      else
        result.errLog.format("%s", opt.getErrorMessage());

    } else {

      // all other datatypes
      NetcdfDataset ncd = openDataset(access, true, task, result);
      if (null != ncd) {
        result.featureDataset = FeatureDatasetFactoryManager.wrap(result.featureType, ncd, task, result.errLog);
      }
    }

    if (null == result.featureDataset)
      result.fatalError = true;
    else {
      result.location = result.featureDataset.getLocation();
      result.featureType = result.featureDataset.getFeatureType();
    }

    return result;
  }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Open a NetcdfDataset from a URL location string. Example URLS: <ul>
   * <li>http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   * <li>thredds:file:c:/dev/netcdf-java-2.2/test/data/catalog/addeStationDataset.xml#AddeSurfaceData (absolute file)
   * <li>thredds:resolve:resolveURL
   * </ul>
   *
   * @param location catalog.xml#datasetId, may optionally start with "thredds:"
   * @param task     may be null
   * @param log      error messages gp here, may be null
   * @param acquire  if true, aquire the dataset, else open it
   * @return NetcdfDataset
   * @throws java.io.IOException on read error
   */
  public NetcdfDataset openDataset(String location, boolean acquire, ucar.nc2.util.CancelTask task, Formatter log) throws IOException {
    Result result = new Result();
    Dataset dataset = openCatalogFromLocation(location, task, result);
    if (result.fatalError || dataset == null) {
      if (log != null) log.format("%s", result.errLog);
      result.close();
      return null;
    }

    return openDataset(dataset, acquire, task, result);
  }

  /**
   * Try to open as a NetcdfDataset.
   *
   * @param Dataset open this
   * @param acquire    if true, aquire the dataset, else open it
   * @param task       may be null
   * @param log        error message, may be null
   * @return NetcdfDataset or null if failure
   * @throws IOException on read error
   */
  public NetcdfDataset openDataset(Dataset Dataset, boolean acquire, ucar.nc2.util.CancelTask task, Formatter log) throws IOException {
    Result result = new Result();
    NetcdfDataset ncd = openDataset(Dataset, acquire, task, result);
    if (log != null) log.format("%s", result.errLog);
    if (result.fatalError) {
      result.close();
      if (ncd != null) ncd.close();
    }
    return (result.fatalError) ? null : ncd;
  }

  private NetcdfDataset openDataset(Dataset dataset, boolean acquire, ucar.nc2.util.CancelTask task, Result result) throws IOException {

    IOException saveException = null;

    List<Access> accessList = new ArrayList<>(dataset.getAccess()); // a list of all the accesses
    while (accessList.size() > 0) {
      Access access = chooseDatasetAccess(accessList);

      // no valid access
      if (access == null) {
        result.errLog.format("No access that could be used in dataset %s %n", dataset);
        if (saveException != null)
          throw saveException;
        return null;
      }

      String datasetLocation = access.getStandardUrlName();
      ServiceType serviceType = access.getService().getType();
      if (debugOpen)
        System.out.println("ThreddsDataset.openDataset try " + datasetLocation + " " + serviceType);

      // deal with RESOLVER type
      if (serviceType == ServiceType.Resolver) {
        Dataset rds = openResolver(datasetLocation, task, result);
        if (rds == null) return null;
        accessList = new ArrayList<>(rds.getAccess());
        continue;
      }

      // ready to open it through netcdf API
      NetcdfDataset ds;

      // try to open
      try {
        ds = openDataset(access, acquire, task, result);

      } catch (IOException e) {
        result.errLog.format("Cant open %s %n err=%s%n", datasetLocation, e.getMessage());
        if (debugOpen) {
          System.out.println("Cant open= " + datasetLocation + " " + serviceType);
          e.printStackTrace();
        }
        String mess = e.getMessage();
        if (mess != null && mess.contains("Unauthorized"))
          break; // bail out

        accessList.remove(access);
        saveException = e;
        continue;
      }

      result.accessUsed = access;
      return ds;
    } // loop over accesses

    // if (saveException != null) throw saveException;
    return null;
  }

  /**
   * Try to open Access as a NetcdfDataset.
   *
   * @param access  open this Access
   * @param acquire if true, aquire the dataset, else open it
   * @param task    may be null
   * @param log     error message, may be null
   * @return NetcdfDataset or null if failure
   * @throws IOException on read error
   */
  public NetcdfDataset openDataset(Access access, boolean acquire, ucar.nc2.util.CancelTask task, Formatter log)
          throws IOException {
    try (Result result = new Result()) {
      NetcdfDataset ncd = openDataset(access, acquire, task, result);
      if (log != null) log.format("%s", result.errLog);
      if (result.fatalError && ncd != null)
        ncd.close();
      return (result.fatalError) ? null : ncd;

      // result.close() will be called at the end of this block, which will close the Result's FeatureDataset, if it
      // exists. That data member won't have been set by any of the code above, so the close() will essentially be a
      // no-op.
    }
  }

  private NetcdfDataset openDataset(Access access, boolean acquire, ucar.nc2.util.CancelTask task, Result result) throws IOException {
    Dataset ds = access.getDataset();
    String datasetId = ds.getId();
    String title = ds.getName();

    String datasetLocation = access.getStandardUrlName();
    ServiceType serviceType = access.getService().getType();
    if (debugOpen) System.out.println("ThreddsDataset.openDataset= " + datasetLocation);

    // deal with RESOLVER type
    if (serviceType == ServiceType.Resolver) {
      Dataset rds = openResolver(datasetLocation, task, result);
      if (rds == null) return null;
      return openDataset(rds, acquire, task, result);
    }

    // ready to open it through netcdf API
    DatasetUrl durl = new DatasetUrl(serviceType, datasetLocation);
    NetcdfDataset ncd = acquire ? NetcdfDataset.acquireDataset(durl, true, task) : NetcdfDataset.openDataset(durl, null, -1, task, null);

    /* String prefix = null;
    if ((serviceType == ServiceType.OPENDAP)|| (serviceType == ServiceType.DODS))
        prefix = "dods:";
    else if(serviceType == ServiceType.DAP4)
        prefix = "dap4:";

    boolean enhanceMode = true;
    if (prefix != null) {
      String curl = datasetLocation;
      if (curl.startsWith("http:")) {
          curl = prefix + datasetLocation.substring(5);
      } else if (curl.startsWith("https:")) {
          curl =  prefix + curl.substring(6);
      }
      ncd = acquire ? NetcdfDataset.acquireDataset(curl, enhanceMode, task) : NetcdfDataset.openDataset(curl, enhanceMode, task);
    }

    // open CdmRemote
    else if (serviceType == ServiceType.CdmRemote) {
      String curl = CdmRemote.canonicalURL(datasetLocation);
      ncd = acquire ? NetcdfDataset.acquireDataset(curl, enhanceMode, task) : NetcdfDataset.openDataset(curl, enhanceMode, task);
    }

    // open HTTPServer
    else if (serviceType == ServiceType.HTTPServer) {
      String curl =  (datasetLocation.startsWith("http:")) ? "httpserver:" + datasetLocation.substring(5) : datasetLocation;
      ncd = acquire ? NetcdfDataset.acquireDataset(curl, enhanceMode, task) : NetcdfDataset.openDataset(curl, enhanceMode, task);
    }

    else {
      // open through NetcdfDataset API
      ncd = acquire ? NetcdfDataset.acquireDataset(datasetLocation, enhanceMode, task) : NetcdfDataset.openDataset(datasetLocation, enhanceMode, task);
    } */

    result.accessUsed = access;
    ncd.setId(datasetId);
    ncd.setTitle(title);
    annotate(ds, ncd);

    /* see if there's NcML metadata LOOK whats this
    List<Metadata> list = ds.getMetadata(Metadata.Type.NcML);
    if (list != null && list.size() > 0) {
      Metadata ncmlMetadata = list.get(0);
      NcMLReader.wrapNcML(ds, ncmlMetadata.getXlinkHref(), null);
    }  */

    return ncd;
  }

  /**
   * Find the "best" access in case theres more than one, based on what the CDM knows
   * how to open and use.
   *
   * @param accessList choose from this list.
   * @return best access method.
   */
  public Access chooseDatasetAccess(List<Access> accessList) {
    if (accessList.size() == 0)
      return null;

    Access access = null;
    if (preferAccess != null) {
      for (ServiceType type : preferAccess) {
        access = findAccessByServiceType(accessList, type);
        if (access != null) break;
      }
    }

    // the order indicates preference
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.CdmRemote);
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.DODS);
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.OPENDAP);
    if (access == null)
       access = findAccessByServiceType(accessList, ServiceType.DAP4);
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.File); // should mean that it can be opened through netcdf API
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.HTTPServer); // should mean that it can be opened through netcdf API

    /* look for HTTP with format we can read
    if (access == null) {
      Access tryAccess = findAccessByServiceType(accessList, ServiceType.HTTPServer);

      if (tryAccess != null) {
        DataFormatType format = tryAccess.getDataFormatType();

        // these are the file types we can read
        if ((DataFormatType.NCML == format) || (DataFormatType.NETCDF == format)) {   // removed 4/4/2015 jc
        //if ((DataFormatType.BUFR == format) || (DataFormatType.GINI == format) || (DataFormatType.GRIB1 == format)
        //        || (DataFormatType.GRIB2 == format) || (DataFormatType.HDF5 == format) || (DataFormatType.NCML == format)
       //         || (DataFormatType.NETCDF == format) || (DataFormatType.NEXRAD2 == format) || (DataFormatType.NIDS == format)) {
          access = tryAccess;
        }
      }
    } */

    // ADDE
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.ADDE);

    // RESOLVER
    if (access == null) {
      access = findAccessByServiceType(accessList, ServiceType.Resolver);
    }

    return access;
  }

  private Dataset openResolver(String urlString, ucar.nc2.util.CancelTask task, Result result) throws IOException {
    CatalogBuilder catFactory = new CatalogBuilder();
    Catalog catalog = catFactory.buildFromLocation(urlString, null);
    if (catalog == null) {
      result.errLog.format("Couldnt open Resolver %s err=%s%n ", urlString, catFactory.getErrorMessage());
      return null;
    }

    for (Dataset ds : catalog.getDatasetsLocal()) {
      if (ds.hasAccess()) return ds;
      for (Dataset nested : ds.getDatasetsLocal())   // cant be more than one deep
        if (nested.hasAccess()) return nested;
    }

    return null;
  }

  /**
   * Add information from the Dataset to the NetcdfDataset.
   *
   * @param ds        get info from here
   * @param ncDataset add to here
   */
  public static void annotate(Dataset ds, NetcdfDataset ncDataset) {
    ncDataset.setTitle(ds.getName());
    ncDataset.setId(ds.getId());

    // add properties as global attributes
    for (Property p : ds.getProperties()) {
      String name = p.getName();
      if (null == ncDataset.findGlobalAttribute(name)) {
        ncDataset.addAttribute(null, new Attribute(name, p.getValue()));
      }
    }

    /* ThreddsMetadata.GeospatialCoverage geoCoverage = ds.getGeospatialCoverage();
   if (geoCoverage != null) {
     if ( null != geoCoverage.getNorthSouthRange()) {
       ncDataset.addAttribute(null, new Attribute("geospatial_lat_min", new Double(geoCoverage.getLatSouth())));
       ncDataset.addAttribute(null, new Attribute("geospatial_lat_max", new Double(geoCoverage.getLatNorth())));
     }
     if ( null != geoCoverage.getEastWestRange()) {
       ncDataset.addAttribute(null, new Attribute("geospatial_lon_min", new Double(geoCoverage.getLonWest())));
       ncDataset.addAttribute(null, new Attribute("geospatial_lon_max", new Double(geoCoverage.getLonEast())));
     }
     if ( null != geoCoverage.getUpDownRange()) {
       ncDataset.addAttribute(null, new Attribute("geospatial_vertical_min", new Double(geoCoverage.getHeightStart())));
       ncDataset.addAttribute(null, new Attribute("geospatial_vertical_max", new Double(geoCoverage.getHeightStart() + geoCoverage.getHeightExtent())));
     }
   }

   DateRange timeCoverage = ds.getTimeCoverage();
   if (timeCoverage != null) {
     ncDataset.addAttribute(null, new Attribute("time_coverage_start", timeCoverage.getStart().toDateTimeStringISO()));
     ncDataset.addAttribute(null, new Attribute("time_coverage_end", timeCoverage.getEnd().toDateTimeStringISO()));
   } */

    ncDataset.finish();
  }

//////////////////////////////////////////////////////////////////////////////////
// image

// look for an access method for an image datatype

  private Access getImageAccess(Dataset ds, ucar.nc2.util.CancelTask task, Result result) throws IOException {

    List<Access> accessList = new ArrayList<>(ds.getAccess()); // a list of all the accesses
    while (accessList.size() > 0) {
      Access access = chooseImageAccess(accessList);
      if (access == null) {
        result.errLog.format("No access that could be used for Image Type %s %n", ds);
        return null;
      }

      // deal with RESOLVER type
      String datasetLocation = access.getStandardUrlName();
      Dataset rds = openResolver(datasetLocation, task, result);
      if (rds == null)
        return null;

      // use the access list from the resolved dataset
      accessList = new ArrayList<>(ds.getAccess());
    }

    return null;
  }

  private Access chooseImageAccess(List<Access> accessList) {
    Access access;

    access = findAccessByDataFormatType(accessList, DataFormatType.JPEG);
    if (access != null) return access;

    access = findAccessByDataFormatType(accessList, DataFormatType.GIF);
    if (access != null) return access;

    access = findAccessByDataFormatType(accessList, DataFormatType.TIFF);
    if (access != null) return access;

    access = findAccessByServiceType(accessList, ServiceType.ADDE);
    if (access != null) {
      String datasetLocation = access.getStandardUrlName();
      if (datasetLocation.indexOf("image") > 0)
        return access;
    }

    return access;
  }

///////////////////////////////////////////////////////////////////

// works against the accessList instead of the dataset list, so we can remove and try again

  private Access findAccessByServiceType(List<Access> accessList, ServiceType type) {
    for (Access a : accessList) {
      ServiceType stype = a.getService().getType();
      if (stype != null && stype.toString().equalsIgnoreCase(type.toString()))
        return a;
    }
    return null;
  }

// works against the accessList instead of the dataset list, so we can remove and try again

  private Access findAccessByDataFormatType(List<Access> accessList, DataFormatType type) {
    for (Access a : accessList) {
      DataFormatType has = a.getDataFormatType();
      if (has != null && type.toString().equalsIgnoreCase(has.toString()))
        return a;
    }
    return null;
  }
}
