// $Id:ThreddsDataFactory.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.thredds;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDatasetCache;

import ucar.nc2.dataset.grid.GridDataset;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.dt.point.PointObsDatasetFactory;

import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;

import thredds.catalog.*;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * This tries to translate a THREDDS InvDataset into a data object that can be used, either a NetcdfDataset or a
 * TypedDataset like PointObsDataset, etc.
 *
 * As input, it can take
 * <ol><li> An InvAccess object.
 * <li> An InvDataset object. If the InvDataset has more that one InvAccess, it has to try to choose which to use,
 *  based on what Service type we know how to work with.
 * <li> A url of the form [thredds:]catalog.xml#datasetId. In this case it opens the catalog, and looks for the
 *  InvDataset with the given datasetId.
 * <li> A url of the form thredds:resolve:resolveURL. In this case it expects that the URL will return a catalog with a
 *  single top level dataset, which is the "resolved" dataset.
 * </ol>
 *
 * It annotates the NetcdfDataset with info from the InvDataset.
 *
 * You can reuse a ThreddsDataFactory, but do not share across threads.
 *
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class ThreddsDataFactory {
  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("thredds/debugOpen");
    debugTypeOpen = debugFlag.isSet("thredds/openDatatype");
  }
  static private boolean debugOpen = false;
  static private boolean debugTypeOpen = false;

  //private InvAccess accessUsed = null;
  private InvCatalogFactory catFactory = new InvCatalogFactory("", false);
  // private String getErrorMessages(StringBuffer log) { return log == null ? "" : log.toString(); }

  /**
   * The result of trying to open a THREDDS dataset.
   * If fatalError is true, the operation failed, errLog should indicate why.
   * Otherwise, the datatype is set:<ul>
   * <li><b>Grid</b>: GridDataset object is set.
   * <li><b>Grid</b>: GridDataset object is set.
   * <li><b>Grid</b>: GridDataset object is set.
   * There may still be warning or diagnostic errors in errLog.
   *
   */
  public static class Result {
    public boolean fatalError;
    public StringBuffer errLog = new StringBuffer();

    public thredds.catalog.DataType dtype;
    public ucar.nc2.dt.GridDataset gridDataset;
    public ucar.nc2.dt.PointObsDataset pobsDataset;
    public ucar.nc2.dt.RadialDatasetSweep radialDataset;
    public String imageURL;

    public String location;
    public InvAccess accessUsed;

    public void close() throws IOException {
      if (gridDataset != null) gridDataset.close();
      if (pobsDataset != null) pobsDataset.close();
    }
  }

  /**
   * Open a TypedDataset from a URL location string. Example URLS: <ul>
   *  <li>http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   *  <li>thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   *  <li>thredds://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   *  <li>thredds:file:c:/test/data/catalog/addeStationDataset.xml#AddeSurfaceData (absolute file)
   *  <li>thredds:resolve:resolveURL
   * </ul>
   * @param location [thredds:]catalog.xml#datasetId
   * @param task may be null
   * @return ThreddsDataFactory.Result check fatalError for validity
   * @throws java.io.IOException
   */
  public ThreddsDataFactory.Result openDatatype( String location, ucar.nc2.util.CancelTask task) throws java.io.IOException {

    ThreddsDataFactory.Result result = new ThreddsDataFactory.Result();
    InvDataset invDataset =  processLocation( location, task, result);
    if (result.fatalError)
      return result;

    return openDatatype( invDataset, task, result);
  }

  private InvDataset processLocation( String location, ucar.nc2.util.CancelTask task, Result result) {
    location = location.trim();
    location = ucar.unidata.util.StringUtil.replace(location, '\\', "/");

    if (location.startsWith("thredds:"))
      location = location.substring(8);

    if (location.startsWith("resolve:")) {
      location = location.substring(8);
      return openResolver(location, task, result);
    }

    if (!location.startsWith("http:") && !location.startsWith("file:"))
      location = "http:" + location;

    InvCatalog catalog;
    InvDataset invDataset;
    String datasetId;

    int pos = location.indexOf('#');
    if (pos < 0) {
      result.fatalError = true;
      result.errLog.append("Must have the form catalog.xml#datasetId"+"\n");
      return null;
    }

    String catalogLocation = location.substring(0,pos);
    catalog = catFactory.readXML(catalogLocation);
    if (!catalog.check( result.errLog)) {
      result.fatalError = true;
      return null;
    }

    datasetId = location.substring( pos+1);
    invDataset = catalog.findDatasetByID( datasetId);
    if (invDataset == null) {
      result.fatalError = true;
      result.errLog.append("Could not find dataset "+datasetId+" in "+catalogLocation+"\n");
      return null;
    }

    return invDataset;
  }

  /**
   * Open a TypedDataset from an InvDataset object, deciding on which InvAccess to use.
   * @param invDataset
   * @param task may be null
   * @return ThreddsDataFactory.Result check fatalError for validity
   * @throws IOException
   */
  public ThreddsDataFactory.Result openDatatype(InvDataset invDataset, ucar.nc2.util.CancelTask task) throws IOException {
    return openDatatype( invDataset, task, new Result());
  }

  private ThreddsDataFactory.Result openDatatype(InvDataset invDataset, ucar.nc2.util.CancelTask task, Result result) throws IOException {

    result.dtype = invDataset.getDataType();

    if (result.dtype == thredds.catalog.DataType.GRID) {
      NetcdfDataset ncd = openDataset( invDataset, true, task, result);   // LOOK always acquire

      if (null == ncd)
        result.fatalError = true;
      else {
        result.location = ncd.getLocation();
        result.gridDataset = new GridDataset( ncd);
      }
    }

    else if ((result.dtype == thredds.catalog.DataType.POINT) || (result.dtype == thredds.catalog.DataType.STATION)) {
      result.pobsDataset = openPointDataset( invDataset, task, result.errLog);
      if (result.pobsDataset != null)
        result.location = result.pobsDataset.getLocationURI();
      if (null == result.pobsDataset)
        result.fatalError = true;
    }

    else if (invDataset.getDataType() == thredds.catalog.DataType.IMAGE) {

      InvAccess access = getImageAccess( invDataset, task, result);
      if (access != null) {
        result.imageURL = access.getStandardUrlName();
        result.location = result.imageURL;
        result.accessUsed = access;
      } else
        result.fatalError = true;
    }

    else {
      // LOOK: heres where we should start to guess.
      result.errLog.append("No datatype is associated with the THREDDS Dataset, open as a Grid\n");

      NetcdfDataset ncd = openDataset( invDataset, true, task, result.errLog);   // LOOK always acquire
      if (null == ncd)
        result.fatalError = true;
      else {
        result.location = ncd.getLocation();
        result.gridDataset = new GridDataset( ncd);
      }
    }

    return result;
  }

 /**
  * Open a TypedDataset from an InvAccess object.
   *
   * @param access use this InvAccess.
   * @param task may be null
  * @return ThreddsDataFactory.Result check fatalError for validity
   * @throws IOException
   */
  public ThreddsDataFactory.Result openDatatype(InvAccess access, ucar.nc2.util.CancelTask task) throws IOException {
    return openDatatype(access, task, new Result());
 }

  private ThreddsDataFactory.Result openDatatype(InvAccess access, ucar.nc2.util.CancelTask task, Result result) throws IOException {

    InvDataset invDataset = access.getDataset();
    result.dtype = invDataset.getDataType();
    result.accessUsed = access;

    if (result.dtype == thredds.catalog.DataType.GRID) {
      NetcdfDataset ncd = openDataset( access, true, task, result.errLog); // LOOK always acquire

      if (null == ncd)
        result.fatalError = true;
      else {
        result.location = ncd.getLocation();
        result.gridDataset = new GridDataset( ncd);
      }
    }

    else if ((result.dtype == thredds.catalog.DataType.POINT) || (result.dtype == thredds.catalog.DataType.STATION)) {
      result.pobsDataset = PointObsDatasetFactory.open( access, task, result.errLog);
      if (result.pobsDataset != null)
        result.location = result.pobsDataset.getLocationURI();
      if (null == result.pobsDataset)
        result.fatalError = true;
    }

    else if (invDataset.getDataType() == thredds.catalog.DataType.IMAGE) {
        result.imageURL = access.getStandardUrlName();
        result.location = result.imageURL;
    }

    else {
      // LOOK: heres where we should start to guess.
      result.errLog.append("No datatype is associated with the THREDDS Dataset, open as a Grid\n");

      NetcdfDataset ncd = openDataset( access, true, task, result); // LOOK always acquire
      if (null == ncd)
        result.fatalError = true;
      else {
        result.location = ncd.getLocation();
        result.gridDataset = new GridDataset( ncd);
      }
    }

    return result;
  }

  /**
   * Open a NetcdfDataset from a URL location string. Example URLS: <ul>
   *  <li>http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   *  <li>thredds:http://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   *  <li>thredds://localhost:8080/test/addeStationDataset.xml#surfaceHourly
   *  <li>thredds:file:c:/dev/netcdf-java-2.2/test/data/catalog/addeStationDataset.xml#AddeSurfaceData (absolute file)
   *  <li>thredds:resolve:resolveURL
   * </ul>
   *
   * @param location catalog.xml#datasetId, may optionally start with "thredds:"
   * @param task may be null
   * @param log error messages gp here, may be null
   * @return NetcdfDataset
   * @throws java.io.IOException
   */
  public NetcdfDataset openDataset( String location, boolean acquire, ucar.nc2.util.CancelTask task, StringBuffer log) throws java.io.IOException {
    Result result = new Result();
    InvDataset invDataset = processLocation( location, task, result);
    if (result.fatalError) {
      if (log != null) log.append( result.errLog);
      return null;
    }

    return openDataset( invDataset, acquire, task, result);
  }

  /**
   * Try to open as a NetcdfDataset.
   * @param invDataset open this
   * @param task may be null
   * @param log error message, may be null
   * @return NetcdfDataset or null if failure
   * @throws IOException
   */
  public NetcdfDataset openDataset( InvDataset invDataset,  boolean acquire, ucar.nc2.util.CancelTask task, StringBuffer log) throws java.io.IOException {
    Result result = new Result();
    NetcdfDataset ncd = openDataset( invDataset, acquire, task, result);
    if (log != null) log.append( result.errLog);
    return (result.fatalError) ? null : ncd;
  }

  private NetcdfDataset openDataset( InvDataset invDataset,  boolean acquire, ucar.nc2.util.CancelTask task, Result result) throws java.io.IOException {

    IOException saveException = null;

    List accessList = new ArrayList( invDataset.getAccess()); // a list of all the accesses
    while (accessList.size() > 0) {
      InvAccess access = chooseDatasetAccess(accessList);

      // no valid access
      if (access == null) {
        result.errLog.append("No access that could be used in dataset "+invDataset+"\n");
        if (saveException != null)
          throw saveException;
        return null;
      }

      String datasetLocation = access.getStandardUrlName();
      ServiceType serviceType = access.getService().getServiceType();
      if (debugOpen)
        System.out.println("ThreddsDataset.openDataset try "+datasetLocation+" "+serviceType);

      // deal with RESOLVER type
      if (serviceType == ServiceType.RESOLVER) {
        InvDatasetImpl rds = openResolver( datasetLocation, task, result);
        if (rds == null) return null;
        accessList = new ArrayList( rds.getAccess());
        continue;
      }

      // ready to open it through netcdf API
      NetcdfDataset ds;

      // try to open
      try {
        ds = openDataset( access, acquire, task, result);
        
      } catch (IOException e) {
        result.errLog.append("Cant open  "+datasetLocation+"\n");
        if (debugOpen) {
          System.out.println("Cant open= "+datasetLocation+" "+serviceType);
          e.printStackTrace();
        }

        accessList.remove( access);
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
   * @param access open this InvAccess
   * @param task may be null
   * @param log error message, may be null
   * @return NetcdfDataset or null if failure
   * @throws IOException
   */
  public NetcdfDataset openDataset( InvAccess access, boolean acquire, ucar.nc2.util.CancelTask task, StringBuffer log) throws java.io.IOException {
     Result result = new Result();
     NetcdfDataset ncd = openDataset( access, acquire, task, result);
     if (log != null) log.append( result.errLog);
     return (result.fatalError) ? null : ncd;
  }

 private NetcdfDataset openDataset( InvAccess access, boolean acquire, ucar.nc2.util.CancelTask task, Result result) throws java.io.IOException {
    InvDataset invDataset = access.getDataset();
    String datasetId = invDataset.getID();
    String title = invDataset.getName();

    String datasetLocation = access.getStandardUrlName();
    ServiceType serviceType = access.getService().getServiceType();
    if (debugOpen) System.out.println("ThreddsDataset.openDataset= "+datasetLocation);

    // deal with RESOLVER type
    if (serviceType == ServiceType.RESOLVER) {
      InvDatasetImpl rds = openResolver( datasetLocation, task, result);
      if (rds == null) return null;
      return openDataset( rds, acquire, task, result);
    }

    // ready to open it through netcdf API
    NetcdfDataset ds;

    // open DODS type
    if ((serviceType == ServiceType.OPENDAP) || (serviceType == ServiceType.DODS)) {
      String curl = DODSNetcdfFile.canonicalURL( datasetLocation);
      ds = acquire ? NetcdfDatasetCache.acquire( curl, task) : NetcdfDataset.openDataset(curl, true, task);
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
      ds = acquire ? NetcdfDatasetCache.acquire( datasetLocation, task) : NetcdfDataset.openDataset(datasetLocation, true, task);
    }

    if (ds != null) {
      ds.setId(datasetId);
      ds.setTitle(title);
      annotate( invDataset, ds);
    }

    // see if there's metadata LOOK whats this
    List list = invDataset.getMetadata( MetadataType.NcML);
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
   * @param accessList choose from this list.
   * @return best access method.
   */
  public InvAccess chooseDatasetAccess(List accessList) {
    if (accessList.size() == 0)
      return null;

    // better to be null, then o try to read unreadable file
    // if (accessList.size() == 1)
    //  return (InvAccess) accessList.get(0);

    // should mean that it can be opened through netcdf API
    InvAccess access = findAccessByServiceType( accessList, ServiceType.FILE);
    if (access == null)
      access = findAccessByServiceType( accessList, ServiceType.NETCDF); //  ServiceType.NETCDF is deprecated, use FILE
    if (access == null)
      access = findAccessByServiceType( accessList, ServiceType.DODS);
    if (access == null)
      access = findAccessByServiceType( accessList, ServiceType.OPENDAP);

      // look for HTTP with format we can read
    if (access == null) {
      InvAccess tryAccess = findAccessByServiceType( accessList, ServiceType.HTTPServer);
      if (tryAccess == null)
        tryAccess = findAccessByServiceType( accessList, ServiceType.HTTP); //  ServiceType.HTTP should be HTTPServer

      if (tryAccess != null) {
        DataFormatType format = tryAccess.getDataFormatType();
        if ((DataFormatType.NETCDF == format) || (DataFormatType.NCML == format))
          access = tryAccess;
      }
    }

      // ADDE
    if (access == null)
      access = findAccessByServiceType( accessList, ServiceType.ADDE);

      // RESOLVER
    if (access == null) {
      access = findAccessByServiceType( accessList, ServiceType.RESOLVER);
    }

    return access;
  }

  private InvDatasetImpl openResolver( String urlString, ucar.nc2.util.CancelTask task, Result result) {
    //try {
      InvCatalogImpl catalog = catFactory.readXML( urlString);
      if (catalog == null) return null;
      StringBuffer buff = new StringBuffer();
      if (!catalog.check( buff)) {
        result.errLog.append("Invalid catalog from Resolver <"+ urlString+">\n");
        result.errLog.append( buff.toString());
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

    /* } catch (Exception e) {
      e.printStackTrace();
      return null;
    } */
  }

  /**
   * Add information from the InvDataset to the NetcdfDataset.
   * @param ds get info from here
   * @param ncDataset add to here
   */
  public static void annotate( InvDataset ds, NetcdfDataset ncDataset) {
    ncDataset.setTitle( ds.getName());
    ncDataset.setId( ds.getID());

    // add properties as global attributes
    java.util.List list = ds.getProperties();
    for (int i=0; i<list.size(); i++) {
      InvProperty p = (InvProperty) list.get(i);
      String name = p.getName();
      if (null == ncDataset.findGlobalAttribute(name)) {
        ncDataset.addAttribute( null, new Attribute(name, p.getValue()));
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
 // point

    /**
   * Try to open as a Point Dataset.
   * @param invDataset open this
   * @param task may be null
   * @param log error message, may be null
   * @return PointObsDataset or null if failure
   * @throws IOException
   */
  public PointObsDataset openPointDataset( InvDataset invDataset, ucar.nc2.util.CancelTask task, StringBuffer log) throws IOException {
     Result result = new Result();
     PointObsDataset pob = openPointDataset( invDataset, task, result);
     if (log != null) log.append( result.errLog);
     return (result.fatalError) ? null : pob;
  }

  private PointObsDataset openPointDataset( InvDataset invDataset, ucar.nc2.util.CancelTask task, Result result) throws IOException {
    // LOOK in the future, this may be more specific to Point Obs
    InvAccess access = chooseDatasetAccess( new ArrayList(invDataset.getAccess()));

    if (access == null) {
      result.errLog.append("No Access that we know how to open");
      return null;
    }

    result.accessUsed = access;
    return PointObsDatasetFactory.open( access, task, result.errLog);
  }

 //////////////////////////////////////////////////////////////////////////////////
 // image

  private InvAccess getImageAccess( InvDataset invDataset, ucar.nc2.util.CancelTask task, Result result) {

    List accessList = new ArrayList( invDataset.getAccess()); // a list of all the accesses
    while (accessList.size() > 0) {
      InvAccess access = chooseImageAccess(accessList);
      if (access != null) return access;

      // next choice is resolver type.
      access = invDataset.getAccess( ServiceType.RESOLVER);

      // no valid access
      if (access == null) {
        result.errLog.append("No access that could be used for Image Type "+invDataset+"\n");
        return null;
      }

      String datasetLocation = access.getStandardUrlName();
      ServiceType serviceType = access.getService().getServiceType();
      if (debugOpen) System.out.println("ThreddsDataset.openDataset= "+datasetLocation+"\n");

      // deal with RESOLVER type
      if (serviceType == ServiceType.RESOLVER) {
        InvDatasetImpl rds = openResolver( datasetLocation, task, result);
        if (rds == null) return null;
        accessList = new ArrayList( invDataset.getAccess());
        continue;
      }

    } // loop over accesses

    return null;
  }

  // see if theres an access method we can open through the netcdf API
  private InvAccess chooseImageAccess(List accessList) {

    InvAccess access;

    access = findAccessByDataFormatType( accessList, DataFormatType.JPEG);
    if (access != null) return access;

    access = findAccessByDataFormatType( accessList, DataFormatType.GIF);
    if (access != null) return access;

    access = findAccessByDataFormatType( accessList, DataFormatType.TIFF);
    if (access != null) return access;

    access = findAccessByServiceType( accessList, ServiceType.ADDE);
    if (access != null) {
      String datasetLocation = access.getStandardUrlName();
      if (datasetLocation.indexOf("image") > 0)
        return access;
    }

    return access;
  }

  ///////////////////////////////////////////////////////////////////

  // works against the accessList instead of the dataset list, so we can remove and try again
  private InvAccess findAccessByServiceType( List accessList, ServiceType type) {
    for (int i = 0; i < accessList.size(); i++) {
      InvAccess a =  (InvAccess) accessList.get(i);
      if (type.toString().equalsIgnoreCase( a.getService().getServiceType().toString()))
        return a;
    }
    return null;
  }

  // works against the accessList instead of the dataset list, so we can remove and try again
  private InvAccess findAccessByDataFormatType( List accessList, DataFormatType type) {
    for (int i = 0; i < accessList.size(); i++) {
      InvAccess a =  (InvAccess) accessList.get(i);
      if (type.toString().equalsIgnoreCase( a.getDataFormatType().toString()))
        return a;
    }
    return null;
  }

}
