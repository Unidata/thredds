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
package thredds.tds;

import junit.framework.*;

import ucar.nc2.dataset.*;
import ucar.nc2.*;
import ucar.nc2.util.IO;

import java.io.IOException;
import java.io.File;

public class TestTdsNetcdfSubsetService extends TestCase {

  public TestTdsNetcdfSubsetService( String name) {
    super(name);
  }

  public void testNetcdfSubsetService() throws IOException {
    String url = "/ncServer/gribCollection/NAM_CONUS_20km_surface_20060316_0000.grib1.nc?grid=K_index&grid=Sweat_index&west=-140&east=-90&north=50&south=20&time_start=3&time_end=12";
    File fileSave = new File("C:/TEMP/testNetcdfSubsetService.nc");

    IO.readURLtoFile(TestTdsLocal.topCatalog+url, fileSave);
    System.out.println("Copied "+ TestTdsLocal.topCatalog+url+" to "+fileSave.getPath());

    NetcdfFile ncd = null;
    try {
      ncd = NetcdfDataset.openFile(fileSave.getPath(), null);
    } catch (Throwable t) {
      IO.copyFile(fileSave.getPath(), System.out);
      return;
    }
    assert ncd != null;

    assert ncd.findVariable("K_index") != null;
    assert ncd.findVariable("Sweat_index") != null;
    assert ncd.findVariable("time") != null;
    assert ncd.findVariable("y") != null;
    assert ncd.findVariable("x") != null;

    Variable v = ncd.findVariable("time");
    assert v.getSize() == 4;

    v = ncd.findVariable("x");
    assert v.getSize() == 235;

    v = ncd.findVariable("y");
    assert v.getSize() == 199;

    ncd.close();
  }


}