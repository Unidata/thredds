/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncSubset.controller;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;

import thredds.server.config.TdsContext;
import thredds.server.ncSubset.NCSSPointDataStream;
import thredds.server.ncSubset.NCSSPointDataStreamFactory;
import thredds.server.ncSubset.dataservice.FeatureDatasetService;
import thredds.server.ncSubset.exception.NcssException;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.server.ncSubset.params.NcssParamsBean;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.util.IO;

/**
 * @author mhermida
 */
@Controller
@RequestMapping("ncss/")
public class FeatureDatasetController extends AbstractFeatureDatasetController {

  static private final Logger log = LoggerFactory.getLogger(FeatureDatasetController.class);

  @Autowired
  FeatureDatasetService datasetService;

  @Autowired
  TdsContext tdsContext;

  /**
    * Handles ncss data requests
    *
    * @param req request
    * @param res result
    * @throws IOException
    * @throws UnsupportedResponseFormatException
    *
    * @throws InvalidRangeException
    * @throws ParseException
    */
   @RequestMapping("**")
   void handleRequest(HttpServletRequest req, HttpServletResponse res,
                        @Valid NcssParamsBean params,
                        BindingResult validationResult) throws IOException, NcssException, ParseException, InvalidRangeException {

     if (validationResult.hasErrors()) {
       handleValidationErrorsResponse(res, HttpServletResponse.SC_BAD_REQUEST, validationResult);
       return;
     }

     String datasetPath = getDatasetPath(req);
     FeatureDataset fd = null;
     try {
       fd = datasetService.findDatasetByPath(req, res, datasetPath);  // LOOK cant we get ft somewhere else first ?

       if (fd == null)
         throw new UnsupportedOperationException("Not a valid Feature Type dataset");

       if (fd.getFeatureType() == FeatureType.GRID) {

         if (!params.hasLatLonPoint()) {
           handleRequestGrid(req, res, params, datasetPath, (GridDataset) fd);
         } else {
           handleRequestGridAsPoint(req, res, params, datasetPath, fd);
         }

       } else if (fd.getFeatureType() == FeatureType.STATION) {
         handleRequestStation(req, res, params, datasetPath, fd);

       } else if (fd.getFeatureType() == FeatureType.POINT) {
         handleRequestStation(req, res, params, datasetPath, fd);

       } else {
           throw new UnsupportedOperationException("Feature Type not supported");
       }

      } finally {
       if (fd != null) fd.close();
     }

   }

  void handleRequestGrid(HttpServletRequest req, HttpServletResponse res,
                         NcssParamsBean params, String datasetPath,
                         GridDataset gridDataset) throws IOException, NcssException, ParseException, InvalidRangeException {

    // Supported formats are netcdf3 (default) and netcdf4 (if available)
    SupportedFormat sf = SupportedOperation.isSupportedFormat(params.getAccept(), SupportedOperation.GRID_REQUEST);

    NetcdfFileWriter.Version version = NetcdfFileWriter.Version.netcdf3;
    if (sf.equals(SupportedFormat.NETCDF4)) {
      version = NetcdfFileWriter.Version.netcdf4;
    }

    GridDataStream gds = GridDataStream.valueOf(gridDataset, datasetPath);
    File netcdfResult = gds.getResponseFile(req, res, params, version);

    // Headers...
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set("Content-Type", sf.getResponseContentType());
    setResponseHeaders(res, httpHeaders);
    IO.copyFileB(netcdfResult, res.getOutputStream(), 60000);
    res.flushBuffer();
    res.getOutputStream().close();
    res.setStatus(HttpServletResponse.SC_OK);
  }

  void handleRequestGridAsPoint(HttpServletRequest req, HttpServletResponse res,
                       NcssParamsBean params, String datasetPath,
                       FeatureDataset fd) throws IOException, NcssException, ParseException, InvalidRangeException {

    SupportedFormat format = SupportedOperation.isSupportedFormat(params.getAccept(), SupportedOperation.POINT_REQUEST);
    NCSSPointDataStream pds = NCSSPointDataStreamFactory.getDataStreamer(fd, params, format, res.getOutputStream());
    setResponseHeaders(res, pds.getResponseHeaders(fd, format, datasetPath));
    pds.pointDataStream(res, fd, datasetPath, params, format);
  }

  void handleRequestStation(HttpServletRequest req, HttpServletResponse res,
                       NcssParamsBean params, String datasetPath,
                       FeatureDataset fd) throws IOException, NcssException, ParseException, InvalidRangeException {

    SupportedFormat sf = SupportedOperation.isSupportedFormat(params.getAccept(), SupportedOperation.POINT_REQUEST);
    NCSSPointDataStream pds = NCSSPointDataStreamFactory.getDataStreamer(fd, params, sf, res.getOutputStream());
    setResponseHeaders(res, pds.getResponseHeaders(fd, sf, datasetPath));
    pds.pointDataStream(res, fd, datasetPath, params, sf);
  }


  /*
  @RequestMapping("**")
  void streamPointData(HttpServletRequest req, HttpServletResponse res,
                       @Valid NcssParamsBean params,
                       BindingResult validationResult) throws IOException, NcssException, ParseException, InvalidRangeException {

    if (validationResult.hasErrors()) {
      handleValidationErrorsResponse(res, HttpServletResponse.SC_BAD_REQUEST, validationResult);

    } else {
      SupportedFormat format = SupportedOperation.isSupportedFormat(params.getAccept(), SupportedOperation.POINT_REQUEST);

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
          SupportedFormat sf = SupportedOperation.isSupportedFormat(params.getAccept(), SupportedOperation.GRID_REQUEST);

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

          SupportedFormat sf = SupportedOperation.isSupportedFormat(params.getAccept(), SupportedOperation.POINT_REQUEST);

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

  private String getDatasetPath(HttpServletRequest req) {

    String servletPath = req.getServletPath();
    String[] servletPathTokens = servletPath.split("/");
    String lastToken = servletPathTokens[servletPathTokens.length - 1];
    if (lastToken.endsWith(".html") || lastToken.endsWith(".xml")) {
      servletPath = servletPath.substring(0, servletPath.length() - lastToken.length() - 1);
    }

    return servletPath.substring(
            FeatureDatasetController.servletPath.length(),
            servletPath.length());
  }


  private void setResponseHeaders(HttpServletResponse response, HttpHeaders httpHeaders) {
    Set<String> keySet = httpHeaders.keySet();
    for (String key : keySet) {
      if (httpHeaders.containsKey(key)) {
        response.setHeader(key, httpHeaders.get(key).get(0));
      }
    }
  }

}
