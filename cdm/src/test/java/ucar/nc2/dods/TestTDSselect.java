// $Id: TestTDSselect.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dods;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author john
 */
public class TestTDSselect extends TestCase {

  public TestTDSselect( String name) {
    super(name);
  }

  public void testStrings() throws IOException, InvalidRangeException {
    String url = TestLocalDodsServer.testdata+"testWrite.nc";
    NetcdfDataset dodsfile = NetcdfDataset.openDataset(url);
    Variable v = null;

    // string
    assert(null != (v = dodsfile.findVariable("svar")));
    assert v.getName().equals("svar");
    assert v.getRank() == 1;
    assert v.getSize() == 80;
    assert v.getDataType() == DataType.CHAR : v.getDataType();

    Array a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 80 : a.getSize();
    assert a.getElementType() == DataType.CHAR.getPrimitiveClassType();

    a = v.read("1:10");
    assert a.getRank() == 1;
    assert a.getSize() == 10 : a.getSize();
    assert a.getElementType() == DataType.CHAR.getPrimitiveClassType();

    // string array
    assert(null != (v = dodsfile.findVariable("names")));
    assert v.getName().equals("names");
    assert v.getRank() == 2;
    assert v.getSize() == 3 * 80;
    assert v.getDataType() == DataType.CHAR : v.getDataType();

    a = v.read();
    assert a.getRank() == 2;
    assert a.getSize() == 3 * 80 : a.getSize();
    assert a.getElementType() == DataType.CHAR.getPrimitiveClassType();

    a = v.read("0:1,1:10");
    assert a.getRank() == 2;
    assert a.getSize() == 2 * 10 : a.getSize();
    assert a.getElementType() == DataType.CHAR.getPrimitiveClassType();
  }
}
