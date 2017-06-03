/*
 * Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

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
import thredds.server.config.ThreddsConfig;
import thredds.server.exception.RequestTooLargeException;
import thredds.server.exception.ServiceNotAllowed;
import thredds.server.ncss.exception.*;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import thredds.server.ncss.params.NcssGridParamsBean;
import thredds.server.ncss.view.dsg.DsgSubsetWriter;
import thredds.server.ncss.view.dsg.DsgSubsetWriterFactory;
import thredds.util.Constants;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter2;
import ucar.nc2.ft2.coverage.writer.CoverageAsPoint;
import ucar.nc2.util.IO;
import ucar.nc2.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Random;

/**
 * Handles NCSS Grid Requests
 * Validation done here.
 *
 * @author caron
 * @since 4/29/2015
 */
@Controller
@RequestMapping("/ncss/grid")
public class NcssGridController extends AbstractNcssController {
  static private final short ESTIMATED_COMPRESION_RATE = 4;  // Compression rate used to estimate the filesize of netcdf4 compressed files

  @Autowired
  private AllowedServices allowedServices;

  protected String getBase() {
    return StandardService.netcdfSubsetGrid.getBase();
  }

  @RequestMapping("**")     // data request
  public void handleRequest(HttpServletRequest req, HttpServletResponse res, @Valid NcssGridParamsBean params, BindingResult validationResult)
          throws Exception {

    if (!allowedServices.isAllowed(StandardService.netcdfSubsetGrid))
      throw new ServiceNotAllowed(StandardService.netcdfSubsetGrid.toString());

    if (validationResult.hasErrors())
      throw new BindException(validationResult);

    String datasetPath = getDatasetPath(req);
    try (CoverageCollection gcd = TdsRequestedDataset.getCoverageCollection(req, res, datasetPath)) {
      if (gcd == null) return;

      Formatter errs = new Formatter();
      if (!params.intersectsTime(gcd.getCalendarDateRange(), errs)) {
        handleValidationErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, errs.toString());
        return;
      }

      // throws exception if grid names not valid
      checkRequestedVars(gcd, params);

      if (params.hasLatLonPoint()) {
        handleRequestGridAsPoint(res, params, datasetPath, gcd);
      } else {
        handleRequestGrid(res, params, datasetPath, gcd);
      }
    }
  }

  private void handleRequestGrid(HttpServletResponse res, NcssGridParamsBean params, String datasetPath, CoverageCollection gcd)
          throws IOException, NcssException, ParseException, InvalidRangeException {

    // Supported formats are netcdf3 (default) and netcdf4 (if available)
    SupportedFormat sf = SupportedOperation.GRID_REQUEST.getSupportedFormat(params.getAccept());
    NetcdfFileWriter.Version version = (sf == SupportedFormat.NETCDF3) ? NetcdfFileWriter.Version.netcdf3 : NetcdfFileWriter.Version.netcdf4;

    // all variables have to have the same vertical axis if a vertical coordinate was set. LOOK can we relax this ?
    if (params.getVertCoord() != null && !checkVarsHaveSameVertAxis(gcd, params) ) {
        throw new NcssException("The variables requested: " + params.getVar() +
                " have different vertical levels. Grid requests with vertCoord must have variables with same vertical levels.");
      }

    String responseFile = getResponseFileName(datasetPath, version);
    File netcdfResult = makeCFNetcdfFile(gcd, responseFile, params, version);

    // filename download attachment
    String suffix = version.getSuffix();
    int pos = datasetPath.lastIndexOf("/");
    String filename = (pos >= 0) ? datasetPath.substring(pos + 1) : datasetPath;
    if (!filename.endsWith(suffix)) {
      filename += suffix;
    }

    // Headers...
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set(ContentType.HEADER, sf.getMimeType());
    httpHeaders.set(Constants.Content_Disposition, Constants.setContentDispositionValue(filename));

    // set content length
    httpHeaders.set(Constants.Content_Length, Constants.getContentLengthValue(netcdfResult));

    setResponseHeaders(res, httpHeaders);

    IO.copyFileB(netcdfResult, res.getOutputStream(), 60000);
    res.flushBuffer();
    res.getOutputStream().close();
    res.setStatus(HttpServletResponse.SC_OK);
  }

  File makeCFNetcdfFile(CoverageCollection gcd, String responseFilename, NcssGridParamsBean params, NetcdfFileWriter.Version version)
          throws NcssException, InvalidRangeException, ParseException, IOException {

    NetcdfFileWriter writer = NetcdfFileWriter.createNew(version, responseFilename, null); // default chunking - let user control at some point
    SubsetParams subset = params.makeSubset(gcd);

    // Test maxFileDownloadSize
    long maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", -1L);
    if (maxFileDownloadSize > 0) {
      Optional<Long> estimatedSizeo = CFGridCoverageWriter2.writeOrTestSize(gcd, params.getVar(), subset, params.isAddLatLon(), true, writer);
      if (!estimatedSizeo.isPresent())
        throw new InvalidRangeException("Request contains no data: " + estimatedSizeo.getErrorMessage());

      long estimatedSize = estimatedSizeo.get();
      if (version == NetcdfFileWriter.Version.netcdf4)
        estimatedSize /= ESTIMATED_COMPRESION_RATE;

      if (estimatedSize > maxFileDownloadSize)
        throw new RequestTooLargeException("NCSS response too large = " + estimatedSize + " max = " + maxFileDownloadSize);
    }

    // write the file
    Optional<Long> estimatedSizeo = CFGridCoverageWriter2.writeOrTestSize(gcd, params.getVar(), subset, params.isAddLatLon(), false, writer);
    if (!estimatedSizeo.isPresent())
      throw new InvalidRangeException("Request contains no data: " + estimatedSizeo.getErrorMessage());

    return new File(responseFilename);
  }

  private String getResponseFileName(String requestPathInfo, NetcdfFileWriter.Version version) {
    Random random = new Random(System.currentTimeMillis());
    int randomInt = random.nextInt();

    String filename = TdsPathUtils.getFileNameForResponse(requestPathInfo, version);
    String pathname = Integer.toString(randomInt) + "/" + filename;
    File ncFile = ncssDiskCache.getDiskCache().getCacheFile(pathname);
    if (ncFile == null)
      throw new IllegalStateException("NCSS misconfigured cache");
    return ncFile.getPath();
  }

  ///////////////////////////////////////////////////////////////

  private void handleRequestGridAsPoint(HttpServletResponse res, NcssGridParamsBean params, String datasetPath, CoverageCollection gcd)
          throws Exception {

    SupportedFormat sf = SupportedOperation.POINT_REQUEST.getSupportedFormat(params.getAccept());

    CoverageAsPoint covp = new CoverageAsPoint(gcd, params.getVar(), params.makeSubset(gcd));
    try (FeatureDatasetPoint fd = covp.asFeatureDatasetPoint()) {

      // all subsetting is done in CoverageAsPoint
      //SubsetParams ncssParams = params.makeSubset(gcd);
      SubsetParams ncssParams = new SubsetParams()
              .set(SubsetParams.timeAll, true)
              .set(SubsetParams.variables, params.getVar());
      DsgSubsetWriter pds = DsgSubsetWriterFactory.newInstance(fd, ncssParams, ncssDiskCache, res.getOutputStream(), sf);
      setResponseHeaders(res, pds.getHttpHeaders(datasetPath, sf.isStream()));
      pds.respond(res, fd, datasetPath, ncssParams, sf);
    }
  }

    /*
    if (sf.isStream()) {
      GridResponder responder = new GridResponder(gcd, "");
      responder.streamGridAsPointResponse(res.getOutputStream(), params, sf);

    } else {
      NetcdfFileWriter.Version version = (sf == SupportedFormat.NETCDF3) ? NetcdfFileWriter.Version.netcdf3 : NetcdfFileWriter.Version.netcdf4;
      String responseFile = getResponseFileName(datasetPath, version);

      GridResponder responder = new GridResponder(gcd, responseFile);
      File netcdfResult = responder.makeDSGnetcdfFile(params, version);

      // filename download attachment
      String suffix = version.getSuffix();
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
  } */

  ///////////////////////////////////////////////////////////

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

    try (CoverageCollection gcd = TdsRequestedDataset.getCoverageCollection(req, res, datasetPath)) {
      if (gcd == null) return null; // restricted dataset
      return ncssShowDatasetInfo.showGridFormTh(gcd, buildDatasetUrl(datasetPath), wantXML);
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


  /**
   * Checks that all the requested vars exist. If "all", fills out the param.vars with all grid names
   * Throws exception if some of the variables in the request are not contained in the dataset
   */
  private void checkRequestedVars(CoverageCollection gcd, NcssGridParamsBean params) throws VariableNotContainedInDatasetException {

    // if var == all --> all variables requested
    if (params.getVar().get(0).equalsIgnoreCase("all")) {
      params.setVar(getAllGridNames(gcd));
      return;
    }

    // Check vars are contained in the grid
    for (String gridName : params.getVar()) {
      Coverage grid = gcd.findCoverage(gridName);
      if (grid == null)
        throw new VariableNotContainedInDatasetException("Variable: " + gridName + " is not contained in the requested dataset");
    }
  }

  private List<String> getAllGridNames(CoverageCollection gcd) {
    List<String> result = new ArrayList<>();
    for (Coverage var : gcd.getCoverages())
      result.add(var.getName());
    return result;
  }

  /**
   * Returns true if all the variables have the same vertical axis (if they have an axis).
   * Could be broadened to allow all with same coordinate unites? coordinate value??
   */
  protected boolean checkVarsHaveSameVertAxis(CoverageCollection gcd, NcssGridParamsBean params) throws VariableNotContainedInDatasetException {
    String zaxisName = null;
    for (String gridName : params.getVar()) {
      Coverage grid = gcd.findCoverage(gridName);
      CoverageCoordAxis zaxis = grid.getCoordSys().getZAxis();
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
