/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.nc2.Group;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;
import ucar.ma2.Array;

import java.io.IOException;

/**
 * Enhance sequence
 *
 * @author caron
 * @since Nov 10, 2009
 */
public class SequenceDS extends StructureDS {
  private ucar.nc2.Sequence orgSeq;

  public SequenceDS(Group g, ucar.nc2.Sequence orgSeq) {
    super(g, orgSeq);
    this.orgSeq = orgSeq;
  }

  public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    return new StructureDataConverter(this, orgSeq.getStructureIterator(bufferSize));
  }

  private static class StructureDataConverter implements StructureDataIterator {
    private StructureDataIterator orgIter;
    private SequenceDS newStruct;
    private int count = 0;

    StructureDataConverter(SequenceDS newStruct, StructureDataIterator orgIter) {
      this.newStruct = newStruct;
      this.orgIter = orgIter;
    }

    @Override
    public boolean hasNext() throws IOException {
      return orgIter.hasNext();
    }

    @Override
    public StructureData next() throws IOException {
      StructureData sdata = orgIter.next();
      return newStruct.convert(sdata, count++);
    }

    @Override
    public void setBufferSize(int bytes) {
      orgIter.setBufferSize(bytes);
    }

    @Override
    public StructureDataIterator reset() {
      orgIter = orgIter.reset();
      return (orgIter == null) ? null : this;
    }

    @Override
    public int getCurrentRecno() {
      return orgIter.getCurrentRecno();
    }

    @Override
    public void close() {
      orgIter.close();
    }
  }

  @Override
  public Array read(ucar.ma2.Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException {
    return read();
  }

  @Override
  public Array read() throws IOException {
    Array data = orgSeq.read();
    return convert(data, null);
  }

}
