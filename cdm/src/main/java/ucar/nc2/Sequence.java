/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import ucar.ma2.*;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Sequence is a one-dimensional Structure with indeterminate length.
 * The only data access is through getStructureIterator().
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

    List<Dimension> dims = new ArrayList<Dimension>();
    dims.add(Dimension.VLEN);
    setDimensions(dims);
    this.dataType = DataType.SEQUENCE;
  }

  /**
   * Get a StructureDataIterator for this Sequence.
   *
   * @return an Iterator over the Structures in this Sequence
   * @throws java.io.IOException if read error
   */
  @Override
  public StructureDataIterator getStructureIterator() throws java.io.IOException {
    return ncfile.getStructureDataIterator(this, null);
  }

  public StructureDataIterator getStructureIterator(SequenceDataCursor c) throws java.io.IOException {
    return ncfile.getStructureDataIterator(this, c);
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
    throw new UnsupportedOperationException();
  }

  /**
   * UnsupportedOperation
   *
   * @throws UnsupportedOperationException
   */
  @Override
  public Array read() throws IOException {
    throw new UnsupportedOperationException();
  }

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
