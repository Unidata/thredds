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
package thredds.server.ncstream;

import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.LastModified;
import org.springframework.web.servlet.ModelAndView;
import thredds.server.config.TdsContext;
import thredds.servlet.UsageLog;
import thredds.servlet.ServletUtil;
import thredds.servlet.DataRootHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.remote.PointStreamProto;
import ucar.nc2.ft.point.remote.PointStream;
import ucar.nc2.stream.NcStreamWriter;
import ucar.nc2.stream.NcStream;

import java.io.*;
import java.util.Formatter;
import java.util.List;

/**
 * @author caron
 * @since Feb 16, 2009
 */
public class PointStreamController extends AbstractController implements LastModified {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private TdsContext tdsContext;
  private boolean allow = true;
  //private String location = "R:\\testdata\\point\\netcdf\\971101.PAM_Atl_met.nc";
  private String location = "C:/data/ft/point/Compilation_eq.nc";
  private FeatureDatasetPoint fd;
  private PointFeatureCollection pfc;

  void init() {
    Formatter errlog = new Formatter();
    try {
      fd = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.POINT, location, null, errlog);
      List<FeatureCollection> coll =  fd.getPointFeatureCollectionList();
      pfc = (PointFeatureCollection) coll.get(0);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public void setTdsContext(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  public void setAllow( boolean allow) {
    this.allow = allow;
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return null;
    }

    if (pfc == null) init();

    log.info(UsageLog.setupRequestContext(req));

    String pathInfo = req.getPathInfo();
    System.out.println("req=" + pathInfo);

    String query = req.getQueryString();
    if (query != null) System.out.println(" query=" + query);

    res.setContentType("application/octet-stream");
    res.setHeader("Content-Description", "ncstream");

    NetcdfFile ncfile = null;
    try {

      OutputStream out = new BufferedOutputStream(res.getOutputStream(), 10 * 1000);
      if (query == null) { // just the header
        ncfile = fd.getNetcdfFile();
        if (ncfile == null) {
          res.setStatus(HttpServletResponse.SC_NOT_FOUND);
          ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, -1);
        }
        NcStreamWriter ncWriter = new NcStreamWriter(ncfile, ServletUtil.getRequest(req));
        ncWriter.sendHeader(out);

      } else { // they want some data

        int count = 0;
        PointFeatureIterator pfIter = pfc.getPointFeatureIterator(-1);
        while( pfIter.hasNext()) {
          PointFeature pf = pfIter.next();
          if (count == 0) {
            PointStreamProto.PointFeatureCollection pfc = PointStream.encodePointFeatureCollection(fd.getLocation(), pf);
            byte[] b = pfc.toByteArray();
            NcStream.writeVInt(out, b.length);
            out.write(b);
          }

          PointStreamProto.PointFeature pfp = PointStream.encodePointFeature(pf);
          byte[] b = pfp.toByteArray();
          NcStream.writeVInt(out, b.length);
          out.write(b);
          System.out.println(" count = "+count+" pf= "+pf.getLocation());
          count++;
        }
      }
      NcStream.writeVInt(out, 0);

      out.flush();
      res.flushBuffer();
      ServletUtil.logServerAccess(HttpServletResponse.SC_OK, -1);

    } catch (FileNotFoundException e) {
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, 0);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());

    } catch (Throwable e) {
      e.printStackTrace();
      ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

    } /* finally {
      if (null != ncfile)
        try {
          ncfile.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + pathInfo);
        }
    } */

    return null;
  }

  private void write(PointFeature pf, OutputStream out) {

  }

  public long getLastModified(HttpServletRequest req) {
    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(req.getPathInfo());
    if ((file != null) && file.exists())
      return file.lastModified();
    return -1;
  }

}
