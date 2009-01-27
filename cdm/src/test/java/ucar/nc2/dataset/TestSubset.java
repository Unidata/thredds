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

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.TestNcML;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Test netcdf dataset in the JUnit framework.
 */

public class TestSubset extends TestCase {

    public TestSubset(String name) {
        super(name);
    }

    static NetcdfFile ncfile = null;
    String filename = "file:./" + TestNcML.topDir + "subsetAwips.xml";

    public void setUp() {
        if (ncfile != null) return;

        try {
            ncfile = new NcMLReader().readNcML(filename, null);
        } catch (java.net.MalformedURLException e) {
            System.out.println("bad URL error = " + e);
        } catch (IOException e) {
            System.out.println("IO error = " + e);
            e.printStackTrace();
        }

    }

    public void testSubsetData() {
        Variable v = ncfile.findVariable("t");
        Variable vsub = ncfile.findVariable("T-MandatoryLevels");
        Array data, dataSub;

        assert v != null;
        assert vsub != null;

        try {
            dataSub = vsub.read();

            int[] origin = new int[v.getRank()];
            int[] shape = v.getShape();
            origin[1] = 1;
            shape[1] = 19;

            data = v.read(origin, shape);
            compare(data, dataSub);

            Array data2 = v.read("*,1:19,*,*");
            compare(data2, dataSub);

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }

    }

    void compare(Array data1, Array data2) {
        assert data1.getSize() == data2.getSize();

        IndexIterator iter1 = data1.getIndexIterator();
        IndexIterator iter2 = data2.getIndexIterator();

        while (iter1.hasNext() && iter2.hasNext()) {
            double d1 = iter1.getDoubleNext();
            double d2 = iter2.getDoubleNext();

            assert TestUtils.close(d1, d2) : d1 + " != " + d2;
        }


    }
}
