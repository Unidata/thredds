/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.ncml;

import static org.junit.Assert.assertArrayEquals;

import org.junit.After;
import org.junit.Before;

import org.junit.Test;
import ucar.ma2.ArrayInt;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/** Test ncml value element in the JUnit framework. */

public class TestNcMLValues {

    NetcdfFile ncfile = null;
    String ncml = null;
    int expectedIntLength;
    int[] expectedIntShape = null;
    ArrayInt expectedIntValues = null;
    String[] intVarNames = null;

    @Before
    public void setUp() {
        ncml =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "   <dimension name='intDim' length='3' />\n" +
            "   <variable name='singleWs' type='int' shape='intDim'>\n" +
            "     <attribute name='description' value='Test Single White Space With Default Separator' />\n" +
            "     <values>0 1 2</values>\n" +
            "   </variable>\n" +
            "   <variable name='multiWs' type='int' shape='intDim'>\n" +
            "     <attribute name='description' value='Test Multi-length White Spaces With Default Separator' />\n" +
            "     <values>0    1  2</values>\n" +
            "   </variable>\n" +
            "   <variable name='tabs' type='int' shape='intDim'>\n" +
            "     <attribute name='description' value='Test Tab Spaces With Default Separator' />\n" +
            "     <values>0\t1\t2</values>\n" +
            "   </variable>\n" +
            "   <variable name='mixedTabSpace' type='int' shape='intDim'>\n" +
            "     <attribute name='description' value='Test Mixed Tab/Single-Space Spaces With Default Separator' />\n" +
            "     <values>0\t1 2</values>\n" +
            "   </variable>\n" +
            "   <variable name='mixedTabSpaces' type='int' shape='intDim'>\n" +
            "     <attribute name='description' value='Test Mixed Tab/Multi-Space Spaces With Default Separator' />\n" +
            "     <values>0\t1    2</values>\n" +
            "   </variable>\n" +
            "   <variable name='mixedSpace' type='int' shape='intDim'>\n" +
            "     <attribute name='description' value='Test Mixed Spaces With Default Separator' />\n" +
            "     <values>0\t  1\t    2</values>\n" +
            "   </variable>\n" +
            "   <variable name='customSep' type='int' shape='intDim'>\n" +
            "     <attribute name='description' value='Test Custom Separator' />\n" +
            "     <values separator='-'>0-1-2</values>\n" +
            "   </variable>\n" +
            "</netcdf>";

        expectedIntLength = 3;
        expectedIntShape = new int[]{expectedIntLength};
        expectedIntValues = new ArrayInt(expectedIntShape);
        intVarNames = new String[]{"singleWs","multiWs","tabs","mixedTabSpace","mixedTabSpaces",
        "mixedSpace", "customSep"};
        Index idx = expectedIntValues.getIndex();
        for(int i=0; i<expectedIntLength; i++) {
            expectedIntValues.set(idx, i);
            idx.incr();
        }

        try {
            ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
        } catch (IOException e) {
            System.out.println("IO error = "+e);
            e.printStackTrace();
        }

    }

    @After
    public void tearDown() throws IOException {
        ncfile.close();
    }

    @Test
    public void testIntVals() throws IOException {
        // build list of variables to test
        List<Variable> varList = new ArrayList<Variable>();

        for (String varName : intVarNames) {
            varList.add(ncfile.findVariable(varName));
        }

        for (Variable var : varList) {
            System.out.println("  " + var.getDescription());
            ArrayInt values = (ArrayInt) var.read();

            assert null != var;
            assertArrayEquals(expectedIntShape, values.getShape());
            assert expectedIntLength == values.getSize();
            assertArrayEquals((int[]) values.get1DJavaArray(Integer.class),
                    (int[]) expectedIntValues.get1DJavaArray(Integer.class));
        }
    }
}
