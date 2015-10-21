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
package ucar.nc2.ft2.coverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft2.coverage.adapter.DtCoverageAdapter;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.util.Optional;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Formatter;

/**
 * factory for CoverageDataset
 * 1) Remote CdmrFeatureDataset: cdmremote:url
 * 2) GRIB collections: must be a grib file, or grib index file
 * 3) DtCoverageDataset (forked from ucar.nc2.dt.grid), the cdm IOSP / CoordSys stack
 * <p>
 * Would like to add a separate implementation for FMRC collections
 *
 * @author caron
 * @since 5/26/2015
 */
public class CoverageDatasetFactory {
  static public final String NOT_GRIB_FILE = "Not a GRIB file";
  static public final String NO_GRIB_CLASS = "GRIB module not loaded";

  /**
   * @param endpoint cdmrFeature:url, local GRIB data or index file, or NetcdfDataset location
   <pre>
   ucar.nc2.util.Optional<FeatureDatasetCoverage> opt = CoverageDatasetFactory.openCoverageDataset(location);
   if (!opt.isPresent()) {
     JOptionPane.showMessageDialog(null, opt.getErrorMessage());
     return false;
   }
   covDatasetCollection = opt.get();
   </pre>
   */
  static public Optional<FeatureDatasetCoverage> openCoverageDataset(String endpoint) throws IOException {

    // remote cdmrFeature datasets
    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME)) {
      Optional<FeatureDataset> opt = ucar.nc2.ft.remote.CdmrFeatureDataset.factory(FeatureType.COVERAGE, endpoint);
      return opt.isPresent() ? Optional.of( (FeatureDatasetCoverage) opt.get()) : Optional.empty(opt.getErrorMessage());
    }

    DatasetUrl durl = DatasetUrl.findDatasetUrl(endpoint);
    if (durl.serviceType == null) { // skip GRIB check for anything not a plain ole file
      // check if its GRIB collection
      Optional<FeatureDatasetCoverage> opt = openGrib(endpoint);
      if (opt.isPresent()) return opt;
      if (!opt.getErrorMessage().startsWith(CoverageDatasetFactory.NOT_GRIB_FILE) &&
              !opt.getErrorMessage().startsWith(CoverageDatasetFactory.NO_GRIB_CLASS)) {
        return opt;  // its a GRIB file with an error
      }
    }

    // adapt a DtCoverageDataset (forked from ucar.nc2.dt.GridDataset), eg a local file
    DtCoverageDataset gds = DtCoverageDataset.open(endpoint);
    if (gds.getGrids().size() > 0) {
      Formatter errlog = new Formatter();
      FeatureDatasetCoverage result = DtCoverageAdapter.factory(gds, errlog);
      if (result != null)
        return Optional.of(result);
      else
        return Optional.empty(errlog.toString());
    }

    return Optional.empty("Could not open as Coverage: " + endpoint);
  }

  /**
   *
   * @param endpoint cdmrFeature:url, local GRIB data or index file, or NetcdfDataset location
   * @return FeatureDatasetCoverage or null on failure. use openCoverageDataset to get error message
   * @throws IOException
   */
  static public FeatureDatasetCoverage open(String endpoint) throws IOException {
    Optional<FeatureDatasetCoverage> opt = openCoverageDataset(endpoint);
    return opt.isPresent() ? opt.get() : null;
  }

  /**
   * @param endpoint local GRIB data or index file
   * @return FeatureDatasetCoverage or null on failure.
   * @throws IOException
   */
  static public Optional<FeatureDatasetCoverage> openGrib(String endpoint) throws IOException {
    try {
      Class<?> c = CoverageDatasetFactory.class.getClassLoader().loadClass("ucar.nc2.grib.coverage.GribCoverageDataset");
      Method method = c.getMethod("open", String.class);
      return (Optional<FeatureDatasetCoverage>) method.invoke(null, endpoint);

    } catch (IllegalAccessException | IllegalArgumentException | ClassNotFoundException | NoSuchMethodException e) {
      //e.printStackTrace();
      return Optional.empty(NO_GRIB_CLASS);

    } catch (InvocationTargetException e) {
      //e.printStackTrace();
      return Optional.empty(e.getCause().getMessage());
    }

  }

}
