/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2;

import ucar.ma2.Range;
import ucar.ma2.Section;

/**
 * The interface to a Variable.
 * @author caron
 */

public interface VariableIF extends VariableSimpleIF {
    String getFullName();
    String getFullNameEscaped();
    String getShortName();
    void getNameAndDimensions(java.util.Formatter result, boolean useFullName, boolean strict);

    boolean isUnlimited();
    ucar.ma2.DataType getDataType();
    EnumTypedef getEnumTypedef();
    int getRank();
    boolean isScalar();
    long getSize();
    int getElementSize();
    int[] getShape();

    java.util.List<Dimension> getDimensions();
    ucar.nc2.Dimension getDimension(int index);
    int findDimensionIndex(String dimName);

    java.util.List<Attribute> getAttributes();
    ucar.nc2.Attribute findAttribute(String attName);
    ucar.nc2.Attribute findAttributeIgnoreCase(String attName);

    ucar.nc2.Group getParentGroup();
    ucar.nc2.Variable section(java.util.List<Range> ranges) throws ucar.ma2.InvalidRangeException;
    Section getShapeAsSection();
    java.util.List<Range> getRanges();

    ucar.ma2.Array read(int[] origin, int[] shape) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    ucar.ma2.Array read(String rangeSpec) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    ucar.ma2.Array read(ucar.ma2.Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException;
    ucar.ma2.Array read() throws java.io.IOException;

    boolean isCoordinateVariable();
    boolean isMemberOfStructure();
    boolean isVariableLength();
    boolean isMetadata();
    ucar.nc2.Structure getParentStructure();

    String getDescription();
    String getUnitsString();

    // use only if isMemberOfStructure
    java.util.List<Dimension> getDimensionsAll();

    // use only if isScalar()
    byte readScalarByte() throws java.io.IOException;
    short readScalarShort() throws java.io.IOException;
    int readScalarInt() throws java.io.IOException;
    long readScalarLong() throws java.io.IOException;
    float readScalarFloat() throws java.io.IOException;
    double readScalarDouble() throws java.io.IOException;
    String readScalarString() throws java.io.IOException;

    // debug
    String toStringDebug();
}
