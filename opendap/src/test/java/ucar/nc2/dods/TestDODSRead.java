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

import java.io.IOException;

import opendap.test.TestSources;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.category.NeedsExternalResource;

/** Test nc2 dods in the JUnit framework.
 *  Open and read various test datasets from the dts server.
 */
@Category(NeedsExternalResource.class)
public class TestDODSRead {

  public static boolean showFile = false, showFileDebug= false;

  static DODSNetcdfFile open(String name) throws IOException {
    String filename = TestSources.XURL1 + "/" + name;
    return openAbs( filename);
  }

  static DODSNetcdfFile openAbs(String filename) throws IOException {
    System.out.println("TestDODSRead = "+filename);
      DODSNetcdfFile dodsfile = new DODSNetcdfFile(filename);
      if (showFileDebug) System.out.println(dodsfile.getDetailInfo());
      if (showFile) System.out.println(dodsfile.toString());
      return dodsfile;
  }

  @org.junit.Test
  public void testRead() throws IOException {
    // simple
    open( "test.01");
    open( "test.02");
    open( "test.03");
    open( "test.04");
    open( "test.05");
    open( "test.06");
    open( "test.06a");
    open( "test.07");
    open( "test.07a");

    // nested
    open( "test.21");
    open( "test.22");
    //open( "test.23");
    //open( "test.31");
    //open( "test.32");

    open( "test.50"); // structure array
    open( "test.53"); // nested structure in structure array
    open( "test.vs5"); // structure array */

  }

}
