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
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;

import ucar.nc2.NetcdfFile;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;

/**
 * Experimental "remote netcdf" data transfer protocol.
 * <pre>
 * Return NcML to populate the NetcdfFile:
 *   http://localhost:8080/thredds/ncremote/test/testData.nc
 *
 * Return binary data, using direct channel transfer to socket outputStream:
 *   http://localhost:8080/thredds/ncremote/test/testData.nc?Z_sfc(0:0,0:94,0:134)
 * </pre>
 *
 * At the moment its just a faster opendap. possible extensions:
 * <pre>
 * Index with coordinate values:
 *   http://localhost:8080/thredds/ncremote/test/testData.nc?Z_sfc(12.3,1000.0:1200.1,500.0:800.9)
 *   http://localhost:8080/thredds/ncremote/test/testData.nc?Z_sfc&alt=12.3m&lat=34.6:40.7&lon=66.6:69.9
 *
 * FeatureDatasets
 *   http://localhost:8080/thredds/netcdf/stream/test/testData.nc?lat=34.6:40.7&lon=66.6:69.9&featureType=pointData
 *   http://localhost:8080/thredds/netcdf/stream/test/testData.nc?var=sfc,P,T&lat=34.6:40.7&lon=66.6:69.9&featureType=pointData
 * </pre>
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

    boolean wantNull = false;
    OutputStream out = new BufferedOutputStream( res.getOutputStream(), 10 * 1000);

    WritableByteChannel wbc = null;
    if (pathInfo.startsWith("/stream")) {
      wbc = Channels.newChannel(out);
      pathInfo = pathInfo.substring(7);
    } else if (pathInfo.startsWith("/null")) {
      wantNull = true;
      pathInfo = pathInfo.substring(5);
    }

    if (wantNull) {
      byte[] b = new byte[1000];
      for (int i=0; i < 20 * 1000; i++) {
        out.write(b);
      }
      out.flush();
      return;
    }

    NetcdfFile ncfile = null;
    try {
      ncfile = DatasetHandler.getNetcdfFile(req, res, pathInfo);

      long length = 0;
      String query = req.getQueryString();

      // they just want the NcML
      if (query == null) {
        res.setContentType("text/xml");        

        ncfile.writeNcML(out, req.getRequestURI());
        ServletUtil.logServerAccess(HttpServletResponse.SC_OK, -1);
        return;
      }

      // otherwise it will be binary data
      res.setContentType("application/octet-stream");

      StringTokenizer stoke = new StringTokenizer(query, "&");
      while (stoke.hasMoreTokens()) {
        if (wbc != null) {
          ncfile.readToByteChannel(stoke.nextToken(), wbc);
        } else {
          Array result = ncfile.readSection(stoke.nextToken());
          length += copy2stream(result, out, false);
        }
      }
      out.flush();
      res.flushBuffer();
      ServletUtil.logServerAccess(HttpServletResponse.SC_OK, length);

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
