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

  public void testLatlon() throws IOException {
    String dataset = server+"testWMS/cmor_pcmdi.nc";
    showGetCapabilities(dataset);
    getMap(dataset, "tos", "C:/temp/wmsLatlon.jpg");
  }

  public void testRot() throws IOException {
    String dataset = server+"testWMS/rotatedLatlon.grb";
    showGetCapabilities(dataset);
    getMap(dataset, "Geopotential_height", "C:/temp/wmsRot.jpg");
  }

  public void testGoogle() throws IOException {
    String g = "testWMS/rotatedlatlon.grb?VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG:4326&WIDTH=512&HEIGHT=512&LAYERS=Geopotential_height&STYLES=BOXFILL/alg&TRANSPARENT=TRUE&FORMAT=image/gif&BBOX=-135,35.34046249175859,135,82.1914946416798";
    saveRead(server+g, "C:/temp/wmsGoogle.gif");
  }


  private void showGetCapabilities(String url) throws IOException {
    showRead(url+"?request=GetCapabilities&version=1.3.0&service=WMS");
  }

  private void getMap(String url, String grid, String filename) throws IOException {
    String opts = "&STYLES=BOXFILL/ncview&";
    opts += "CRS=CRS:84&";
    opts += "BBOX=0,-90,360,90&";
    opts += "WIDTH=1000&";
    opts += "HEIGHT=500&";
    opts += "FORMAT=image/png&";
    //opts += "time=2001-08-14T00:00:00Z&";

    saveRead(url+"?request=GetMap&version=1.3.0&service=WMS&Layers="+grid+opts, filename);
  }

  private void getMap3(String url, String grid, String filename) throws IOException {
    String styles = "&STYLES=BOXFILL/redblue&";
    String srs = "CRS=CRS:84&";
    String bb = "BBOX=-100,30,-40,40&";
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
