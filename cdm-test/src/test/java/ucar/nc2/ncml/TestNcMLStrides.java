/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestNcMLStrides extends TestCase {

  public TestNcMLStrides(String name) {
    super(name);
  }

  NetcdfFile ncfile = null;
  String location = "file:"+ TestDir.cdmUnitTestDir + "agg/strides/strides.ncml";

  public void setUp() {
    try {
      ncfile = NcMLReader.readNcML(location, null);
      //System.out.println("ncfile opened = "+location);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = "+e);
    } catch (IOException e) {
      System.out.println("IO error = "+e);
      e.printStackTrace();
    }
  }

  protected void tearDown() throws IOException {
    ncfile.close();
  }

  public void testStride() throws IOException, InvalidRangeException {
    System.out.println("ncfile opened = "+location+"\n"+ncfile);
    Variable time = ncfile.findVariable("time");

    ArrayInt all = (ArrayInt) time.read();
    for (int i=0; i<all.getSize(); i++)
      assert (all.getInt(i) == i+1);

    testStride("0:13:3");

    for (int i=1;i<12;i++)
      testStride("0:13:"+i);
  }

  public void testStride(String stride) throws IOException, InvalidRangeException {
    Variable time = ncfile.findVariable("time");
    ArrayInt all = (ArrayInt) time.read();

    ArrayInt correct = (ArrayInt) all.section(new Section(stride).getRanges());
    System.out.printf("correct(%s) %s", stride, NCdumpW.toString(correct));
    ArrayInt data = (ArrayInt) time.read(stride);
    System.out.printf("data(%s) %s%n", stride, NCdumpW.toString(data));
    Index ci = correct.getIndex();
    Index di = data.getIndex();
    for (int i=0; i<data.getSize(); i++)
      assert (data.getInt(di.set(i)) == correct.getInt(ci.set(i))) : stride +" index " + i + " = " + data.getInt(di.set(i)) +" != "+ correct.getInt(ci.set(i));
  }

}
