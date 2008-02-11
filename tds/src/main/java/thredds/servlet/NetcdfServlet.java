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
package thredds.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Random;

import ucar.nc2.dataset.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.FileWriter;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.dt.fmrc.ForecastModelRunInventory;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.StringUtil;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.DataType;
import ucar.ma2.Array;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;

/**
 * Netcdf Grid subsetting.
 *
 * @deprecated - see thredds.server.ncss
 * @author caron
 */
public class NetcdfServlet extends AbstractServlet {
  private ucar.nc2.util.DiskCache2 diskCache = null;
  private boolean allow = false, deleteImmediately = true;

  // must end with "/"
  protected String getPath() {
    return "ncServer/";
  }

  protected void makeDebugActions() {
  }


  public void init() throws ServletException {
    super.init();

    /*   <NetcdfSubsetService>
    <allow>true</allow>
    <dir>/temp/ncache/</dir>
    <maxFileDownloadSize>1 Gb</maxFileDownloadSize>
  </NetcdfSubsetService> */

    allow = ThreddsConfig.getBoolean("NetcdfSubsetService.allow", false);
    long maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", (long) 1000 * 1000 * 1000);
    String cache = ThreddsConfig.get("NetcdfSubsetService.dir", contentPath + "/cache");
    File cacheDir = new File(cache);
    cacheDir.mkdirs();

    int scourSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.scour", 60 * 10);
    int maxAgeSecs = ThreddsConfig.getSeconds("NetcdfSubsetService.maxAge", -1);
    maxAgeSecs = Math.max(maxAgeSecs, 60 * 5);  // give at least 5 minutes to download before scouring kicks in.
    scourSecs = Math.max(scourSecs, 60 * 5);  // always need to scour, in case user doesnt get the file, we need to clean it up

    // LOOK: what happens if we are still downloading when the disk scour starts?
    diskCache = new DiskCache2(cache, false, maxAgeSecs / 60, scourSecs / 60);
  }

  public void destroy() {
    if (diskCache != null)
      diskCache.exit();
    super.destroy();
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    doGet(req, res);
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    ServletUtil.logServerAccessSetup(req);
    ServletUtil.showRequestDetail(this, req);

    String pathInfo = req.getPathInfo();


    // just echo back the request
    String showRequest = req.getParameter("showRequest");
    if (showRequest != null) {
      OutputStream out = res.getOutputStream();
      PrintStream ps = new PrintStream(out);

      ps.println("**NetcdfService req=" + pathInfo);
      ps.println(ServletUtil.showRequestDetail(this, req));
      ps.flush();
      return;
    }

    /// we have already created the file, now its going to get it
    if (pathInfo.startsWith("/cache/")) {
      pathInfo = pathInfo.substring(7);
      File ncFile = diskCache.getCacheFile(pathInfo);
      res.setHeader("Content-Disposition", "attachment; filename=");

      ServletUtil.returnFile(this, "", ncFile.getPath(), req, res, "application/x-netcdf");
      if (deleteImmediately)
        ncFile.delete();
      return;
    }

    // otherwise we are processing a new request to create a subset
    ForecastModelRunInventory fmr = null;
    try {
      GridDataset gds = DatasetHandler.openGridDataset(req, res, pathInfo);
      if (gds == null) return;
      
      fmr = ForecastModelRunInventory.open(gds, null);
      // the name must be just the last part of the pathInfo; it becomes the "action" == reletive URL in the form
      int pos = pathInfo.lastIndexOf("/");
      String name = (pos <= 1) ? pathInfo : pathInfo.substring(pos+1);
      fmr.setName(name);

      String wantXML = req.getParameter("wantXML");
      String showForm = req.getParameter("showForm");
      if (showForm != null) {
        try {
          showForm(res, fmr, wantXML != null);
        } catch (Exception e) {
          log.error("showForm", e);
          ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return;
      }

      // parse the parameters

      // allowed form: grid=gridName or grid=gridName;gridName;gridName;...
      List<String> varList = new ArrayList<String>();
      String[] vars = ServletUtil.getParameterValuesIgnoreCase(req, "grid");
      if (vars != null) {
        for (String var : vars) {
          StringTokenizer stoke = new StringTokenizer(var, ";");
          while (stoke.hasMoreTokens()) {
            String gridName = StringUtil.unescape(stoke.nextToken());
            varList.add(gridName);
          }
        }
      }

      // make sure all requested variables exist
      int count = 0;
      StringBuffer buff = new StringBuffer();
      for (String varName : varList) {
        if (null == gds.findGridDatatype(varName)) {
          buff.append(varName);
          if (count > 0) buff.append(";");
          count++;
        }
      }
      if (buff.length() != 0) {
        ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Grid(s) not found in dataset=" + buff);
        return;
      }

      // check for bounding box
      boolean hasBB = false;
      double north = 0.0, south = 0.0, west = 0.0, east = 0.0;
      String bb = ServletUtil.getParameterIgnoreCase(req, "bb");
      if (bb != null) {
        boolean err = false;
        StringTokenizer stoke = new StringTokenizer(bb, ",");
        if (stoke.countTokens() != 4)
          err = true;
        else try {
          north = Double.parseDouble(stoke.nextToken());
          south = Double.parseDouble(stoke.nextToken());
          west = Double.parseDouble(stoke.nextToken());
          east = Double.parseDouble(stoke.nextToken());
          hasBB = true;
        } catch (NumberFormatException e) {
          err = true;
        }
        if (err) {
          ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "BoundingBox parameter must be 'north,south,west,east'");
          return;
        }
      } else {
        String northS = ServletUtil.getParameterIgnoreCase(req, "north");
        String southS = ServletUtil.getParameterIgnoreCase(req, "south");
        String eastS = ServletUtil.getParameterIgnoreCase(req, "west");
        String westS = ServletUtil.getParameterIgnoreCase(req, "east");

        boolean haveSome = ((northS != null) && (northS.trim()).length() > 0) ||
                ((southS != null) && (southS.trim()).length() > 0) ||
                ((eastS != null) && (eastS.trim()).length() > 0) ||
                ((westS != null) && (westS.trim()).length() > 0);

        //if ya have one gotta have em all
        if (haveSome) {
          boolean err = false;
          boolean haveAll = ((northS != null) && (northS.trim()).length() > 0) &&
                  ((southS != null) && (southS.trim()).length() > 0) &&
                  ((eastS != null) && (eastS.trim()).length() > 0) &&
                  ((westS != null) && (westS.trim()).length() > 0);

          if (!haveAll) {
            err = true;

          } else try {
            north = Double.parseDouble(northS);
            south = Double.parseDouble(southS);
            west = Double.parseDouble(eastS);
            east = Double.parseDouble(westS);

            LatLonRect maxBB = fmr.getBB();
            if (null == maxBB)
              hasBB = true;
            else {
              hasBB = !ucar.nc2.util.Misc.closeEnough(north, maxBB.getUpperRightPoint().getLatitude()) ||
                      !ucar.nc2.util.Misc.closeEnough(south, maxBB.getLowerLeftPoint().getLatitude()) ||
                      !ucar.nc2.util.Misc.closeEnough(east, maxBB.getUpperRightPoint().getLongitude()) ||
                      !ucar.nc2.util.Misc.closeEnough(west, maxBB.getLowerLeftPoint().getLongitude());
            }
          } catch (NumberFormatException e) {
            err = true;
          }

          if (err) {
            ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must have valid north, south, west, east parameters");
            return;
          }
        }
      }
      LatLonRect llbb = hasBB ? new LatLonRect(new LatLonPointImpl(south, west), new LatLonPointImpl(north, east)) : null;

      // look for time range
      boolean hasTimeRange = false;
      double time_start = -1.0, time_end = -1.0;
      /*String time_range = ServletUtil.getParameterIgnoreCase(req, "time_range");
      if (time_range != null) {
        boolean err = false;
        StringTokenizer stoke = new StringTokenizer( bb, ",");
        if (stoke.countTokens() != 2)
          err = true;
        else {
          time_start = formatter.getISODate( stoke.nextToken());
          time_end = formatter.getISODate( stoke.nextToken());
          err = (time_start == null) || (time_end == null);
          hasTimeRange = true;
        }
        if (err) {
          ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "time_range must have start,end as valid ISO date strings");
          return;
        }
      } else {  */

      String startS = ServletUtil.getParameterIgnoreCase(req, "time_start");
      String endS = ServletUtil.getParameterIgnoreCase(req, "time_end");

      boolean haveSome = ((startS != null) && (startS.trim()).length() > 0) ||
              ((endS != null) && (endS.trim()).length() > 0);

      //if ya have one gotta have em all
      if (haveSome) {
        boolean err = false;
        boolean haveAll = ((startS != null) && (startS.trim()).length() > 0) &&
                ((endS != null) && (endS.trim()).length() > 0);

        if (!haveAll) {
          err = true;
        } else {
          time_start = Double.parseDouble(startS);
          time_end = Double.parseDouble(endS);
          hasTimeRange = true;
        }
        if (err) {
          ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must have time_start and time_end parameters as offsets in hours");
          return;
        }
      }

      // look for strides
      boolean hasStride = false;
      int stride_xy = -1;
      String s = ServletUtil.getParameterIgnoreCase(req, "stride_xy");
      if (s != null) {
        try {
          stride_xy = Integer.parseInt(s);
          hasStride = true;
        } catch (NumberFormatException e) {
          ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "stride_xy must have valid integer argument");
          return;
        }
      }
      int stride_z = -1;
      s = ServletUtil.getParameterIgnoreCase(req, "stride_z");
      if (s != null) {
        try {
          stride_z = Integer.parseInt(s);
          hasStride = true;
        } catch (NumberFormatException e) {
          ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "stride_z must have valid integer argument");
          return;
        }
      }
      int stride_time = -1;
      s = ServletUtil.getParameterIgnoreCase(req, "stride_time");
      if (s != null) {
        try {
          stride_time = Integer.parseInt(s);
          hasStride = true;
        } catch (NumberFormatException e) {
          ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "stride_time must have valid integer argument");
          return;
        }
      }

      boolean addLatLon = ServletUtil.getParameterIgnoreCase(req, "addLatLon") != null;

      try {
        sendFile(req, res, gds, varList, llbb, hasTimeRange, time_start, time_end, addLatLon, stride_xy, stride_z, stride_time);
      } catch (InvalidRangeException e) {
        ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Lat/Lon or Time Range");
      }

    } catch ( FileNotFoundException e) {
        ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, 0);
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");

    } catch ( IOException ioe) {
        ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, 0);
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Request");

    } finally {
      if (null != fmr)
        try {
          fmr.close();
        } catch (IOException ioe) {
          log.error("Failed to close = " + pathInfo);
        }
    }
  }

  private void sendFile(HttpServletRequest req, HttpServletResponse res, GridDataset gds, List gridList,
          LatLonRect llbb,
          boolean hasTime, double time_start, double time_end,
          boolean addLatLon,
          int stride_xy, int stride_z, int stride_time) throws IOException, InvalidRangeException {


    NetcdfDataset ncd = (NetcdfDataset) gds.getNetcdfFile();

    ArrayList varList = new ArrayList();
    ArrayList varNameList = new ArrayList();

    for (int i = 0; i < gridList.size(); i++) {

      String gridName = (String) gridList.get(i);
      if (varNameList.contains(gridName))
        continue;
      varNameList.add(gridName);

      GridDatatype grid = gds.findGridDatatype(gridName);
      GridCoordSystem gcsOrg = grid.getCoordinateSystem();

      Range timeRange = null;
      if (hasTime) {
        CoordinateAxis1D timeAxis = gcsOrg.getTimeAxis1D();
        int startIndex = timeAxis.findCoordElement(time_start);
        int endIndex = timeAxis.findCoordElement(time_end);
        timeRange = new Range(startIndex, endIndex);
      }

      if ((llbb != null) || (null != timeRange)) {
        grid = grid.makeSubset(timeRange, null, llbb, 1, 1, 1);
      }

      Variable gridV = (Variable) grid.getVariable();
      varList.add(gridV);

      GridCoordSystem gcs = grid.getCoordinateSystem();
      List axes = gcs.getCoordinateAxes();
      for (int j = 0; j < axes.size(); j++) {
        Variable axis = (Variable) axes.get(j);
        if (!varNameList.contains(axis.getName())) {
          varNameList.add(axis.getName());
          varList.add(axis);
        }
      }

      // looking for coordinate transform variables
      List ctList = gcs.getCoordinateTransforms();
      for (int j = 0; j < ctList.size(); j++) {
        CoordinateTransform ct = (CoordinateTransform) ctList.get(j);
        Variable v = ncd.findVariable(ct.getName()); // LOOK WRONG
        if (!varNameList.contains(ct.getName()) && (null != v)) {
          varNameList.add(ct.getName());
          varList.add(v);
        }
      }

      if (addLatLon) {
        Projection proj = gcs.getProjection();
        if ((null != proj) && !(proj instanceof LatLonProjection)) {
          addLatLon2D(ncd, varList, proj, gcs.getXHorizAxis(), gcs.getYHorizAxis());
          addLatLon = false;
        }
      }
    }

    String filename = req.getRequestURI();
    int pos = filename.lastIndexOf("/");
    filename = filename.substring(pos + 1);
    if (!filename.endsWith(".nc"))
      filename = filename + ".nc";

    Random random = new Random(System.currentTimeMillis());
    int randomInt = random.nextInt();

    String pathname = Integer.toString(randomInt) + "/" + filename;
    File ncFile = diskCache.getCacheFile(pathname);
    String cacheFilename = ncFile.getPath();

    String url = ServletUtil.getContextPath()+"/ncServer/cache/" + pathname;

    try {
      FileWriter writer = new FileWriter(cacheFilename, true);
      writer.writeVariables(varList);

      String location = gds.getNetcdfFile().getLocation();
      writer.writeGlobalAttribute(new Attribute("History", "GridDatatype extracted from dataset " + location));

      List gatts = ncd.getGlobalAttributes();
      for (int i = 0; i < gatts.size(); i++) {
        Attribute att = (Attribute) gatts.get(i);
        writer.writeGlobalAttribute(att);
      }

      //writer.writeGlobalAttribute( new Attribute("Convention", "_Coordinates"));
      writer.finish();
    } catch (IOException ioe) {
      log.error("Writing to " + cacheFilename, ioe);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
      return;
    }

    res.addHeader("Content-Location", url);
    res.setHeader("Content-Disposition", "attachment; filename="+filename);

    ServletUtil.returnFile(this, "", cacheFilename, req, res, "application/x-netcdf");
  }

  private void addLatLon2D(NetcdfFile ncfile, List varList, Projection proj, CoordinateAxis xaxis, CoordinateAxis yaxis) throws IOException {

    double[] xData = (double[]) xaxis.read().get1DJavaArray(double.class);
    double[] yData = (double[]) yaxis.read().get1DJavaArray(double.class);

    Variable latVar = new Variable(ncfile, null, null, "lat");
    latVar.setDataType(DataType.DOUBLE);
    latVar.setDimensions("y x");
    latVar.addAttribute(new Attribute("units", "degrees_north"));
    latVar.addAttribute(new Attribute("long_name", "latitude coordinate"));
    latVar.addAttribute(new Attribute("standard_name", "latitude"));
    latVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

    Variable lonVar = new Variable(ncfile, null, null, "lon");
    lonVar.setDataType(DataType.DOUBLE);
    lonVar.setDimensions("y x");
    lonVar.addAttribute(new Attribute("units", "degrees_east"));
    lonVar.addAttribute(new Attribute("long_name", "longitude coordinate"));
    lonVar.addAttribute(new Attribute("standard_name", "longitude"));
    lonVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

    int nx = xData.length;
    int ny = yData.length;

    // create the data
    ProjectionPointImpl projPoint = new ProjectionPointImpl();
    LatLonPointImpl latlonPoint = new LatLonPointImpl();
    double[] latData = new double[nx * ny];
    double[] lonData = new double[nx * ny];
    for (int i = 0; i < ny; i++) {
      for (int j = 0; j < nx; j++) {
        projPoint.setLocation(xData[j], yData[i]);
        proj.projToLatLon(projPoint, latlonPoint);
        latData[i * nx + j] = latlonPoint.getLatitude();
        lonData[i * nx + j] = latlonPoint.getLongitude();
      }
    }
    Array latDataArray = Array.factory(DataType.DOUBLE.getClassType(), new int []{ny, nx}, latData);
    latVar.setCachedData(latDataArray, false);

    Array lonDataArray = Array.factory(DataType.DOUBLE.getClassType(), new int []{ny, nx}, lonData);
    lonVar.setCachedData(lonDataArray, false);

    varList.add(latVar);
    varList.add(lonVar);
  }


  private void showForm(HttpServletResponse res, ForecastModelRunInventory fmr, boolean wantXml) throws IOException {
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

  private InputStream getXSLT(String xslName) {
    Class c = this.getClass();
    String resource = "/resources/xsl/" + xslName;
    InputStream is = c.getResourceAsStream(resource);
    if (null == is)
      log.error( "Cant load XSLT resource = " + resource);

    return is;
  }

}
