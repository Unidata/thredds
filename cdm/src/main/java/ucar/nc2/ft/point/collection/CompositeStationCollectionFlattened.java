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

import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.ft.point.PointIteratorAbstract;
import ucar.nc2.ft.*;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.units.DateRange;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;

import java.util.List;
import java.util.Formatter;
import java.util.Iterator;
import java.io.IOException;

/**
 * CompositeStationCollection that has been flattened into a PointCollection
 *
 * @author caron
 * @since Aug 28, 2009
 */


public class CompositeStationCollectionFlattened extends PointCollectionImpl {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CompositeStationCollectionFlattened.class);

  private TimedCollection stnCollections;
  private LatLonRect bbSubset;
  private List<String> stationsSubset;
  private DateRange dateRange;
  private List<VariableSimpleIF> varList;
  private boolean wantStationsubset = false;

  protected CompositeStationCollectionFlattened(String name, List<String> stations, DateRange dateRange,
                List<VariableSimpleIF> varList, TimedCollection stnCollections) throws IOException {
    super( name);
    this.stationsSubset = stations; // note these will be from the original collection, must transfer
    this.dateRange = dateRange;
    this.varList = varList;
    this.stnCollections = stnCollections;

    wantStationsubset = (stations != null) && (stations.size() > 0);
  }

  protected CompositeStationCollectionFlattened(String name, LatLonRect bbSubset, DateRange dateRange, TimedCollection stnCollections) throws IOException {
    super( name);
    this.bbSubset = bbSubset;
    this.dateRange = dateRange;
    this.stnCollections = stnCollections;
  }

  public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
    PointIterator iter = new PointIterator();
    if ((boundingBox == null) || (dateRange == null) || (npts < 0))
      iter.setCalculateBounds(this);
    return iter;
  }

  private class PointIterator extends PointIteratorAbstract {
    private boolean finished = false;
    private int bufferSize = -1;
    private Iterator<TimedCollection.Dataset> iter;
    private FeatureDatasetPoint currentDataset;
    private PointFeatureIterator pfIter = null;

    PointIterator() {
      iter = stnCollections.getDatasets().iterator();
    }

    private PointFeatureIterator getNextIterator() throws IOException {
      if (!iter.hasNext()) return null;
      TimedCollection.Dataset td = iter.next();
      Formatter errlog = new Formatter();

      // open the next dataset
      currentDataset = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.STATION, td.getLocation(), null, errlog);
      if (currentDataset == null) {
        logger.error("FeatureDatasetFactoryManager failed to open: "+td.getLocation()+" \nerrlog = " + errlog);
        return getNextIterator();
      }

      if (CompositeDatasetFactory.debug)
        System.out.printf("CompositeStationCollectionFlattened.Iterator open new dataset: %s%n", td.getLocation());

      // it will have a StationTimeSeriesFeatureCollection
      List<FeatureCollection> fcList = currentDataset.getPointFeatureCollectionList();
      StationTimeSeriesFeatureCollection stnCollection = (StationTimeSeriesFeatureCollection) fcList.get(0);

      PointFeatureCollection pc = null;
      if (wantStationsubset)
        pc = stnCollection.flatten(stationsSubset, dateRange, varList);
      else
        pc = stnCollection.flatten(bbSubset, dateRange);

      return pc.getPointFeatureIterator(bufferSize);
    }

    public boolean hasNext() throws IOException {
      
        if (pfIter == null) {
        pfIter = getNextIterator();
        if (pfIter == null) {
          finish();
          return false;
        }
      }

      if (!pfIter.hasNext()) {
        pfIter.finish();
        currentDataset.close();
        if (CompositeDatasetFactory.debug)
           System.out.printf("CompositeStationCollectionFlattened.Iterator close dataset: %s%n", currentDataset.getLocation());
        pfIter = getNextIterator();
        return hasNext();
      }
;
      return true;
    }

    public PointFeature next() throws IOException {
      return pfIter.next();
    }

    public void finish() {
      if (finished) return;

      if (pfIter != null)
        pfIter.finish();
      finishCalcBounds();

      if (currentDataset != null)
        try {
          currentDataset.close();
          if (CompositeDatasetFactory.debug)
              System.out.printf("CompositeStationCollectionFlattened close dataset: %s%n", currentDataset.getLocation());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      finished = true;
    }

    public void setBufferSize(int bytes) {
      bufferSize = bytes;
    }
  }

}

