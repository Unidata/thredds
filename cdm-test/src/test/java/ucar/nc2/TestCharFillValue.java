/*********************************************************************
 * Copyright 2018, UCAR/Unidata
 * See netcdf/COPYRIGHT file for copying and redistribution conditions.
 ********************************************************************/

/*
The code that demonstrates the problem can be found below. The issue is due
to the special checks we do for fill value in Nc4Iosp, specifically around
line 2822 and line 2841. Thanks for taking a look!
*/

package ucar.nc2;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.File;
import java.io.IOException;

public class TestCharFillValue extends UnitTestCommon
{

    static String fileName = "charAttr.nc4";
    static String charVarName = "charVar";
    static String charFillVal = "F";

    @Test
    public void
    testCharFillValue()
    {
        try {
            try (NetcdfFileWriter ncfw =
                         NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, fileName);) {
                Dimension charDim = ncfw.addDimension("charDim", 3);
                Variable charVar = ncfw.addVariable(charVarName, DataType.CHAR,
                        charDim.getFullName());
                // works
                Array charArray = ArrayChar.makeFromString(charFillVal, 1);
                Attribute charAttr = new Attribute("charAttrName", charArray);
                charVar.addAttribute(charAttr);
                Array charArrayFillValue =
                        ArrayChar.makeFromString(charFillVal, 1);
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
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (NetcdfFile ncf = NetcdfFile.open(fileName)) {
                Variable charVarFromFile = ncf.findVariable(charVarName);
                H5header.Vinfo h5 = (H5header.Vinfo)
                        charVarFromFile.getSPobject();
                System.out.println("use fill value: " + h5.useFillValue());
                // should be 3 charFillVal characters
                Array arr = charVarFromFile.read();
                char[] javaArr = (char[]) arr.get1DJavaArray(DataType.CHAR);
                String s = new String(javaArr);
                System.out.println("expected fill value: " + charFillVal);
                System.out.println("actual fill value: " + s.substring(0));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            File f = new File(fileName);
            if(false && f.exists())
                f.delete();
        }
    }
}
