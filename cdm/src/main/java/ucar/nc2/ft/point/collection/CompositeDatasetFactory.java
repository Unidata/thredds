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
package ucar.nc2.ft.point.collection;

import ucar.nc2.ft.*;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.NetcdfFile;
import ucar.nc2.VariableSimpleIF;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPointImpl;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Formatter;
import java.util.List;
import java.util.ArrayList;

import org.jdom.input.SAXBuilder;
import org.jdom.*;

/**
 * Factory for feature dataset collections (CompositePointDataset).
 * Uses Composite pattern, more or less.
 * Standard factory uses a "collection specification" string, see thredds.inventory.CollectionSpecParser
 *
 * @author caron
 * @since May 20, 2009
 */

public class CompositeDatasetFactory {
  static public final String SCHEME = "collection:";
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CompositeDatasetFactory.class);
  static boolean debug = false;

  static public FeatureDataset factory(String locationURI, File configFile, Formatter errlog) throws IOException {
    SAXBuilder builder = new SAXBuilder();
    Document configDoc;
    try {
      configDoc = builder.build(configFile);
    } catch (JDOMException e) {
      errlog.format("CompositeDatasetFactory failed to read config document %s err= %s %n", configFile.getPath(), e.getMessage());
      return null;
    }

    Element root = configDoc.getRootElement();
    String type = root.getChild("type").getText();
    FeatureType wantFeatureType = FeatureType.getType(type);

    String location = root.getChild("location").getText();
    String dateFormatMark = root.getChild("dateFormatMark").getText();

    Element geo = root.getChild("geospatialCoverage");
    Element northsouth = geo.getChild("northsouth");
    double latStart = readDouble(northsouth.getChild("start"), errlog);
    double latSize = readDouble(northsouth.getChild("size"), errlog);
    Element eastwest = geo.getChild("eastwest");
    double lonStart = readDouble(eastwest.getChild("start"), errlog);
    double lonSize = readDouble(eastwest.getChild("size"), errlog);
    LatLonRect llbb = new LatLonRect(new LatLonPointImpl(latStart, lonStart), latSize, lonSize);

    Element timeCoverage = root.getChild("timeCoverage");
    DateType start = readDate(timeCoverage.getChild("start"), errlog);
    DateType end = readDate(timeCoverage.getChild("end"), errlog);
    TimeDuration duration = readDuration(timeCoverage.getChild("duration"), errlog);

    DateRange dateRange = null;
    try {
      dateRange = new DateRange(start, end, duration, null);
    } catch (java.lang.IllegalArgumentException e) {
      errlog.format(" ** warning: TimeCoverage error = %s%n", e.getMessage());
      return null;
    }

    // LOOK : WRONG
    CompositePointDataset fd = (CompositePointDataset) factory(locationURI, wantFeatureType, location + "?" + dateFormatMark, errlog);
    if (fd == null) return null;

    fd.setBoundingBox(llbb);
    fd.setDateRange(dateRange);
    return fd;
  }

  static double readDouble(Element elem, Formatter errlog) {
    if (elem == null) return Double.NaN;
    String text = elem.getText();
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      errlog.format(" ** Parse error: Bad double format %s%n", text);
      return Double.NaN;
    }
  }

  static DateType readDate(Element elem, Formatter errlog) {
    if (elem == null) return null;
    String format = elem.getAttributeValue("format");
    String type = elem.getAttributeValue("type");
    String text = elem.getText();
    if (text == null) return null;
    try {
      return new DateType(text, format, type);
    } catch (java.text.ParseException e) {
      errlog.format(" ** Parse error: Bad date format = %s%n", text);
      return null;
    }
  }

  static TimeDuration readDuration(Element elem, Formatter errlog) {
    if (elem == null) return null;
    String text = null;
    try {
      text = elem.getText();
      return new TimeDuration(text);
    } catch (java.text.ParseException e) {
      errlog.format(" ** Parse error: Bad duration format = %s%n", text);
      return null;
    }
  }

  static public FeatureDataset factory(String location, FeatureType wantFeatureType, String spec, Formatter errlog) throws IOException {
    if (spec.startsWith(SCHEME))
      spec = spec.substring(SCHEME.length());

    TimedCollection collection = new TimedCollectionImpl(spec, errlog);
    if (collection.getDatasets().size() == 0) {
      throw new FileNotFoundException("Collection is empty; spec="+spec);
    }

    LatLonRect bb = null;
    FeatureCollection fc = null;
    switch (wantFeatureType) {
      case POINT:
        CompositePointCollection pfc = new CompositePointCollection(spec, collection);
        bb = pfc.getBoundingBox();
        fc = pfc;
        break;
      case STATION:
        CompositeStationCollection sfc = new CompositeStationCollection(spec, collection, null, null);
        bb = sfc.getBoundingBox();
        fc = sfc;
        break;
      default:
        return null;
    }

    return new CompositePointDataset(location, wantFeatureType, fc, collection, bb);
  }


  private static class CompositePointDataset extends PointDatasetImpl {
    private TimedCollection datasets;
    private FeatureCollection pfc;

    public CompositePointDataset(String location, FeatureType featureType, FeatureCollection pfc,
                                 TimedCollection datasets, LatLonRect bb) {
      super(featureType);
      setLocationURI(location);
      setPointFeatureCollection(pfc);

      this.pfc = pfc;
      this.datasets = datasets;

      if (datasets.getDateRange() != null)
         setDateRange(datasets.getDateRange());

      if (bb != null)
         setBoundingBox(bb);

    }

    // deffer this is possible
   public List<VariableSimpleIF> getDataVariables() {
      if (dataVariables == null) {
        if (pfc instanceof CompositePointCollection)
          dataVariables = ((CompositePointCollection) pfc).getDataVariables();
        else if (pfc instanceof CompositeStationCollection)
          dataVariables = ((CompositeStationCollection) pfc).getDataVariables();


      }

    return dataVariables;
  }

    @Override
    protected void setDateRange(DateRange dateRange) {
      super.setDateRange(dateRange);
    }

    @Override
    protected void setBoundingBox(LatLonRect boundingBox) {
      super.setBoundingBox(boundingBox);
    }

    @Override
    public NetcdfFile getNetcdfFile() {
      FeatureDatasetPoint proto;

      TimedCollection.Dataset td = datasets.getPrototype();
      if (td == null) return null;

      String loc = td.getLocation();
      Formatter errlog = new Formatter();
      try {
        proto = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, loc, null, errlog); // LOOK kludge
        return proto.getNetcdfFile();
      } catch (IOException e) {
        log.error(errlog.toString());
        e.printStackTrace();
      }
      return null;
    }

  }

}

