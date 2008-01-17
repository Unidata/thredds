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
package thredds.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.StringTokenizer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ucar.nc2.NetcdfFile;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;

/**
 * Experimental testing.
 *
 * @author caron
 */
public class RemoteNetcdfServlet extends AbstractServlet {
  private boolean allow = true;

  // must end with "/"
  protected String getPath() {
    return "netcdf/";
  }

  protected void makeDebugActions() {
  }


  public void init() throws ServletException {
    super.init();
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    ServletUtil.logServerAccessSetup(req);
    ServletUtil.showRequestDetail(this, req);

    String pathInfo = req.getPathInfo();

    NetcdfFile ncfile = null;
    try {
      ncfile = DatasetHandler.getNetcdfFile(req, res);

      long length = 0;
      String query = req.getQueryString();
      StringTokenizer stoke = new StringTokenizer(query, ",");
      while (stoke.hasMoreTokens()) {
        Array result = ncfile.read(stoke.nextToken(), false);
        length += copy2stream(result, res.getOutputStream(), true);
      }
      System.out.println(query + ": length = " + length);


    } catch (InvalidRangeException e) {
      ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Lat/Lon or Time Range");

    } finally {
      if (null != ncfile)
        try {
          ncfile.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + pathInfo);
        }
    }
  }

  private long copy2stream(Array a, OutputStream out, boolean digest) throws IOException {
    int elem = 0;
    long size = a.getSize();
    DataOutputStream dout = null;

    MessageDigest md = null;
    try {
      OutputStream bout = new BufferedOutputStream(out);

      if (digest) {
        md = MessageDigest.getInstance("MD2");
        bout = new DigestOutputStream(out, md);
      }
      dout = new DataOutputStream(bout);

      dout.writeLong(size);

      if (a.getElementType() == double.class) {
        elem = 8;
        double[] data = (double[]) a.get1DJavaArray(double.class);
        for (int i = 0; i < data.length; i++)
          dout.writeDouble(data[i]);

      } else if (a.getElementType() == float.class) {
        elem = 4;
        float[] data = (float[]) a.get1DJavaArray(float.class);
        for (int i = 0; i < data.length; i++)
          dout.writeFloat(data[i]);
      }

      if (digest) {
        byte[] result = md.digest();
        System.out.print("digest=");
        for (int i = 0; i < result.length; i++) {
          dout.write(result[i]);
          if (i>0)System.out.print(',');
          System.out.print(result[i]);
        }
        System.out.println();
      }

    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } finally {
      dout.flush();
    }


    return a.getSize() * elem;
  }


}
