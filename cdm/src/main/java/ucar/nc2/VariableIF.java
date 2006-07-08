package ucar.nc2;

import java.util.List;

/**
 * The public interface to a Variable.
 * @author caron
 * @version $Revision$ $Date$
 *
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