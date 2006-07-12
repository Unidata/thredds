// $Id$
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
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Random;

import ucar.nc2.dataset.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.FileWriter;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.dt.grid.ForecastModelRun;
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
 * @author caron
 * @version $Revision$ $Date$
 */
public class NetcdfServlet extends AbstractServlet {
  private ucar.nc2.util.DiskCache2 fmrCache = null;
  private boolean debug = false;

  // must end with "/"
  protected String getPath() { return "ncServer/"; }

  protected void makeDebugActions() { }


  public void init() throws ServletException {
    super.init();

    String cache = ServletParams.getInitParameter("NetcdfServletCachePath", contentPath + "/cache");

    // cache the fmr inventory xml: keep for 1 day, scour once a day */
    fmrCache = new DiskCache2(cache, false, 60 * 24, 60 * 24);
    //fmrCache.setCachePathPolicy( DiskCache2.CACHEPATH_POLICY_NESTED_TRUNCATE, "grid/");
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

    String pathInfo = req.getPathInfo();

    if (pathInfo.startsWith("/cache/")) {
      pathInfo = pathInfo.substring(7);
      File ncFile = fmrCache.getCacheFile(pathInfo);
      ServletUtil.returnFile(this, "", ncFile.getPath(), req, res, "application/x-netcdf");
      return;
    }

    // dorky thing to get the transferred file to end in ".nc"
    // LOOK: problem when it does end in nc !!
    if (pathInfo.endsWith(".nc"))
       pathInfo = pathInfo.substring(0,pathInfo.length()-3);

    String datasetPath = DataRootHandler2.getInstance().translatePath( pathInfo );
    // @todo Should instead use ((CrawlableDatasetFile)catHandler2.findRequestedDataset( path )).getFile();
    if (datasetPath == null) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    log.debug("**NetcdfService req="+datasetPath);

    // for convenince, we open as an FMR, since it already has the XML we need for the form
    ForecastModelRun fmr = ForecastModelRun.open( fmrCache, datasetPath, ForecastModelRun.OPEN_NORMAL);
    fmr.setName( req.getRequestURI());

    String wantXML = req.getParameter("wantXML");
    String showForm = req.getParameter("showForm");
    if (showForm != null) {
      try {
        showForm( res, fmr, wantXML != null);
      } catch (Exception e) {
        log.error("showForm", e);
        ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      return;
    }

    // heres where we process the request for the netcdf subset
    GridDataset gds = DatasetHandler.openGridDataset( pathInfo);

    String showRequest = req.getParameter("showRequest");
    if (showRequest != null) {
      OutputStream out = res.getOutputStream();
      PrintStream ps = new PrintStream( out);

      ps.println("**NetcdfService req="+datasetPath);
      ps.println(ServletUtil.showRequestDetail( this, req ));
      ps.flush();
      return;
    }

    // allowed form: grid=gridName or grid=gridName;gridName;gridName;...
    ArrayList varList = new ArrayList();
    String[] vars = ServletUtil.getParameterValuesIgnoreCase(req, "grid");
    if (vars != null) {
      for (int i = 0; i < vars.length; i++) {
        StringTokenizer stoke = new StringTokenizer( vars[i], ";");
        while (stoke.hasMoreTokens()) {
          String gridName = StringUtil.unescape( stoke.nextToken());
          varList.add(gridName);
        }
      }
    }

    // make sure all requested variables exist
    int count = 0;
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < varList.size(); i++) {
      String varName = (String) varList.get(i);
      if (null == gds.findGridDatatype(varName)) {
        buff.append(varName);
        if (count > 0) buff.append(";");
        count++;
      }
    }
    if (buff.length() != 0) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Grid(s) not found in dataset="+buff);
      return;
    }

    // check for bounding box
    boolean hasBB = false;
    double north=0.0, south=0.0, west=0.0, east=0.0;
    String bb = ServletUtil.getParameterIgnoreCase(req, "bb");
    if (bb != null) {
      boolean err = false;
      StringTokenizer stoke = new StringTokenizer( bb, ",");
      if (stoke.countTokens() != 4)
        err = true;
      else try {
        north = Double.parseDouble( stoke.nextToken());
        south = Double.parseDouble( stoke.nextToken());
        west = Double.parseDouble( stoke.nextToken());
        east = Double.parseDouble( stoke.nextToken());
        hasBB = true;
      } catch (NumberFormatException e) {
        err = true;
      }
      if (err) {
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
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
          north = Double.parseDouble( northS);
          south = Double.parseDouble( southS);
          west = Double.parseDouble( eastS);
          east = Double.parseDouble( westS);

          LatLonRect maxBB = fmr.getBB();
          if (null == maxBB)
            hasBB = true;
          else {
            hasBB = !closeEnough( north, maxBB.getUpperRightPoint().getLatitude()) ||
                    !closeEnough( south, maxBB.getLowerLeftPoint().getLatitude()) ||
                    !closeEnough( east, maxBB.getUpperRightPoint().getLongitude()) ||
                    !closeEnough( west, maxBB.getLowerLeftPoint().getLongitude());
          }
        } catch (NumberFormatException e) {
          err = true;
        }

        if (err) {
          ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must have valid north, south, west, east parameters");
          return;
        }
      }
    }
    LatLonRect llbb = hasBB ? new LatLonRect( new LatLonPointImpl(south, west), new LatLonPointImpl(north, east)) : null;

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
        time_start = Double.parseDouble( startS);
        time_end = Double.parseDouble( endS);
        hasTimeRange = true;
      }
      if (err) {
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
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
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
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
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
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
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "stride_time must have valid integer argument");
        return;
      }
    }

    /* they want the entire file
    // LOOK: conversion to CF
    if ((varList.size() == 0) && !hasBB && !hasTimeRange && !hasStride) {
      File result = new File(datasetPath);
      if (!ncfileIn.isNetcdf3FileFormat()) {
        String fileOut = "C:/temp/"+result.getName();
        result = new File(fileOut);
        if (result.exists())
          result.delete();

        NetcdfFile ncfileOut = FileWriter.writeToFile(ncfileIn, fileOut);
        ncfileOut.close();
      }
      ncfileIn.close();

      ServletUtil.returnFile(this, req, res, result, "application/x-netcdf");
      return;

    }   */

    boolean addLatLon = ServletUtil.getParameterIgnoreCase(req, "addLatLon") != null;

    try {
      sendFile(req, res, gds, varList, llbb, hasTimeRange, time_start, time_end, addLatLon, stride_xy, stride_z, stride_time);
    } catch (InvalidRangeException e) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, 0 );
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Lat/Lon Range");
      return;
    }
  }

  private boolean closeEnough( double d1, double d2) {
    if (d1 < 1.0e-5) return Math.abs(d1-d2) < 1.0e-5;
    return Math.abs((d1-d2)/d1) < 1.0e-5;
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
      varNameList.add( gridName);

      GridDatatype grid = gds.findGridDatatype(gridName);
      GridCoordSystem gcsOrg = grid.getGridCoordSystem();

      Range timeRange = null;
      if (hasTime) {
        CoordinateAxis1D timeAxis = gcsOrg.getTimeAxis();
        int startIndex = timeAxis.findCoordElement(time_start);
        int endIndex = timeAxis.findCoordElement(time_end);
        timeRange = new Range(startIndex, endIndex);
      }

      if ((llbb != null) || (null != timeRange)) {
        grid = grid.makeSubset(timeRange, null, llbb, 1, 1, 1);
      }

      Variable gridV = (Variable) grid.getVariable();
      varList.add( gridV);

      GridCoordSystem gcs = grid.getGridCoordSystem();
      List axes = gcs.getCoordinateAxes();
      for (int j = 0; j < axes.size(); j++) {
        Variable axis = (Variable) axes.get(j);
        if (!varNameList.contains(axis.getName())) {
          varNameList.add( axis.getName());
          varList.add(axis);
        }
      }

      // looking for coordinate transform variables
      List ctList = gcs.getCoordinateTransforms();
      for (int j = 0; j < ctList.size(); j++) {
        CoordinateTransform ct = (CoordinateTransform) ctList.get(j);
        Variable v = ncd.findVariable( ct.getName());
        if (!varNameList.contains(ct.getName()) && (null != v)) {
          varNameList.add( ct.getName());
          varList.add(v);
        }
      }

      if (addLatLon) {
        Projection proj = gcs.getProjection();
        if ((null != proj) && !(proj instanceof LatLonProjection)) {
          addLatLon2D(ncd, varList, proj, gcs.getXHorizAxis() , gcs.getYHorizAxis());
          addLatLon = false;
        }
      }
    }

    String path = req.getRequestURI();
    int pos = path.lastIndexOf("/");
    path = path.substring(pos+1);
    Random random = new Random( System.currentTimeMillis());
    int randomInt = random.nextInt();

    String filename = Integer.toString(randomInt) + "/" + path;
    File ncFile = fmrCache.getCacheFile(filename);
    String cacheFilename = ncFile.getPath();

    String url = "/thredds/ncServer/cache/" + filename;

    try {
      FileWriter writer = new FileWriter( cacheFilename, true);
      writer.writeVariables( varList);

      String location = gds.getNetcdfFile().getLocation();
      writer.writeGlobalAttribute( new Attribute("History", "GridDatatype extracted from dataset "+location));

      List gatts = ncd.getGlobalAttributes();
      for (int i = 0; i < gatts.size(); i++) {
        Attribute att = (Attribute) gatts.get(i);
        writer.writeGlobalAttribute( att);
      }

      //writer.writeGlobalAttribute( new Attribute("Convention", "_Coordinates"));
      writer.finish();
    } catch (IOException ioe) {
      log.error("Writing to "+cacheFilename, ioe);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
      return;
    }

    res.addHeader("Content-Location", url);
    //res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);

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
    latVar.addAttribute(new Attribute("_CoordinateAxisType", AxisType.Lat.toString()));

    Variable lonVar = new Variable(ncfile, null, null, "lon");
    lonVar.setDataType(DataType.DOUBLE);
    lonVar.setDimensions("y x");
    lonVar.addAttribute(new Attribute("units", "degrees_east"));
    lonVar.addAttribute(new Attribute("long_name", "longitude coordinate"));
    lonVar.addAttribute(new Attribute("standard_name", "longitude"));
    lonVar.addAttribute(new Attribute("_CoordinateAxisType", AxisType.Lon.toString()));

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
