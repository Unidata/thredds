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

import junit.framework.TestCase;
import ucar.ma2.*;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Class Description
 *
 * @author caron
 * @since May 13, 2009
 */
public class TestMisc extends TestCase {


    public void testCompareLongs() {

        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date[] dateList = new Date[] {
                sdf.parse("2002-01-01"),
                sdf.parse("2002-01-02"),
                sdf.parse("2002-01-03"),
                sdf.parse("2002-01-04"),
                sdf.parse("2002-02-05"),
                sdf.parse("2002-03-06")
            };

            Arrays.sort(dateList, new DateComparator1());

            System.out.println("sort error: " + Arrays.toString(dateList));

            Arrays.sort(dateList, new DateComparator2());

            System.out.println("sort fix:   "+Arrays.toString(dateList));


        } catch (Exception e) {
            e.printStackTrace();
        }

      Long.toString(0);

    }


    // reverse sort - latest come first
    private class DateComparator1 implements Comparator<Date> {
        public int compare(Date f1, Date f2) {
            System.out.print(f2+"-"+f1+" ="+f2.getTime()+"-"+f1.getTime()+" =  int: "+(int) (f2.getTime() - f1.getTime()));
            System.out.println("  long: "+(f2.getTime() - f1.getTime()));

            return (int) (f2.getTime() - f1.getTime());
        }
    }

    // reverse sort - latest come first
    private class DateComparator2 implements Comparator<Date> {
        public int compare(Date f1, Date f2) {
          // return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));

          if (f2.getTime() == f1.getTime()) return 0;
          return  (f2.getTime() > f1.getTime()) ? 1 : -1;
        }
    }



/*  The 3D coordinate array does not return correct shape and values. Just
running this
simple code to get z values..

uri=http://coast-enviro.er.usgs.gov/models/share/erie_test.ncml;
var='temp';

z is of shape 20x2x87, it should be 20x87x193.
*/

  public void testErie() throws IOException, InvalidRangeException {
    String uri = "C:\\data\\work\\signell/erie_test.ncml";
    String var = "temp";

    GridDataset ds = GridDataset.open(uri);
    GeoGrid grid = ds.findGridByName(var);
    Section s = new Section(grid.getShape());
    System.out.printf("var = %s %n", s);

    GridCoordSystem gcs = grid.getCoordinateSystem();
    VerticalTransform vt = gcs.getVerticalTransform();
    ArrayDouble.D3 z = vt.getCoordinateArray(0);
    Section sv = new Section(z.getShape());
    System.out.printf("3dcoord = %s %n", sv);

    s = s.removeRange(0);
    assert s.equals(sv);
  }


  public void testFileClosing() throws IOException, InvalidRangeException {

    File inDir = new File("C:/data/work/ansari/");
    RandomAccessFile.setDebugLeaks(true);
    for (File file : inDir.listFiles()) {

      NetcdfFile ncIn = null;
      try {

        System.out.println("PROCESSING: " + file);
        ncIn = NetcdfFile.open(file.toURI().toURL().toString());
        //ucar.nc2.FileWriter.writeToFile(ncIn, new File(outDir.toString()+File.separator+file.getName()+"-out.nc").toString());

      } catch (Exception e) {
        System.err.println("CAUGHT EXCEPTION: " + e);
      } finally {
        try {
          ncIn.close();
        } catch (Exception e) {

        }
      }


    }

    System.out.printf("open files:%n");
    for (String raf : RandomAccessFile.getOpenFiles())
      System.out.printf(" %s %n", raf);

  }


  // Xiaoshen.Li@noaa.gov
    public void utestModifyNCfile() throws IOException, InvalidRangeException {
        final String inputFileName = "C:/tmp/input.nc";

        final String outputFileName = "C:/tmp/output.nc";

        final NetcdfFileWriteable writableFile = NetcdfFileWriteable.openExisting(inputFileName);

        writableFile.setRedefineMode(true);

        final Variable ffgVariable = writableFile.findVariable("FFG");
        final Array ffgVarArray = ffgVariable.read();
        final Index ffgIndex = ffgVarArray.getIndex();

        writableFile.setName(outputFileName);
//setName() is deprecated. If commented out this method, "input.nc" will be overwritten
        writableFile.create();

        //re-set variable FFG values
        ffgVarArray.setDouble(ffgIndex.set(0), 10.1);
        ffgVarArray.setDouble(ffgIndex.set(1), 10.2);
        ffgVarArray.setDouble(ffgIndex.set(2), 10.3);
        ffgVarArray.setDouble(ffgIndex.set(3), 10.4);

        // writableFile.write("FFG", ffgVarArray);

        writableFile.flush();

        writableFile.close();

    }

  public static void main(String[] args) {
    String s1 = "CoastWatch/MODSCW/closest_chlora/Mean/CB05/P2009190";
    String s2 = "CoastWatch/MODSCW/closest_chlora/Mean/SE05/P2009190";
    System.out.printf("s1 = %d s2 = %d%n", s1.hashCode(), s2.hashCode());

  }

}
