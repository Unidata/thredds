package ucar.nc2.iosp.noaa;

import ucar.ma2.*;
import ucar.unidata.io.RandomAccessFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create a StructureData by using a java.util.regex.Pattern on an ascii file.
 *
 * @author caron
 * @since Feb 26, 2011
 */
public class StructureDataRegexp extends StructureData {
  protected Matcher matcher;

  public StructureDataRegexp(StructureMembers members, Matcher m) {
    super(members);
    this.matcher = m;
  }

  protected Object parse(DataType dt, VinfoField vinfo) throws NumberFormatException {
    return parse(dt, vinfo, vinfo.fldno);
  }

  protected Object parse(DataType dt, VinfoField vinfo, int fldno) throws NumberFormatException {
    String svalue = (fldno <= matcher.groupCount()) ? matcher.group(fldno) : " ";
    //  System.out.printf("HEY! %d>= %d %n", field, matcher.groupCount());
    //String svalue = matcher.group(field);

    if ((dt == DataType.STRING) || (dt == DataType.CHAR))
      return svalue.trim();

    try {
      svalue = svalue.trim();
      boolean isBlank = (svalue.length() == 0);
      if (dt == DataType.DOUBLE)
        return isBlank ? 0.0 : new Double(svalue);
      else if (dt == DataType.FLOAT) {
        float result = isBlank ? 0.0f : new Float(svalue);
        return (vinfo.hasScale) ? result * vinfo.scale : result;
      } else if (dt == DataType.INT) {
        return isBlank ? 0 : new Integer(svalue);
      }
      else if (dt == DataType.LONG)
        return isBlank ? 0 : new Long(svalue);

    } catch (NumberFormatException e) {
      System.out.printf("  %d = <%s> %n", fldno, svalue);
      throw e;
    }

    return null;
  }


  @Override
  public Array getArray(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();

    if (m.getDataType() == DataType.STRING) {
      String result = matcher.group(f.fldno);
      return new ArrayObject(String.class, new int[] {},  new Object[] {result.trim()});

    } else if (m.getDataType() == DataType.SEQUENCE) {
      return getArraySequence(m);

    } else if (!m.isScalar()) {
      if (m.getDataType() == DataType.FLOAT) {
        float[] ja = getJavaArrayFloat(m);
        return Array.factory(DataType.FLOAT, m.getShape(), ja);

      } else if (m.getDataType() == DataType.CHAR) {
        char[] ja = getJavaArrayChar(m);
        return Array.factory(DataType.CHAR, m.getShape(), ja);

      } else if (m.getDataType() == DataType.BYTE) {
        byte[] ja = getJavaArrayByte(m);
        return Array.factory(DataType.BYTE, m.getShape(), ja);
      }
    }

    Object result = parse(m.getDataType(), f);
    if (m.getDataType() == DataType.CHAR)
      return new ArrayChar((String) result);
    else
      return new ArrayScalar(result);
  }

  @Override
  public float convertScalarFloat(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return ((Number) parse(m.getDataType(), f)).floatValue();
  }

  @Override
  public double convertScalarDouble(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return ((Number) parse(m.getDataType(), f)).doubleValue();
  }

  @Override
  public int convertScalarInt(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return ((Number) parse(m.getDataType(), f)).intValue();
  }

  @Override
  public long convertScalarLong(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return ((Number) parse(m.getDataType(), f)).longValue();
  }

  @Override
  public double getScalarDouble(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return (Double) parse(m.getDataType(), f);
  }

  @Override
  public double[] getJavaArrayDouble(StructureMembers.Member m) {
    return new double[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public float getScalarFloat(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    Object result =  parse(m.getDataType(), f);
    return (result instanceof Float) ? (Float) result : ((Double) result).floatValue();
  }

  @Override
  public float[] getJavaArrayFloat(StructureMembers.Member m) {
    int n = m.getSize();
    float[] result = new float[n];
    VinfoField f = (VinfoField) m.getDataObject();
    for (int i=0; i<n; i++)
      result[i] = (Float) parse(m.getDataType(), f, f.fldno + f.stride*i);
    return result;
  }

  @Override
  public byte getScalarByte(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return (Byte) parse(m.getDataType(), f);
  }

  @Override
  public byte[] getJavaArrayByte(StructureMembers.Member m) {
    int n = m.getSize();
    byte[] result = new byte[n];
    VinfoField f = (VinfoField) m.getDataObject();
    for (int i=0; i<n; i++) {
      String s = (String) parse(m.getDataType(), f, f.fldno + f.stride*i);
      result[i] = (byte) s.charAt(0);
    }
    return result;
  }

  @Override
  public int getScalarInt(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return (Integer) parse(m.getDataType(), f);
  }

  @Override
  public int[] getJavaArrayInt(StructureMembers.Member m) {
    return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public short getScalarShort(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return (Short) parse(m.getDataType(), f);
  }

  @Override
  public short[] getJavaArrayShort(StructureMembers.Member m) {
    return new short[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public long getScalarLong(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return (Long) parse(m.getDataType(), f);
  }

  @Override
  public long[] getJavaArrayLong(StructureMembers.Member m) {
    return new long[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public char getScalarChar(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    String result = (String) parse(m.getDataType(), f);
    return result.charAt(0);
  }

  @Override
  public char[] getJavaArrayChar(StructureMembers.Member m) {
    int n = m.getSize();
    char[] result = new char[n];
    VinfoField f = (VinfoField) m.getDataObject();
    for (int i=0; i<n; i++) {
      String s = (String) parse(m.getDataType(), f, f.fldno + f.stride*i);
      result[i] = s.charAt(0);
    }
    return result;
  }

  @Override
  public String getScalarString(StructureMembers.Member m) {
    VinfoField f = (VinfoField) m.getDataObject();
    return (String) parse(m.getDataType(), f);
  }

  @Override
  public String[] getJavaArrayString(StructureMembers.Member m) {
    return new String[] {getScalarString(m)};
  }

  @Override
  public StructureData getScalarStructure(StructureMembers.Member m) {
    return null;
  }

  @Override
  public ArrayStructure getArrayStructure(StructureMembers.Member m) {
    return null;
  }

  @Override
  public ArraySequence getArraySequence(StructureMembers.Member m) {
    return null;
  }
  
  static public class Vinfo {
    RandomAccessFile rafile;
    StructureMembers sm;
    Pattern p;
    int nelems = -1;

    public Vinfo(RandomAccessFile raff, StructureMembers sm, Pattern p) {
      this.sm = sm;
      this.rafile = raff;
      this.p = p;
    }
  }

  static public class VinfoField {
    int fldno;
    int stride = 4;
    float scale;
    boolean hasScale;

    public VinfoField(int fldno) {
      this.fldno = fldno;
    }
  }
  

}

