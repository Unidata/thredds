/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
