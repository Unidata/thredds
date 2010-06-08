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
package ucar.nc2.thredds;

import ucar.nc2.*;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.stream.CdmRemoteFeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.constants.FeatureType;

import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;

import thredds.catalog.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.io.IOException;

/**
 * This tries to translate a THREDDS InvDataset into a data object that can be used, either as a NetcdfDataset or as a FeatureDataset.
 * <p/>
 * As input, it can take
 * <ol><li> An InvAccess object.
 * <li> An InvDataset object. If the InvDataset has more that one InvAccess, it has to try to choose which to use,
 * based on what Service type we know how to work with.
 * <li> A url of the form [thredds:]catalog.xml#datasetId. In this case it opens the catalog, and looks for the
 * InvDataset with the given datasetId.
 * <li> A url of the form thredds:resolve:resolveURL. In this case it expects that the URL will return a catalog with a
 * single top level dataset, which is the "resolved" dataset.
 * </ol>
 * <p/>
 * It annotates the NetcdfDataset with info from the InvDataset.
 * <p/>
 * You can reuse a ThreddsDataFactory, but only within a single thread.
 *
 * @author caron
 */
public class ThreddsDataFactory {
  static public final String SCHEME = "thredds:";

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
  public static class Result {
    public boolean fatalError;
    public Formatter errLog = new Formatter();

    public FeatureType featureType;
    public ucar.nc2.ft.FeatureDataset featureDataset;
    public String imageURL;

    public String location;
    public InvAccess accessUsed;
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
  public ThreddsDataFactory.Result openFeatureDataset(String urlString, ucar.nc2.util.CancelTask task) throws IOException {

    ThreddsDataFactory.Result result = new ThreddsDataFactory.Result();
    InvDataset invDataset = processLocation(urlString, task, result);
    if (result.fatalError)
      return result;

    return openFeatureDataset(null, invDataset, task, result);
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
  public ThreddsDataFactory.Result openFeatureDataset(FeatureType wantFeatureType, String urlString, ucar.nc2.util.CancelTask task)
          throws IOException {
    ThreddsDataFactory.Result result = new ThreddsDataFactory.Result();
    InvDataset invDataset = processLocation(urlString, task, result);
    if (result.fatalError)
      return result;

    return openFeatureDataset(wantFeatureType, invDataset, task, result);
  }

  private InvDataset processLocation(String location, ucar.nc2.util.CancelTask task, Result result) {
    location = location.trim();
    location = ucar.unidata.util.StringUtil.replace(location, '\\', "/");

    if (location.startsWith(SCHEME))
      location = location.substring(8);

    if (location.startsWith("resolve:")) {
      location = location.substring(8);
      return openResolver(location, task, result);
    }

    if (!location.startsWith("http:") && !location.startsWith("file:"))   // LOOK whats this for??
      location = "http:" + location;

    InvCatalog catalog;
    InvDataset invDataset;
    String datasetId;

    int pos = location.indexOf('#');
    if (pos < 0) {
      result.fatalError = true;
      result.errLog.format("Must have the form catalog.xml#datasetId%n");
      return null;
    }

    InvCatalogFactory catFactory = new InvCatalogFactory("", false);
    String catalogLocation = location.substring(0, pos);
    catalog = catFactory.readXML(catalogLocation);
    StringBuilder buff = new StringBuilder();
    if (!catalog.check(buff)) {
      result.errLog.format("Invalid catalog from Resolver <%s>%n%s%n", catalogLocation, buff.toString());
      result.fatalError = true;
      return null;
    }

    datasetId = location.substring(pos + 1);
    invDataset = catalog.findDatasetByID(datasetId);
    if (invDataset == null) {
      result.fatalError = true;
      result.errLog.format("Could not find dataset %s in %s %n", datasetId, catalogLocation);
      return null;
    }

    return invDataset;
  }

  /**
   * Open a FeatureDataset from an InvDataset object, deciding on which InvAccess to use.
   *
   * @param invDataset use this to figure out what type, how to open, etc
   * @param task       allow user to cancel; may be null
   * @return ThreddsDataFactory.Result check fatalError for validity
   * @throws IOException on read error
   */
  public ThreddsDataFactory.Result openFeatureDataset(InvDataset invDataset, ucar.nc2.util.CancelTask task) throws IOException {
    return openFeatureDataset(null, invDataset, task, new Result());
  }

  public ThreddsDataFactory.Result openFeatureDataset(FeatureType wantFeatureType, InvDataset invDataset, ucar.nc2.util.CancelTask task, Result result)
          throws IOException {

    result.featureType = invDataset.getDataType();
    if (result.featureType == null)
      result.featureType = wantFeatureType;

    // look for remote FeatureDataset
    if ((result.featureType != null) && result.featureType.isPointFeatureType()) {
      InvAccess access = findAccessByServiceType(invDataset.getAccess(), ServiceType.CdmRemote);
      if (access != null)
        return openFeatureDataset(result.featureType, access, task, result);
    }

    // special handling for images
    if (result.featureType == FeatureType.IMAGE) {
      InvAccess access = getImageAccess(invDataset, task, result);
      if (access != null) {
        return openFeatureDataset(result.featureType, access, task, result);
      } else
        result.fatalError = true;
      return result;
    }

    // special handling for DQC
    InvAccess qc = invDataset.getAccess(ServiceType.QC);
    if (qc != null) {
      String dqc_location = qc.getStandardUrlName();

      if (result.featureType == FeatureType.STATION) {

        /* DqcFactory dqcFactory = new DqcFactory(true);
        QueryCapability dqc = dqcFactory.readXML(dqc_location);
        if (dqc.hasFatalError()) {
          result.errLog.append(dqc.getErrorMessages());
          result.fatalError = true;
        } */

        result.featureDataset = null; // LOOK FIX ucar.nc2.thredds.DqcStationObsDataset.factory(invDataset, dqc_location, result.errLog);
        result.fatalError = (result.featureDataset == null);

      } else {
        result.errLog.format("DQC must be station DQC, dataset = %s %n", invDataset.getName());
        result.fatalError = true;
      }

      return result;
    }

    NetcdfDataset ncd = openDataset(invDataset, true, task, result.errLog);
    if (null != ncd)
      result.featureDataset = FeatureDatasetFactoryManager.wrap(result.featureType, ncd, task, result.errLog);

    if (null == result.featureDataset)
      result.fatalError = true;
    else {
      result.location = result.featureDataset.getLocation();
      if ((result.featureType == null) && (result.featureDataset != null))
        result.featureType = result.featureDataset.getFeatureType();
    }

    return result;
  }

  /**
   * Open a FeatureDataset from an InvAccess object.
   *
   * @param access use this InvAccess.
   * @param task   may be null
   * @return ThreddsDataFactory.Result check fatalError for validity
   * @throws IOException on read error
   */
  public ThreddsDataFactory.Result openFeatureDataset(InvAccess access, ucar.nc2.util.CancelTask task) throws IOException {
    InvDataset invDataset = access.getDataset();
    ThreddsDataFactory.Result result = new Result();
    if (invDataset.getDataType() == null) {
      result.errLog.format("InvDatasert must specify a FeatureType%n");
      result.fatalError = true;
      return result;
    }

    return openFeatureDataset(invDataset.getDataType(), access, task, result);
  }

  private ThreddsDataFactory.Result openFeatureDataset(FeatureType wantFeatureType, InvAccess access, ucar.nc2.util.CancelTask task, Result result)
          throws IOException {
    result.featureType = wantFeatureType;
    result.accessUsed = access;

    // special handling for IMAGE
    if (result.featureType == FeatureType.IMAGE) {
      result.imageURL = access.getStandardUrlName();
      result.location = result.imageURL;
      return result;
    }

    if (access.getService().getServiceType() == ServiceType.CdmRemote) {
      result.featureDataset = CdmRemoteFeatureDataset.factory(wantFeatureType, access.getStandardUrlName());

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
      if ((result.featureType == null) && (result.featureDataset != null))
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
    InvDataset invDataset = processLocation(location, task, result);
    if (result.fatalError) {
      if (log != null) log.format("%s", result.errLog);
      return null;
    }

    return openDataset(invDataset, acquire, task, result);
  }

  /**
   * Try to open as a NetcdfDataset.
   *
   * @param invDataset open this
   * @param acquire    if true, aquire the dataset, else open it
   * @param task       may be null
   * @param log        error message, may be null
   * @return NetcdfDataset or null if failure
   * @throws IOException on read error
   */
  public NetcdfDataset openDataset(InvDataset invDataset, boolean acquire, ucar.nc2.util.CancelTask task, Formatter log) throws IOException {
    Result result = new Result();
    NetcdfDataset ncd = openDataset(invDataset, acquire, task, result);
    if (log != null) log.format("%s", result.errLog);
    return (result.fatalError) ? null : ncd;
  }

  private NetcdfDataset openDataset(InvDataset invDataset, boolean acquire, ucar.nc2.util.CancelTask task, Result result) throws IOException {

    IOException saveException = null;

    List<InvAccess> accessList = new ArrayList<InvAccess>(invDataset.getAccess()); // a list of all the accesses
    while (accessList.size() > 0) {
      InvAccess access = chooseDatasetAccess(accessList);

      // no valid access
      if (access == null) {
        result.errLog.format("No access that could be used in dataset %s %n", invDataset);
        if (saveException != null)
          throw saveException;
        return null;
      }

      String datasetLocation = access.getStandardUrlName();
      ServiceType serviceType = access.getService().getServiceType();
      if (debugOpen)
        System.out.println("ThreddsDataset.openDataset try " + datasetLocation + " " + serviceType);

      // deal with RESOLVER type
      if (serviceType == ServiceType.RESOLVER) {
        InvDatasetImpl rds = openResolver(datasetLocation, task, result);
        if (rds == null) return null;
        accessList = new ArrayList<InvAccess>(rds.getAccess());
        continue;
      }

      // ready to open it through netcdf API
      NetcdfDataset ds;

// try to open
      try {
        ds = openDataset(access, acquire, task, result);

      } catch (IOException e) {
        result.errLog.format("Cant open %s %n", datasetLocation);
        if (debugOpen) {
          System.out.println("Cant open= " + datasetLocation + " " + serviceType);
          e.printStackTrace();
        }

        accessList.remove(access);
        saveException = e;
        continue;
      }

      result.accessUsed = access;
      return ds;
    } // loop over accesses

    if (saveException != null) throw saveException;
    return null;
  }

  /**
   * Try to open invAccess as a NetcdfDataset.
   *
   * @param access  open this InvAccess
   * @param acquire if true, aquire the dataset, else open it
   * @param task    may be null
   * @param log     error message, may be null
   * @return NetcdfDataset or null if failure
   * @throws IOException on read error
   */
  public NetcdfDataset openDataset(InvAccess access, boolean acquire, ucar.nc2.util.CancelTask task, Formatter log) throws IOException {
    Result result = new Result();
    NetcdfDataset ncd = openDataset(access, acquire, task, result);
    if (log != null) log.format("%s", result.errLog);
    return (result.fatalError) ? null : ncd;
  }

  private static boolean enhanceMode = false;

  private NetcdfDataset openDataset(InvAccess access, boolean acquire, ucar.nc2.util.CancelTask task, Result result) throws IOException {
    InvDataset invDataset = access.getDataset();
    String datasetId = invDataset.getID();
    String title = invDataset.getName();

    String datasetLocation = access.getStandardUrlName();
    ServiceType serviceType = access.getService().getServiceType();
    if (debugOpen) System.out.println("ThreddsDataset.openDataset= " + datasetLocation);

    // deal with RESOLVER type
    if (serviceType == ServiceType.RESOLVER) {
      InvDatasetImpl rds = openResolver(datasetLocation, task, result);
      if (rds == null) return null;
      return openDataset(rds, acquire, task, result);
    }

    // ready to open it through netcdf API
    NetcdfDataset ds;

    // open DODS type
    if ((serviceType == ServiceType.OPENDAP) || (serviceType == ServiceType.DODS)) {
      String curl = DODSNetcdfFile.canonicalURL(datasetLocation);
      ds = acquire ? NetcdfDataset.acquireDataset(curl, enhanceMode, task) : NetcdfDataset.openDataset(curl, enhanceMode, task);
    }

    // open CdmRemote
    else if (serviceType == ServiceType.CdmRemote) {
      String curl = CdmRemote.canonicalURL(datasetLocation);
      ds = acquire ? NetcdfDataset.acquireDataset(curl, enhanceMode, task) : NetcdfDataset.openDataset(curl, enhanceMode, task);
    }

    /* open ADDE type
   else if (serviceType == ServiceType.ADDE) {
     try {
       ds = ucar.nc2.adde.AddeDatasetFactory.openDataset(access, task);

     } catch (IOException e) {
       log.append("Cant open as ADDE dataset= "+datasetLocation);
       accessList.remove( access);
       continue;
     }
   } */

    else {
      // open through NetcdfDataset API
      ds = acquire ? NetcdfDataset.acquireDataset(datasetLocation, enhanceMode, task) : NetcdfDataset.openDataset(datasetLocation, enhanceMode, task);
    }

    if (ds != null) {
      ds.setId(datasetId);
      ds.setTitle(title);
      annotate(invDataset, ds);
    }

    // see if there's metadata LOOK whats this
    List list = invDataset.getMetadata(MetadataType.NcML);
    if (list.size() > 0) {
      InvMetadata ncmlMetadata = (InvMetadata) list.get(0);
      NcMLReader.wrapNcML(ds, ncmlMetadata.getXlinkHref(), null);
    }

    result.accessUsed = access;
    return ds;
  }

  /**
   * Find the "best" access in case theres more than one, based on what the CDM knows
   * how to open and use.
   *
   * @param accessList choose from this list.
   * @return best access method.
   */
  public InvAccess chooseDatasetAccess(List<InvAccess> accessList) {
    if (accessList.size() == 0)
      return null;

// better to be null, then o try to read unreadable file
// if (accessList.size() == 1)
//  return (InvAccess) accessList.get(0);

    // should mean that it can be opened through netcdf API
    InvAccess access = findAccessByServiceType(accessList, ServiceType.FILE);
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.NETCDF); //  ServiceType.NETCDF is deprecated, use FILE
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.DODS);
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.OPENDAP);
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.CdmRemote);

    // look for HTTP with format we can read
    if (access == null) {
      InvAccess tryAccess = findAccessByServiceType(accessList, ServiceType.HTTPServer);
      if (tryAccess == null)
        tryAccess = findAccessByServiceType(accessList, ServiceType.HTTP); //  ServiceType.HTTP should be HTTPServer

      if (tryAccess != null) {
        DataFormatType format = tryAccess.getDataFormatType();

        // these are the file types we can read
        if ((DataFormatType.BUFR == format) || (DataFormatType.GINI == format) || (DataFormatType.GRIB1 == format)
                || (DataFormatType.GRIB2 == format) || (DataFormatType.HDF5 == format) || (DataFormatType.NCML == format)
                || (DataFormatType.NETCDF == format) || (DataFormatType.NEXRAD2 == format) || (DataFormatType.NIDS == format)
                )
          access = tryAccess;
      }
    }

    // ADDE
    if (access == null)
      access = findAccessByServiceType(accessList, ServiceType.ADDE);

    // RESOLVER
    if (access == null) {
      access = findAccessByServiceType(accessList, ServiceType.RESOLVER);
    }

    return access;
  }

  private InvDatasetImpl openResolver(String urlString, ucar.nc2.util.CancelTask task, Result result) {
    InvCatalogFactory catFactory = new InvCatalogFactory("", false);
    InvCatalogImpl catalog = catFactory.readXML(urlString);
    if (catalog == null) {
      result.errLog.format("Couldnt open Resolver %s %n ", urlString);
      return null;
    }
    StringBuilder buff = new StringBuilder();
    if (!catalog.check(buff)) {
      result.errLog.format("Invalid catalog from Resolver <%s>%n%s%n", urlString, buff.toString());
      result.fatalError = true;
      return null;
    }
    InvDataset top = catalog.getDataset();
    if (top.hasAccess())
      return (InvDatasetImpl) top;
    else {
      java.util.List datasets = top.getDatasets();
      return (InvDatasetImpl) datasets.get(0);
    }
  }

  /**
   * Add information from the InvDataset to the NetcdfDataset.
   *
   * @param ds        get info from here
   * @param ncDataset add to here
   */
  public static void annotate(InvDataset ds, NetcdfDataset ncDataset) {
    ncDataset.setTitle(ds.getName());
    ncDataset.setId(ds.getID());

// add properties as global attributes
    for (InvProperty p : ds.getProperties()) {
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

  private InvAccess getImageAccess(InvDataset invDataset, ucar.nc2.util.CancelTask task, Result result) {

    List<InvAccess> accessList = new ArrayList<InvAccess>(invDataset.getAccess()); // a list of all the accesses
    while (accessList.size() > 0) {
      InvAccess access = chooseImageAccess(accessList);
      if (access != null) return access;

// next choice is resolver type.
      access = invDataset.getAccess(ServiceType.RESOLVER);

// no valid access
      if (access == null) {
        result.errLog.format("No access that could be used for Image Type %s %n", invDataset);
        return null;
      }

      // deal with RESOLVER type
      String datasetLocation = access.getStandardUrlName();
      InvDatasetImpl rds = openResolver(datasetLocation, task, result);
      if (rds == null)
        return null;

// use the access list from the resolved dataset
      accessList = new ArrayList<InvAccess>(invDataset.getAccess());
    } // loop over accesses

    return null;
  }

  private InvAccess chooseImageAccess(List<InvAccess> accessList) {
    InvAccess access;

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

  private InvAccess findAccessByServiceType(List<InvAccess> accessList, ServiceType type) {
    for (InvAccess a : accessList) {
      if (type.toString().equalsIgnoreCase(a.getService().getServiceType().toString()))
        return a;
    }
    return null;
  }

// works against the accessList instead of the dataset list, so we can remove and try again

  private InvAccess findAccessByDataFormatType(List<InvAccess> accessList, DataFormatType type) {
    for (InvAccess a : accessList) {
      if (type.toString().equalsIgnoreCase(a.getDataFormatType().toString()))
        return a;
    }
    return null;
  }

}
