/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds.server.wcs;

import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.Collection;

/**
 * Test encoding output in it framework
 *
 * @author caron
 * @since 11/7/13
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestUTF8GetCapabilitiesEncoding {

  @Parameterized.Parameters(name="{0}")
  public static Collection<Object[]> getTestParameters(){
 		return Arrays.asList(new Object[][]{
            {"/wcs/scanCdmUnitTests/tds/ncep/GFS_Global_2p5deg_20100602_1200.grib2", "service=WCS&version=1.0.0&request=GetCapabilities"},
            {"/wms/scanCdmUnitTests/tds/ncep/GFS_Global_2p5deg_20100602_1200.grib2", "service=WMS&version=1.3.0&request=GetCapabilities"},
    });      //wms/scanCdmUnitTests/tds/ncep/GFS_Global_2p5deg_20100602_1200.grib2?service=WMS&version=1.3.0&request=GetCapabilities
 	}

  String path, query;
  public TestUTF8GetCapabilitiesEncoding(String path, String query) {
    this.path = path;
    this.query = query;
  }

  @Test
  public void readCapabilities() {
    String endpoint = TestWithLocalServer.withPath(path + "?" + query);
    System.out.printf("GetCapabilities req = '%s'%n", endpoint);
    try {
      try (HTTPMethod method = HTTPFactory.Get(endpoint)) {
        int statusCode = method.execute();

        Assert.assertEquals(200, statusCode);
        byte[] content = method.getResponseAsBytes();
        assert content.length > 1000;
        //System.out.printf("%s%n", new String(content, "UTF-8"));

        ByteArrayInputStream bin = new ByteArrayInputStream(content);
        try {
          SAXBuilder builder = new SAXBuilder();
          org.jdom2.Document tdoc = builder.build(bin);
          org.jdom2.Element root = tdoc.getRootElement();

        } catch (Throwable t) {
          // if fail, go find where it barfs
          isValidUTF8(content);
          assert false;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }
  }

  // jesus youd think they could tell you where the problem is
  public static void isValidUTF8(byte[] input) {

    CharsetDecoder cs = Charset.forName("UTF-8").newDecoder();
    ByteBuffer bb = ByteBuffer.wrap(input);

    for (int pos = 0; pos < input.length; pos++) {
      bb.limit(pos);
      try {
        cs.decode(bb);

      } catch (CharacterCodingException e) {
        System.out.printf("barf at %d %s%n", pos, e.getMessage());

        bb.limit(input.length);
        int len = 100;
        byte[] dst = new byte[len];
        for (int i = 0; i < len; i++)
          dst[i] = bb.get(pos-len+i);
        String s = new String(dst);
        System.out.printf("before = %s%n", s);

        for (int i = 0; i < 12; i++) {
          int b = bb.get(pos - 10 + i);
          System.out.printf("  %3d == %s == '%s'%n", b, Long.toHexString((long) b), (char) b);
        }

        break;
      }
    }
  }

}
