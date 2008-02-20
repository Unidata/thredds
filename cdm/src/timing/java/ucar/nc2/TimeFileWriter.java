/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2;

import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.netcdf3.N3channelWriter;
import ucar.ma2.Array;
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

    NetcdfFile ds = NetcdfDataset.openFile( location, null);
    NetcdfFile ncfileOut = ucar.nc2.FileWriter.writeToFile(ds, fileOut, false, 0);
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
