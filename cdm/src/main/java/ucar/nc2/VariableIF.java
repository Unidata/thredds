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
    public void getNameAndDimensions(java.util.Formatter result, boolean useFullName, boolean strict);

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