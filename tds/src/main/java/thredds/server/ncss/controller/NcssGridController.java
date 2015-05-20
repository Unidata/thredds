/* Copyright */
package thredds.server.ncss.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import thredds.core.AllowedServices;
import thredds.core.StandardService;
import thredds.core.TdsRequestedDataset;
import thredds.server.exception.ServiceNotAllowed;
import thredds.server.ncss.exception.*;
import thredds.server.ncss.format.FormatsAvailabilityService;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import thredds.server.ncss.params.NcssGridParamsBean;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.util.Constants;
import thredds.util.ContentType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.ft2.coverage.grid.GridCoordAxis;
import ucar.nc2.ft2.coverage.grid.GridCoordSys;
import ucar.nc2.ft2.coverage.grid.GridCoverage;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;
import ucar.nc2.util.IO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Handles all Ncss Grid Requests
 * Validation done here, not in Responders
 *
 * @author caron
 * @since 4/29/2015
 */
@Controller
@RequestMapping("/ncss/grid")
public class NcssGridController extends AbstractNcssController {

  @Autowired
  private AllowedServices allowedServices;

  protected String getBase() {
    return StandardService.netcdfSubsetGrid.getBase();
  }

  @RequestMapping("**")
  public void handleRequest(HttpServletRequest req, HttpServletResponse res, @Valid NcssGridParamsBean params, BindingResult validationResult)
          throws BindException, IOException, ParseException, NcssException, InvalidRangeException {

    if (!allowedServices.isAllowed(StandardService.netcdfSubsetGrid))
      throw new ServiceNotAllowed(StandardService.netcdfSubsetGrid.toString());

    if (validationResult.hasErrors())
      throw new BindException(validationResult);

    String datasetPath = getDatasetPath(req);
    try (GridCoverageDataset gcd = TdsRequestedDataset.getGridCoverage(req, res, datasetPath)) {
      if (gcd == null) return;

      Formatter errs = new Formatter();
      if (!params.intersectsTime(gcd.getCalendarDateRange(), errs)) {
        handleValidationErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, errs.toString());
        return;
      }

      // throws exception if grid names not valid
      checkRequestedVars(gcd, params);

      if (!params.hasLatLonPoint()) {
        handleRequestGrid(res, params, datasetPath, gcd);
      } /* else {
        handleRequestGridAsPoint(res, params, datasetPath, gcd);
      } */
    }
  }

  private void handleRequestGrid(HttpServletResponse res, NcssGridParamsBean params, String datasetPath, GridCoverageDataset gcd)
          throws IOException, NcssException, ParseException, InvalidRangeException {

    // Supported formats are netcdf3 (default) and netcdf4 (if available)
    SupportedFormat sf = SupportedOperation.GRID_REQUEST.getSupportedFormat(params.getAccept());
    NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf3;
    if (sf.equals(SupportedFormat.NETCDF4)) {
      if (FormatsAvailabilityService.isFormatAvailable(SupportedFormat.NETCDF4)) {
        version = NetcdfFileWriter.Version.netcdf4;
      } else {
        handleValidationErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, "NetCDF-4 format not available");
        return;
      }
    }

    // all variables have to have the same vertical axis if a vertical coordinate was set.
    if (params.getVertCoord() != null && !checkVarsHaveSameVertAxis(gcd, params) ) {
        throw new NcssException("The variables requested: " + params.getVar() +
                " have different vertical levels. Grid requests with vertCoord must have variables with same vertical levels.");
      }

    GridResponder gds = new GridResponder(gcd, datasetPath, ncssDiskCache);
    File netcdfResult = gds.getResponseFile(params, version);

    // filename download attachment
    String suffix = (version == NetcdfFileWriter.Version.netcdf4) ? ".nc4" : ".nc";
    int pos = datasetPath.lastIndexOf("/");
    String filename = (pos >= 0) ? datasetPath.substring(pos + 1) : datasetPath;
    if (!filename.endsWith(suffix)) {
      filename += suffix;
    }

    // Headers...
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set(ContentType.HEADER, sf.getMimeType());
    httpHeaders.set(Constants.Content_Disposition, Constants.setContentDispositionValue(filename));
    setResponseHeaders(res, httpHeaders);

    IO.copyFileB(netcdfResult, res.getOutputStream(), 60000);
    res.flushBuffer();
    res.getOutputStream().close();
    res.setStatus(HttpServletResponse.SC_OK);
  }

  /* void handleRequestGridAsPoint(HttpServletResponse res, NcssParamsBean params, String datasetPath, GridCoverageDataset gcd)
          throws IOException, ParseException, InvalidRangeException, NcssException {

    // use the first grid
    String gridName;
    List<String> wantVars = params.getVar();
    if (wantVars.size() > 0) gridName = wantVars.get(0);
    else gridName = gcd.getGrids().get(0).getName();

		// Check if the requested point is within boundaries
    LatLonPoint latlon = new LatLonPointImpl(params.getLatitude(), params.getLongitude());
    if (!gcd.containsLatLonPoint(gridName, latlon)) {
			throw new OutOfBoundariesException("Requested Lat/Lon Point (+" + latlon + ") is not contained in the Data. "+
					"Data Bounding Box = " + gcd.getLatLonBoundingBox().toString2());
		}

    SupportedFormat format = SupportedOperation.POINT_REQUEST.getSupportedFormat(params.getAccept());
    GridAsPointResponder pds =  new GridAsPointResponder(gcd, params, ncssDiskCache, format, res.getOutputStream());
    setResponseHeaders(res, pds.getResponseHeaders(gcd, format, datasetPath));
    pds.respond(params);
  }  */

  @RequestMapping(value = {"**/dataset.html", "**/dataset.xml", "**/pointDataset.html", "**/pointDataset.xml"})
  public ModelAndView getDatasetDescription(HttpServletRequest req, HttpServletResponse res) throws IOException, NcssException {

    if (!allowedServices.isAllowed(StandardService.netcdfSubsetGrid))
      throw new ServiceNotAllowed(StandardService.netcdfSubsetGrid.toString());

    if (!req.getParameterMap().isEmpty())
      throw new NcssException("Invalid info request.");

    // the forms and dataset description
    String path = req.getServletPath();
    boolean wantXML = path.endsWith("/dataset.xml") || path.endsWith("/pointDataset.xml");
    boolean showPointForm = path.endsWith("/pointDataset.html");
    String datasetPath = getDatasetPath(req);

    try (GridCoverageDataset gcd = TdsRequestedDataset.getGridCoverage(req, res, datasetPath)) {
      if (gcd == null) return null; // restricted dataset
      return ncssShowDatasetInfo.showGridForm(gcd, buildDatasetUrl(datasetPath), wantXML, showPointForm);
    }
  }

  /* @RequestMapping("** /datasetBoundaries.xml")
  void getDatasetBoundaries(NcssParamsBean params, HttpServletRequest req, HttpServletResponse res) throws IOException, UnsupportedResponseFormatException {

    //Checking request format...
    SupportedFormat sf = getSupportedFormat(params, SupportedOperation.DATASET_BOUNDARIES_REQUEST);
    String datasetPath = getDatasetPath(req);

    try (GridCoverageDataset gcd = TdsRequestedDataset.getGridCoverage(req, res, datasetPath)) {
      if (gcd == null) return;

      String boundaries = getBoundaries(sf, gcd);

      res.setContentType(sf.getMimeType());
      res.getWriter().write(boundaries);
      res.getWriter().flush();
    }
  }

  private String getBoundaries(SupportedFormat format, GridCoverageDataset gcd) {

    String boundaries = "";
    GridBoundariesExtractor gbe = GridBoundariesExtractor.valueOf(gcd);

    if (format == SupportedFormat.WKT)
      boundaries = gbe.getDatasetBoundariesWKT();
    if (format == SupportedFormat.JSON)
      boundaries = gbe.getDatasetBoundariesGeoJSON();

    return boundaries;
  }  */

  protected SupportedFormat getSupportedFormat(NcssParamsBean params, SupportedOperation operation) throws UnsupportedResponseFormatException {

    //Checking request format...
    SupportedFormat sf;
    if (params.getAccept() == null) {
      //setting the default format
      sf = operation.getDefaultFormat();
      params.setAccept(sf.getFormatName());
    } else {
      sf = operation.getSupportedFormat(params.getAccept());
      if (sf == null) {
        operation.getSupportedFormat(params.getAccept());
        throw new UnsupportedResponseFormatException("Requested format: " + params.getAccept() + " is not supported for " + operation.getName().toLowerCase());
      }
    }

    return sf;
  }


  /**
   * Checks that all the requested vars exist. If "all", fills out the param.vars with all grid names
   * Throws exception if some of the variables in the request are not contained in the dataset
   */
  private void checkRequestedVars(GridCoverageDataset gcd, NcssGridParamsBean params) throws VariableNotContainedInDatasetException {

    // if var == all --> all variables requested
    if (params.getVar().get(0).equalsIgnoreCase("all")) {
      params.setVar(getAllGridNames(gcd));
      return;
    }

    // Check vars are contained in the grid
    for (String gridName : params.getVar()) {
      GridCoverage grid = gcd.findCoverage(gridName);
      if (grid == null)
        throw new VariableNotContainedInDatasetException("Variable: " + gridName + " is not contained in the requested dataset");
    }
  }

  private List<String> getAllGridNames(GridCoverageDataset gcd) {
    List<String> result = new ArrayList<>();
    for (GridCoverage var : gcd.getGrids())
      result.add(var.getName());
    return result;
  }

  /**
   * Returns true if all the variables have the same vertical axis (if they have an axis).
   * Could be broadened to allow all with same coordinate unites? coordinate value??
   */
  protected boolean checkVarsHaveSameVertAxis(GridCoverageDataset gcd, NcssGridParamsBean params) throws VariableNotContainedInDatasetException {
    String zaxisName = null;
    for (String gridName : params.getVar()) {
      GridCoverage grid = gcd.findCoverage(gridName);
      GridCoordSys gcs = gcd.findCoordSys(grid.getCoordSysName());
      GridCoordAxis zaxis = gcd.getZAxis(gcs);
      if (zaxis != null) {
        if (zaxisName == null)
          zaxisName = zaxis.getName();
        else if (!zaxisName.equals(zaxis.getName()))
          return false;
      }
    }
    return true;
  }

}
