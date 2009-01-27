/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.dt.point;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.*;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.NetcdfFile;
import ucar.nc2.util.CancelTask;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class SubsetServiceDataset implements StationObsDataset {
  private static Namespace defNS = null;

  private NetcdfDataset ncd;
  private ArrayList stations = new ArrayList();

  public SubsetServiceDataset(String ncml, String stations) throws IOException {
    ncd = NetcdfDataset.openDataset(ncml, false, null);
    readStations(stations);
  }

  private void readStations(String stationXML) throws IOException {
    org.jdom.Document doc;
    try {
      SAXBuilder builder = new SAXBuilder(stationXML);
      doc = builder.build(stationXML);
    } catch (JDOMException e) {
      throw new IOException(e.getMessage());
    }
    Element rootElem = doc.getRootElement();

    // look for stations
    java.util.List stations = rootElem.getChildren("station", defNS);
    for (int j = 0; j < stations.size(); j++) {
      StationImpl station = readStation((Element) stations.get(j));
      stations.add(station);
    }
  }

  private StationImpl readStation(Element elem) {
    String name = elem.getAttributeValue("name");
    String value = elem.getAttributeValue("value");

    StationImpl station = new StationImpl();
    station.setName(value);
    station.setDescription(name);

    Element locationElem = elem.getChild("location", defNS);
    if (null == locationElem)
      locationElem = elem.getChild("location3D", defNS);
    readLocation(station, locationElem);

    return station;
  }

  private void readLocation(StationImpl s, Element locationElem) {
    if (locationElem == null)
      return;
    String latitude = locationElem.getAttributeValue("latitude");
    String longitude = locationElem.getAttributeValue("longitude");
    String elevation = locationElem.getAttributeValue("elevation");
    String latitude_units = locationElem.getAttributeValue("latitude_units");
    String longitude_units = locationElem.getAttributeValue("longitude_units");
    String elevation_units = locationElem.getAttributeValue("elevation_units");

    s.setLatitude(Double.parseDouble(latitude));
    s.setLongitude(Double.parseDouble(longitude));
    if (elevation != null) {
      s.setAltitude(Double.parseDouble(elevation));
    }
  }

  public String getTitle() {
    return ncd.getTitle();
  }

  public String getDescription() {
    return null;
  }

  public String getLocationURI() {
    return ncd.getLocation();
  }

  public Date getStartDate() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Date getEndDate() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public LatLonRect getBoundingBox() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getGlobalAttributes() {
    return ncd.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase(String name) {
    return ncd.findGlobalAttributeIgnoreCase( name);
  }

  public List getDataVariables() {
    return ncd.getVariables();
  }

  public VariableSimpleIF getDataVariable(String shortName) {
    return (VariableSimpleIF) ncd.findVariable(shortName);
  }

  public NetcdfFile getNetcdfFile() {
    return ncd;
  }

  public void close() throws IOException {
    ncd.close();
  }

  public String getDetailInfo() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Class getDataClass() {
    return StationObsDatatype.class;
  }

  public DateUnit getTimeUnits() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData() throws IOException {
    return getData( (CancelTask) null);
  }

  public List getData(CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public int getDataCount() {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(LatLonRect boundingBox) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(LatLonRect boundingBox, Date start, Date end) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getStations() throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getStations(CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getStations(LatLonRect boundingBox) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getStations(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Station getStation(String name) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public int getStationDataCount(Station s) {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(Station s) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(Station s, CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(Station s, Date start, Date end) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(Station s, Date start, Date end, CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(List stations) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(List stations, CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(List stations, Date start, Date end) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public List getData(List stations, Date start, Date end, CancelTask cancel) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public DataIterator getDataIterator(Station s) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public DataIterator getDataIterator(Station s, Date start, Date end) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }


}
