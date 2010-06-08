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
import ucar.nc2.units.DateRange;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.VariableSimpleIF;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.util.Formatter;

/**
 * PointCollection composed of other PointCollections
 *
 * @author caron
 * @since May 19, 2009
 */
public class CompositePointCollection extends PointCollectionImpl {
  private TimedCollection pointCollections;
  protected List<VariableSimpleIF> dataVariables;

  protected CompositePointCollection(String name, TimedCollection pointCollections) throws IOException {
    super(name);
    this.pointCollections = pointCollections;
  }

  public List<VariableSimpleIF> getDataVariables()  {
    if (dataVariables == null) {
      // must open a prototype in order to get the data variable
      TimedCollection.Dataset td = pointCollections.getPrototype();
      if (td == null)
        throw new RuntimeException("No datasets in the collection");

      Formatter errlog = new Formatter();
      FeatureDatasetPoint openDataset = null;
      try {
        openDataset = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.POINT, td.getLocation(), null, errlog);
        dataVariables = openDataset.getDataVariables();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);

      } finally {
        try {
          if (openDataset != null) openDataset.close();
        } catch (Throwable t) {
        }
      }
    }
    return dataVariables;
  }

  @Override
  public PointFeatureCollection subset(LatLonRect boundingBox, DateRange dateRange) throws IOException {
    if ((dateRange == null) && (boundingBox == null))
      return this;
    else if (dateRange == null)
      return new PointCollectionSubset(this, boundingBox, dateRange);
    else {
      CompositePointCollection dateSubset = new CompositePointCollection(name, pointCollections.subset(dateRange));
      return new PointCollectionSubset(dateSubset, boundingBox, dateRange);
    }
  }

  public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
    CompositePointFeatureIterator iter = new CompositePointFeatureIterator();
    if ((boundingBox == null) || (dateRange == null) || (npts < 0))
      iter.setCalculateBounds(this);
    return iter;
  }

  private class CompositePointFeatureIterator extends PointIteratorAbstract {
    private boolean finished = false;
    private int bufferSize = -1;
    private Iterator<TimedCollection.Dataset> iter;
    private FeatureDatasetPoint currentDataset;
    private PointFeatureIterator pfIter = null;

    CompositePointFeatureIterator() {
      iter = pointCollections.getDatasets().iterator();
    }

    private PointFeatureIterator getNextIterator() throws IOException {
      if (!iter.hasNext()) return null;
      TimedCollection.Dataset td = iter.next();
      Formatter errlog = new Formatter();
      currentDataset = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.POINT, td.getLocation(), null, errlog);
      if (CompositeDatasetFactory.debug)
        System.out.printf("CompositePointFeatureIterator open dataset %s%n", td.getLocation());
      List<FeatureCollection> fcList = currentDataset.getPointFeatureCollectionList();
      PointFeatureCollection pc = (PointFeatureCollection) fcList.get(0);
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
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      finished = true;
    }

    public void setBufferSize(int bytes) {
      bufferSize = bytes;
    }
  }

  private class CompositePointFeatureIteratorMultithreaded extends PointIteratorAbstract {
    private boolean finished = false;
    private int bufferSize = -1;
    private Iterator<TimedCollection.Dataset> iter;
    private FeatureDatasetPoint currentDataset;
    private PointFeatureIterator pfIter = null;

    CompositePointFeatureIteratorMultithreaded() {
      iter = pointCollections.getDatasets().iterator();
    }

    private PointFeatureIterator getNextIterator() throws IOException {
      if (!iter.hasNext()) return null;
      TimedCollection.Dataset td = iter.next();
      Formatter errlog = new Formatter();
      currentDataset = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.POINT, td.getLocation(), null, errlog);
      if (CompositeDatasetFactory.debug)
        System.out.printf("CompositePointFeatureIterator open dataset %s%n", td.getLocation());
      List<FeatureCollection> fcList = currentDataset.getPointFeatureCollectionList();
      PointFeatureCollection pc = (PointFeatureCollection) fcList.get(0);
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
