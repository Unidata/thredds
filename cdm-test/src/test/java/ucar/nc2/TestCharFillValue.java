/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/*
The code that demonstrates the problem can be found below. The issue is due
to the special checks we do for fill value in Nc4Iosp, specifically around
line 2822 and line 2841. Thanks for taking a look!
*/

package ucar.nc2;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestCharFillValue extends UnitTestCommon
{

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    String fileName = "charAttr.nc4";
    String charVarName = "charVar";
    String charFillValue = "F";
    String charNullFillValue = "\0";

    @Test
    public void
    testCharFillValue() throws IOException {
    logger.info("*** Test Non-Null Character Fill Value");

        try (NetcdfFileWriter ncfw =
                     NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, fileName);) {
            Dimension charDim = ncfw.addDimension("charDim", 3);
            Variable charVar = ncfw.addVariable(charVarName, DataType.CHAR,
                    charDim.getFullName());
            // works
            Array charArray = ArrayChar.makeFromString(charFillValue, 1);
            Attribute charAttr = new Attribute("charAttrName", charArray);
            charVar.addAttribute(charAttr);
            Array charArrayFillValue =
                    ArrayChar.makeFromString(charFillValue, 1);
            Attribute charAttrFillValue;
            // Try to do _FillValue two ways
            if(true) {
                charAttrFillValue = new Attribute("_FillValue",
                        charArrayFillValue);
            } else {
                charAttrFillValue = new Attribute("_FillValue",DataType.CHAR);
                charAttrFillValue.setValues(charArrayFillValue);
            }
            charVar.addAttribute(charAttrFillValue);
            ncfw.create();
        }

        try (NetcdfFile ncf = NetcdfFile.open(fileName)) {
            Variable charVarFromFile = ncf.findVariable(charVarName);
            H5header.Vinfo h5 = (H5header.Vinfo)
                    charVarFromFile.getSPobject();
            logger.debug("use fill value: {}", h5.useFillValue());
            // should be 3 charFillVal characters
            Array arr = charVarFromFile.read();
            char[] javaArr = (char[]) arr.get1DJavaArray(DataType.CHAR);
            String s = new String(javaArr);
            logger.debug("expected fill value: |{}|", charFillValue);
            logger.debug("actual filled value: |{}|", s );
            for(int i=0;i<s.length();i++)
                Assert.assertTrue("position "+i,charFillValue.charAt(0) == s.charAt(i));
        }
    }

    // Re: https://github.com/Unidata/thredds/pull/1262

    @Test
    public void
    testNullCharFillValue() throws IOException {
        logger.info("\n*** Test Null Character Fill Value");

        try (NetcdfFileWriter ncfw =
                     NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, fileName);) {
            Dimension charDim = ncfw.addDimension("charDim", 3);
            Variable charVar = ncfw.addVariable(charVarName, DataType.CHAR,
                    charDim.getFullName());
            Array charArrayFillValue =
                    ArrayChar.makeFromString(charNullFillValue, 1);
            Attribute charAttrFillValue;
            charAttrFillValue = new Attribute("_FillValue",
                    charArrayFillValue);
            charVar.addAttribute(charAttrFillValue);
            ncfw.create();
        }

        try (NetcdfFile ncf = NetcdfFile.open(fileName)) {
            Variable charVarFromFile = ncf.findVariable(charVarName);
            H5header.Vinfo h5 = (H5header.Vinfo)
                    charVarFromFile.getSPobject();
            logger.debug("use fill value: {}", h5.useFillValue());
            // should be 3 charFillVal characters
            Array arr = charVarFromFile.read();
            char[] javaArr = (char[]) arr.get1DJavaArray(DataType.CHAR);
            String s = new String(javaArr);
            logger.debug("expected fill value: |{}|", s.charAt(0) == '\0' ? "" : charNullFillValue);
            logger.debug("actual fill value: |");
            for (int i = 0; i < s.length(); i++)
                if (s.charAt(i) != '\0') logger.debug("{}", s.charAt(i));
            logger.debug("|");
            for (int i = 0; i < s.length(); i++)
                Assert.assertTrue("position " + i, charNullFillValue.charAt(0) == s.charAt(i));
        }
    }

    @After
    public void cleanup() {
        // this runs after each test, so we should always see the cleanup
        // of the file, even if the test goes up in flames
        File f = new File(fileName);
        if(f.exists()) {
            f.delete();
        }
    }
}
