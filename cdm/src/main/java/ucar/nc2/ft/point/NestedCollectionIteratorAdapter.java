/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.nc2.ft.PointFeatureCCIterator;

import java.io.IOException;
import java.util.Iterator;

/**
 * adapt a NestedPointFeatureCollectionIterator to an Iterator<PointFeatureCC>
 *
 * @author caron
 * @since 9/24/2015.
 */
public class NestedCollectionIteratorAdapter<T> implements Iterator<T> {
  PointFeatureCCIterator pfIterator;

  public NestedCollectionIteratorAdapter(PointFeatureCCIterator pfIterator) {
    this.pfIterator = pfIterator;
  }

  @Override
  public boolean hasNext() {
    try {
      return pfIterator.hasNext();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T next() {
    try {
      return (T) pfIterator.next();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
