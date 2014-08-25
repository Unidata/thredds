package ucar.nc2.thredds.server;

import ucar.nc2.util.IO;

import java.io.File;
import java.io.IOException;

public class TestRadarServer {
  // static String server = "http://localhost:8081/";
  static String server = "http://thredds-dev.unidata.ucar.edu/";
  static String pathStart = "thredds/radarServer/";
  static String[] requests = {
      "catalog.xml",
      "nexrad/level2/catalog.xml",
      "nexrad/level3/catalog.xml",
      "nexrad/level2/stations.xml",
      "nexrad/level3/stations.xml",
      "terminal/level2/stations.xml",
      "terminal/level2/stations.xml",
      "nexrad/level2/IDD?stn=KFTG",
      "nexrad/level3/IDD?var=N0R&stn=FTG",
      "terminal/level3/IDD?var=TR0&stn=MIA",
  };


  public static void main(String args[]) {
    File where = new File("C:/temp/RadarServer/");
    where.mkdirs();
    System.out.println("Copy inventory files to " + where);
    for (int i = 0; i < requests.length; i++) {
      File file = new File(where, i + ".xml");
      String fullPath = server + pathStart + requests[i];
      try {
        IO.readURLtoFileWithExceptions(fullPath, file);
        System.out.printf("Copied %s to %s%n", fullPath, file.getName());
      } catch (IOException ioe) {
        System.out.printf("Failed %s err=%s%n", fullPath, ioe.getMessage());
      }
    }
  }

}
