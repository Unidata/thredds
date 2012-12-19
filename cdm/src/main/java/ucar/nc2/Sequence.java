/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

    List<Dimension> dims = new ArrayList<Dimension>();
    dims.add(Dimension.VLEN);
    setDimensions(dims);
    this.dataType = DataType.SEQUENCE;
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
