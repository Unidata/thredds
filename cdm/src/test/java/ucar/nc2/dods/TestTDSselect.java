// $Id: TestTDSselect.java 51 2006-07-12 17:13:13Z caron $
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
