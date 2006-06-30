package ucar.nc2.dt.radial;

import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dt.RadialDataset;
import ucar.nc2.dataset.*;

/**
 * Factory to create RadialDatasets
 */
public class RadialDatasetFactory {

  private StringBuffer log;
  public String getErrorMessages() { return log == null ? "" : log.toString(); }

  public RadialDataset open( String location, ucar.nc2.util.CancelTask cancelTask) throws java.io.IOException {
    log = new StringBuffer();
    NetcdfDataset ncd = NetcdfDatasetCache.acquire( location, cancelTask);
    return open( ncd);
  }

  public RadialDataset open( NetcdfDataset ncd) {
    log = new StringBuffer();

    String convention = ncd.findAttValueIgnoreCase(null, "Conventions", null);
    if ((null != convention) && convention.equals("_Coordinates")) {
      String format = ncd.findAttValueIgnoreCase(null, "format", null);
      if (format.equals("AR2V0001"))
        return new Nexrad2Dataset( ncd);
    }

    return null;
  }


}
