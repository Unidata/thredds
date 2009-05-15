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

package ucar.nc2.ft.radial;

import ucar.nc2.ft.FeatureDatasetFactory;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.TypedDatasetFactoryIF;
import ucar.nc2.dt.TypedDataset;

import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Adapt existing Radial Datasets implementing TypedDatasetFactoryIF, for FeatureDatasetFactory.
 *
 * @author caron
 * @since Feb 17, 2009
 */
public class RadialDatasetStandardFactory implements FeatureDatasetFactory {

  private static List<TypedDatasetFactoryIF> factories = new ArrayList<TypedDatasetFactoryIF>(10);

  static {
    registerFactory(ucar.nc2.dt.radial.Netcdf2Dataset.class);
    registerFactory(ucar.nc2.dt.radial.Dorade2Dataset.class);
    registerFactory(ucar.nc2.dt.radial.LevelII2Dataset.class);
    registerFactory(ucar.nc2.dt.radial.Nids2Dataset.class);
    registerFactory(ucar.nc2.dt.radial.UF2Dataset.class);
  }

  static void registerFactory(Class c) {
    if (!(TypedDatasetFactoryIF.class.isAssignableFrom(c)))
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

    factories.add((TypedDatasetFactoryIF) instance);
  }

  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {

    if ((wantFeatureType != FeatureType.RADIAL) && (wantFeatureType != FeatureType.NONE) && (wantFeatureType != FeatureType.ANY))
      return null;

    for (TypedDatasetFactoryIF fac : factories) {
      if (fac.isMine(ds))
        return fac;
    }

    return null;

    /* boolean hasAzimuth = false;
    boolean hasDistance = false;
    boolean hasElev = false;
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      if (axis.getAxisType() == AxisType.RadialAzimuth)
        hasAzimuth = true;
      if (axis.getAxisType() == AxisType.RadialDistance)
        hasDistance = true;
      if (axis.getAxisType() == AxisType.RadialElevation)
        hasElev = true;
    }

    // minimum we need
    if (!(hasAzimuth && hasDistance && hasElev)) {
      errlog.format("RadialDataset must have azimuth,elevation,distance coordinates");
      return null;
    }

    //  ok
    return new Object(); */
  }


  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, Object analysis, CancelTask task, Formatter errlog) throws IOException {
    TypedDatasetFactoryIF fac = (TypedDatasetFactoryIF) analysis;
    StringBuilder sbuff = new StringBuilder();
    TypedDataset result = fac.open(ncd, task, sbuff);
    errlog.format("%s", sbuff);
    return (FeatureDataset) result;
  }

  public FeatureType[] getFeatureType() {
    return new FeatureType[]{FeatureType.RADIAL};
  }
}

