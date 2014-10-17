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

import org.junit.Test;
import ucar.ma2.*;
import ucar.nc2.util.Misc;

import java.io.IOException;

/** Test remote netcdf over HTTP in the JUnit framework. */

public class TestHTTP  {

  @Test
  public void testNC2() throws IOException {
    //NetcdfFile ncfile = NetcdfFile.open("http://remotetest.unidata.ucar.edu/thredds/fileServer/public/dataset/testdata/mydata1.nc");
    NetcdfFile ncfile = NetcdfFile.open("http://remotetest.unidata.ucar.edu/thredds/fileServer/testdata/mydata1.nc");
    assert ncfile != null;

    assert(null != ncfile.findDimension("lat"));
    assert(null != ncfile.findDimension("lon"));

    assert("face".equals(ncfile.findAttValueIgnoreCase(null, "yo", "barf")));

    Variable temp = null;
    assert (null != (temp = ncfile.findVariable("temperature")));
    assert("K".equals(ncfile.findAttValueIgnoreCase(temp, "units", "barf")));

    Attribute att = temp.findAttribute("scale");
    assert( null != att);
    assert( att.isArray());
    assert( 3 == att.getLength());
    assert( 3 == att.getNumericValue(2).intValue());

    att = temp.findAttribute("versionD");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1.2 == att.getNumericValue().doubleValue());

    att = temp.findAttribute("versionF");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1.2f == att.getNumericValue().floatValue());
    assert( Misc.closeEnough(1.2, att.getNumericValue().doubleValue()));

    att = temp.findAttribute("versionI");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1 == att.getNumericValue().intValue());

    att = temp.findAttribute("versionS");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 2 == att.getNumericValue().shortValue());

    att = temp.findAttribute("versionB");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 3 == att.getNumericValue().byteValue());

    // read
    Array A = temp.read();

    int i,j;
    Index ima = A.getIndex();
    int[] shape = A.getShape();

    // write
    for (i=0; i<shape[0]; i++) {
      for (j=0; j<shape[1]; j++) {
        assert( A.getDouble(ima.set(i,j)) == (double) (i*1000000+j*1000));
      }
    }

    //System.out.println( "ncfile = "+ ncfile);
    ncfile.close();
    System.out.println( "*****************  Test HTTP done");
  }

}
