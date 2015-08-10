/* Copyright */
package ucar.nc2.ft2.coverage;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.ft2.coverage.adapter.DtCoverageAdapter;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.ft2.coverage.remote.CdmrFeatureDataset;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * factory for CoverageDataset
 *
 * @author caron
 * @since 5/26/2015
 */
public class CoverageDatasetFactory {
  static private final Logger logger = LoggerFactory.getLogger(CoverageDatasetFactory.class);

  static public CoverageCollection open(String endpoint) throws IOException {

    // remote CdmrFeatureDataset
    if (endpoint.startsWith(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME)) {

      endpoint = endpoint.substring(ucar.nc2.ft.remote.CdmrFeatureDataset.SCHEME.length());
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      return new CoverageCollection(reader.openCoverageDataset(), reader.openCoverageDataset());

    } else if (endpoint.startsWith("http:")) {
      CdmrFeatureDataset reader = new CdmrFeatureDataset(endpoint);
      if (reader.isCmrfEndpoint()) {
        return new CoverageCollection(reader.openCoverageDataset(), reader.openCoverageDataset());
      } // otherwise let it fall through

    } else if (endpoint.startsWith("file:")) {
      endpoint = endpoint.substring("file:".length());
    }

    // check if its GRIB collection
    try {
      Class<?> c = CoverageDatasetFactory.class.getClassLoader().loadClass("ucar.nc2.grib.coverage.GribCoverageDataset");
      Method method = c.getMethod("open", String.class);
      CoverageCollection result = (CoverageCollection) method.invoke(null, endpoint);
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
      return DtCoverageAdapter.factory(gds);

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
