/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.Misc;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 2/24/2016.
 */
public class TestRdaReading {

  // time2D coordinate, not orthogonal, but times are unique
  // GribCollectionImmutable assumes time2D -> orthogonal
  // doesnt actually work since we only have the gbx9
  @Test
  public void testNonOrthMRUTC() throws IOException, InvalidRangeException {
    String endpoint = "D:/work/rdavm/ds277.6/monthly/ds277.6.ncx4";
    String ccName = "ds277.6#MRUTC-LatLon_418X360-4p8338S-179p5000W";
    String covName = "Salinity_depth_below_sea_Average";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, fdc);
      CoverageCollection cc = fdc.findCoverageDataset(ccName);
      Assert.assertNotNull(ccName, cc);
      Coverage cov = cc.findCoverage(covName);
      Assert.assertNotNull(covName, cov);

      SubsetParams subset = new SubsetParams().setTimePresent();
      GeoReferencedArray geo = cov.readData(subset);
      Array data = geo.getData();
      System.out.printf(" read data from %s shape = %s%n", cov.getName(), Misc.showInts(data.getShape()));
    }
  }

  // /thredds/cdmrfeature/grid/aggregations/g/ds084.3/1/TwoD?req=data&var=v-component_of_wind_potential_vorticity_surface&timePresent=true
  @Test
  public void testNegDataSize() throws IOException, InvalidRangeException {
    String endpoint = "D:/work/rdavm/ds084.3/ds084.3.ncx4";
    String covName = "v-component_of_wind_potential_vorticity_surface";
    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(endpoint)) {
      Assert.assertNotNull(endpoint, fdc);
      CoverageCollection cc = fdc.findCoverageDataset(FeatureType.FMRC);
      Assert.assertNotNull(FeatureType.FMRC.toString(), cc);
      Coverage cov = cc.findCoverage(covName);
      Assert.assertNotNull(covName, cov);

      SubsetParams subset = new SubsetParams().setTimePresent();
      GeoReferencedArray geo = cov.readData(subset);
      Array data = geo.getData();
      System.out.printf(" read data from %s shape = %s%n", cov.getName(), Misc.showInts(data.getShape()));
    }
  }
}
