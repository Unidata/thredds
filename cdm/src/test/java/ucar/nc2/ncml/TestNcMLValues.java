/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import static org.junit.Assert.assertArrayEquals;

import org.junit.After;
import org.junit.Before;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayInt;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/** Test ncml value element in the JUnit framework. */

public class TestNcMLValues {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        expectedIntValues = new ArrayInt(expectedIntShape, false);
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
