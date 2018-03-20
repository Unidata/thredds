/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;

import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;

/**
 * A PointFeatureIterator which uses a StructureDataIterator to iterate over members of a Structure,
 * with optional filtering and calculation of time range and bounding box.
 * <p>
 * Subclass must implement makeFeature() to turn the StructureData into a PointFeature.
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class PointIteratorFromStructureData extends PointIteratorAbstract {

  // makeFeature may return null, if so then skip it and go to next iteration
  protected abstract PointFeature makeFeature(int recnum, StructureData sdata) throws IOException;

  private PointFeatureIterator.Filter filter;
  private StructureDataIterator structIter;
  private PointFeature feature = null; // hasNext must cache
  private boolean finished = false;

  /**
   * Constructor
   *
   * @param structIter original iterator
   * @param filter     optional filter
   * @throws IOException
   */
  public PointIteratorFromStructureData(StructureDataIterator structIter, PointFeatureIterator.Filter filter) throws IOException {
    this.structIter = structIter;
    this.filter = filter;
  }

  @Override
  public boolean hasNext() {
    try {
      while (true) {
        StructureData sdata = nextStructureData();
        if (sdata == null) break;
        feature = makeFeature(structIter.getCurrentRecno(), sdata);
        if (feature == null) continue;
        if (feature.getLocation().isMissing()) {
          continue;
        }
        if (filter == null || filter.filter(feature))
          return true;
      }

      // all done
      feature = null;
      close();
      return false;

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public PointFeature next() {
    if (feature == null) return null;
    calcBounds(feature);
    return feature;
  }

  @Override
  public void close() {
    if (finished) return;
    finishCalcBounds();
    finished = true;
    structIter.close();
  }

  private StructureData nextStructureData() throws IOException {
    return structIter.hasNext() ? structIter.next() : null;
  }
}
