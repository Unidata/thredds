/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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

package thredds.wms;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;

import ucar.nc2.util.IO;

/**
 * Class Description.
 *
 * @author caron
 * @since Oct 6, 2008
 */
public class TestWmsServer extends TestCase {
  private String server = "http://localhost:8080/thredds/wms/";

  public TestWmsServer( String name) {
    super(name);
  }

  public void testOne() throws IOException {
    String dataset = server+"testAll/2004050400_eta_211.nc";
    //showGetCapabilities(dataset);
    getMap(dataset, "Z_sfc", "C:/temp/wmsTest.jpg");
  }

  private void showGetCapabilities(String url) throws IOException {
    showRead(url+"?request=GetCapabilities&version=1.3.0&service=WMS");
  }

  /*
            <BoundingBox CRS="CRS:84" minx="-135.33123756731953" maxx="-31.54351738422973" miny="19.672744972339057"
                       maxy="60.30152674055779"/>
   */
  private void getMap(String url, String grid, String filename) throws IOException {
    String styles = "&STYLES=BOXFILL/redblue&";
    String srs = "CRS=CRS:84&";
    String bb = "BBOX=-100,30,-40,60&";
    String width="WIDTH=600&";
    String height="HEIGHT=500&";
    String format="FORMAT=image/png";
    String opts=styles+srs+bb+width+height+format;

    saveRead(url+"?request=GetMap&version=1.3.0&service=WMS&Layers="+grid+opts, filename);
  }

  private void showRead(String url) throws IOException {
    System.out.println("****************\n");
    System.out.println(url+"\n");
    String contents = IO.readURLcontentsWithException( url);
    System.out.println(contents);
  }

  private void saveRead(String url, String filename) throws IOException {
    System.out.println("****************\n");
    System.out.println("Read "+url+"\n");
    File file = new File(filename);
    String result = IO.readURLtoFile(url, file);
    System.out.println("Save to "+filename+" result = "+result+"\n");

    System.out.println("****************\n");
    //showRead(url);
  }




}
