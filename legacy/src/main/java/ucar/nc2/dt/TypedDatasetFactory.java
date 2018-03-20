/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.radial.*;

/**
 * Manager of factories for TypedDatasets.
 * NOTE: use ucar.nc2.ft.FeatureDatasetFactoryManager instead, starting with 4.0
 *
 * @deprecated
 * @author caron
 */
public class TypedDatasetFactory {

  static private List<Factory> transformList = new ArrayList<Factory>();
  static private boolean userMode = false;

  // search in the order added
  static {
    registerFactory(FeatureType.POINT, ucar.nc2.dt.point.UnidataPointObsDataset.class);
    registerFactory(FeatureType.STATION, ucar.nc2.dt.point.UnidataStationObsDataset.class);
    registerFactory(FeatureType.STATION, ucar.nc2.dt.point.UnidataStationObsMultidimDataset.class);
    //registerFactory(FeatureType.POINT, ucar.nc2.dt.point.DapperDataset.class);
    //registerFactory(FeatureType.STATION, ucar.nc2.dt.point.SequenceObsDataset.class);
    registerFactory(FeatureType.STATION, ucar.nc2.dt.point.UnidataStationObsDataset2.class);
    registerFactory(FeatureType.STATION, ucar.nc2.dt.point.NdbcDataset.class);
    registerFactory(FeatureType.STATION, ucar.nc2.dt.point.MadisStationObsDataset.class);
    registerFactory(FeatureType.STATION, ucar.nc2.dt.point.OldUnidataStationObsDataset.class);
    registerFactory(FeatureType.POINT, ucar.nc2.dt.point.OldUnidataPointObsDataset.class);

    registerFactory(FeatureType.RADIAL, NsslRadialAdapter.class);
    registerFactory(FeatureType.RADIAL, Dorade2RadialAdapter.class);
    registerFactory(FeatureType.RADIAL, Nexrad2RadialAdapter.class);
    registerFactory(FeatureType.RADIAL, NidsRadialAdapter.class);
    registerFactory(FeatureType.RADIAL, UF2RadialAdapter.class);
    registerFactory(FeatureType.RADIAL, CFRadialAdapter.class);

    registerFactory(FeatureType.TRAJECTORY, ucar.nc2.dt.trajectory.RafTrajectoryObsDataset.class);
    registerFactory(FeatureType.TRAJECTORY, ucar.nc2.dt.trajectory.UnidataTrajectoryObsDataset.class);
    registerFactory(FeatureType.TRAJECTORY, ucar.nc2.dt.trajectory.SimpleTrajectoryObsDataset.class);
    registerFactory(FeatureType.TRAJECTORY, ucar.nc2.dt.trajectory.Float10TrajectoryObsDataset.class);
    registerFactory(FeatureType.TRAJECTORY, ucar.nc2.dt.trajectory.ZebraClassTrajectoryObsDataset.class);
    registerFactory(FeatureType.TRAJECTORY, ucar.nc2.dt.trajectory.ARMTrajectoryObsDataset.class);

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
    if (!(TypedDatasetFactoryIF.class.isAssignableFrom( c)))
      throw new IllegalArgumentException("Class "+c.getName()+" must implement TypedDatasetFactoryIF");

    // fail fast - check newInstance works
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
      transformList.add( 0, new Factory( datatype, c, (TypedDatasetFactoryIF) instance));
    else
      transformList.add( new Factory( datatype, c, (TypedDatasetFactoryIF)instance));

  }

  static private class Factory {
    FeatureType datatype;
    Class c;
    TypedDatasetFactoryIF instance;

    Factory(FeatureType datatype, Class c, TypedDatasetFactoryIF instance) {
      this.datatype = datatype;
      this.c = c;
      this.instance = instance;
    }
  }

  /**
   * Open a dataset as a TypedDataset.
   *
   * @param datatype open this kind of Typed Dataset; may be null, which means search all factories.
   *   If datatype is not null, only return correct TypedDataset (eg PointObsDataset for DataType.POINT).
   * @param location URL or file location of the dataset
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of TypedDataset
   * @throws java.io.IOException on io error
   */
  static public TypedDataset open( FeatureType datatype, String location, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    NetcdfDataset ncd = NetcdfDataset.acquireDataset(durl, true, task);
    return open( datatype, ncd, task, errlog);
  }

  /**
   * Open a dataset as a TypedDataset.
   *
   * @param datatype open this kind of Typed Dataset; may be null, which means search all factories.
   *   If datatype is not null, only return correct TypedDataset (eg PointObsDataset for DataType.POINT).
   * @param ncd  the NetcdfDataset to wrap in a TypedDataset
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of TypedDataset, or null if cant find
   * @throws java.io.IOException on io error
   */
  static public TypedDataset open( FeatureType datatype, NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuilder errlog) throws IOException {

    // look for a Factory that claims this dataset
    Class useClass = null;
    for (Factory fac : transformList) {
      if ((datatype != null) && (datatype != fac.datatype)) continue;

      if (fac.instance.isMine(ncd)) {
        useClass = fac.c;
        break;
      }
    }

    // Factory not found
    if (null == useClass) {

      // POINT is also a STATION
      if (datatype == FeatureType.POINT) {
        return open( FeatureType.STATION, ncd, task, errlog);
      }

      // if explicitly requested, give em a GridDataset even if no Grids
      if (datatype == FeatureType.GRID) {
        return null; // new ucar.nc2.dt.grid.GridDataset( ncd);   LOOK have to copy old  GridDataset to archive
      }

      if (null == datatype) {
        // if no datatype was requested, give em a GridDataset only if some Grids are found.
        ucar.nc2.dt.grid.GridDataset gds = new ucar.nc2.dt.grid.GridDataset( ncd);
        if (gds.getGrids().size() > 0)
          return null; // gds;    LOOK have to copy old  GridDataset to archive
      }

      errlog.append("**Failed to find Datatype Factory for= ").append(ncd.getLocation()).append(" datatype= ").append(datatype).append("\n");
      return null;
    }

    // get a new instance of the Factory class, for thread safety
    TypedDatasetFactoryIF builder = null;
    try {
      builder = (TypedDatasetFactoryIF) useClass.newInstance();
    } catch (InstantiationException e) {
      errlog.append(e.getMessage()).append("\n");
    } catch (IllegalAccessException e) {
      errlog.append(e.getMessage()).append("\n");
    }
    if (null == builder) {
      errlog.append("**Error on TypedDatasetFactory object from class= ").append(useClass.getName()).append("\n");
      return null;
    }

    return builder.open( ncd, task, errlog);
  }

}
