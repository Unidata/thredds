package ucar.nc2.thredds.server;

import ucar.nc2.util.IO;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Robb
 * Date: Feb 14, 2010
 * Time: 4:22:20 PM
 */
public class TestRadarServer {
  //static String server = "http://motherlode.ucar.edu:8081";
  static String server = "http://localhost:8080/";
  static String pathStart = "/thredds/radarServer/";
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
    String outputDir;
    File where = new File("C:/data/RadarServer/");
    if (where.exists()) {
      outputDir = where.getPath();
    } else {
      outputDir = "/local/robb/data/radar/RadarServer/";
    }
    System.out.println("Copy inventory files to " + outputDir);
    for (int i = 0; i < requests.length; i++) {
      File file = new File(outputDir + i + ".xml");
      try {
        IO.readURLtoFileWithExceptions(server + pathStart + requests[i], file);
        System.out.println("Copied " + requests[i] + " to " + file.getName());
      } catch (IOException ioe) {
        System.out.println("Failed " + requests[i]);
      }
    }
  }

}
