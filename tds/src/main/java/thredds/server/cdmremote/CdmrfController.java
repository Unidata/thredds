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
package thredds.server.cdmremote;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import thredds.core.TdsRequestedDataset;
import thredds.server.cdmremote.params.CdmrfQueryBean;
import thredds.server.cdmremote.stream.CdmrfStreamFactory;
import thredds.server.cdmremote.view.CdmrfTextViewFactory;
import thredds.server.config.TdsContext;
import thredds.servlet.ServletUtil;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;

/**
 * @author mhermida
 */
@Deprecated // use ncss
@Controller
@RequestMapping("/cdmrfeature")
public class CdmrfController {


  private static final String CDMRF_BASE_SERVICE = "/cdmrfeature/";

  //private static final Logger log = org.slf4j.LoggerFactory.getLogger(CdmrfController.class);

  @Autowired
  private TdsContext tdsContext;


  /*
   * Has to be set through a configuration option
   */
  private boolean allow = true;

  @RequestMapping(value = {"/**"}, params = {"req!=data", "req!=dataForm", "req!=header"}, method = {RequestMethod.GET})
  public ResponseEntity<String> metadataRequestHandler(HttpServletRequest req, HttpServletResponse res, @Valid CdmrfQueryBean query, BindingResult validationResult) throws IOException {

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return null;
    }

    if (validationResult.hasErrors()) {
      //Validation errors...gotta handle!
    }


    // absolute path of the dataset endpoint
    String absPath = ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath();
    String path = getPathForDataset(req);

    try (FeatureDatasetPoint fdp = findFeatureDatasetPointByPath(req, res, path)) {
      String content = CdmrfTextViewFactory.getInstance().getFormViewForDataset(req, res, fdp, absPath, query);
      HttpHeaders responseHeaders = new HttpHeaders();

      responseHeaders.setContentLength(content == null ? 0 : content.length());
      responseHeaders.setContentType(query.getMediaType());

      return new ResponseEntity<>(content, responseHeaders, HttpStatus.OK);
    }

		/*try {
          CdmRemoteQueryBean.RequestType reqType = query.getRequestType();
	        CdmRemoteQueryBean.ResponseType resType = query.getResponseType();
	        switch (reqType) {
	          case capabilities:
	          case form:
	            return processXml(req, res, fdp, absPath, query);

	          case header:
	            return processHeader(absPath, res, fdp, query);

	          case dataForm:
	          case data:
	            return processData(req, res, fdp, path, query);

	          case stations:
	            if (resType == CdmRemoteQueryBean.ResponseType.xml)
	              return processXml(req, res, fdp, absPath, query);
	            else
	              return processStations(res, fdp, query);
	        }

	      } catch (FileNotFoundException e) {
	        res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
	        return null;

	      } catch (Throwable t) {
	        log.error("CdmRemoteController exception:", t);
	        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
	        return null;

	      } finally {
	        if (showReq) System.out.printf(" done%n");
	        if (null != fdp)
	          try {
	            fdp.close();
	          } catch (IOException ioe) {
	            log.error("Failed to close = " + path);
	          }
	      }

	      return null; */

  }

  @RequestMapping(value = {"/**"}, params = {"req=header"}, method = {RequestMethod.GET})
  public void headerRequestHandler(HttpServletRequest req, HttpServletResponse res, @Valid CdmrfQueryBean query, BindingResult validationResult) throws IOException {

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    if (validationResult.hasErrors()) {
      //Validation errors...gotta handle!
    }

    // absolute path of the dataset endpoint
    String absPath = ServletUtil.getRequestServer(req) + req.getContextPath() + req.getServletPath();
    String path = getPathForDataset(req);

    try (FeatureDatasetPoint fdp = findFeatureDatasetPointByPath(req, res, path)) {
      CdmrfStreamFactory.getInstance(tdsContext).headerStream(absPath, res, fdp, query);
    }

  }

  @RequestMapping(value = {"/**"}, params = {"req=data"}, method = {RequestMethod.GET})
  public void dataRequestHandler(HttpServletRequest req, HttpServletResponse res, @Valid CdmrfQueryBean query, BindingResult validationResult) throws IOException {

    if (!allow) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "Service not supported");
      return;
    }

    if (validationResult.hasErrors()) {
      //Validation errors...gotta handle!
    }

    // absolute path of the dataset endpoint
    String path = getPathForDataset(req);

    try (FeatureDatasetPoint fdp = findFeatureDatasetPointByPath(req, res, path)) {
      CdmrfStreamFactory.getInstance(tdsContext).dataStream(req, res, fdp, path, query);
    }

  }


  private String getPathForDataset(HttpServletRequest req) {
    return req.getServletPath().substring(CDMRF_BASE_SERVICE.length(), req.getServletPath().length());
  }

  public FeatureDatasetPoint findFeatureDatasetPointByPath(HttpServletRequest req, HttpServletResponse res, String path) throws IOException {

    // this looks for a featureCollection
    FeatureDataset fc = TdsRequestedDataset.getFeatureDataset(req, res, path);

    FeatureDatasetPoint fdp = null;
    if (fc != null) {
      fdp = (FeatureDatasetPoint) fc;

    } else {
      // tom kunicki 12/18/10
      // allows a single NetcdfFile to be turned into a FeatureDataset
      NetcdfFile ncfile = TdsRequestedDataset.getNetcdfFile(req, res, path); // LOOK above call should do thie ??
      if (ncfile == null) return null;  // restricted access

      FeatureDataset fd = FeatureDatasetFactoryManager.wrap(
              FeatureType.ANY,                  // will check FeatureType below if needed...
              NetcdfDataset.wrap(ncfile, null),
              null,
              new Formatter(System.err));       // better way to do this?
      if (fd instanceof FeatureDatasetPoint)
        fdp = (FeatureDatasetPoint) fd;
    }

    //---//
    if (fdp == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "not a point or station dataset");
      return null;
    }

    List<FeatureCollection> list = fdp.getPointFeatureCollectionList();
    if (list.size() == 0) {
      // log.error(fdp.getLocation() + " does not have any PointFeatureCollections");
      res.sendError(HttpServletResponse.SC_NOT_FOUND, fdp.getLocation() + " does not have any PointFeatureCollections");
      return null;
    }

    // check on feature type, using suffix convention LOOK
    FeatureType ft = null;
    if (path.endsWith("/station")) {
      ft = FeatureType.STATION;
      path = path.substring(0, path.lastIndexOf('/'));
    } else if (path.endsWith("/point")) {
      ft = FeatureType.POINT;
      path = path.substring(0, path.lastIndexOf('/'));
    }

    if (ft != null && ft != fdp.getFeatureType()) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "feature type mismatch:  expetected " + ft + " found" + fdp.getFeatureType());
    }

    return fdp;

  }

}
