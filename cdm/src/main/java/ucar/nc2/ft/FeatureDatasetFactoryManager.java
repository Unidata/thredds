/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package ucar.nc2.ft;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.PointDatasetStandardFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;
import java.io.IOException;

/**
 * Manager of factories for FeatureDatasets.
 *
 * @author caron
 * @since Mar 19, 2008
 */
public class FeatureDatasetFactoryManager {

  static private List<Factory> factoryList = new ArrayList<Factory>();
  static private boolean userMode = false;

  // search in the order added
  static {
    //registerFactory(FeatureType.STATION_PROFILE, ucar.nc2.dt2.point.NmcStationProfileDatasetFactory.class);

    //registerFactory(FeatureType.STATION, UnidataStationFeatureDatasetFactory.class);

    //registerFactory(FeatureType.POINT, UnidataPointFeatureDatasetFactory.class);
    registerFactory(FeatureType.ANY_POINT, PointDatasetStandardFactory.class);

    // further calls to registerFactory are by the user
    userMode = true;
  }


   /**
    * Register a class that implements a TypedDatasetFactoryIF.
    * @param className name of class that implements TypedDatasetFactoryIF.
    * @param datatype  scientific data type
    * @throws ClassNotFoundException if loading error
    */
   static public void registerFactory( FeatureType datatype, String className) throws ClassNotFoundException {
     Class c = Class.forName( className);
     registerFactory( datatype, c);
   }

   /**
    * Register a class that implements a TypedDatasetFactoryIF.
    * @param datatype scientific data type
    * @param c class that implements TypedDatasetFactoryIF.
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
   * Open a dataset location as a FeatureDataset.
   *
   * @param datatype open this kind of FeatureDataset; may be null, which means search all factories.
   *   If datatype is not null, only return correct FeatureDataset (eg PointFeatureDataset for DataType.POINT).
   * @param location URL or file location of the dataset
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of FeatureDataset
   * @throws java.io.IOException on io error
   */
  static public FeatureDataset open( FeatureType datatype, String location, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {
    /* special processing for thredds: datasets
    if (location.startsWith("thredds:") && (datatype != null)) {
      ThreddsDataFactory.Result result = new ThreddsDataFactory().openDatatype( location, task);
      errlog.append( result.errLog);
      return (result.fatalError) ? null : result.tds;
    } */

    NetcdfDataset ncd = NetcdfDataset.acquireDataset( location, task);
    return wrap( datatype, ncd, task, errlog);
  }

  /**
   * Wrap a NetcdfDataset as a FeatureDataset.
   *
   * @param featureType open this kind of FeatureDataset; may be null, which means search all factories.
   *   If datatype is not null, only return FeatureDataset with objects of that type
   * @param ncd  the NetcdfDataset to wrap as a FeatureDataset
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of FeatureDataset
   * @throws java.io.IOException on io error
   */
  static public FeatureDataset wrap( FeatureType featureType, NetcdfDataset ncd, ucar.nc2.util.CancelTask task, Formatter errlog) throws IOException {

    // look for a Factory that claims this dataset
    FeatureDatasetFactory useFactory = null;
    for (Factory fac : factoryList) {
      if (!featureTypeOk(featureType, fac.featureType)) continue;

      if (fac.factory.isMine(featureType, ncd)) {
        useFactory = fac.factory;
        break;
      }
    }

    /* Factory not found
    if (null == useClass) {

      // POINT is also a STATION
      if (datatype == DataType.POINT) {
        return wrap( DataType.STATION, ncd, task, errlog);
      }

      /* if explicitly requested, give em a GridDataset even if no Grids
      if (datatype == DataType.GRID) {
        return new ucar.nc2.dt.grid.GridDataset( ncd);
      }

      if (null == datatype) {
        // if no datatype was requested, give em a GridDataset only if some Grids are found.
        ucar.nc2.dt.grid.GridDataset gds = new ucar.nc2.dt.grid.GridDataset( ncd);
        if (gds.getGrids().size() > 0)
          return gds;
      } */

   if (null == useFactory) {
      errlog.format("**Failed to find Datatype Factory for= %s datatype=%s\n", ncd.getLocation(), featureType);
      return null;
    }

    // get a new instance of the Factory class, for thread safety
    FeatureDatasetFactory builder = useFactory.copy();
    return builder.open( featureType, ncd, task, errlog);
  }

  static public boolean featureTypeOk( FeatureType want, FeatureType facType) {
    if (want == null) return true;
    if (want == facType) return true;

    // bit of a kludge I guess
    if (want == FeatureType.ANY_POINT) {
      return ((facType == FeatureType.POINT) || (facType == FeatureType.STATION) || (facType == FeatureType.TRAJECTORY) ||
          (facType == FeatureType.PROFILE) || (facType == FeatureType.STATION_PROFILE) && (facType == FeatureType.SECTION));
    }

    return false;
  }

}
