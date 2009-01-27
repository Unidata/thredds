/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
