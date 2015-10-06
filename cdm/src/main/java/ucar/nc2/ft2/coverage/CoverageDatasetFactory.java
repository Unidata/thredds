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
import ucar.nc2.ft2.coverage.adapter.DtCoverageAdapter;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.ft2.coverage.remote.CdmrFeatureDataset;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Formatter;

/**
 * factory for CoverageDataset
 *  1) Remote CdmrFeatureDataset
 *  2) GRIB collections
 *  3) DtCoverageDataset (forked from ucar.nc2.dt.grid), the cdm IOSP / CoordSys stack
 *
 *  Would like to add a seperate implementation for FMRC collections
 *
 * @author caron
 * @since 5/26/2015
 */
public class CoverageDatasetFactory {
  static private final Logger logger = LoggerFactory.getLogger(CoverageDatasetFactory.class);

  static public FeatureDatasetCoverage open(String endpoint) throws IOException {

    // remote CdmrFeatureDataset
    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME)) {

      endpoint = endpoint.substring(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME.length());
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      CoverageCollection covColl = reader.openCoverageDataset();
      return new FeatureDatasetCoverage(endpoint, covColl, covColl);

    } else if (endpoint.startsWith("http:")) {
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      if (reader.isCmrfEndpoint()) {
        CoverageCollection covColl = reader.openCoverageDataset();
        return new FeatureDatasetCoverage(endpoint, covColl, covColl);
      } // otherwise let it fall through

    } else if (endpoint.startsWith("file:")) {
      endpoint = endpoint.substring("file:".length());
    }

    // check if its GRIB collection
    try {
      Class<?> c = CoverageDatasetFactory.class.getClassLoader().loadClass("ucar.nc2.grib.coverage.GribCoverageDataset");
      Method method = c.getMethod("open", String.class);
      FeatureDatasetCoverage result = (FeatureDatasetCoverage) method.invoke(null, endpoint);
      if (result != null) return result;
    } catch (ClassNotFoundException e) {
      // ok, GRIB module not loaded, fall through
    } catch (Throwable e) {
      logger.error("Error GribCoverageDataset: ", e);
      // fall through
    }

    // adapt a DtCoverageDataset (forked from ucar.nc2.dt.GridDataset), eg a local file
    DtCoverageDataset gds = DtCoverageDataset.open(endpoint);
    if (gds.getGrids().size() > 0)
      return DtCoverageAdapter.factory(gds, new Formatter());

    return null;
  }

}

/*

  static public List<CoverageDataset> openCoverage(String endpoint) throws IOException {

    // remote CdmrFeatureDataset
    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME)) {

      endpoint = endpoint.substring(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME.length());
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      return Lists.newArrayList(reader.openCoverageDataset());

    } else if (endpoint.startsWith("http:")) {
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      if (reader.isCmrfEndpoint()) {
        return Lists.newArrayList(reader.openCoverageDataset());
      } // otherwise let it fall through to DtCoverageDataset.open(endpoint)

    } else if (endpoint.startsWith("file:")) {
      endpoint = endpoint.substring("file:".length());
    }

    boolean hasGribLoaded =

    // see if its grib
    if (endpoint.endsWith(".ncx3"))  {

    }

     // see if its grib 2
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDataset.acquireDataset(null, endpoint, null, -1, null, null);
    if (ds.getFileTypeId().contains("GRIB")) {
    }

    // adapt a DtCoverageDataset (forked from ucar.nc2.dt.GridDataset), eg a local file
    DtCoverageDataset gds = new DtCoverageDataset(ds);
    if (gds.getGrids().size() > 0)
      return Lists.newArrayList(DtCoverageAdapter.factory(gds));

    return null;
  }
 */
