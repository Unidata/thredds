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

import ucar.nc2.dt.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.StructureData;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Iterator;
import java.io.IOException;
import java.text.ParseException;

/**
 * A Collection of StationDatasets
 */
public class StationDatasetCollection {
  private boolean debug = false;

  private ArrayList sobsList = new ArrayList();
  private StationObsDataset typical = null;
  private StringBuilder log = new StringBuilder();

  public void add(String location) throws IOException {
    StationObsDataset sobs = (StationObsDataset) TypedDatasetFactory.open(FeatureType.STATION, location, null, log);
    if (typical == null)
      typical = sobs;

    sobsList.add(sobs);
  }

  /**
   * Get all the Stations in the collection.
   *
   * @return List of Station
   * @throws java.io.IOException I/O error
   */
  public List getStations() throws IOException {
    return typical.getStations();
  }

  /**
   * Get all the Stations within a bounding box.
   *
   * @param boundingBox within this bounding box
   * @return List of Station
   * @throws java.io.IOException I/O error
   */
  public List getStations(ucar.unidata.geoloc.LatLonRect boundingBox) throws IOException {
    return typical.getStations(boundingBox);
  }

  /**
   * Find a Station by name
   *
   * @param name name of the Station
   * @return Station with that name, or null if not found
   */
  public ucar.unidata.geoloc.Station getStation(String name) {
    return typical.getStation(name);
  }

  /**
   * Get all data for this Station.
   *
   * @param s Station
   * @return iterator over type getDataClass()
   * @throws java.io.IOException I/O error
   */
  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s) throws IOException {
    return new StationDataIterator(s);
  }

  /**
   * Get data for this Station within the specified date range.
   *
   * @param s     Station
   * @param start starting Date
   * @param end   ending Date
   * @return Iterator over type getDataClass()
   * @throws java.io.IOException I/O error
   */
  public DataIterator getDataIterator(ucar.unidata.geoloc.Station s, Date start, Date end) throws IOException {
    return new StationDateDataIterator(s, start, end);
  }

  private class StationDataIterator implements DataIterator {
    String stationName;
    Iterator iterSobs;
    DataIterator dataIter;

    StationDataIterator(ucar.unidata.geoloc.Station s) throws IOException {
      this.stationName = s.getName();
      iterSobs = sobsList.iterator();
    }

    public boolean hasNext() {
      if (dataIter == null)
        dataIter = getNextDataIterator();
      if (dataIter == null)
        return false;

      if (dataIter.hasNext())
        return true;

      dataIter = getNextDataIterator();
      if (dataIter == null)
        return false;

      return hasNext();
    }

    public Object nextData() throws IOException {
      // System.out.println("nextData dataIter =" + dataIter);
      return dataIter.nextData();
    }

    public Object next() {
      return dataIter.next();
    }

    /**
     * not supported
     */
    public void remove() {
      throw new UnsupportedOperationException();
    }

    private DataIterator getNextDataIterator() {
      if (!iterSobs.hasNext())
        return null;
      StationObsDataset sobs = (StationObsDataset) iterSobs.next();
      DataIterator dataIter = makeDataIterator(sobs);
      if (debug && dataIter != null) System.out.println("next sobs =" + sobs.getLocationURI());
      return dataIter == null ? getNextDataIterator() : dataIter;
    }

    protected DataIterator makeDataIterator(StationObsDataset sobs) {
      ucar.unidata.geoloc.Station s = sobs.getStation(stationName);
      if (s == null) return null;
      return sobs.getDataIterator(s);
    }
  }

  private class StationDateDataIterator extends StationDataIterator {
    private Date want_start, want_end;

    StationDateDataIterator(ucar.unidata.geoloc.Station s, Date start, Date end) throws IOException {
      super(s);
      this.want_start = start;
      this.want_end = end;
    }

    protected DataIterator makeDataIterator(StationObsDataset sobs) {
      Date start = sobs.getStartDate();
      if (start.after(want_end))
        return null;
      Date end = sobs.getEndDate();
      if (end.before(want_start))
        return null;
      ucar.unidata.geoloc.Station s = sobs.getStation(stationName);
      if (s == null) return null;
      return sobs.getDataIterator(s, start, end);
    }
  }

  public static void main(String args[]) throws IOException, ParseException {
    StationDatasetCollection sdc = new StationDatasetCollection();
    DateFormatter format = new DateFormatter();

    sdc.add("C:/data/metars/Surface_METAR_20070326_0000.nc");
    sdc.add("C:/data/metars/Surface_METAR_20070329_0000.nc");
    sdc.add("C:/data/metars/Surface_METAR_20070330_0000.nc");
    sdc.add("C:/data/metars/Surface_METAR_20070331_0000.nc");

    ucar.unidata.geoloc.Station s = sdc.getStation("ACK");
    DataIterator iter = sdc.getDataIterator(s);
    while (iter.hasNext()) {
      Object o = iter.nextData();
      assert (o instanceof StationObsDatatype);
      StationObsDatatype sod = (StationObsDatatype) o;
      ucar.unidata.geoloc.Station ss = sod.getStation();
      assert (ss.getName().equals(s.getName()));

      System.out.println(ss.getName() + " " + format.toDateTimeStringISO( sod.getObservationTimeAsDate()));

      StructureData sdata = sod.getData();
      assert sdata != null;
    }

    System.out.println("------------------\n");
    Date start = format.isoDateTimeFormat("2007-03-27T09:18:56Z");
    Date end = format.isoDateTimeFormat("2007-03-30T10:52:48Z");

    iter = sdc.getDataIterator(s, start, end);
    while (iter.hasNext()) {
      Object o = iter.nextData();
      assert (o instanceof StationObsDatatype);
      StationObsDatatype sod = (StationObsDatatype) o;
      ucar.unidata.geoloc.Station ss = sod.getStation();
      assert (ss.getName().equals(s.getName()));

      System.out.println(ss.getName() + " " + format.toDateTimeStringISO( sod.getObservationTimeAsDate()));

      StructureData sdata = sod.getData();
      assert sdata != null;
    }

  }
}
