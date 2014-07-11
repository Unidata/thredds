/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.Dimension;
import ucar.nc2.Structure;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.TableConfig;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 6/26/2014
 */
public class CFpointObsExt extends CFpointObs {

  @Override
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String conv = ds.findAttValueIgnoreCase(null, CDM.CONVENTIONS, null);
    return conv != null && (conv.equalsIgnoreCase(CDM.CF_EXTENDED));
  }

  @Override
  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) throws IOException {
    EncodingInfo info = new EncodingInfo();

    // figure out the actual feature type of the dataset
    CF.FeatureType ftype = CF.FeatureType.getFeatureTypeFromGlobalAttribute(ds);
    if (ftype == null) ftype = CF.FeatureType.point;

    // make sure lat, lon, time coordinates exist
    if (!checkCoordinates(ds, info, errlog)) return null; // fail fast

    switch (ftype) {
      case timeSeries:
        return getStationConfig(ds, info, errlog);
      case profile:
        return getProfileConfig(ds, info, errlog);
      case trajectory:
        return getTrajectoryConfig(ds, info, errlog);
      case timeSeriesProfile:
        return getTimeSeriesProfileConfig(ds, info, errlog);
      case trajectoryProfile:
        return getSectionConfig(ds, info, errlog);
    }

    return null;
  }

  @Override
  protected boolean identifyEncodingStation(NetcdfDataset ds, EncodingInfo info, CF.FeatureType ftype, Formatter errlog) {
    Structure obs =  info.time.getParentStructure();
    if (obs.getRank() == 0) {
      errlog.format("CFpointObs: must have a non-scalar Time coordinate%n");
      return false;
    }
    Dimension obsDim = obs.getDimension(0);

    Structure station =  info.lat.getParentStructure();
    if (station.getRank() == 0) { // could be scalar
      info.set(Encoding.single, null, obsDim);
    }
    Dimension stnDim = station.getDimension(0);

    // the raggeds
    if (identifyRaggeds(ds, info, stnDim, obsDim, errlog))
      return true;


    errlog.format("CFpointObs: %s Must have Lat/Lon coordinates of rank 0 or 1%n", ftype);
    return false;
  }



}
