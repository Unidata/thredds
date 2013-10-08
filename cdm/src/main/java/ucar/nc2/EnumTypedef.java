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

import net.jcip.annotations.Immutable;
import ucar.ma2.DataType;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Enumeration Typedef map integers to Strings.
 * For ENUM1, ENUM2, ENUM4 enumeration types.
 * Immutable.
 *
 * @author caron
 */
@Immutable
public class EnumTypedef extends CDMNode {

  // Constants for the unsigned max values for enum(1,2,4)
  static public final int UBYTE_MAX = 255;
  static public final int USHORT_MAX = 65535;
  //not used static public final long UINT_MAX = 4294967295L;

  private final Map<Integer, String> map;
  private ArrayList<String> enumStrings;
  private DataType basetype;

  public EnumTypedef(String name, Map<Integer, String> map) {
    this(name,map,DataType.ENUM4); //default basetype
  }

  public EnumTypedef(String name, Map<Integer, String> map, DataType basetype)
  {
    super(name);
    assert(validateMap(map,basetype));
    this.map = map;
    setBaseType(basetype); // default
  }

  /*Obsolete
  public String getName() { return getShortName(); }
  */

  public List<String> getEnumStrings() {
    if (enumStrings != null) {
      enumStrings = new ArrayList<String>(map.values());
      Collections.sort(enumStrings);
    }
    return enumStrings;
  }
  public Map<Integer, String> getMap() {
    return map;
  }

  public DataType getBaseType() {return this.basetype;}
  public void setBaseType(DataType basetype)
  {
     switch (basetype) {
     case ENUM1:    
     case ENUM2:
     case ENUM4:
	this.basetype = basetype;
	break;
     default: assert(false) : "Illegal Enum basetype";
     }
  }

  public boolean
  validateMap(Map<Integer, String> map, DataType basetype)
  {
     if(map == null || basetype == null) return false;
     for(Integer I: map.keySet()) {
        // WARNING, we do not have signed/unsigned info available
        int i = (int)I;
        switch (basetype) {
        case ENUM1:
            if(i < Byte.MIN_VALUE || i > UBYTE_MAX)
                return false;
            break;
        case ENUM2:
           if(i < Short.MIN_VALUE || i > USHORT_MAX)
            return false;
           break;
        case ENUM4:
            break; // enum4 is always ok
        default:
            return false;
        }
     }
     return true;
  }

  private boolean
  IgnoreinRange(int i)
  {
    // WARNING, we do not have signed/unsigned info available
    if(this.basetype == DataType.ENUM1
       && (i >= Byte.MIN_VALUE || i <= UBYTE_MAX))
        return true;
    else if(this.basetype == DataType.ENUM2
       && (i >= Short.MIN_VALUE || i <= USHORT_MAX))
        return true;
    else if(this.basetype == DataType.ENUM4) // always ok
        return true;
    else
        return false;
  }

  public String lookupEnumString(int e) {
    String result = map.get(e);
    return (result == null) ? "Unknown enum value="+e : result;
  }

  /** String representation.
   * @param strict if true, write in strict adherence to CDL definition.
   * @return CDL representation.
   */
  public String writeCDL(boolean strict) {
    Formatter out = new Formatter();
    writeCDL(out, new Indent(2), strict);
    return out.toString();
  }

  protected void writeCDL(Formatter out, Indent indent, boolean strict) {
    String name = strict ? NetcdfFile.makeValidCDLName(getShortName()) : getShortName();
    String basetype = "";
    switch (this.basetype) {
    case ENUM1: basetype = "byte "; break;
    case ENUM2: basetype = "short "; break;
    case ENUM4: basetype = ""; break;
    default: assert false : "Internal error";
    }
    out.format("%s%senum %s { ", indent, basetype, name);
    int count = 0;
    List<Object> keyset = Arrays.asList(map.keySet().toArray());
    //Collections.sort(keyset);
    for (Object key : keyset) {
      String s = map.get(key);
      if (0 < count++) out.format(", ");
      if (strict)
        out.format("%s = %s", NetcdfFile.makeValidCDLName(s), key);
      else
        out.format("'%s' = %s", s, key);
    }
    out.format("};");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    EnumTypedef that = (EnumTypedef) o;

    if (map != null ? !map.equals(that.map) : that.map != null) return false;
    String name = getShortName();
    String thatname = that.getShortName();
    if (name != null ? !name.equals(thatname) : thatname != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
if(CDMNode.OBJECTHASH) return super.hashCode(); else {
    String name = getShortName();
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (map != null ? map.hashCode() : 0);
    return result;
}
  }

  @Override
  public String toString() {
    final Formatter f = new Formatter();
    f.format("EnumTypedef %s: ", getShortName());
    for (int key : map.keySet()) {
      f.format("%d=%s,", key, map.get(key));
    }
    return f.toString();
  }
}
