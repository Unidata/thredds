/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package thredds.tdr;

import ucar.nc2.util.IO;

import java.io.OutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestTdrSimple {
  String server = "";
  public void add(String urlString, OutputStream out, int bufferSize) throws IOException {
    URL url;
    java.io.InputStream is = null;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      throw new IOException("** MalformedURLException on URL <" + urlString + ">\n" + e.getMessage() + "\n");
    }

    try {
      java.net.URLConnection connection = url.openConnection();

      if (connection instanceof HttpURLConnection) {
        java.net.HttpURLConnection httpConnection = (HttpURLConnection) connection;
        // check response code is good
        int responseCode = httpConnection.getResponseCode();
        if (responseCode / 100 != 2)
          throw new IOException("** Cant open URL <" + urlString + ">\n Response code = " + responseCode
                  + "\n" + httpConnection.getResponseMessage() + "\n");
      }

      // read it
      is = connection.getInputStream();
      IO.copyB(is, out, bufferSize);

    } catch (java.net.ConnectException e) {
      throw new IOException("** ConnectException on URL: <" + urlString + ">\n" +
              e.getMessage() + "\nServer probably not running");

    } finally {
      if (is != null) is.close();
    }
  }

  public static void main(String args[]) {
    
  }


}
