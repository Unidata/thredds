/*
 * Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */
package thredds.server.ncss.controller;

import org.jdom2.Document;
import org.jdom2.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
import thredds.server.ncss.params.NcssParamsBean;
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
import ucar.nc2.ft2.coverage.writer.CoverageDatasetCapabilities;
import ucar.nc2.ft2.coverage.writer.CoverageSubsetter2;
import ucar.nc2.util.IO;
import ucar.nc2.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
  // Compression rate used to estimate the filesize of netcdf4 compressed files
  static private final short ESTIMATED_COMPRESION_RATE = 4;

  @Autowired
  private AllowedServices allowedServices;

  protected String getBase() {
    return StandardService.netcdfSubsetGrid.getBase();
  }

  @RequestMapping("**")     // data request
  public void handleRequest(HttpServletRequest req, HttpServletResponse res, @Valid NcssGridParamsBean params,
          BindingResult validationResult) throws Exception {
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

  private void handleRequestGrid(HttpServletResponse res, NcssGridParamsBean params, String datasetPath,
          CoverageCollection gcd) throws IOException, NcssException, InvalidRangeException {
    // Supported formats are netcdf3 (default) and netcdf4 (if available)
    SupportedFormat sf = SupportedOperation.GRID_REQUEST.getSupportedFormat(params.getAccept());
    NetcdfFileWriter.Version version =
            (sf == SupportedFormat.NETCDF3) ? NetcdfFileWriter.Version.netcdf3 : NetcdfFileWriter.Version.netcdf4;

    // all variables have to have the same vertical axis if a vertical coordinate was set. LOOK can we relax this ?
    if (params.getVertCoord() != null && !checkVarsHaveSameVertAxis(gcd, params) ) {
        throw new NcssException("The variables requested: " + params.getVar() + " have different vertical levels. " +
                "Grid requests with vertCoord must have variables with same vertical levels.");
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

  /*
  hvandam   Added support for wrf June 2019
*/
  @RequestMapping(value = "/**", method = RequestMethod.GET, params = "accept=wrf")
  public void handleDataRequest(HttpServletRequest req, HttpServletResponse res,
                                @Valid NcssGridParamsBean params, BindingResult validationResult)
          throws IOException, BindException, InvalidRangeException {

    if (!allowedServices.isAllowed(StandardService.netcdfSubsetGrid))
      throw new ServiceNotAllowed(StandardService.netcdfSubsetGrid.toString());

    if (validationResult.hasErrors())
      throw new BindException(validationResult);

    String datasetPath = getDatasetPath(req);
    //NetcdfFile ncfile = TdsRequestedDataset.getNetcdfFile(req, res, datasetPath);

    try (CoverageCollection gcd = TdsRequestedDataset.getCoverageCollection(req, res, datasetPath)) {
      if (gcd == null) return;

      Formatter errs = new Formatter();
      if (!params.intersectsTime(gcd.getCalendarDateRange(), errs)) {
        handleValidationErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, errs.toString());
        return;
      }

      // throws exception if grid names not valid
      checkRequestedVars(gcd, params);
      SubsetParams subsetParams = params.makeSubset(gcd);

      // We need global attributes, subsetted axes, transforms, and the coverages with attributes and referencing
      // subsetted axes.
      Optional<CoverageCollection> opt = CoverageSubsetter2.makeCoverageDatasetSubset(gcd, params.getVar(), subsetParams);
      //if (!opt.isPresent())
      // set an an error message and return
      // ucar.nc2.util.Optional.empty(opt.getErrorMessage());

      CoverageCollection subsetDataset = opt.get();

      //  call the wrf file writer
/**********************
 NcWRFFileWriter writer = new NcWRFFileWriter();

 if(writer.writeWRFFile( subsetDataset, res.getOutputStream() )){

 res.setContentType(ContentType.binary.getContentHeader());
 res.setHeader("Content-Description", "wrf-intermediate");
 res.getOutputStream().flush();

 } else{
 // return an error message
 }
 ***************/

    } catch (Throwable t) {
      throw new RuntimeException("NcssGridController on dataset " + datasetPath, t);
    }
  }

  ////////////////
  private File makeCFNetcdfFile(CoverageCollection gcd, String responseFilename, NcssGridParamsBean params,
          NetcdfFileWriter.Version version) throws InvalidRangeException, IOException {
    SubsetParams subset = params.makeSubset(gcd);

    // Test maxFileDownloadSize
    long maxFileDownloadSize = ThreddsConfig.getBytes("NetcdfSubsetService.maxFileDownloadSize", -1L);
    if (maxFileDownloadSize > 0) {
      Optional<Long> estimatedSizeo = CFGridCoverageWriter2.writeOrTestSize(
              gcd, params.getVar(), subset, params.isAddLatLon(), true, null);
      if (!estimatedSizeo.isPresent())
        throw new InvalidRangeException("Request contains no data: " + estimatedSizeo.getErrorMessage());

      long estimatedSize = estimatedSizeo.get();
      if (version == NetcdfFileWriter.Version.netcdf4)
        estimatedSize /= ESTIMATED_COMPRESION_RATE;

      if (estimatedSize > maxFileDownloadSize)
        throw new RequestTooLargeException(
                "NCSS response too large = " + estimatedSize + " max = " + maxFileDownloadSize);
    }

    // write the file
    NetcdfFileWriter writer = NetcdfFileWriter.createNew(
            version, responseFilename, null);  // default chunking - let user control at some point

    Optional<Long> estimatedSizeo = CFGridCoverageWriter2.writeOrTestSize(
            gcd, params.getVar(), subset, params.isAddLatLon(), false, writer);
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


  private void handleRequestGridAsPoint(HttpServletResponse res, NcssGridParamsBean params, String datasetPath,
          CoverageCollection gcd) throws Exception {
    SupportedFormat sf = SupportedOperation.POINT_REQUEST.getSupportedFormat(params.getAccept());

    CoverageAsPoint covp = new CoverageAsPoint(gcd, params.getVar(), params.makeSubset(gcd));
    try (FeatureDatasetPoint fd = covp.asFeatureDatasetPoint()) {

      // all subsetting is done in CoverageAsPoint
      //SubsetParams ncssParams = params.makeSubset(gcd);
      SubsetParams ncssParams = new SubsetParams()
              .set(SubsetParams.timeAll, true)
              .set(SubsetParams.variables, params.getVar());
      DsgSubsetWriter pds = DsgSubsetWriterFactory.newInstance(
              fd, ncssParams, ncssDiskCache, res.getOutputStream(), sf);
      setResponseHeaders(res, pds.getHttpHeaders(datasetPath, sf.isStream()));
      pds.respond(res, fd, datasetPath, ncssParams, sf);
    }
  }

  ///////////////////////////////////////////////////////////

  @RequestMapping(value = {"**/dataset.xml", "**/pointDataset.xml"})
  // Same response for both Grid and GridAsPoint.
  public ModelAndView getDatasetDescriptionXml(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String datasetPath = getDatasetPath(req);

    try (CoverageCollection gcd = TdsRequestedDataset.getCoverageCollection(req, res, datasetPath)) {
      if (gcd == null) return null; // restricted dataset
      String datasetUrlPath = buildDatasetUrl(datasetPath);

      CoverageDatasetCapabilities writer = new CoverageDatasetCapabilities(gcd, "path");
      Document doc = writer.makeDatasetDescription();
      Element root = doc.getRootElement();
      root.setAttribute("location", datasetUrlPath);
      root.addContent(makeAcceptXML(SupportedOperation.GRID_REQUEST));

      return new ModelAndView("threddsXmlView", "Document", doc);
    }
  }

  @RequestMapping(value = "**/dataset.html")
  public ModelAndView getGridDatasetDescriptionHtml(HttpServletRequest req, HttpServletResponse res)
          throws IOException {
    return getDatasetDescriptionHtml(req, res, SupportedOperation.GRID_REQUEST);
  }

  @RequestMapping(value = "**/pointDataset.html")
  public ModelAndView getGridAsPointDatasetDescriptionHtml(HttpServletRequest req, HttpServletResponse res)
          throws IOException {
    return getDatasetDescriptionHtml(req, res, SupportedOperation.GRID_AS_POINT_REQUEST);
  }

  private ModelAndView getDatasetDescriptionHtml(HttpServletRequest req, HttpServletResponse res,
          SupportedOperation op) throws IOException {
    String datasetPath = getDatasetPath(req);

    try (CoverageCollection gcd = TdsRequestedDataset.getCoverageCollection(req, res, datasetPath)) {
      if (gcd == null) return null; // restricted dataset
      String datasetUrlPath = buildDatasetUrl(datasetPath);

      Map<String, Object> model = new HashMap<>();
      model.put("gcd", gcd);
      model.put("datasetPath", datasetUrlPath);
      model.put("horizExtentWKT", gcd.getHorizCoordSys().getLatLonBoundaryAsWKT(50, 100));
      model.put("accept", makeAcceptList(op));

      switch (op) {
        case GRID_REQUEST: return new ModelAndView("templates/ncssGrid", model);
        case GRID_AS_POINT_REQUEST: return new ModelAndView("templates/ncssGridAsPoint", model);
        default: throw new AssertionError("Who passed in a " + op + "?");
      }
    }
  }

  ///////////////////////////////////////////////////////////

  // Supported for backwards compatibility. We prefer that datasetBoundaries.wkt or datasetBoundaries.json are used.
  @RequestMapping("**/datasetBoundaries.xml")
  public void getDatasetBoundaries(NcssParamsBean params, HttpServletRequest req, HttpServletResponse res)
          throws IOException, UnsupportedResponseFormatException {
    SupportedFormat format = SupportedOperation.DATASET_BOUNDARIES_REQUEST.getSupportedFormat(params.getAccept());

    switch (format) {
      case WKT:  getDatasetBoundariesWKT(req, res);     break;
      case JSON: getDatasetBoundariesGeoJSON(req, res); break;
      default: throw new IllegalArgumentException(String.format(
              "Expected %s or %s, but got %s", SupportedFormat.WKT, SupportedFormat.JSON, format));
    }
  }

  @RequestMapping("**/datasetBoundaries.wkt")
  public void getDatasetBoundariesWKT(HttpServletRequest req, HttpServletResponse res) throws IOException {
    try (CoverageCollection gcd = TdsRequestedDataset.getCoverageCollection(req, res, getDatasetPath(req))) {
      if (gcd == null) return;

      res.setContentType(SupportedFormat.WKT.getMimeType());
      res.getWriter().write(gcd.getHorizCoordSys().getLatLonBoundaryAsWKT());
      res.getWriter().flush();
    }
  }

  @RequestMapping("**/datasetBoundaries.json")
  public void getDatasetBoundariesGeoJSON(HttpServletRequest req, HttpServletResponse res) throws IOException {
    try (CoverageCollection gcd = TdsRequestedDataset.getCoverageCollection(req, res, getDatasetPath(req))) {
      if (gcd == null) return;

      res.setContentType(SupportedFormat.JSON.getMimeType());
      res.getWriter().write(gcd.getHorizCoordSys().getLatLonBoundaryAsGeoJSON());
      res.getWriter().flush();
    }
  }

  ///////////////////////////////////////////////////////////

  /**
   * Checks that all the requested vars exist. If "all", fills out the param.vars with all grid names
   * Throws exception if some of the variables in the request are not contained in the dataset
   */
  private void checkRequestedVars(CoverageCollection gcd, NcssGridParamsBean params)
          throws VariableNotContainedInDatasetException {

    // if var == all --> all variables requested
    if (params.getVar().get(0).equalsIgnoreCase("all")) {
      params.setVar(getAllGridNames(gcd));
      return;
    }

    // Check vars are contained in the grid
    for (String gridName : params.getVar()) {
      Coverage grid = gcd.findCoverage(gridName);
      if (grid == null)
        throw new VariableNotContainedInDatasetException(
                "Variable: " + gridName + " is not contained in the requested dataset");
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
  protected boolean checkVarsHaveSameVertAxis(CoverageCollection gcd, NcssGridParamsBean params) {
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
