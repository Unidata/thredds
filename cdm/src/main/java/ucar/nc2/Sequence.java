/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.ma2.*;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Sequence is a one-dimensional Structure with indeterminate length.
 * The only data access is through getStructureIterator().
 * However, read() will read in the entire data and return an ArraySequence.
 *
 * @author caron
 * @since Feb 23, 2008
 */
public class Sequence extends Structure {

  /* Sequence Constructor
  *
  * @param ncfile    the containing NetcdfFile.
  * @param group     the containing group; if null, use rootGroup
  * @param parent    parent Structure, may be null
  * @param shortName variable shortName, must be unique within the Group
  */
  public Sequence(NetcdfFile ncfile, Group group, Structure parent, String shortName) {
    super(ncfile, group, parent, shortName);

    List<Dimension> dims = new ArrayList<>();
    dims.add(Dimension.VLEN);
    setDimensions(dims);
    setDataType(DataType.SEQUENCE);
  }

  public StructureDataIterator getStructureIterator(int bufferSize) throws java.io.IOException {
    return ncfile.getStructureIterator(this, bufferSize);
  }

  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public Array read(int[] origin, int[] shape) throws IOException, InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public Array read(String sectionSpec) throws IOException, InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public Array read(List<Range> ranges) throws IOException, InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public Array read(ucar.ma2.Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException {
    return read();
  }

  /*
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   *
  @Override
  public Array read() throws IOException {
    throw new UnsupportedOperationException();
  } */

  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public StructureData readStructure() throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public StructureData readStructure(int index) throws IOException, ucar.ma2.InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public ArrayStructure readStructure(int start, int count) throws IOException, ucar.ma2.InvalidRangeException {
    throw new UnsupportedOperationException();
  }


  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public Variable slice(int dim, int value) throws InvalidRangeException {
    throw new UnsupportedOperationException();
  }

  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public Variable section(Section subsection) throws InvalidRangeException {
    throw new UnsupportedOperationException();
  }

}
