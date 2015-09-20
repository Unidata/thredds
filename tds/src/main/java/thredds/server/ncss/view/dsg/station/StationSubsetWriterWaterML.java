/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
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
package thredds.server.ncss.view.dsg.station;

import net.opengis.waterml.x20.CollectionDocument;
import net.opengis.waterml.x20.CollectionType;
import org.springframework.http.HttpHeaders;
import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.params.NcssParamsBean;
import thredds.server.ncss.controller.NcssRequestUtils;
import thredds.server.ncss.params.NcssPointParamsBean;
import thredds.util.ContentType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.ogc.om.NcOMObservationPropertyType;
import ucar.nc2.ogc.waterml.NcDocumentMetadataPropertyType;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cwardgar on 2014/06/04.
 */
public class StationSubsetWriterWaterML extends AbstractStationSubsetWriter {
  private final OutputStream out;
  private final CollectionDocument collectionDoc;
  private final CollectionType collection;

  public StationSubsetWriterWaterML(FeatureDatasetPoint fdPoint, SubsetParams ncssParams, OutputStream out)
          throws XMLStreamException, NcssException, IOException {
    super(fdPoint, ncssParams);

    this.out = out;
    this.collectionDoc = CollectionDocument.Factory.newInstance();
    this.collection = collectionDoc.addNewCollection();
  }

  @Override
  public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream) {
    HttpHeaders httpHeaders = new HttpHeaders();

    if (!isStream) {
      httpHeaders.set("Content-Location", datasetPath);
      String fileName = NcssRequestUtils.getFileNameForResponse(datasetPath, ".xml");
      httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
    }

    httpHeaders.set(ContentType.HEADER, ContentType.xml.getContentHeader());
    return httpHeaders;
  }

  @Override
  protected void writeHeader(StationPointFeature stationPointFeat) throws Exception {
    MarshallingUtil.resetIds();

    // @gml:id
    String id = MarshallingUtil.createIdForType(CollectionType.class);
    collection.setId(id);

    // wml2:metadata
    NcDocumentMetadataPropertyType.initMetadata(collection.addNewMetadata());
  }

  @Override
  protected int writeStationTimeSeriesFeature(StationTimeSeriesFeature stationFeat) throws Exception {
    if (!headerDone) {
      writeHeader(null);
      headerDone = true;
    }

    for (VariableSimpleIF wantedVar : wantedVariables) {
      // wml2:observationMember
      NcOMObservationPropertyType.initObservationMember(
              collection.addNewObservationMember(), stationFeat, wantedVar);
    }

    return 1; // ??
  }

  @Override
  protected void writeStationPointFeature(StationPointFeature stationPointFeat) throws Exception {
    throw new UnsupportedOperationException("Method not used in " + getClass());
  }

  @Override
  protected void writeFooter() throws Exception {
    MarshallingUtil.writeObject(collectionDoc, out, true);
    out.flush();
  }
}
