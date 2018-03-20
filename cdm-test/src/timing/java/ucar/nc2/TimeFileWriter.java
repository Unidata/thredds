/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.netcdf3.N3channelWriter;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Class Description.
 *
 * @author caron
 */
public class TimeFileWriter {

  public static void timeFileWriter( String location, String fileOut) throws IOException {
    long startTime = System.currentTimeMillis();

    try (NetcdfFile ds = NetcdfDataset.openFile( location, null)) {
      FileWriter2 writer = new FileWriter2(ds, fileOut, NetcdfFileWriter.Version.netcdf3, null);
      NetcdfFile ncfileOut = writer.write();
      ncfileOut.close();
    }

   // NetcdfFile ncfileOut = ucar.nc2.FileWriter.writeToFile(ds, fileOut, false, 0);
    long diff = System.currentTimeMillis() - startTime;   // msecs

    File result = new File( fileOut);
    double size = .001 * result.length(); // km
    double rate = size / diff; // Mb/sec
    System.out.println("FileWriter.writeToFile "+location+"  took "+diff+ " msecs rate= "+rate+" Mb/sec");
  }

  public static void timeChannelWriter( String location, String fileOut) throws IOException, InvalidRangeException {
    long startTime = System.currentTimeMillis();

    NetcdfFile ncfile = NetcdfDataset.openFile( location, null);

    FileOutputStream fout = new FileOutputStream(fileOut);
    N3channelWriter.writeToChannel(ncfile, fout.getChannel());
    long diff = System.currentTimeMillis() - startTime;   // msecs

    File result = new File( fileOut);
    double size = .001 * result.length(); // km
    double rate = size / diff; // Mb/sec
    System.out.println("N3channelWriter.writeToFile "+location+"  took "+diff+ " msecs rate= "+rate+" Mb/sec");
  }

  public static void main(String args[]) throws IOException, InvalidRangeException {
   // timeChannelWriter("dods://motherlode.ucar.edu:8080/thredds/dodsC/modelsNc/NCEP/RUC/CONUS_80km/RUC_CONUS_80km_20080215_2300.nc", "C:/temp/testOut14.nc");
    //timeFileWriter("dods://motherlode.ucar.edu:8080/thredds/dodsC/modelsNc/NCEP/RUC/CONUS_80km/RUC_CONUS_80km_20080215_2200.nc", "C:/temp/testOut25.nc");
    //timeFileWriter("dods://motherlode.ucar.edu:8080/thredds/dodsC/modelsNc/NCEP/GFS/Global_5x2p5deg/GFS_Global_5x2p5deg_20080215_1800.nc", "C:/temp/testOut35.nc");
    //timeFileWriter("dods://motherlode.ucar.edu:8080/thredds/dodsC/modelsNc/NCEP/GFS/CONUS_80km/GFS_CONUS_80km_20080215_1800.nc", "C:/temp/testOut45.nc");

    //timeFileWriter("dods://motherlode.ucar.edu:8080/thredds/dodsC/fmrc/NCEP/GFS/Alaska_191km/files/GFS_Alaska_191km_20080215_0000.grib1", "C:/temp/testOut114.nc");
    timeFileWriter("dods://motherlode.ucar.edu:8081/thredds/dodsC/fmrc/NCEP/NAM/CONUS_12km/files/NAM_CONUS_12km_20080215_0600.grib2", "C:/temp/testOut124.nc");
  }

}
