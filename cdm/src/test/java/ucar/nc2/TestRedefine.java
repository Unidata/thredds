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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;

import java.io.IOException;

public class TestRedefine {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testRedefine() throws IOException, InvalidRangeException {
        String filename = tempFolder.newFile("testRedefine.nc").getAbsolutePath();

        try (NetcdfFileWriteable ncWriter = NetcdfFileWriteable.createNew(filename, true)) {
            ncWriter.addGlobalAttribute("Conventions", "globulate");
            ncWriter.addGlobalAttribute(CDM.HISTORY, "lava");
            ncWriter.addGlobalAttribute("att8", "12345678");

            Dimension time   = ncWriter.addDimension("time", 4, true, false, false);
            Dimension dims[] = { time };

            /* Add time */
            ncWriter.addVariable("time", DataType.DOUBLE, dims);
            ncWriter.addVariableAttribute("time", "quantity", "time");
            ncWriter.addVariableAttribute("time", "units", "s");

            /* Add a dependent variable */
            ncWriter.addVariable("h", DataType.DOUBLE, dims);
            ncWriter.addVariableAttribute("h", "quantity", "Height");
            ncWriter.addVariableAttribute("h", "units", "m");
            ncWriter.create();

            double         td[] = { 1.0, 2.0, 3.0, 4.0 };
            double         hd[] = { 0.0, 0.1, 0.3, 0.9 };
            ArrayDouble.D1 ta   = new ArrayDouble.D1(4);
            ArrayDouble.D1 ha   = new ArrayDouble.D1(4);
            for (int i = 0; i < 4; i++) {
                ta.set(i, td[i]);
                ha.set(i, hd[i]);
            }

            ncWriter.write("time", ta);
            ncWriter.write("h", ha);

            //////////////////////////////////////////
            ncWriter.setRedefineMode(true);

            ncWriter.renameGlobalAttribute(CDM.HISTORY, "lamp");
            ncWriter.addGlobalAttribute(CDM.HISTORY, "final");
            ncWriter.deleteGlobalAttribute("Conventions");

            ncWriter.addVariableAttribute("h", "units", "meters"); // duplicates existing
            ncWriter.addVariableAttribute("h", "new", "stuff");
            ncWriter.renameVariableAttribute("time", "quantity", "quality");

            ncWriter.renameVariable("time", "date");
            ncWriter.renameDimension("time", "date");

            /////////////////////////////////////////////////
            ncWriter.setRedefineMode(false);

            Attribute att = ncWriter.findGlobalAttribute("Conventions");
            assert att == null;
            att = ncWriter.findGlobalAttribute(CDM.HISTORY);
            assert att.getStringValue().equals("final");
            att = ncWriter.findGlobalAttribute("lamp");
            assert att.getStringValue().equals("lava");

            Variable v = ncWriter.findVariable("h");
            att = v.findAttribute("units");
            assert att != null;
            assert att.getStringValue().equals("meters");

            assert ncWriter.findVariable("time") == null;
            v = ncWriter.findVariable("date");
            assert v != null;
            assert v.getRank() == 1;
            assert null != v.findAttribute("quality");

            Dimension d = v.getDimension(0);
            assert d.getShortName().equals("date");

            assert ncWriter.findDimension("time") == null;
            Dimension dim = ncWriter.findDimension("date");
            assert dim != null;
            assert dim.getShortName().equals("date");
            assert dim.equals(d);
            assert dim == d;
        }

        try (NetcdfFileWriteable ncWriter = NetcdfFileWriteable.openExisting(filename, true)) {
            ncWriter.setRedefineMode(true);

            ncWriter.addGlobalAttribute("att8", "1234567");

            /////////////////////////////////////////////////
            boolean rewriteAll = ncWriter.setRedefineMode(false);
            assert !rewriteAll;

            Attribute att = ncWriter.findGlobalAttribute("att8");
            assert att != null;
            assert att.getStringValue().equals("1234567") : att.getStringValue();
        }

        try (NetcdfFileWriteable ncWriter = NetcdfFileWriteable.openExisting(filename, true)) {
            ncWriter.setRedefineMode(true);

            ncWriter.addGlobalAttribute("att8", "123456789");

            /////////////////////////////////////////////////
            boolean rewriteAll = ncWriter.setRedefineMode(false);
            assert rewriteAll;

            Attribute att = ncWriter.findGlobalAttribute("att8");
            assert att != null;
            assert att.getStringValue().equals("123456789") : att.getStringValue();

            ncWriter.close();
        }
    }

    @Test
    public void testRewriteHeader3() throws IOException, InvalidRangeException {
        String filename = tempFolder.newFile("testRedefine2.nc").getAbsolutePath();

        try (NetcdfFileWriteable file = NetcdfFileWriteable.createNew(filename, true)) {
            file.addGlobalAttribute("att8", "1234567890");
            file.setExtraHeaderBytes(10);
            file.create();

            file.setRedefineMode(true);
            file.addGlobalAttribute("att8", "123456789012345");
            boolean rewriteAll = file.setRedefineMode(false);
            assert !rewriteAll;

            Attribute att = file.findGlobalAttribute("att8");
            assert att != null;
            assert att.getStringValue().equals("123456789012345") : att.getStringValue();
        }
    }

    @Test
    public void testRedefineClose() throws IOException {
        String filename = tempFolder.newFile("testRedefine.nc").getAbsolutePath();

        // Create a new file
        try (NetcdfFileWriter file = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename)) {
            Attribute attr = new Attribute("att", 5);
            file.addGroupAttribute(null, attr);
            file.create();
        }

        // Re-open file in redefine mode
        try (NetcdfFileWriter file = NetcdfFileWriter.openExisting(filename)) {
            file.setRedefineMode(true);
            Attribute attr = new Attribute("att2", "foobar");
            file.addGroupAttribute(null, attr);
        }

        // Check that attribute is present
        try (NetcdfFileWriter file = NetcdfFileWriter.openExisting(filename)) {
            Assert.assertNotNull(file.findGlobalAttribute("att"));
            Assert.assertNotNull(file.findGlobalAttribute("att2"));
        }
    }
}
