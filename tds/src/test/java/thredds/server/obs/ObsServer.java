package thredds.server.obs;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * Point Obs Server
 */
public class ObsServer {
  private String template = "C:/data/metars/METAR_template.ncml";

  void makeDailyFile(String location) throws IOException {

    NetcdfDataset ncd = NetcdfDataset.openDataset(template, false, null);
    NetcdfFile ncdnew = ucar.nc2.FileWriter.writeToFile(ncd, location);
    ncd.close();
    ncdnew.close();

  }

  static public void main( String args[]) throws IOException {
    for (int i=0; i<args.length; i++) {
    }

    ObsServer server = new ObsServer();
    server.makeDailyFile("C:/data/metars/METAR_template.nc");

  }
}
