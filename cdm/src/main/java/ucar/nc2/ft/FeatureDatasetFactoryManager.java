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

package ucar.nc2.ft;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.ft.point.standard.PointDatasetStandardFactory;
import ucar.nc2.ft.grid.GridDatasetStandardFactory;
import ucar.nc2.iosp.IOServiceProvider;

import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.io.IOException;

/**
 * Manager of factories for FeatureDatasets.
 * This supercedes ucar.nc2.dt.TypedDatasetFactory
 * <p> all point fetures are going through PointDatasetStandardFactory, which uses TableAna;yzer to deal
 *   with specific datasets.
 *
 * @author caron
 * @since Mar 19, 2008
 */
public class FeatureDatasetFactoryManager {

  static private List<Factory> factoryList = new ArrayList<Factory>();
  static private boolean userMode = false;
  static private boolean debug = false;

  // search in the order added
  static {
    registerFactory(FeatureType.ANY_POINT, PointDatasetStandardFactory.class);
    registerFactory(FeatureType.GRID, GridDatasetStandardFactory.class);

    // further calls to registerFactory are by the user
    userMode = true;
  }


   /**
    * Register a class that implements a FeatureDatasetFactory.
    * @param className name of class that implements FeatureDatasetFactory.
    * @param datatype  scientific data type
    * @throws ClassNotFoundException if loading error
    */
   static public void registerFactory( FeatureType datatype, String className) throws ClassNotFoundException {
     Class c = Class.forName( className);
     registerFactory( datatype, c);
   }

   /**
    * Register a class that implements a FeatureDatasetFactory.
    * @param datatype scientific data type
    * @param c class that implements FeatureDatasetFactory.
    */
  static public void registerFactory( FeatureType datatype, Class c) {
    if (!(FeatureDatasetFactory.class.isAssignableFrom( c)))
      throw new IllegalArgumentException("Class "+c.getName()+" must implement FeatureDatasetFactory");

    // fail fast - get Instance
    Object instance;
    try {
      instance = c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("CoordTransBuilderIF Class "+c.getName()+" cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("CoordTransBuilderIF Class "+c.getName()+" is not accessible");
    }

    // user stuff gets put at top
    if (userMode)
      factoryList.add( 0, new Factory( datatype, c, (FeatureDatasetFactory) instance));
    else
      factoryList.add( new Factory( datatype, c, (FeatureDatasetFactory)instance));

  }

  static private class Factory {
    FeatureType featureType;
    Class c;
    FeatureDatasetFactory factory;

    Factory(FeatureType featureType, Class c, FeatureDatasetFactory factory) {
      this.featureType = featureType;
      this.c = c;
      this.factory = factory;
    }
  }

  /**
   * Open a dataset as a FeatureDataset.
   *
   * @param wantFeatureType open this kind of FeatureDataset; may be null, which means search all factories.
   *   If datatype is not null, only return correct FeatureDataset (eg PointFeatureDataset for DataType.POINT).
   * @param location URL or file location of the dataset
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of FeatureDataset, or null if no factory was found
   * @throws java.io.IOException on io error
   */
  static public FeatureDataset open( FeatureType wantFeatureType, String location, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    /* special processing for thredds: datasets
    if (location.startsWith("thredds:") && (datatype != null)) {
      ThreddsDataFactory.Result result = new ThreddsDataFactory().openDatatype( location, task);
      errlog.append( result.errLog);
      return (result.fatalError) ? null : result.tds;
    } */

    NetcdfDataset ncd = NetcdfDataset.acquireDataset( location, task);
    return wrap(wantFeatureType, ncd, task, errlog);
  }

  /**
   * Wrap a NetcdfDataset as a FeatureDataset.
   *
   * @param wantFeatureType open this kind of FeatureDataset; may be null, which means search all factories.
   *   If datatype is not null, only return FeatureDataset with objects of that type
   * @param ncd  the NetcdfDataset to wrap as a FeatureDataset
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of FeatureDataset, or null if no factory was found
   * @throws java.io.IOException on io error
   */
  static public FeatureDataset wrap( FeatureType wantFeatureType, NetcdfDataset ncd, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    if (debug) System.out.println("wrap "+ncd.getLocation()+" want = "+wantFeatureType);

    // the case where we dont know what type it is
    if ((wantFeatureType == null) || (wantFeatureType == FeatureType.NONE)) {
      return wrapUnknown( ncd, task, errlog);
    }

    // look for a Factory that claims this dataset by passing back an "analysis result" object
    Object analysis = null;
    FeatureDatasetFactory useFactory = null;
    for (Factory fac : factoryList) {
      if (!featureTypeOk(wantFeatureType, fac.featureType)) continue;
      if (debug) System.out.println(" wrap try factory "+fac.factory.getClass().getName());

      analysis = fac.factory.isMine(wantFeatureType, ncd, errlog);
      if (analysis != null) {
        useFactory = fac.factory;
        break;
      }
    }

   if (null == useFactory) {
      // errlog.format("**Failed to find Datatype Factory for= %s datatype=%s\n", ncd.getLocation(), wantFeatureType);
      return null;
    }

    // this call must be thread safe - done by implementation
    return useFactory.open( wantFeatureType, ncd, analysis, task, errlog);
  }

  static private FeatureDataset wrapUnknown( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    FeatureType ft = findFeatureType(ncd);
    if (ft != null)
      return wrap( ft, ncd, task, errlog);

    // analyse the coordsys rank
    if (isGrid(ncd.getCoordinateSystems())) {
      ucar.nc2.dt.grid.GridDataset gds = new ucar.nc2.dt.grid.GridDataset( ncd);
      if (gds.getGrids().size() > 0) {
        if (debug) System.out.println(" wrapUnknown found grids ");        
        return gds;
      }
    }

    // look for a Factory that claims this dataset
    Object analysis = null;
    FeatureDatasetFactory useFactory = null;
    for (Factory fac : factoryList) {
      if (!featureTypeOk(null, fac.featureType)) continue;
      if (debug) System.out.println(" wrapUnknown try factory "+fac.factory.getClass().getName());

      analysis = fac.factory.isMine(null, ncd, errlog);
      if (null != analysis) {
        useFactory = fac.factory;
        break;
      }
    }

    // Factory not found
    if (null == useFactory) {
      // if no datatype was requested, give em a GridDataset only if some Grids are found.
      ucar.nc2.dt.grid.GridDataset gds = new ucar.nc2.dt.grid.GridDataset( ncd);
      if (gds.getGrids().size() > 0)
        return gds;
    }

   if (null == useFactory) {
      // errlog.format("**Failed to find Datatype Factory for= %s\n", ncd.getLocation());
      return null;
    }

    // this call must be thread safe - done by implementation
    return useFactory.open( null, ncd, analysis, task, errlog);
  }

  static private boolean isGrid(java.util.List<CoordinateSystem> csysList) {
    CoordinateSystem use = null;
    for (CoordinateSystem csys : csysList) {
      if (use == null) use = csys;
      else if (csys.getCoordinateAxes().size() > use.getCoordinateAxes().size() )
        use = csys;
    }

    if (use == null) return false;
    CoordinateAxis lat = use.getLatAxis();
    CoordinateAxis lon = use.getLonAxis();
    if ((lat != null) && (lat.getSize() <= 1)) return false; // COARDS singletons
    if ((lon != null) && (lon.getSize() <= 1)) return false;

    // hueristics - cant say i like this, multidim point features could eaily violate
    return (use.getRankDomain() > 2) && (use.getRankDomain() <= use.getRankRange());
  }

  /**
   * Determine if factory type matches wanted feature type.
   * @param want looking for this FeatureType
   * @param facType factory is of this type
   * @return true if match
   */
  static public boolean featureTypeOk( FeatureType want, FeatureType facType) {
    if (want == null) return true;
    if (want == facType) return true;

    if (want == FeatureType.ANY_POINT) {
      return facType.isPointFeatureType();
    }

    if (facType == FeatureType.ANY_POINT) {
      return want.isPointFeatureType();
    }

    return false;
  }

  static public FeatureType findFeatureType( NetcdfDataset ncd)  {
    // look for explicit guidance
    String cdm_datatype = ncd.findAttValueIgnoreCase(null, "cdm_data_type", null);
    if (cdm_datatype == null)
      cdm_datatype = ncd.findAttValueIgnoreCase(null, "cdm_datatype", null);
    if (cdm_datatype == null)
      cdm_datatype = ncd.findAttValueIgnoreCase(null, "thredds_data_type", null);

    if (cdm_datatype != null) {
      for (FeatureType ft : FeatureType.values())
        if (cdm_datatype.equalsIgnoreCase(ft.name())) {
          if (debug) System.out.println(" wrapUnknown found cdm_datatype "+cdm_datatype);
          return ft;
        }
    }

    // LOOK - compare to CF names when those are finilized
    String cf_datatype = ncd.findAttValueIgnoreCase(null, "CF:featureType", null);
    if (cf_datatype == null)
      cf_datatype = ncd.findAttValueIgnoreCase(null, "CFfeatureType", null);

    if (cf_datatype != null) {
      if (debug) System.out.println(" wrapUnknown found cf_datatype "+cdm_datatype);
      return FeatureType.getType(cdm_datatype);
    }
    
    return null;
  }

}
