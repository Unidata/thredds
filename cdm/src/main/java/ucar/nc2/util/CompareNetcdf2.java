/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestCompare.java 51 2006-07-12 17:13:13Z caron $

package ucar.nc2.util;

import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.*;
import ucar.nc2.*;
import ucar.ma2.*;
import ucar.nc2.iosp.netcdf4.Nc4;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Formatter;
import java.util.ArrayList;

/**
 * Compare two NetcdfFile.
 * Dont use assert, place results in Formatter.
 *
 * @author john
 */
public class CompareNetcdf2 {

  public interface ObjFilter {
    // if true, compare attribute, else skip comparision
    boolean attCheckOk(Variable v, Attribute att);

    // if true, compare variable, else skip comparision
    boolean varDataTypeCheckOk(Variable v);
  }

  public static class Netcdf4ObjectFilter implements ObjFilter {
    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      // if (v != null && v.isMemberOfStructure()) return false;
      String name = att.getShortName();

      // added by cdm
      if (name.equals(CDM.CHUNK_SIZES)) return false;
      if (name.equals(CDM.FILL_VALUE)) return false;
      if (name.equals("_lastModified")) return false;

      // hidden by nc4
      if (name.equals(Nc4.NETCDF4_DIMID)) return false;  // preserve the order of the dimensions
      if (name.equals(Nc4.NETCDF4_COORDINATES)) return false;  // ??
      if (name.equals(Nc4.NETCDF4_STRICT)) return false;

      if(name.startsWith("_")) return false;

      // not implemented yet
      //if (att.getDataType().isEnum()) return false;

      return true;
    }

    @Override
    public boolean varDataTypeCheckOk(Variable v) {
      if (v.getDataType() == DataType.CHAR) return false;    // temp workaround
      return v.getDataType() != DataType.STRING;
    }
  }

  static public boolean compareFiles(NetcdfFile org, NetcdfFile copy, Formatter f) {
    return compareFiles(org, copy, f, false, false, false);
  }

  static public boolean compareFiles(NetcdfFile org, NetcdfFile copy, Formatter f, boolean _compareData, boolean _showCompare, boolean _showEach) {
    CompareNetcdf2 tc = new CompareNetcdf2(f, _showCompare, _showEach, _compareData);
    return tc.compare(org, copy);
  }

  static public boolean compareLists(List org, List copy, Formatter f) {
    boolean ok1 = checkContains("first", org, copy, f);
    boolean ok2 = checkContains("second", copy, org, f);
    return ok1 && ok2;
  }

  static private boolean checkContains(String what, List container, List wantList, Formatter f) {
    boolean ok = true;

    for (Object want1 : wantList) {
      int index2 = container.indexOf(want1);
      if (index2 < 0) {
        f.format("  ** %s missing in %s %n", want1, what);
        ok = false;
      }
    }

    return ok;
  }

  /////////

  private Formatter f;
  private boolean showCompare = false;
  private boolean showEach = false;
  private boolean compareData = false;

  public CompareNetcdf2() {
    this(new Formatter(System.out));
  }

  public CompareNetcdf2(Formatter f) {
    this(f, false, false, false);
  }

  public CompareNetcdf2(Formatter f, boolean showCompare, boolean showEach, boolean compareData) {
    this.f = f;
    this.compareData = compareData;
    this.showCompare = showCompare;
    this.showEach = showEach;
  }

  public boolean compare(NetcdfFile org, NetcdfFile copy) {
    return compare(org, copy, showCompare, showEach, compareData);
  }

  public boolean compare(NetcdfFile org, NetcdfFile copy, boolean showCompare, boolean showEach, boolean compareData) {
    return compare(org, copy, null, showCompare, showEach, compareData);
  }

  public boolean compare(NetcdfFile org, NetcdfFile copy, ObjFilter filter, boolean showCompare, boolean showEach, boolean compareData) {
    this.compareData = compareData;
    this.showCompare = showCompare;
    this.showEach = showEach;

    f.format(" First file = %s%n", org.getLocation());
    f.format(" Second file= %s%n", copy.getLocation());

    long start = System.currentTimeMillis();

    boolean ok = compareGroups(org.getRootGroup(), copy.getRootGroup(), filter);
    f.format(" Files are the same = %s%n", ok);

    long took = System.currentTimeMillis() - start;
    f.format(" Time to compare = %d msecs%n", took);

    // coordinate systems
    if (org instanceof NetcdfDataset && copy instanceof NetcdfDataset) {
      NetcdfDataset orgds = (NetcdfDataset) org;
      NetcdfDataset copyds = (NetcdfDataset) copy;

      List matches = new ArrayList();
      ok &= checkAll("Dataset CS:", orgds.getCoordinateSystems(), copyds.getCoordinateSystems(), matches);
      for (int i = 0; i < matches.size(); i += 2) {
        CoordinateSystem orgCs = (CoordinateSystem) matches.get(i);
        CoordinateSystem copyCs = (CoordinateSystem) matches.get(i + 1);
        ok &= compareCoordinateSystem(orgCs, copyCs, filter);
      }
    }

    return ok;
  }

  public boolean compareVariables(NetcdfFile org, NetcdfFile copy) {
    f.format("Original = %s%n", org.getLocation());
    f.format("CompareTo= %s%n", copy.getLocation());
    boolean ok = true;

    for (Variable orgV : org.getVariables()) {
      //if (orgV.isCoordinateVariable()) continue;

      Variable copyVar = copy.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" MISSING '%s' in 2nd file%n", orgV.getFullName());
        ok = false;
      } else {
        ok &= compareVariables(orgV, copyVar, null, compareData, true);
      }
    }

    f.format("%n");
    for (Variable orgV : copy.getVariables()) {
      //if (orgV.isCoordinateVariable()) continue;
      Variable copyVar = org.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" MISSING '%s' in 1st file%n", orgV.getFullName());
        ok = false;
      }
    }

    return ok;
  }

  /* private boolean compare(List<Dimension> dims1, List<Dimension> dims2) {
    if (dims1.size() != dims2.size()) return false;
    for (int i = 0; i < dims1.size(); i++) {
      Dimension dim1 = dims1.get(i);
      Dimension dim2 = dims2.get(i);
      //if (!dim1.getName().equals(dim2.getName())) return false;
      if (dim1.getLength() != dim2.getLength()) return false;
    }
    return true;
  }  */

  private boolean compareGroups(Group org, Group copy, ObjFilter filter) {
    if (showCompare) f.format("compare Group %s to %s %n", org.getShortName(), copy.getShortName());
    boolean ok = true;

    if (!org.getShortName().equals(copy.getShortName())) {
      f.format(" ** names are different %s != %s %n", org.getShortName(), copy.getShortName());
      ok = false;
    }

    // dimensions
    ok &= checkDimensions(org.getDimensions(), copy.getDimensions());
    ok &= checkDimensions(copy.getDimensions(), org.getDimensions());

    // attributes
    ok &= checkAttributes(null, org.getAttributes(), copy.getAttributes(), filter);

    // enums
    ok &= checkEnums(org, copy);

    // variables
    // cant use object equality, just match on short name
    for (Variable orgV : org.getVariables()) {
      Variable copyVar = copy.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" ** cant find variable %s in 2nd file%n", orgV.getFullName());
        ok = false;
      } else {
        ok &= compareVariables(orgV, copyVar, filter, compareData, true);
      }
    }

    for (Variable copyV : copy.getVariables()) {
      Variable orgV = org.findVariable(copyV.getShortName());
      if (orgV == null) {
        f.format(" ** cant find variable %s in 1st file%n", copyV.getFullName());
        ok = false;
      }
    }

    // nested groups
    List groups = new ArrayList();
    String name = org.isRoot() ? "root" : org.getFullName();
    ok &= checkAll(name, org.getGroups(), copy.getGroups(), groups);
    for (int i = 0; i < groups.size(); i += 2) {
      Group orgGroup = (Group) groups.get(i);
      Group copyGroup = (Group) groups.get(i + 1);
      ok &= compareGroups(orgGroup, copyGroup, filter);
    }

    return ok;
  }


  public boolean compareVariable(Variable org, Variable copy) {
    return compareVariables(org, copy, null, compareData, true);
  }

  private boolean compareVariables(Variable org, Variable copy, ObjFilter filter, boolean compareData, boolean justOne) {
    boolean ok = true;

    if (showCompare) f.format("compare Variable %s to %s %n", org.getFullName(), copy.getFullName());
    if (!org.getFullName().equals(copy.getFullName())) {
      f.format(" ** names are different %s != %s %n", org.getFullName(), copy.getFullName());
      ok = false;
    }
    if (filter != null && filter.varDataTypeCheckOk(org) && (org.getDataType() != copy.getDataType())) {
      f.format(" ** %s dataTypes are different %s != %s %n", org.getFullName(), org.getDataType(), copy.getDataType());
      ok = false;
    }

    // dimensions
    ok &= checkDimensions(org.getDimensions(), copy.getDimensions());
    ok &= checkDimensions(copy.getDimensions(), org.getDimensions());

    // attributes
    ok &= checkAttributes(org, org.getAttributes(), copy.getAttributes(), filter);

    // data !!
    if (compareData) {
      try {
        ok &= compareVariableData(org, copy, showCompare, justOne);

      } catch (IOException e) {
        StringWriter sw = new StringWriter(5000);
        e.printStackTrace(new PrintWriter(sw));
        f.format("%s", sw.toString());
        return false;
      }
    }

    // nested variables
    if (org instanceof Structure) {
      if (!(copy instanceof Structure)) {
        f.format("  ** %s not Structure%n", org);
        ok = false;

      } else {
        Structure orgS = (Structure) org;
        Structure ncmlS = (Structure) copy;

        List vars = new ArrayList();
        ok &= checkAll("struct " + orgS.getNameAndDimensions(), orgS.getVariables(), ncmlS.getVariables(), vars);
        for (int i = 0; i < vars.size(); i += 2) {
          Variable orgV = (Variable) vars.get(i);
          Variable ncmlV = (Variable) vars.get(i + 1);
          ok &= compareVariables(orgV, ncmlV, filter, false, true);
        }
      }
    }

    // coordinate systems
    if (org instanceof VariableEnhanced && copy instanceof VariableEnhanced) {
      VariableEnhanced orgds = (VariableEnhanced) org;
      VariableEnhanced copyds = (VariableEnhanced) copy;

      List matches = new ArrayList();
      ok &= checkAll(orgds.getFullName(), orgds.getCoordinateSystems(), copyds.getCoordinateSystems(), matches);
      for (int i = 0; i < matches.size(); i += 2) {
        CoordinateSystem orgCs = (CoordinateSystem) matches.get(i);
        CoordinateSystem copyCs = (CoordinateSystem) matches.get(i + 1);
        ok &= compareCoordinateSystem(orgCs, copyCs, filter);
      }
    }

    return ok;
  }


  private boolean compareCoordinateSystem(CoordinateSystem cs1, CoordinateSystem cs2, ObjFilter filter) {
    if (showCompare)
      f.format("compare CoordinateSystem '%s' to '%s' %n", cs1.getName(), cs2.getName());

    List matchAxes = new ArrayList();
    boolean ok = checkAll(cs1.getName(), cs1.getCoordinateAxes(), cs2.getCoordinateAxes(), matchAxes);
    for (int i = 0; i < matchAxes.size(); i += 2) {
      CoordinateAxis orgCs = (CoordinateAxis) matchAxes.get(i);
      CoordinateAxis copyCs = (CoordinateAxis) matchAxes.get(i + 1);
      ok &= compareCoordinateAxis(orgCs, copyCs, filter);
    }

    List matchTransforms = new ArrayList();
    ok &= checkAll(cs1.getName(), cs1.getCoordinateTransforms(), cs2.getCoordinateTransforms(), matchTransforms);
    return ok;
  }

  private boolean compareCoordinateAxis(CoordinateAxis a1, CoordinateAxis a2, ObjFilter filter) {
    if (showCompare)
      f.format("  compare CoordinateAxis '%s' to '%s' %n", a1.getShortName(), a2.getShortName());

    compareVariable(a1, a2);
    return true;
  }


  // make sure each object in wantList is contained in container, using equals().

  // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paired objects.

  private boolean checkAttributes(Variable v, List<Attribute> list1, List<Attribute> list2, ObjFilter filter) {
    boolean ok = true;

    String name = v == null ? "global" : "variable " + v.getFullName();
    for (Attribute att1 : list1) {
      if (filter == null || filter.attCheckOk(v, att1))
        ok &= checkEach(name, att1, "file1", list1, "file2", list2, null);
    }

    for (Attribute att2 : list2) {
      if (filter == null || filter.attCheckOk(v, att2))
        ok &= checkEach(name, att2, "file2", list2, "file1", list1, null);
    }

    return ok;
  }

  private boolean checkDimensions(List<Dimension> list1, List<Dimension> list2) {
    boolean ok = true;

    for (Dimension d1 : list1) {
      if (d1.isShared()) {
        boolean hasit = list2.contains(d1);
        if (!hasit)
          f.format("  ** Missing dim %s not in file2 %n", d1);
        ok &= hasit;
      }
    }

    return ok;
  }


  // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paired objects.

  private boolean checkEnums(Group org, Group copy) {
    boolean ok = true;

    for (EnumTypedef enum1 : org.getEnumTypedefs()) {
      if (showCompare) f.format("compare Enum %s%n", enum1.getShortName());
      EnumTypedef enum2 = copy.findEnumeration(enum1.getShortName());
      if (enum2 == null) {
        f.format("  ** Enum %s not in file2 %n", enum1.getShortName());
        ok = false;
        continue;
      }
      if (!enum1.equals(enum2)) {
        f.format("  ** Enum %s not equal%n  %s%n  %s%n", enum1.getShortName(), enum1, enum2);
        ok = false;
      }
    }

    for (EnumTypedef enum2 : copy.getEnumTypedefs()) {
      EnumTypedef enum1 = org.findEnumeration(enum2.getShortName());
      if (enum1 == null) {
        f.format("  ** Enum %s not in file1 %n", enum2.getShortName());
        ok = false;
      }
    }
    return ok;
  }


  // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paired objects.

  private boolean checkAll(String what, List list1, List list2, List result) {
    boolean ok = true;

    for (Object aList1 : list1) {
      ok &= checkEach(what, aList1, "file1", list1, "file2", list2, result);
    }

    for (Object aList2 : list2) {
      ok &= checkEach(what, aList2, "file2", list2, "file1", list1, null);
    }

    return ok;
  }

  // check that want is in both list1 and list2, using object.equals()

  private boolean checkEach(String what, Object want1, String name1, List list1, String name2, List list2, List result) {
    boolean ok = true;
    try {
      int index2 = list2.indexOf(want1);
      if (index2 < 0) {
        f.format("  ** %s: %s 0x%x (%s) not in %s %n", what, want1, want1.hashCode(), name1, name2);
        ok = false;
      } else { // found it in second list
        Object want2 = list2.get(index2);
        int index1 = list1.indexOf(want2);
        if (index1 < 0) { // can this happen ??
          f.format("  ** %s: %s 0x%x (%s) not in %s %n", what, want2, want2.hashCode(), name2, name1);
          ok = false;

        } else { // found it in both lists
          Object want = list1.get(index1);
          if (!want.equals(want1)) {
            f.format("  ** %s: %s 0x%x (%s) not equal to %s 0x%x (%s) %n", what, want1, want1.hashCode(), name1, want2, want2.hashCode(), name2);
            ok = false;
          } else {
            if (showEach)
              f.format("  OK <%s> equals <%s>%n", want1, want2);
            if (result != null) {
              result.add(want1);
              result.add(want2);
            }
          }
        }
      }

    } catch (Throwable t) {
      t.printStackTrace();
      f.format(" *** Throwable= %s %n", t.getMessage());
    }

    return ok;
  }

  private boolean compareVariableData(Variable var1, Variable var2, boolean showCompare, boolean justOne) throws IOException {
    Array data1 = var1.read();
    Array data2 = var2.read();

    if (showCompare)
      f.format(" compareArrays %s unlimited=%s size=%d%n", var1.getNameAndDimensions(), var1.isUnlimited(), data1.getSize());
    boolean ok = compareData(var1.getFullName(), data1, data2, justOne);
    if (showCompare) f.format("   ok=%s%n", ok);
    return ok;
  }

  public boolean compareData(String name, Array data1, double[] data2) {
    Array data2a = Array.factory(DataType.DOUBLE, new int[]{data2.length}, data2);
    return compareData(name, data1, data2a, false, false);
  }

  public boolean compareData(String name, double[] data1, double[] data2) {
    Array data1a = Array.factory(DataType.DOUBLE, new int[]{data1.length}, data1);
    Array data2a = Array.factory(DataType.DOUBLE, new int[]{data2.length}, data2);
    return compareData(name, data1a, data2a, false, false);
  }

  public boolean compareData(String name, Array data1, Array data2, boolean justOne) {
    return compareData(name, data1, data2, justOne, true);
  }

  public boolean compareData(String name, Array data1, Array data2) {
    return compareData(name, data1, data2, false, true);
  }

  private boolean compareData(String name, Array data1, Array data2, boolean justOne, boolean testTypes) {
    boolean ok = true;
    if (data1.getSize() != data2.getSize()) {
      f.format(" DIFF %s: size %d !== %d%n", name, data1.getSize(), data2.getSize());
      ok = false;
    }

    if (testTypes && data1.getElementType() != data2.getElementType()) {
      f.format(" DIFF %s: element type %s !== %s%n", name, data1.getElementType(), data2.getElementType());
      ok = false;
    }

    if (testTypes && data1.getDataType() != data2.getDataType()) {
      f.format(" DIFF %s: data type %s !== %s%n", name, data1.getDataType(), data2.getDataType());
      ok = false;
    }

    if (!ok) return false;

    DataType dt = data1.getDataType();

    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    if (data1.isVlen()) {
      while (iter1.hasNext() && iter2.hasNext()) {
        Object v1 = iter1.getObjectNext();
        Object v2 = iter2.getObjectNext();
        if (v1.getClass() != v2.getClass()) {
          f.format(" DIFF %s: ArrayObject class %s != %s %n", name, v1.getClass().getName(), v2.getClass().getName());
          ok = false;
          if (justOne) break;

        } else if (v1 instanceof Array) {
          ok &= compareData(name, (Array) v1, (Array) v2, justOne, testTypes);
        }
      }

    } else if (dt == DataType.DOUBLE) {
      while (iter1.hasNext() && iter2.hasNext()) {
        double v1 = iter1.getDoubleNext();
        double v2 = iter2.getDoubleNext();
        if (!Misc.nearlyEquals(v1, v2)) {
          f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt == DataType.FLOAT) {
      while (iter1.hasNext() && iter2.hasNext()) {
        float v1 = iter1.getFloatNext();
        float v2 = iter2.getFloatNext();
        if (!Misc.nearlyEquals(v1, v2)) {
          f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt.getPrimitiveClassType() == int.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        int v1 = iter1.getIntNext();
        int v2 = iter2.getIntNext();
        if (v1 != v2) {
          f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt.getPrimitiveClassType() == short.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        short v1 = iter1.getShortNext();
        short v2 = iter2.getShortNext();
        if (v1 != v2) {
          f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt.getPrimitiveClassType() == byte.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        byte v1 = iter1.getByteNext();
        byte v2 = iter2.getByteNext();
        if (v1 != v2) {
          f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt.getPrimitiveClassType() == long.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        long v1 = iter1.getLongNext();
        long v2 = iter2.getLongNext();
        if (v1 != v2) {
          f.format(createNumericDataDiffMessage(dt, name, v1, v2, iter1));
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt.getPrimitiveClassType() == char.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        char v1 = iter1.getCharNext();
        char v2 = iter2.getCharNext();
        if (v1 != v2) {
          f.format(" DIFF char %s: %s != %s count=%s%n", name, v1, v2, iter1);
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt == DataType.STRING) {
      while (iter1.hasNext() && iter2.hasNext()) {
        String v1 = (String) iter1.getObjectNext();
        String v2 = (String) iter2.getObjectNext();
        if (!v1.equals(v2)) {
          f.format(" DIFF string %s: %s != %s count=%s%n", name, v1, v2, iter1);
          ok = false;
          if (justOne) break;
        }
      }

    } else if (dt == DataType.STRUCTURE) {
      while (iter1.hasNext() && iter2.hasNext()) {
        compareStructureData((StructureData) iter1.next(), (StructureData) iter2.next(), justOne);
      }

    } else if (dt == DataType.OPAQUE) {
      while (iter1.hasNext() && iter2.hasNext()) {
        ByteBuffer obj1 = (ByteBuffer) iter1.next();
        ByteBuffer obj2 = (ByteBuffer) iter2.next();
        if (obj1.limit() != obj2.limit()) {
          f.format(" DIFF %s: opaque size %d != %d%n", name, obj1.limit(), obj2.limit() );
          ok = false;
          if (justOne) break;
        }
      }

    } else {
      ok = false;
      f.format(" %s: Unknown data type %s%n", name, data1.getClass().getName());
    }

    return ok;
  }

  private String createNumericDataDiffMessage(DataType dt, String name, Number v1, Number v2, IndexIterator iter) {
    return String.format(" DIFF %s %s: %s != %s;  count = %s, absDiff = %s, relDiff = %s %n",
            dt, name, v1, v2, iter, Misc.absoluteDifference(v1.doubleValue(), v2.doubleValue()),
            Misc.relativeDifference(v1.doubleValue(), v2.doubleValue()));
  }

  public boolean compareStructureData(StructureData sdata1, StructureData sdata2, boolean justOne) {
    boolean ok = true;

    StructureMembers sm1 = sdata1.getStructureMembers();
    StructureMembers sm2 = sdata2.getStructureMembers();
    if (sm1.getMembers().size() != sm2.getMembers().size()) {
      f.format(" size %d !== %d%n", sm1.getMembers().size(), sm2.getMembers().size());
      ok = false;
    }

    for (StructureMembers.Member m1 : sm1.getMembers()) {
      // if (m1.getName().equals("time")) continue;
      StructureMembers.Member m2 = sm2.findMember(m1.getName());
      Array data1 = sdata1.getArray(m1);
      Array data2 = sdata2.getArray(m2);
      ok &= compareData(m1.getName(), data1, data2, justOne, true);
    }

    return ok;
  }

  public static void main(String arg[]) throws IOException {
    String usage = "usage: ucar.nc2.util.CompareNetcdf2 file1 file2 [-showEach] [-compareData]";
    if (arg.length < 2) {
      System.out.println(usage);
      System.exit(0);
    }

    boolean showEach = false;
    boolean compareData = false;

    String file1 = arg[0];
    String file2 = arg[1];

    for (int i = 2; i < arg.length; i++) {
      String s = arg[i];
      if (s.equalsIgnoreCase("-showEach")) showEach = true;
      if (s.equalsIgnoreCase("-compareData")) compareData = true;
    }

    NetcdfFile ncfile1 = NetcdfDataset.open(file1);
    NetcdfFile ncfile2 = NetcdfDataset.open(file2);
    compareFiles(ncfile1, ncfile2, new Formatter(System.out), true, compareData, showEach);
    ncfile1.close();
    ncfile2.close();
  }

}
