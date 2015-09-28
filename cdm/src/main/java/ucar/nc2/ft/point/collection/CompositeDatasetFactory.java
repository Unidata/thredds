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
package ucar.nc2.ft.point.collection;

import thredds.inventory.MFileCollectionManager;
import thredds.inventory.TimedCollection;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.PointDatasetImpl;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * Factory for point feature dataset collections (CompositePointDataset).
 * Uses Composite pattern, more or less.
 *
 * @author caron
 * @since May 20, 2009
 */

public class CompositeDatasetFactory {
  static public final String SCHEME = "collection:";
  // static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CompositeDatasetFactory.class);
  static boolean debug = false;

  static public FeatureDataset factory(String location, FeatureType wantFeatureType, MFileCollectionManager dcm, Formatter errlog) throws IOException {

    TimedCollection collection = new TimedCollection(dcm, errlog);
    if (collection.getDatasets().size() == 0) {
      throw new FileNotFoundException("Collection is empty; spec=" + dcm);
    }

    DsgFeatureCollection first;
    TimedCollection.Dataset d = collection.getPrototype();
    try (FeatureDatasetPoint proto = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(wantFeatureType, d.getLocation(), null, errlog)) {
      if (proto == null) {
        throw new FileNotFoundException("Collection dataset is not a FeatureDatasetPoint; spec=" + dcm);
      }
      if (wantFeatureType == FeatureType.ANY_POINT) wantFeatureType = proto.getFeatureType();

      List<DsgFeatureCollection> fcList = proto.getPointFeatureCollectionList();
      if (fcList.size() == 0) {
        throw new FileNotFoundException("FeatureCollectionList is empty; spec=" + dcm);
      }
      first = fcList.get(0);

      //LatLonRect bb = null;
      DsgFeatureCollection fc;
      switch (wantFeatureType) {
        case POINT:
          PointFeatureCollection firstPc = (PointFeatureCollection) first;
          CompositePointCollection pfc = new CompositePointCollection(dcm.getCollectionName(), firstPc.getTimeUnit(), firstPc.getAltUnits(), collection);
          //bb = pfc.getBoundingBox();
          fc = pfc;
          break;
        case STATION:
          PointFeatureCC firstNpc = (PointFeatureCC) first;
          CompositeStationCollection sfc = new CompositeStationCollection(
                  dcm.getCollectionName(), firstNpc.getTimeUnit(), firstNpc.getAltUnits(), collection);
          //bb = sfc.getBoundingBox();
          fc = sfc;
          break;
        default:
          return null;
      }

      return new CompositePointDataset(location, wantFeatureType, fc, collection, null);
    }
  }

  private static class CompositePointDataset extends PointDatasetImpl implements UpdateableCollection  {
    private DsgFeatureCollection pfc;
    private List<Attribute> globalAttributes;

    public CompositePointDataset(String location, FeatureType featureType, DsgFeatureCollection pfc,
                                 TimedCollection datasets, LatLonRect bb) {
      super(featureType);
      setLocationURI(location);
      setPointFeatureCollection(pfc);

      this.pfc = pfc;
      this.dateRange = datasets.getDateRange();

      if (datasets.getDateRange() != null)
         setDateRange(datasets.getDateRange());

      if (bb != null)
         setBoundingBox(bb);

    }

    // defer this if possible
    @Override
   public List<VariableSimpleIF> getDataVariables() {
      if (dataVariables == null) {
        if (pfc instanceof CompositePointCollection)
          dataVariables = ((CompositePointCollection) pfc).getDataVariables();
        else if (pfc instanceof CompositeStationCollection)
          dataVariables = ((CompositeStationCollection) pfc).getDataVariables();
      }

    return dataVariables;
  }

    public List<Attribute> getGlobalAttributes() {
      if (globalAttributes == null) {
        if (pfc instanceof CompositePointCollection)
          globalAttributes = ((CompositePointCollection) pfc).getGlobalAttributes();
        else if (pfc instanceof CompositeStationCollection)
          globalAttributes = ((CompositeStationCollection) pfc).getGlobalAttributes();
      }

      return globalAttributes;
    }

    @Override
    public void setDateRange(CalendarDateRange dateRange) {
      super.setDateRange(dateRange);
    }

    @Override
    public void setBoundingBox(LatLonRect boundingBox) {
      super.setBoundingBox(boundingBox);
    }


    @Override
    public CalendarDateRange update() throws IOException {
      UpdateableCollection uc = (UpdateableCollection) pfc;
      return uc.update();
    }    

    /* @Override
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
    } */

  }

}