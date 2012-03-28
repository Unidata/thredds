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
package ucar.nc2.dataset;

import junit.framework.TestCase;
import ucar.nc2.ncml.NcMLReader;

import java.io.IOException;
import java.io.StringReader;

/**
 * test CoordinateAxis1D.findCoord()
 *
 * @author caron
 * @since Jul 8, 2010
 */


public class TestFindCoord extends TestCase {

  public TestFindCoord(String name) {
    super(name);
  }

  public void testRegular() throws IOException {
    String ncml =
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "    <dimension name='lat'  length='2' />\n" +
            "    <dimension name='lon'  length='2' />\n" +
            "    <dimension name='bnds' length='2' />\n" +
            "    <attribute name='Conventions' value='CF-1.0' />\n" +
            "    <variable name='lat' shape='lat' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_north' />\n" +
            "        <attribute name='bounds' type='String' value='lat_bnds' />\n" +
            "        <values>-45 45</values>\n" +
            "    </variable>\n" +
            "    <variable name='lat_bnds' shape='lat bnds' type='double'>\n" +
            "        <values>-90 0 0 90</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon' shape='lon' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_east' />\n" +
            "        <attribute name='bounds' type='String' value='lon_bnds' />\n" +
            "        <values>90 270</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon_bnds' shape='lon bnds' type='double'>\n" +
            "        <values>0 180 180 360</values>\n" +
            "    </variable>\n" +
            "</netcdf>";

    doTest(ncml, "lat", false,
        new double[]{-91, -90, -67.5, -45, -22.5, 0, 22.5, 45, 67.5, 90, 91},
        new int[]   {-1,    0,     0,   0,     0, 1,    1,  1,    1,  -1, -1});

    doTest(ncml, "lon", false,
        new double[]{-91, -90, 0, 45, 90, 135, 180, 225, 270, 315, 360, 450, 451},
        new int[]   {-1,    -1,0,  0,  0,    0,  1,   1,  1,    1,  -1, -1,    -1});

    doTest(ncml, "lat", true,
        new double[]{-91, -90, -67.5, -45, -22.5, 0, 22.5, 45, 67.5, 90, 91},
        new int[]   {0,    0,     0,   0,     0, 1,    1,  1,    1,  1, 1});

    doTest(ncml, "lon", true,
        new double[]{-91, -90, 0, 45, 90, 135, 180, 225, 270, 315, 360, 450, 451},
        new int[]   {  0,    0,0,  0,  0,    0,  1,   1,  1,    1,   1,   1,   1});
  }

  public void testRegularDescending() throws IOException {
    String ncml =
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "    <dimension name='lat'  length='2' />\n" +
            "    <dimension name='lon'  length='2' />\n" +
            "    <dimension name='bnds' length='2' />\n" +
            "    <attribute name='Conventions' value='CF-1.0' />\n" +
            "    <variable name='lat' shape='lat' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_north' />\n" +
            "        <attribute name='bounds' type='String' value='lat_bnds' />\n" +
            "        <values>45 -45</values>\n" +
            "    </variable>\n" +
            "    <variable name='lat_bnds' shape='lat bnds' type='double'>\n" +
            "        <values>90 0 0 -90</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon' shape='lon' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_east' />\n" +
            "        <attribute name='bounds' type='String' value='lon_bnds' />\n" +
            "        <values>270 90</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon_bnds' shape='lon bnds' type='double'>\n" +
            "        <values>360 180 180 0</values>\n" +
            "    </variable>\n" +
            "</netcdf>";

    doTest(ncml, "lat", false,
        new double[]{-91, -90, -67.5, -45, -22.5, 0, 22.5, 45, 67.5, 90, 91},
        new int[]   {-1,    -1,    1, 1, 1, 1, 0, 0, 0, 0, -1});

    doTest(ncml, "lon", false,
        new double[]{-91, -90, 0, 45, 90, 135, 180, 225, 270, 315, 360, 450, 451},
        new int[]   {-1,   -1, -1, 1,  1,   1,   1,   0,   0,   0,   0, -1, -1});

    doTest(ncml, "lat", true,
        new double[]{-91, -90, -67.5, -45, -22.5, 0, 22.5, 45, 67.5, 90, 91},
        new int[]   {1,    1,  1,    1,  1, 1, 0,    0,     0,   0,     0, });

    doTest(ncml, "lon", true,
        new double[]{-91, -90, 0, 45, 90, 135, 180, 225, 270, 315, 360, 450, 451},
        new int[]   {  1,   1,  1,    1,   1,   1,   1, 0,    0,0,  0,  0,    0});
  }

  public void testIrregular() throws IOException {
    String ncml =
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "    <dimension name='lat'  length='3' />\n" +
            "    <dimension name='lon'  length='3' />\n" +
            "    <dimension name='bnds' length='2' />\n" +
            "    <attribute name='Conventions' value='CF-1.0' />\n" +
            "    <variable name='lat' shape='lat' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_north' />\n" +
            "        <attribute name='bounds' type='String' value='lat_bnds' />\n" +
            "        <values>-45 10 45</values>\n" +
            "    </variable>\n" +
            "    <variable name='lat_bnds' shape='lat bnds' type='double'>\n" +
            "        <values>-90 5 5 15 15 90</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon' shape='lon' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_east' />\n" +
            "        <attribute name='bounds' type='String' value='lon_bnds' />\n" +
            "        <values>90 200 270</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon_bnds' shape='lon bnds' type='double'>\n" +
            "        <values>0 180 180 220 220 360</values>\n" +
            "    </variable>\n" +
            "</netcdf>";

    doTest(ncml, "lat", false,
        new double[]{-91, -90, -67.5, -45, -22.5, 0, 10, 45, 67.5, 90, 91},
        new int[] {-1,  0,  0,  0,  0,  0,  1,  2,  2,  2, -1});

    doTest(ncml, "lon", false,
        new double[]{-91, -90, 0, 45, 90, 135, 180, 210, 270, 315, 360, 450, 451},
        new int[] {-1, -1,  0,  0,  0,  0,  1,  1,  2,  2,  2, -1, -1, });

    doTest(ncml, "lat", true,
        new double[]{-91, -90, -67.5, -45, -22.5, 0, 10, 45, 67.5, 90, 91},
        new int[] {0,  0,  0,  0,  0,  0,  1,  2,  2,  2, 2});

    doTest(ncml, "lon", true,
        new double[]{-91, -90, 0, 45, 90, 135, 180, 210, 270, 315, 360, 450, 451},
        new int[] {0, 0,  0,  0,  0,  0,  1,  1,  2,  2,  2, 2, 2 });
  }

  public void testIrregularDescending() throws IOException {
    String ncml =
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "    <dimension name='lat'  length='3' />\n" +
            "    <dimension name='lon'  length='3' />\n" +
            "    <dimension name='bnds' length='2' />\n" +
            "    <attribute name='Conventions' value='CF-1.0' />\n" +
            "    <variable name='lat' shape='lat' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_north' />\n" +
            "        <attribute name='bounds' type='String' value='lat_bnds' />\n" +
            "        <values>44 40 30</values>\n" +
            "    </variable>\n" +
            "    <variable name='lat_bnds' shape='lat bnds' type='double'>\n" +
            "        <values>50 42 42 35 35 0</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon' shape='lon' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_east' />\n" +
            "        <attribute name='bounds' type='String' value='lon_bnds' />\n" +
            "        <values>9 0 -20</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon_bnds' shape='lon bnds' type='double'>\n" +
            "        <values>20 0 0 -10 -10 -90</values>\n" +
            "    </variable>\n" +
            "</netcdf>";

    doTest(ncml, "lat", false,
        new double[]{-90, -0, 0, 10, 33, 44, 55, 90},
        new int[] {-1,  2,  2,  2,  2,  0, -1, -1});

    doTest(ncml, "lon", false,
        new double[]{-91, -90, -12, -2, 0, 2, 22, 90},
        new int[] {-1,  2,  2,  1,  1,  0, -1, -1, });

    doTest(ncml, "lat", true,
        new double[]{-90, -0, 0, 10, 33, 44, 55, 90},
        new int[]{ 2,  2,  2,  2,  2,  0,  0,  0, });

    doTest(ncml, "lon", true,
        new double[]{-91, -90, -12, -2, 0, 2, 22, 90},
        new int[]{ 2,  2,  2,  1,  1,  0,  0,  0, });
  }

  public void testNonContig() throws IOException {
    String ncml =
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "    <dimension name='lat'  length='3' />\n" +
            "    <dimension name='lon'  length='3' />\n" +
            "    <dimension name='bnds' length='2' />\n" +
            "    <attribute name='Conventions' value='CF-1.0' />\n" +
            "    <variable name='lat' shape='lat' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_north' />\n" +
            "        <attribute name='bounds' type='String' value='lat_bnds' />\n" +
            "        <values>10 20 90</values>\n" +
            "    </variable>\n" +
            "    <variable name='lat_bnds' shape='lat bnds' type='double'>\n" +
            "        <values>0 11 18 22 30 90</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon' shape='lon' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_east' />\n" +
            "        <attribute name='bounds' type='String' value='lon_bnds' />\n" +
            "        <values>0 10 90</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon_bnds' shape='lon bnds' type='double'>\n" +
            "        <values>0 5 5 10 80 90</values>\n" +
            "    </variable>\n" +
            "</netcdf>";

    doTest(ncml, "lat", false,
        new double[]{90, 50, 48, 41.5, 40, 33, 15, 0, -10},
        new int[] { 2,  2,  2,  2,  2,  2, -1,  0, -1, });

    doTest(ncml, "lon", false,
        new double[]{90, 20, 18, 5, 4, 0, -10, -15, -20, -45, -90, -100},
        new int[] { 2, -1, -1,  0,  0,  0, -1, -1, -1, -1, -1, -1, });

    doTest(ncml, "lat", true,
        new double[]{90, 50, 48, 41.5, 40, 33, 15, 0, -10},
        new int[] { 2,  2,  2,  2,  2,  2,  1,  0,  0, });

    doTest(ncml, "lon", true,
        new double[]{90, 20, 18, 5, 4, 0, -10, -15, -20, -45, -90, -100},
        new int[]{ 2,  1,  1,  0,  0,  0,  0,  0,  0,  0,  0,  0, });
  }


  public void testNonContigDescending() throws IOException {
    String ncml =
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "    <dimension name='lat'  length='3' />\n" +
            "    <dimension name='lon'  length='3' />\n" +
            "    <dimension name='bnds' length='2' />\n" +
            "    <attribute name='Conventions' value='CF-1.0' />\n" +
            "    <variable name='lat' shape='lat' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_north' />\n" +
            "        <attribute name='bounds' type='String' value='lat_bnds' />\n" +
            "        <values>44 40 30</values>\n" +
            "    </variable>\n" +
            "    <variable name='lat_bnds' shape='lat bnds' type='double'>\n" +
            "        <values>50 42 41 35 32 0</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon' shape='lon' type='double'>\n" +
            "        <attribute name='units' type='String' value='degrees_east' />\n" +
            "        <attribute name='bounds' type='String' value='lon_bnds' />\n" +
            "        <values>9 0 -20</values>\n" +
            "    </variable>\n" +
            "    <variable name='lon_bnds' shape='lon bnds' type='double'>\n" +
            "        <values>20 5 0 -10 -20 -90</values>\n" +
            "    </variable>\n" +
            "</netcdf>";

    doTest(ncml, "lat", false,
        new double[]{90, 50, 48, 41.5, 40, 33, 15, 0, -10},
        new int[]{-1,  0,  0, -1,  1, -1,  2,  2, -1, });

    doTest(ncml, "lon", false,
        new double[]{90, 20, 18, 5, 4, 0, -10, -15, -20, -45, -90, -100},
        new int[] {-1,  0,  0,  0, -1,  1,  1, -1,  2,  2,  2, -1, });

    doTest(ncml, "lat", true,
        new double[]{90, 50, 48, 41.5, 40, 33, 15, 0, -10},
         new int[] { 0,  0,  0,  1,  1,  2,  2,  2,  2, });

    doTest(ncml, "lon", true,
        new double[]{90, 20, 18, 5, 4, 0, -10, -15, -20, -45, -90, -100},
        new int[] { 0,  0,  0,  0,  1,  1,  1,  2,  2,  2,  2,  2, });
  }


  public void doTest(String ncml, String varName, boolean bounded, double[] vals, int[] expect) throws IOException {
    NetcdfDataset nc = NcMLReader.readNcML(new StringReader(ncml), null);
    NetcdfDataset dataset = new NetcdfDataset(nc, true);

    try {
      CoordinateAxis1D axis1D = (CoordinateAxis1D) dataset.findVariable(varName);
      if (axis1D.isContiguous()) {
        double[] edge = axis1D.getCoordEdges();
        System.out.printf("%s =", varName);
        for (int i =0; i<edge.length;i++)
          System.out.printf("%2f, ", edge[i]);
        System.out.printf("%n");

      } else {
        System.out.printf("%s =", varName);
        double[] bound1 =axis1D.getBound1();
        double[] bound2 =axis1D.getBound2();
        for (int i =0; i<axis1D.getSize();i++)
          System.out.printf("(%f,%f) ", bound1[i],bound2[i]);
        System.out.printf("%n");
      }
      
      for (int i =0; i<vals.length;i++) {
        double v = vals[i];
        int index = bounded ? axis1D.findCoordElementBounded(v) :  axis1D.findCoordElement(v);
        System.out.printf(" %5.1f, index= %2d %n", v, index);
        if (expect != null) assert index == expect[i];
      }
      System.out.printf("{");
      for (int i =0; i<vals.length;i++) {
        double v = vals[i];
        int index = bounded ? axis1D.findCoordElementBounded(v) :  axis1D.findCoordElement(v);
        System.out.printf("%2d, ", index);
      }
      System.out.printf("}%n%n");
    } finally {
      dataset.close();
    }
  }
}
