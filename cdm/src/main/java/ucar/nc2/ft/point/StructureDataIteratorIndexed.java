/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;
import ucar.nc2.Structure;

import java.io.IOException;
import java.util.List;
import java.util.Iterator;

/**
 * A StructureDataIterator which takes a list of record numbers (in a structure).
 *
 * @author caron
 * @since Feb 11, 2009
 */
public class StructureDataIteratorIndexed implements StructureDataIterator {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StructureDataIteratorLinked.class);

  private Structure s;
  private List<Integer> index;
  private Iterator<Integer> indexIter;
  private int currRecord;

  public StructureDataIteratorIndexed(Structure s, List<Integer> index) throws IOException {
    this.s = s;
    this.index = index;
    reset();
  }

  @Override
  public StructureData next() throws IOException {
    StructureData sdata;
    currRecord = indexIter.next();
    try {
      sdata = s.readStructure(currRecord);
    } catch (ucar.ma2.InvalidRangeException e) {
      log.error("StructureDataIteratorIndexed.nextStructureData recno=" + currRecord, e);
      throw new IOException(e.getMessage());
    }
    return sdata;
  }

  @Override
  public boolean hasNext() throws IOException {
    return indexIter.hasNext();
  }

  @Override
  public StructureDataIterator reset() {
    this.indexIter = index.iterator();
    return this;
  }

  @Override
  public int getCurrentRecno() {
    return currRecord;
  }

}