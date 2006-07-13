// $Id:VariableIF.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import java.util.List;

/**
 * The public interface to a Variable.
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public interface VariableIF {
    public java.lang.String getName();
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

    public java.util.List getDimensions();
    public ucar.nc2.Dimension getDimension(int index);
    public int findDimensionIndex(java.lang.String dimName);
    public List getDimensionsAll();
    public Dimension getCoordinateDimension();

    public java.util.List getAttributes();
    public ucar.nc2.Attribute findAttribute(java.lang.String attName);
    public ucar.nc2.Attribute findAttributeIgnoreCase(java.lang.String attName);

    public ucar.nc2.Group getParentGroup();
    // public boolean isSection();
    public ucar.nc2.Variable section(java.util.List ranges) throws ucar.ma2.InvalidRangeException;
    public java.util.List getRanges();

    public ucar.ma2.Array read(int[] origin, int[] shape) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    public ucar.ma2.Array read(java.lang.String rangeSpec) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    public ucar.ma2.Array read(java.util.List ranges) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    public ucar.ma2.Array read() throws java.io.IOException;
    public boolean isMemberOfStructure();
    public boolean isVariableLength();
    public boolean isMetadata();
    public ucar.nc2.Structure getParentStructure();

    public ucar.ma2.Array readAllStructuresSpec(java.lang.String rangeSpec, boolean flatten)
       throws java.io.IOException, ucar.ma2.InvalidRangeException;
    public ucar.ma2.Array readAllStructures(java.util.List ranges, boolean flatten)
       throws java.io.IOException, ucar.ma2.InvalidRangeException;
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