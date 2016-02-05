/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

import junit.framework.TestCase;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.StringReader;

/**
 * Class Description
 *
 * @author caron
 * @since Jul 3, 2009
 */


public class TestValuesFromAttribute extends TestCase {

  public TestValuesFromAttribute(String name) {
    super(name);
  }

  String ncml =
      "<?xml version='1.0' encoding='UTF-8'?>\n" +
          "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='file:src/test/data/ncml/nc/lflx.mean.nc'>\n" +
          "   <variable name='titleAsVariable' type='String' shape=''>\n" +
          "     <values fromAttribute='title'/>\n" +
          "   </variable>\n" +
          "   <variable name='titleAsVariable2' type='String' shape=''>\n" +
          "     <values fromAttribute='@title'/>\n" +
          "   </variable>\n" +
          "   <variable name='VariableAttribute' type='double' shape='2'>\n" +
          "     <values fromAttribute='time@actual_range'/>\n" +
          "   </variable>\n" +
          "</netcdf>";

  public void testValuesFromAttribute() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "TestValuesFromAttribute.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);
    System.out.println(" TestNcmlAggExisting.open " + filename + "\n" + ncfile);

    Variable newVar = ncfile.findVariable("titleAsVariable");
    assert null != newVar;

    assert newVar.getShortName().equals("titleAsVariable");
    assert newVar.getRank() == 0;
    assert newVar.getSize() == 1;
    assert newVar.getDataType() == DataType.STRING;

    Array data = newVar.read();
    assert data.getElementType() == String.class;

    Object val = data.getObject(0);
    assert val instanceof String;
    assert val.equals("COADS 1-degree Equatorial Enhanced");

    ////////////////
    newVar = ncfile.findVariable("titleAsVariable2");
    assert null != newVar;

    assert newVar.getShortName().equals("titleAsVariable2");
    assert newVar.getRank() == 0;
    assert newVar.getSize() == 1;
    assert newVar.getDataType() == DataType.STRING;

    data = newVar.read();
    assert data.getElementType() == String.class;

    val = data.getObject(0);
    assert val instanceof String;
    assert val.equals("COADS 1-degree Equatorial Enhanced");

    ///////////////
    newVar = ncfile.findVariable("VariableAttribute");
    assert null != newVar;

    assert newVar.getShortName().equals("VariableAttribute");
    assert newVar.getRank() == 1;
    assert newVar.getSize() == 2;
    assert newVar.getDataType() == DataType.DOUBLE;

    data = newVar.read();
    assert data.getRank() == 1;
    assert data.getSize() == 2;
    assert data.getElementType() == double.class;

    double[] result = new double[] { 715511.0, 729360.0};
    for (int i=0; i<result.length; i++) {
      assert result[i] == data.getDouble(i);
    }

    ncfile.close();
  }

}
