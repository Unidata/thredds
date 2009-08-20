/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.writer;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Document;
import org.jdom.Element;

import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * generate capabilities XML for a FeatureDatasetPoint
 *
 * @author caron
 * @since Aug 19, 2009
 */
public class FeatureDatasetPointXML {
  private FeatureDatasetPoint fdp;
  private String path;
  private StationTimeSeriesFeatureCollection sobs;

  public FeatureDatasetPointXML(FeatureDatasetPoint fdp, String path) {
    this.fdp = fdp;
    this.path = path;

    List<FeatureCollection> list = fdp.getPointFeatureCollectionList();
    this.sobs = (StationTimeSeriesFeatureCollection) list.get(0);
  }

  /**
   * Write stationObsDataset XML document
   */
  public String getCapabilities() {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(getCapabilitiesDocument());
  }

  public void getCapabilities(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(getCapabilitiesDocument(), os);
  }

  /**
   * Write stationCollection XML document
   */
  public String writeStationCollectionXML() throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    return fmt.outputString(makeStationCollectionDocument());
  }

  public void writeStationCollectionXML(OutputStream os) throws IOException {
    XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
    fmt.output(makeStationCollectionDocument(), os);
  }

  /**
   * Create an XML document from this info
   */
  public Document makeStationCollectionDocument() throws IOException {
    Element rootElem = new Element("stationCollection");
    Document doc = new Document(rootElem);

    for (Station s : sobs.getStations()) {
      Element sElem = new Element("station");
      sElem.setAttribute("name", s.getName());
      if (s.getWmoId() != null)
        sElem.setAttribute("wmo_id", s.getWmoId());
      if (s.getDescription() != null)
        sElem.addContent(new Element("description").addContent(s.getDescription()));

      sElem.addContent(new Element("longitude").addContent(ucar.unidata.util.Format.d(s.getLongitude(), 6)));
      sElem.addContent(new Element("latitide").addContent(ucar.unidata.util.Format.d(s.getLatitude(), 6)));
      if (!Double.isNaN(s.getAltitude()))
        sElem.addContent(new Element("altitude").addContent(ucar.unidata.util.Format.d(s.getAltitude(), 6)));
      rootElem.addContent(sElem);
    }

    return doc;
  }

  /**
   * Create an XML document from this info
   */
  public Document getCapabilitiesDocument() {
    Element rootElem = new Element("capabilities");
    Document doc = new Document(rootElem);
    if (null != path) {
      rootElem.setAttribute("location", path);
      Element elem = new Element("featureDataset");
      elem.setAttribute("type", fdp.getFeatureType().toString().toLowerCase());
      elem.setAttribute("url", path +"/"+ fdp.getFeatureType().toString().toLowerCase());
      rootElem.addContent(elem);
    }

    // data variables
    List<? extends VariableSimpleIF> vars = fdp.getDataVariables();
    Collections.sort(vars);
    for (VariableSimpleIF v : vars) {
      rootElem.addContent(writeVariable(v));
    }

    // add lat/lon bounding box
    LatLonRect bb = sobs.getBoundingBox();
    if (bb != null)
      rootElem.addContent(writeBoundingBox(bb));

    // add date range
    Date start = fdp.getStartDate();
    Date end = fdp.getEndDate();
    if ((start != null) && (end != null)) {
      DateFormatter format = new DateFormatter();
      Element dateRange = new Element("TimeSpan"); // from KML
      dateRange.addContent(new Element("begin").addContent(format.toDateTimeStringISO(start)));
      dateRange.addContent(new Element("end").addContent(format.toDateTimeStringISO(end)));
      rootElem.addContent(dateRange);
    }

    // add accept list
    Element elem = new Element("AcceptList");
    //elem.addContent(new Element("accept").addContent("raw"));
    elem.addContent(new Element("accept").addContent("csv"));
    elem.addContent(new Element("accept").addContent("xml"));
    elem.addContent(new Element("accept").addContent("netcdf"));
    elem.addContent(new Element("accept").addContent("ncstream"));
    rootElem.addContent(elem);

    return doc;
  }

  private Element writeBoundingBox(LatLonRect bb) {
    Element bbElem = new Element("LatLonBox"); // from KML
    bbElem.addContent(new Element("west").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMin(), 6)));
    bbElem.addContent(new Element("east").addContent(ucar.unidata.util.Format.dfrac(bb.getLonMax(), 6)));
    bbElem.addContent(new Element("south").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMin(), 6)));
    bbElem.addContent(new Element("north").addContent(ucar.unidata.util.Format.dfrac(bb.getLatMax(), 6)));
    return bbElem;
  }

  private Element writeVariable(VariableSimpleIF v) {

    Element varElem = new Element("variable");
    varElem.setAttribute("name", v.getName());

    ucar.ma2.DataType dt = v.getDataType();
    if (dt != null)
      varElem.setAttribute("type", dt.toString());

    // attributes
    Iterator atts = v.getAttributes().iterator();
    while (atts.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute) atts.next();
      varElem.addContent(ucar.nc2.ncml.NcMLWriter.writeAttribute(att, "attribute", null));
    }

    return varElem;
  }


  /**
   * debug
   */
  public static void main(String args[]) throws IOException {
    String location = "D:/datasets/metars/Surface_METAR_20070516_0000.nc";
    String path = "http://motherlode.ucar.edu:9080/thredds/cdmremote/idd/metar/gempak/collection";

    FeatureDataset fd = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null, new Formatter(System.out));
    FeatureDatasetPointXML xml = new FeatureDatasetPointXML((FeatureDatasetPoint) fd, path);
    xml.getCapabilities(System.out);

    File f = new File("C:/TEMP/stationCapabilities.xml");
    FileOutputStream fos = new FileOutputStream(f);
    xml.getCapabilities(fos);
    fos.close();

    /* File f = new File("C:/TEMP/stationCollection.xml");
    FileOutputStream fos = new FileOutputStream(f);
    xml.writeStationCollectionXML(fos);
    fos.close();
    System.out.println(" size xml=" + f.length());
    long s1 = f.length();

    f = new File("C:/TEMP/stationCollection.xml.gzip");
    fos = new FileOutputStream(f);
    GZIPOutputStream zout = new GZIPOutputStream(fos);
    xml.writeStationCollectionXML(zout);
    zout.close();
    double s2 = (double) f.length();
    System.out.printf(" size xml zipped=%d ratio=%f%n", f.length(), s1/s2);  */
  }

}


