/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.EscapeStrings;
import ucar.ma2.*;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * misc testing
 *
 * @author caron
 * @since May 13, 2009
 */
public class TestMisc extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void testBackslashTokens() {
    testBackslashTokens("group/name.member.mom");
    testBackslashTokens("var\\.name.member.mom");
    testBackslashTokens("var\\.name.member.mom\\");
    testBackslashTokens("var\\.name.member.mom.");
    testBackslashTokens(".var\\.name.member.mom.");
    testBackslashTokens("...mom.");
  }

  private void testBackslashTokens(String escapedName) {
    System.out.printf("%s%n", escapedName);
    List<String> result = EscapeStrings.tokenizeEscapedName(escapedName);
    for (String r : result)
      System.out.printf("   %s%n", r);
    System.out.printf("%n");
  }

  public void testCompareLongs() {

    try {

      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Date[] dateList = new Date[]{
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

      System.out.println("sort fix:   " + Arrays.toString(dateList));


    } catch (Exception e) {
      e.printStackTrace();
    }

    Long.toString(0);

  }


  // reverse sort - latest come first
  private class DateComparator1 implements Comparator<Date> {
    public int compare(Date f1, Date f2) {
      System.out.print(f2 + "-" + f1 + " =" + f2.getTime() + "-" + f1.getTime() + " =  int: " + (int) (f2.getTime() - f1.getTime()));
      System.out.println("  long: " + (f2.getTime() - f1.getTime()));

      return (int) (f2.getTime() - f1.getTime());
    }
  }

  // reverse sort - latest come first
  private class DateComparator2 implements Comparator<Date> {
    public int compare(Date f1, Date f2) {
      // return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));

      if (f2.getTime() == f1.getTime()) return 0;
      return (f2.getTime() > f1.getTime()) ? 1 : -1;
    }
  }


  public Array concatArray(Array array1, Array array2) {
    float[] data1 = (float[]) array1.copyTo1DJavaArray();
    float[] data2 = (float[]) array2.copyTo1DJavaArray();
    float[] result = new float[data1.length + data2.length];

    System.arraycopy(data1, 0, result, 0, data1.length);
    System.arraycopy(data2, 0, result, data1.length, data2.length);

    // now put it back into an ucar.ma2.Array
    int[] resultShape = new int[] {result.length};
    return Array.factory(DataType.FLOAT, resultShape, result);
  }

}
