/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.radial;

import ucar.nc2.dt.radial.*;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;

import java.util.ArrayList;
import java.util.Formatter;
import java.io.IOException;
import java.util.List;

/**
 * Adapt existing Radial Datasets implementing FeatureDatasetFactory.
 *
 * @author caron
 * @since Feb 17, 2009
 */
public class RadialDatasetStandardFactory implements FeatureDatasetFactory {

  private static List<FeatureDatasetFactory> factories = new ArrayList<>(10);

  static {
    registerFactory(NsslRadialAdapter.class);
    registerFactory(Dorade2RadialAdapter.class);
    registerFactory(Nexrad2RadialAdapter.class);
    registerFactory(NidsRadialAdapter.class);
    registerFactory(UF2RadialAdapter.class);
    registerFactory(CFRadialAdapter.class);
  }

  static void registerFactory(Class c) {
    if (!(FeatureDatasetFactory.class.isAssignableFrom(c)))
      throw new IllegalArgumentException("Class " + c.getName() + " must implement TypedDatasetFactoryIF");

    // fail fast - get Instance
    Object instance;
    try {
      instance = c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("FeatureDatasetFactoryManager Class " + c.getName() + " cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("FeatureDatasetFactoryManager Class " + c.getName() + " is not accessible");
    }

    factories.add((FeatureDatasetFactory) instance);
  }

  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {

    if ((wantFeatureType != FeatureType.RADIAL) && (wantFeatureType != FeatureType.ANY))
      return null;

    for (FeatureDatasetFactory fac : factories) {
      Object anal = fac.isMine(FeatureType.RADIAL, ds, errlog);
      if (anal != null)
        return anal;
    }

    return null;
  }


  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, Object analysis, CancelTask task, Formatter errlog) throws IOException {
    FeatureDatasetFactory fac = (FeatureDatasetFactory) analysis;
    return fac.open(FeatureType.RADIAL, ncd, null, task, errlog);
  }

  public FeatureType[] getFeatureTypes() {
    return new FeatureType[]{FeatureType.RADIAL};
  }
}

