/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestCompare.java 51 2006-07-12 17:13:13Z caron $

package ucar.unidata.util.test;

import org.junit.Assert;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.*;
import ucar.ma2.*;
import ucar.nc2.util.Misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Compare two NetcdfFile. Uses assert.
 * Test classes only.
 *
 * @author john
 */
public class CompareNetcdf {

  static public boolean compareFiles(NetcdfFile org, NetcdfFile copy) {
    return compareFiles(org, copy, false, false, false);
  }

  static public boolean compareFiles(NetcdfFile org, NetcdfFile copy, boolean _compareData, boolean _showCompare, boolean _showEach) {
    CompareNetcdf tc = new CompareNetcdf(_showCompare, _showEach, _compareData);
    return tc.compare(org, copy, new Formatter(System.out));
  }

  /////////

  private boolean showCompare = false;
  private boolean showEach = false;
  private boolean compareData = false;

  public CompareNetcdf(boolean showCompare, boolean showEach, boolean compareData) {
    this.compareData = compareData;
    this.showCompare = showCompare;
    this.showEach = showEach;
  }

  public boolean compare(NetcdfFile org, NetcdfFile copy, Formatter f) {
    f.format("First file = %s%n", org.getLocation());
    f.format("Second file= %s%n", copy.getLocation());

    long start = System.currentTimeMillis();

    boolean ok = compareGroups(org.getRootGroup(), copy.getRootGroup(), f);
    f.format("Files are the same = %s%n", ok);

    long took = System.currentTimeMillis() - start;
    f.format("Time to compare = %d msecs%n", took);

    return ok;
  }

  public boolean compareVariables(NetcdfFile org, NetcdfFile copy, Formatter f) {
    f.format("Original = %s%n", org.getLocation());
    f.format("CompareTo= %s%n", copy.getLocation());
    boolean ok = true;

    for (Variable orgV : org.getVariables()) {
      if (orgV.isCoordinateVariable()) continue;
      Variable copyVar = copy.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" MISSING '%s' in 2nd file%n", orgV.getFullName());
        ok = false;
      } else {
        List<Dimension> dims1 = orgV.getDimensions();
        List<Dimension> dims2 = copyVar.getDimensions();
        if (!compare(dims1, dims2)) {
          f.format(" %s != %s%n", orgV.getNameAndDimensions(), copyVar.getNameAndDimensions());
        } else {
          // f.format("   ok %s%n", orgV.getName());
        }
      }
    }

    f.format("%n");
    for (Variable orgV : copy.getVariables()) {
      if (orgV.isCoordinateVariable()) continue;
      Variable copyVar = org.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" MISSING '%s' in 1st file%n", orgV.getFullName());
        ok = false;
      }
    }



    return ok;
  }

  private boolean compare(List<Dimension> dims1, List<Dimension> dims2) {
    if (dims1.size() != dims2.size()) return false;
    for (int i=0; i<dims1.size(); i++) {
      Dimension dim1 = dims1.get(i);
      Dimension dim2 = dims2.get(i);
      //if (!dim1.getName().equals(dim2.getName())) return false;
      if (dim1.getLength() != dim2.getLength()) return false;
    }
    return true;
  }

  private boolean compareGroups(Group org, Group copy, Formatter f) {
    if (showCompare) f.format("compare Group %s to %s %n", org.getFullName(), copy.getFullName());
    boolean ok = true;

    if (!org.getFullName().equals(copy.getFullName())) {
      f.format(" ** names are different %s != %s %n", org.getShortName(), copy.getShortName());
      ok = false;
    }

    // dimensions
    ok &= checkAll(org.getDimensions(), copy.getDimensions(), null, f);

    // attributes
    ok &= checkAll(org.getAttributes(), copy.getAttributes(), null, f);

    // variables
    // cant use object equality, just match on short name
    for (Variable orgV : org.getVariables()) {
      Variable copyVar = copy.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" ** cant find variable %s in 2nd file%n", orgV.getFullName());
        ok = false;
      } else {
        ok &= compareVariables(orgV, copyVar, compareData, f);
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
    ok &= checkAll(org.getGroups(), copy.getGroups(), groups, f);
    for (int i = 0; i < groups.size(); i += 2) {
      Group orgGroup = (Group) groups.get(i);
      Group ncmlGroup = (Group) groups.get(i + 1);
      ok &= compareGroups(orgGroup, ncmlGroup, f);
    }

    return ok;
  }


  public boolean compareVariable(Variable org, Variable copy, Formatter f) {
    return compareVariables(org, copy, compareData, f);
  }

  private boolean compareVariables(Variable org, Variable copy, boolean compareData, Formatter f) {
    boolean ok = true;

    if (showCompare) f.format("compare Variable %s to %s %n", org.getFullName(), copy.getFullName());
    if (!org.getFullName().equals(copy.getFullName())) {
      f.format(" ** names are different %s != %s %n", org.getFullName(), copy.getFullName());
      ok = false;
    }

    // dimensions
    ok &= checkAll(org.getDimensions(), copy.getDimensions(), null, f);

    // attributes
    ok &= checkAll(org.getAttributes(), copy.getAttributes(), null, f);

    // coord sys
    if ((org instanceof VariableEnhanced) && (copy instanceof VariableEnhanced)) {
      VariableEnhanced orge = (VariableEnhanced) org;
      VariableEnhanced copye = (VariableEnhanced) copy;
      ok &= checkAll(orge.getCoordinateSystems(), copye.getCoordinateSystems(), null, f);
    }

    // data !!
    if (compareData) {
      try {
        compareVariableData(org, copy, showCompare, f);

      } catch (IOException e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        e.printStackTrace(new PrintStream(bos));
        f.format("%s", bos.toString());
      }
    }

    // nested variables
    if (org instanceof Structure) {
      assert (copy instanceof Structure);
      Structure orgS = (Structure) org;
      Structure ncmlS = (Structure) copy;

      List vars = new ArrayList();
      ok &= checkAll(orgS.getVariables(), ncmlS.getVariables(), vars, f);
      for (int i = 0; i < vars.size(); i += 2) {
        Variable orgV = (Variable) vars.get(i);
        Variable ncmlV = (Variable) vars.get(i + 1);
        ok &= compareVariables(orgV, ncmlV, false, f);
      }
    }

    return ok;
  }

  // make sure each object in wantList is contained in container, using equals().
  static public boolean checkContains(List container, List wantList, Formatter f) {
    boolean ok = true;

    for (Object want1 : wantList) {
      int index2 = container.indexOf(want1);
      if (index2 < 0) {
         f.format("  ** %s %s missing %n", want1.getClass().getName(), want1);
         ok = false;
      }
    }

    return ok;
  }


  // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paired objects.
  private boolean checkAll(List list1, List list2, List result, Formatter f) {
    boolean ok = true;

    for (Object aList1 : list1) {
      ok &= checkEach(aList1, "file1", list1, "file2", list2, result, f);
    }

    for (Object aList2 : list2) {
      ok &= checkEach(aList2, "file2", list2, "file1", list1, result, f);
    }

    return ok;
  }

  // check that want is in both list1 and list2, using object.equals()
  private boolean checkEach(Object want1, String name1, List list1, String name2, List list2, List result, Formatter f) {
    boolean ok = true;
    try {
      int index2 = list2.indexOf(want1);
      if (index2 < 0) {
        f.format("  ** %s %s (%s) not in %s %n", want1.getClass().getName(), want1, name1, name2);
        ok = false;
      } else { // found it in second list
        Object want2 = list2.get(index2);
        int index1 = list1.indexOf(want2);
        if (index1 < 0) { // can this happen ??
          f.format("  ** %s %s (%s) not in %s %n", want2.getClass().getName(), want2, name2, name1);
          ok = false;

        } else { // found it in both lists
          Object want = list1.get(index1);
          if (want != want1) {
            f.format("  ** %s %s (%s) not equal to %s (%s) %n", want1.getClass().getName(), want1, name1, want2, name2);
            ok = false;
          } else {
            if (showEach) f.format("  OK <%s> equals <%s>%n", want1, want2);
            if (result != null) {
              result.add(want1);
              result.add(want2);
            }
          }
        }
      }

    } catch (Throwable t) {
      f.format(" *** Throwable= %s %n", t.getMessage());
    }

    return ok;
  }

  static private void compareVariableData(Variable var1, Variable var2, boolean showCompare, Formatter f) throws IOException {
    Array data1 = var1.read();
    Array data2 = var2.read();

    if (showCompare)
      f.format(" compareArrays %s unlimited=%s size=%d%n", var1.getNameAndDimensions(), var1.isUnlimited(), data1.getSize());
    compareData(data1, data2);
    if (showCompare) f.format("   ok%n");
  }

  static public void compareData(Array data1, Array data2) {
    compareData(data1, data2, true);
  }

  static public void compareData(Array data1, double[] data2) {
    Array data2a = Array.factory(DataType.DOUBLE, new int[]{data2.length}, data2);
    compareData( data1, data2a, false);
  }

  static private void compareData(Array data1, Array data2, boolean checkType) {
    Assert.assertEquals("data size", data1.getSize(), data2.getSize());
    Assert.assertEquals("data unsigned", data1.isUnsigned(), data2.isUnsigned());
    if (checkType)
      assert data1.getElementType() == data2.getElementType() : data1.getElementType() + "!=" + data2.getElementType();
    DataType dt = DataType.getType(data1);

    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    if (dt == DataType.DOUBLE) {
      while (iter1.hasNext() && iter2.hasNext()) {
        double v1 = iter1.getDoubleNext();
        double v2 = iter2.getDoubleNext();
        assert Misc.nearlyEquals(v1, v2) : String.format("%f != %f;  count = %s, absDiff = %f, relDiff = %f",
                v1, v2, iter1, Misc.absoluteDifference(v1, v2), Misc.relativeDifference(v1, v2));
      }
    } else if (dt == DataType.FLOAT) {
      while (iter1.hasNext() && iter2.hasNext()) {
        float v1 = iter1.getFloatNext();
        float v2 = iter2.getFloatNext();
        assert Misc.nearlyEquals(v1, v2) : String.format("%f != %f;  count = %s, absDiff = %f, relDiff = %f",
                v1, v2, iter1, Misc.absoluteDifference(v1, v2), Misc.relativeDifference(v1, v2));
      }
    } else if (dt.getPrimitiveClassType() == int.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        int v1 = iter1.getIntNext();
        int v2 = iter2.getIntNext();
        assert v1 == v2 : v1 + " != " + v2 + " count=" + iter1;
      }
    } else if (dt.getPrimitiveClassType() == short.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        short v1 = iter1.getShortNext();
        short v2 = iter2.getShortNext();
        assert v1 == v2 : v1 + " != " + v2 + " count=" + iter1;
      }
    } else if (dt.getPrimitiveClassType() == byte.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        byte v1 = iter1.getByteNext();
        byte v2 = iter2.getByteNext();
        assert v1 == v2 : v1 + " != " + v2 + " count=" + iter1;
      }
    } else if (dt.getPrimitiveClassType() == long.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        long v1 = iter1.getLongNext();
        long v2 = iter2.getLongNext();
        assert v1 == v2 : v1 + " != " + v2 + " count=" + iter1;
      }
    } else if (dt == DataType.STRUCTURE) {
      while (iter1.hasNext() && iter2.hasNext()) {
        compareStructureData((StructureData) iter1.next(), (StructureData) iter2.next());
      }
    }
  }

  static public void compareStructureData(StructureData sdata1, StructureData sdata2) {
    StructureMembers sm1 = sdata1.getStructureMembers();
    StructureMembers sm2 = sdata2.getStructureMembers();
    assert sm1.getMembers().size() == sm2.getMembers().size();

    for (StructureMembers.Member m1 : sm1.getMembers()) {
      if (m1.getName().equals("time")) continue;
      StructureMembers.Member m2 = sm2.findMember(m1.getName());
      Array data1 = sdata1.getArray(m1);
      Array data2 = sdata2.getArray(m2);
      compareData(data1, data2, true);
    }
  }
}
