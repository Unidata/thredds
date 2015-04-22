/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncss.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.core.TdsRequestedDataset;
import thredds.server.config.FormatsAvailabilityService;
import thredds.server.config.TdsContext;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.exception.UnsupportedOperationException;
import thredds.server.ncss.exception.UnsupportedResponseFormatException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.view.dsg.DsgSubsetWriterFactory;
import thredds.servlet.ServletUtil;
import thredds.util.Constants;
import thredds.util.ContentType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.IO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Formatter;
import java.util.Set;

/**
 * Annotated controller for Netcdf Subset Service
 *
 * @author jcaron
 * @author mhermida
 */
@Controller
@RequestMapping("/ncss")
public class NcssController extends AbstractNcssController {
  //static private final Logger log = LoggerFactory.getLogger(NcssController.class);

  //@Autowired
  //FeatureDatasetService datasetService;

  @Autowired
  TdsContext tdsContext;

  /* @RequestMapping("/ncss/grid/**")
  public String forwardGrid(HttpServletRequest req) {
    String reqString = req.getServletPath();
    assert reqString.startsWith("/ncss/grid");
    reqString = reqString.substring(10);
    String forwardString = "forward:/ncss" + reqString;  // strip off '?/grid
    if (null != req.getQueryString())
      forwardString += "?"+req.getQueryString();

     return forwardString;
  }  */

  /**
   * Handles ncss data requests.
   * Dont know what responder to use until we can open the dataset.
   *
   * @param req request
   * @param res result
   * @throws IOException
   * @throws UnsupportedResponseFormatException
   * @throws InvalidRangeException
   * @throws ParseException
   */
  @RequestMapping("**")
  public void handleRequest(HttpServletRequest req, HttpServletResponse res,
                            @Valid NcssParamsBean params,
                            BindingResult validationResult) throws Exception {

    // System.out.printf("%s%n", ServletUtil.showRequestDetail(null, req));

    if (validationResult.hasErrors()) {
      handleValidationErrorsResponse(res, HttpServletResponse.SC_BAD_REQUEST, validationResult);
      return;
    }

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
      } else if (ft == FeatureType.POINT) {
        handleRequestDsg(res, params, datasetPath, fd);
      } else if (ft == FeatureType.STATION) {
        handleRequestDsg(res, params, datasetPath, fd);
      } else {
        throw new UnsupportedOperationException("Feature Type " + ft.toString() + " not supported");
      }

    }
  }

  void handleRequestGrid(HttpServletResponse res, NcssParamsBean params, String datasetPath,
                         GridDataset gridDataset) throws IOException, NcssException, ParseException, InvalidRangeException {

    //params.isValidGridRequest(); ???

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

    //File netcdfResult = null;
    //try {
    GridResponder gds = GridResponder.factory(gridDataset, datasetPath);
    File netcdfResult = gds.getResponseFile(res, params, version);
    //} catch (Exception e) {
    //  handleValidationErrorMessage(res, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    //  return;
    //}

    // filename download attachment
    String suffix = (version == NetcdfFileWriter.Version.netcdf4) ? ".nc4" : ".nc";
    int pos = datasetPath.lastIndexOf("/");
    String filename = (pos >= 0) ? datasetPath.substring(pos + 1) : datasetPath;
    if (!filename.endsWith(suffix)) {
      filename += suffix;
    }

    // Headers...
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set(ContentType.HEADER, sf.getResponseContentType());
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
    DiskCache2 diskCache = NcssDiskCache.getInstance().getDiskCache();

    NcssResponder pds = GridAsPointResponder.factory(diskCache, format, res.getOutputStream());
    setResponseHeaders(res, pds.getResponseHeaders(fd, format, datasetPath));
    pds.respond(res, fd, datasetPath, params, format);
  }

  void handleRequestDsg(HttpServletResponse res, NcssParamsBean params, String datasetPath,
                        FeatureDataset fd) throws Exception {
    SupportedOperation supportedOp;
    switch (fd.getFeatureType()) {
      case POINT:
        supportedOp = SupportedOperation.POINT_REQUEST;
        break;
      case STATION:
        supportedOp = SupportedOperation.STATION_REQUEST;
        break;
      default:
        throw new UnsupportedOperationException(String.format(
                "%s format not currently supported for DSG subset writing.", fd.getFeatureType()));
    }

    SupportedFormat format = supportedOp.getSupportedFormat(params.getAccept());
    DiskCache2 diskCache = NcssDiskCache.getInstance().getDiskCache();

    NcssResponder pds = DsgSubsetWriterFactory.newInstance(
            (FeatureDatasetPoint) fd, params, diskCache, res.getOutputStream(), format);
    setResponseHeaders(res, pds.getResponseHeaders(fd, format, datasetPath));
    pds.respond(res, fd, datasetPath, params, format);
  }

  /*
  @RequestMapping("**")
  void streamPointData(HttpServletRequest req, HttpServletResponse res,
                       @Valid NcssParamsBean params,
                       BindingResult validationResult) throws IOException, NcssException, ParseException,
                       InvalidRangeException {

    if (validationResult.hasErrors()) {
      handleValidationErrorsResponse(res, HttpServletResponse.SC_BAD_REQUEST, validationResult);

    } else {
      SupportedFormat format = SupportedOperation.isSupportedFormat(params.getAccept(),
      SupportedOperation.POINT_REQUEST);

      String datasetPath = getDatasetPath(req);
      FeatureDataset fd = null;
      try {
        fd = datasetService.findDatasetByPath(req, res, datasetPath);

        if (fd == null)
          throw new UnsupportedOperationException("Feature Type not supported");

        NCSSPointDataStream pds = NCSSPointDataStreamFactory.getDataStreamer(fd, params, format, res.getOutputStream());
        setResponseHeaders(res, pds.getResponseHeaders(fd, format, datasetPath));
        pds.pointDataStream(res, fd, datasetPath, params, format);

      } finally {
        if (fd != null) fd.close();
      }
    }
  }


  @RequestMapping(value = "**", params = {"!latitude", "!longitude", "!subset", "!req"})
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

  private void setResponseHeaders(HttpServletResponse response, HttpHeaders httpHeaders) {
    Set<String> keySet = httpHeaders.keySet();
    for (String key : keySet) {
      if (httpHeaders.containsKey(key)) { // LOOK why test again?
        response.setHeader(key, httpHeaders.get(key).get(0));  // LOOK why only first one ?
      }
    }
  }
}
