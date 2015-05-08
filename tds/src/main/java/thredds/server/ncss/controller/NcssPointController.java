/* Copyright */
package thredds.server.ncss.controller;

import org.jdom2.Document;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import thredds.core.StandardService;
import thredds.core.TdsRequestedDataset;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.params.NcssPointParamsBean;
import thredds.server.ncss.view.dsg.DsgSubsetWriter;
import thredds.server.ncss.view.dsg.DsgSubsetWriterFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.FeatureDatasetPointXML;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Formatter;

/**
 * Handles all Ncss Point Requests
 *
 * @author caron
 * @since 4/29/2015
 */
@Controller
@RequestMapping("/ncss/point")
public class NcssPointController extends AbstractNcssController {

  protected String getBase() {
    return StandardService.netcdfSubsetPoint.getBase();
  }

  @RequestMapping("**")
  public void handleRequest(HttpServletRequest req, HttpServletResponse res,
                            @Valid NcssPointParamsBean params,
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
       if (ft == FeatureType.POINT) {
        handleRequestDsg(res, params, datasetPath, fd);
      } else if (ft == FeatureType.STATION) {
        handleRequestDsg(res, params, datasetPath, fd);
      } else {
         throw new NcssException("Dataset Feature Type is " + ft.toString() + " but request is for Points or Stations");
      }
    }
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
    DsgSubsetWriter pds = DsgSubsetWriterFactory.newInstance((FeatureDatasetPoint) fd, params, ncssDiskCache, res.getOutputStream(), format);
    setResponseHeaders(res, pds.getResponseHeaders(fd, format, datasetPath));
    pds.respond(res, fd, datasetPath, params, format);
  }

  @RequestMapping(value = {"**/dataset.html", "**/dataset.xml", "**/pointDataset.html", "**/pointDataset.xml"})
  public ModelAndView getDatasetDescription(HttpServletRequest req, HttpServletResponse res) throws IOException {

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

   @RequestMapping(value = {"**/station.xml"})
   ModelAndView getStations(HttpServletRequest req, HttpServletResponse res, NcssParamsBean params) throws IOException {

     String datasetPath = getDatasetPath(req);
     try (FeatureDataset fd = TdsRequestedDataset.getFeatureDataset(req, res, datasetPath)) {
       if (fd == null) return null;

       if (fd.getFeatureType() != FeatureType.STATION)
         throw new java.lang.UnsupportedOperationException("Station list request is only supported for Station features");

       FeatureDatasetPointXML xmlWriter = new FeatureDatasetPointXML((FeatureDatasetPoint) fd, buildDatasetUrl(datasetPath));

       String[] stnsList = new String[]{};
       if (params.getStns() != null)
         stnsList = params.getStns().toArray(stnsList);
       else
         stnsList = null;

       LatLonRect llrect = null;
       if (params.getNorth() != null && params.getSouth() != null && params.getEast() != null && params.getWest() != null)
         llrect = new LatLonRect(new LatLonPointImpl(params.getSouth(), params.getWest()), new LatLonPointImpl(params.getNorth(), params.getEast()));

       Document doc = xmlWriter.makeStationCollectionDocument(llrect, stnsList);
       return new ModelAndView("threddsXmlView", "Document", doc);
     }
   }
}
