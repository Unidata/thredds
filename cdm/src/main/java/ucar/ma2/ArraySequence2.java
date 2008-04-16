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
package ucar.ma2;

/**
 * @author caron
 * @since Feb 27, 2008
 */
public class ArraySequence2 extends ArrayStructure {
  private StructureDataIterator iter;

  public ArraySequence2(StructureMembers members, StructureDataIterator iter) {
    super(members, new int[] {0});
    this.iter = iter;
  }

  @Override
  public Class getElementType() {
    return StructureDataIterator.class;
  }

  @Override
  public StructureDataIterator getStructureDataIterator() throws java.io.IOException {
    iter = iter.reset();
    return iter;
  }

  protected StructureData makeStructureData(ArrayStructure as, int index) {
    throw new UnsupportedOperationException("Cannot subset a Sequence");
  }

}
