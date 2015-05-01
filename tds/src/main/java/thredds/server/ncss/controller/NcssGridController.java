/* Copyright */
package thredds.server.ncss.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import thredds.core.StandardService;
import thredds.core.TdsRequestedDataset;
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
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.gis.GridBoundariesExtractor;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.util.IO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Formatter;

/**
 * Handles all Ncss Grid Requests
 *
 * @author caron
 * @since 4/29/2015
 */
@Controller
@RequestMapping("/ncss/grid")
public class NcssGridController extends NcssController {

  protected String getBase() {
    return StandardService.netcdfSubsetGrid.getBase();
  }

  @RequestMapping("**")
  public void handleRequest(HttpServletRequest req, HttpServletResponse res,
                            @Valid NcssGridParamsBean params,
                            BindingResult validationResult) throws Exception {

    if (validationResult.hasErrors())
      throw new BindException(validationResult);

    String datasetPath = getDatasetPath(req);
    try (FeatureDataset fd = TdsRequestedDataset.getFeatureDataset(req, res, datasetPath)) {
      if (fd == null) return;

      Formatter errs = new Formatter();
      if (!params.intersectsTime(fd, errs)) {
        handleValidationErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, errs.toString());
        return;
      }

      FeatureType ft = fd.getFeatureType();
      if (ft == FeatureType.GRID) {
        if (!params.hasLatLonPoint()) {
          handleRequestGrid(res, params, datasetPath, (GridDataset) fd);
        } else {
          handleRequestGridAsPoint(res, params, datasetPath, fd);
        }
      } else {
        throw new NcssException("Dataset Feature Type is " + ft.toString() + " but request is for Grids");
      }
    }
  }

  void handleRequestGrid(HttpServletResponse res, NcssParamsBean params, String datasetPath,
                         GridDataset gridDataset) throws IOException, NcssException, ParseException, InvalidRangeException {

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

    GridResponder gds = GridResponder.factory(gridDataset, datasetPath, ncssDiskCache);
    File netcdfResult = gds.getResponseFile(res, params, version);

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

  void handleRequestGridAsPoint(HttpServletResponse res, NcssParamsBean params, String datasetPath,
                                FeatureDataset fd) throws Exception {
    SupportedFormat format = SupportedOperation.POINT_REQUEST.getSupportedFormat(params.getAccept());
    NcssResponder pds = GridAsPointResponder.factory(ncssDiskCache, format, res.getOutputStream());
    setResponseHeaders(res, pds.getResponseHeaders(fd, format, datasetPath));
    pds.respond(res, fd, datasetPath, params, format);
  }

  @RequestMapping(value = {"**/dataset.html", "**/dataset.xml", "**/pointDataset.html", "**/pointDataset.xml"})
  public ModelAndView getDatasetDescription(HttpServletRequest req, HttpServletResponse res) throws IOException, NcssException {
    if (!req.getParameterMap().isEmpty())
      throw new NcssException("Invalid info request.");

    // the forms and dataset description
    String path = req.getServletPath();
    boolean wantXML = path.endsWith("/dataset.xml") || path.endsWith("/pointDataset.xml");
    boolean showPointForm = path.endsWith("/pointDataset.html");
    String datasetPath = getDatasetPath(req);

    try (FeatureDataset fd = TdsRequestedDataset.getFeatureDataset(req, res, datasetPath)) {
      if (fd == null) return null; // restricted dataset
      return ncssShowDatasetInfo.showForm(fd, buildDatasetUrl(datasetPath), wantXML, showPointForm);
    }
  }

  @RequestMapping("**/datasetBoundaries.xml")
  void getDatasetBoundaries(NcssParamsBean params, HttpServletRequest req, HttpServletResponse res) throws IOException, UnsupportedResponseFormatException {

    //Checking request format...
    SupportedFormat sf = getSupportedFormat(params, SupportedOperation.DATASET_BOUNDARIES_REQUEST);
    String datasetPath = getDatasetPath(req);

    try (FeatureDataset fd = TdsRequestedDataset.getFeatureDataset(req, res, datasetPath)) {
      if (fd == null) return;

      if (fd.getFeatureType() != FeatureType.GRID)
        throw new java.lang.UnsupportedOperationException("Dataset Boundaries request is only supported on Grid features");

      String boundaries = getBoundaries(sf, (GridDataset) fd);

      res.setContentType(sf.getMimeType());
      res.getWriter().write(boundaries);
      res.getWriter().flush();
    }
  }

  private String getBoundaries(SupportedFormat format, GridDataset gridDataset) {

    String boundaries = "";
    GridBoundariesExtractor gbe = GridBoundariesExtractor.valueOf(gridDataset);

    if (format == SupportedFormat.WKT)
      boundaries = gbe.getDatasetBoundariesWKT();
    if (format == SupportedFormat.JSON)
      boundaries = gbe.getDatasetBoundariesGeoJSON();

    return boundaries;
  }

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



   /* @RequestMapping(value = "**", params = {"!latitude", "!longitude", "!subset", "!req"})
   void getGridSubset(@Valid GridDataRequestParamsBean params,
                      BindingResult validationResult, HttpServletResponse response,
                      HttpServletRequest request) throws NcssException, IOException,
           InvalidRangeException, ParseException {

     if (validationResult.hasErrors()) {
       handleValidationErrorsResponse(response, HttpServletResponse.SC_BAD_REQUEST, validationResult);

     } else {
       String pathInfo = getDatasetPath(request);
       FeatureDataset fd = null;
       try {
         fd = datasetService.findDatasetByPath(request, response, pathInfo);
         if (fd == null)
           throw new UnsupportedOperationException("Feature Type not supported");

         if (fd.getFeatureType() == FeatureType.GRID) {
           // Supported formats are netcdf3 (default) and netcdf4 (if available)
           SupportedFormat sf = SupportedOperation.isSupportedFormat(params.getAccept(),
           SupportedOperation.GRID_REQUEST);

           NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf3;
           if (sf.equals(SupportedFormat.NETCDF4)) {
             version = NetcdfFileWriter.Version.netcdf4;
           }

           GridDataset gridDataset = (GridDataset) fd;
           GridDataStream gds = GridDataStream.valueOf(gridDataset, pathInfo);
           File netcdfResult = gds.getResponseFile(request, response, params, version);

           // Headers...
           HttpHeaders httpHeaders = new HttpHeaders();
           httpHeaders.set("Content-Type", sf.getResponseContentType());
           setResponseHeaders(response, httpHeaders);
           IO.copyFileB(netcdfResult, response.getOutputStream(), 60000);
           response.flushBuffer();
           response.getOutputStream().close();
           response.setStatus(HttpServletResponse.SC_OK);

           gridDataset.close();

         } else if (fd.getFeatureType() == FeatureType.STATION) {

           SupportedFormat sf = SupportedOperation.isSupportedFormat(params.getAccept(),
           SupportedOperation.POINT_REQUEST);

           PointDataRequestParamsBean pdr = RequestParamsAdapter.adaptGridParamsToPointParams(params);

           NCSSPointDataStream pds = NCSSPointDataStreamFactory.getDataStreamer(fd, pdr, sf, response.getOutputStream());

           setResponseHeaders(response, pds.getResponseHeaders(fd, sf, pathInfo));
           pds.pointDataStream(response, fd, pathInfo, pdr, sf);

         }

       } finally {
         if (fd != null) fd.close();
       }
     }

   }  */

}
