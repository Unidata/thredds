// $Id: StationObsCollectionServlet.java 51 2006-07-12 17:13:13Z caron $
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

import ucar.nc2.util.DiskCache2;
import ucar.nc2.dt.grid.ForecastModelRun;
import ucar.nc2.dt.point.MetarCollection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Apr 1, 2006
 * Time: 10:38:58 PM
 * To change this template use File | Settings | File Templates.
 */
  public class StationObsCollectionServlet extends AbstractServlet {
    private ucar.nc2.util.DiskCache2 fmrCache = null;
    private boolean debug = false;

    // must end with "/"
    protected String getPath() { return "obsServer/"; }

    protected void makeDebugActions() { }


    public void init() throws ServletException {
      super.init();

      // cache the fmr inventory xml: keep for 10 days, scour once a day */
      fmrCache = new DiskCache2(contentPath+"/cache", false, 60 * 24 * 10, 60 * 24);
      fmrCache.setCachePathPolicy( DiskCache2.CACHEPATH_POLICY_NESTED_TRUNCATE, "grid/");
    }

    public void destroy() {
      if (fmrCache != null)
        fmrCache.exit();
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
       doGet( req, res);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

      ServletUtil.logServerAccessSetup( req );
      ServletUtil.showRequestDetail( this, req );

      log.debug("**StationObsCollectionServlet req=" + ServletUtil.getRequest(req));
      // log.debug( ServletUtil.showRequestDetail(this, req));

      String datasetPath = DataRootHandler.getInstance().translatePath( req );
      // @todo Should instead use ((CrawlableDatasetFile)catHandler.findRequestedDataset( path )).getFile();
      if (datasetPath == null) {
        ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      log.debug("**StationObsCollectionServlet req="+datasetPath);

      String path = req.getRequestURI();
      MetarCollection dataset = new MetarCollection( datasetPath);

      ParamParser pp = new ParamParser();

      pp.parseNames(req, "stn");
      pp.parseBB(req, null);
      pp.parseDateRange(req, true);

      if (pp.errMessage != null) {
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, pp.errMessage.toString());
        return;
      }

      StringBuffer sbuff = new StringBuffer();
      try {
        int count;
        if (pp.nameList.size() > 0)
          count = dataset.extract(pp.nameList, pp.date_start, pp.date_end, "report", sbuff);
        else if (pp.llbb != null)
          count = dataset.extract(pp.llbb, pp.date_start, pp.date_end, "report", sbuff);
        else {
          ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "must have Station or Bounding Box paramaters");
          return;
        }

        OutputStream out = res.getOutputStream();
        if (count == 0)
          out.write("No records were found".getBytes());
        else
          out.write(sbuff.toString().getBytes());
        out.flush();

        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, sbuff.length());

      } catch (Exception e) {
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        return;
      }
    }

    private void showForm(HttpServletResponse res, ForecastModelRun fmr, boolean wantXml) throws IOException {
      String infoString;

      if (wantXml) {
        infoString = fmr.writeXML();

      } else {
        InputStream xslt = getXSLT("ncServerForm.xsl");
        Document doc = fmr.writeDocument();

        try {
          XSLTransformer transformer = new XSLTransformer(xslt);
          Document html = transformer.transform(doc);
          XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
          infoString = fmt.outputString(html);

        } catch (Exception e) {
          log.error("ForecastModelRunServlet internal error", e);
          ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ForecastModelRunServlet internal error");
          return;
        }
      }

      res.setContentLength(infoString.length());
      if (wantXml)
        res.setContentType("text/xml; charset=iso-8859-1");
      else
        res.setContentType("text/html; charset=iso-8859-1");

      OutputStream out = res.getOutputStream();
      out.write(infoString.getBytes());
      out.flush();

      ServletUtil.logServerAccess(HttpServletResponse.SC_OK, infoString.length());
    }

    static private InputStream getXSLT(String xslName) {
      Class c = ForecastModelRunServlet.class;
      return c.getResourceAsStream("/resources/thredds/xsl/" + xslName);
    }

  }

