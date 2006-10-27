// $Id: $
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

package ucar.nc2.dt;

import java.util.ArrayList;
import java.io.IOException;

import ucar.nc2.dataset.NetcdfDataset;
import thredds.catalog.DataType;

/**
 * Class Description.
 *
 * @author caron
 * @version $Revision$ $Date$
 */
public class TypedDatasetFactory {

  static private ArrayList transformList = new ArrayList();
  static private boolean userMode = false;

  // search in the order added
  static {
    registerFactory(DataType.POINT, ucar.nc2.dt.point.UnidataPointObsDataset.class);
    registerFactory(DataType.STATION, ucar.nc2.dt.point.UnidataStationObsDataset.class);
    registerFactory(DataType.POINT, ucar.nc2.dt.point.DapperDataset.class);
    registerFactory(DataType.STATION, ucar.nc2.dt.point.SequenceObsDataset.class);
    registerFactory(DataType.STATION, ucar.nc2.dt.point.UnidataStationObsDataset2.class);
    registerFactory(DataType.STATION, ucar.nc2.dt.point.NdbcDataset.class);
    registerFactory(DataType.STATION, ucar.nc2.dt.point.MadisStationObsDataset.class);
    registerFactory(DataType.STATION, ucar.nc2.dt.point.OldUnidataStationObsDataset.class);
    registerFactory(DataType.POINT, ucar.nc2.dt.point.OldUnidataPointObsDataset.class);

    registerFactory(DataType.RADIAL, ucar.nc2.dt.radial.Dorade2Dataset.class);
    registerFactory(DataType.RADIAL, ucar.nc2.dt.radial.LevelII2Dataset.class);
    registerFactory(DataType.RADIAL, ucar.nc2.dt.radial.Nids2Dataset.class);

    registerFactory(DataType.TRAJECTORY, ucar.nc2.dt.trajectory.RafTrajectoryObsDataset.class);
    registerFactory(DataType.TRAJECTORY, ucar.nc2.dt.trajectory.SimpleTrajectoryObsDataset.class);
    registerFactory(DataType.TRAJECTORY, ucar.nc2.dt.trajectory.Float10TrajectoryObsDataset.class);
    registerFactory(DataType.TRAJECTORY, ucar.nc2.dt.trajectory.ZebraClassTrajectoryObsDataset.class);
    registerFactory(DataType.TRAJECTORY, ucar.nc2.dt.trajectory.ARMTrajectoryObsDataset.class);

    // further calls to registerFactory are by the user
    userMode = true;
  }


   /**
    * Register a class that implements a Coordinate Transform.
    * @param className name of class that implements CoordTransBuilderIF.
    */
   static public void registerFactory( thredds.catalog.DataType datatype, String className) throws ClassNotFoundException {
     Class c = Class.forName( className);
     registerFactory( datatype, c);
   }

   /**
    * Register a class that implements a Coordinate Transform.
    * @param datatype kind of data
    * @param c class that implements CoordTransBuilderIF.
    */
  static public void registerFactory( thredds.catalog.DataType datatype, Class c) {
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
    DataType datatype;
    Class c;
    TypedDatasetFactoryIF instance;

    Factory(DataType datatype, Class c, TypedDatasetFactoryIF instance) {
      this.datatype = datatype;
      this.c = c;
      this.instance = instance;
    }
  }

  /**
   * Open a dataset as a TypedDataset.
   * @param datatype may be null, which means search all factories
   * @param location URL or file location of the dataset
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of TypedDataset
   */
  static public TypedDataset open( thredds.catalog.DataType datatype, String location, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    NetcdfDataset ncd = NetcdfDataset.acquireDataset( location, task);
    return open( datatype, ncd, task, errlog);
  }

  /**
   * Open a dataset as a TypedDataset.
   *
   * @param datatype, may be null, which means search all factories
   * @param ncd  the NetcdfDataset to wrap in a TypedDataset
   * @param task user may cancel
   * @param errlog place errors here, may not be null
   * @return a subclass of TypedDataset
   */
  static public TypedDataset open( thredds.catalog.DataType datatype, NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {

    // look for a Factory that claims this dataset
    Class useClass = null;
    for (int i = 0; i < transformList.size(); i++) {
      Factory fac = (Factory) transformList.get(i);
      if ((datatype != null) && (datatype != fac.datatype)) continue;

      if (fac.instance.isMine( ncd)) {
        useClass = fac.c;
        break;
      }
    }

    // Factory not found
    if (null == useClass) {

      // if explicitly requested, give em a GridDataset even if no Grids
      if (datatype == DataType.GRID) {
        return new ucar.nc2.dt.grid.GridDataset( ncd);
      }

      if (null == datatype) {
        // if no datatype was requested, give em a GridDataset only if some Grids are found.
        ucar.nc2.dt.grid.GridDataset gds = new ucar.nc2.dt.grid.GridDataset( ncd);
        if (gds.getGrids().size() > 0)
          return gds;
      }

      errlog.append("**Failed to find Datatype Factory for= "+ncd.getLocation()+" datatype= "+datatype+"\n");
      return null;
    }

    // get a new instance of the Factory class, for thread safety
    TypedDatasetFactoryIF builder = null;
    try {
      builder = (TypedDatasetFactoryIF) useClass.newInstance();
    } catch (InstantiationException e) {
      errlog.append(e.getMessage()+"\n");
    } catch (IllegalAccessException e) {
      errlog.append(e.getMessage()+"\n");
    }
    if (null == builder) {
      errlog.append("**Error on TypedDatasetFactory object from class= "+useClass.getName()+"\n");
      return null;
    }

    return builder.open( ncd, task, errlog);
  }

}
