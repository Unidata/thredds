/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

import ucar.ma2.Range;
import ucar.ma2.Section;

/**
 * The public interface to a Variable.
 * @author caron
 */

public interface VariableIF extends VariableSimpleIF {
    public java.lang.String getName();
    public java.lang.String getNameEscaped();
    public java.lang.String getShortName();
    public void getNameAndDimensions(java.lang.StringBuffer result, boolean useFullName, boolean strict);

    public boolean isUnlimited();
    public boolean isUnsigned();
    public ucar.ma2.DataType getDataType();
    public int getRank();
    public boolean isScalar();
    public long getSize();
    public int getElementSize();
    public int[] getShape();

    public java.util.List<Dimension> getDimensions();
    public ucar.nc2.Dimension getDimension(int index);
    public int findDimensionIndex(java.lang.String dimName);

    public java.util.List<Attribute> getAttributes();
    public ucar.nc2.Attribute findAttribute(java.lang.String attName);
    public ucar.nc2.Attribute findAttributeIgnoreCase(java.lang.String attName);

    public ucar.nc2.Group getParentGroup();
    public ucar.nc2.Variable section(java.util.List<Range> ranges) throws ucar.ma2.InvalidRangeException;
    public Section getShapeAsSection();
    public java.util.List<Range> getRanges();

    public ucar.ma2.Array read(int[] origin, int[] shape) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    public ucar.ma2.Array read(java.lang.String rangeSpec) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    //public ucar.ma2.Array read(java.util.List<Range> ranges) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    public ucar.ma2.Array read(ucar.ma2.Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    public ucar.ma2.Array read() throws java.io.IOException;

    public boolean isCoordinateVariable();
    public boolean isMemberOfStructure();
    public boolean isVariableLength();
    public boolean isMetadata();
    public ucar.nc2.Structure getParentStructure();

    public String getDescription();
    public String getUnitsString();

    // use only if isMemberOfStructure
    public java.util.List<Dimension> getDimensionsAll();
    public ucar.ma2.Array readAllStructuresSpec(java.lang.String rangeSpec, boolean flatten) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    //public ucar.ma2.Array readAllStructures(java.util.List<Range> ranges, boolean flatten) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    public ucar.ma2.Array readAllStructures(ucar.ma2.Section section, boolean flatten) throws java.io.IOException, ucar.ma2.InvalidRangeException;

    // use only if isScalar()
    public byte readScalarByte() throws java.io.IOException;
    public short readScalarShort() throws java.io.IOException;
    public int readScalarInt() throws java.io.IOException;
    public long readScalarLong() throws java.io.IOException;
    public float readScalarFloat() throws java.io.IOException;
    public double readScalarDouble() throws java.io.IOException;
    public java.lang.String readScalarString() throws java.io.IOException;

    // debug
    public java.lang.String toStringDebug();
}