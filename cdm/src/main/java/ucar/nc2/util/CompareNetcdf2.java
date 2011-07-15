// $Id: TestCompare.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2.util;

import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.*;
import ucar.ma2.*;

import java.util.List;
import java.util.Formatter;
import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

/**
 * Compare two NetcdfFile.
 * Dont use assert, place results in Formatter.
 *
 * @author john
 */
public class CompareNetcdf2 {

  static public void compareFiles(NetcdfFile org, NetcdfFile copy, Formatter f) {
    compareFiles(org, copy, f, false, false, false);
  }

  static public void compareFiles(NetcdfFile org, NetcdfFile copy, Formatter f, boolean _compareData, boolean _showCompare, boolean _showEach) {
    CompareNetcdf2 tc = new CompareNetcdf2(f, _showCompare, _showEach, _compareData);
    tc.compare(org, copy);
  }

  /////////

  private Formatter f;
  private boolean showCompare = false;
  private boolean showEach = false;
  private boolean compareData = false;

  public CompareNetcdf2() {
    this(new Formatter(System.out), false, false, false);
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
    this.compareData = compareData;
    this.showCompare = showCompare;
    this.showEach = showEach;

    f.format("First file = %s%n", org.getLocation());
    f.format("Second file= %s%n", copy.getLocation());

    long start = System.currentTimeMillis();

    boolean ok = compareGroups(org.getRootGroup(), copy.getRootGroup());
    f.format(" Files are the same = %s%n", ok);

    long took = System.currentTimeMillis() - start;
    f.format(" Time to compare = %d msecs%n", took);

    return ok;
  }

  public boolean compareVariables(NetcdfFile org, NetcdfFile copy) {
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
    for (int i = 0; i < dims1.size(); i++) {
      Dimension dim1 = dims1.get(i);
      Dimension dim2 = dims2.get(i);
      //if (!dim1.getName().equals(dim2.getName())) return false;
      if (dim1.getLength() != dim2.getLength()) return false;
    }
    return true;
  }

  private boolean compareGroups(Group org, Group copy) {
    if (showCompare) f.format("compare Group %s to %s %n", org.getName(), copy.getName());
    boolean ok = true;

    if (!org.getName().equals(copy.getName())) {
      f.format(" ** names are different %s != %s %n", org.getName(), copy.getName());
      ok = false;
    }

    // dimensions
    ok &= checkAll(org.getDimensions(), copy.getDimensions(), null);

    // attributes
    ok &= checkAll(org.getAttributes(), copy.getAttributes(), null);

    // variables
    // cant use object equality, just match on short name
    for (Variable orgV : org.getVariables()) {
      Variable copyVar = copy.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" ** cant find variable %s in 2nd file%n", orgV.getFullName());
        ok = false;
      } else {
        ok &= compareVariables(orgV, copyVar, compareData, true);
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
    ok &= checkAll(org.getGroups(), copy.getGroups(), groups);
    for (int i = 0; i < groups.size(); i += 2) {
      Group orgGroup = (Group) groups.get(i);
      Group ncmlGroup = (Group) groups.get(i + 1);
      ok &= compareGroups(orgGroup, ncmlGroup);
    }

    return ok;
  }


  public boolean compareVariable(Variable org, Variable copy) {
    return compareVariables(org, copy, compareData, true);
  }

  public boolean compareVariables(Variable org, Variable copy, boolean compareData, boolean justOne) {
    boolean ok = true;

    if (showCompare) f.format("compare Variable %s to %s %n", org.getFullName(), copy.getFullName());
    if (!org.getFullName().equals(copy.getFullName())) {
      f.format(" ** names are different %s != %s %n", org.getFullName(), copy.getFullName());
      ok = false;
    }

    // dimensions
    ok &= checkAll(org.getDimensions(), copy.getDimensions(), null);

    // attributes
    ok &= checkAll(org.getAttributes(), copy.getAttributes(), null);

    // coord sys
    if ((org instanceof VariableEnhanced) && (copy instanceof VariableEnhanced)) {
      VariableEnhanced orge = (VariableEnhanced) org;
      VariableEnhanced copye = (VariableEnhanced) copy;
      ok &= checkAll(orge.getCoordinateSystems(), copye.getCoordinateSystems(), null);
    }

    // data !!
    if (compareData) {
      try {
        compareVariableData(org, copy, showCompare, justOne);

      } catch (IOException e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        e.printStackTrace(new PrintStream(bos));
        f.format("%s", bos.toString());
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
        ok &= checkAll(orgS.getVariables(), ncmlS.getVariables(), vars);
        for (int i = 0; i < vars.size(); i += 2) {
          Variable orgV = (Variable) vars.get(i);
          Variable ncmlV = (Variable) vars.get(i + 1);
          ok &= compareVariables(orgV, ncmlV, false, true);
        }
      }
    }

    return ok;
  }

  // make sure each object in wantList is contained in container, using equals().

  private boolean checkContains(List container, List wantList) {
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

  private boolean checkAll(List list1, List list2, List result) {
    boolean ok = true;

    for (Object aList1 : list1) {
      ok &= checkEach(aList1, "file1", list1, "file2", list2, result);
    }

    for (Object aList2 : list2) {
      ok &= checkEach(aList2, "file2", list2, "file1", list1, result);
    }

    return ok;
  }

  // check that want is in both list1 and list2, using object.equals()

  private boolean checkEach(Object want1, String name1, List list1, String name2, List list2, List result) {
    boolean ok = true;
    try {
      int index2 = list2.indexOf(want1);
      if (index2 < 0) {
        f.format("  ** %s %s 0x%x (%s) not in %s %n", want1.getClass().getName(), want1, want1.hashCode(), name1, name2);
        ok = false;
      } else { // found it in second list
        Object want2 = list2.get(index2);
        int index1 = list1.indexOf(want2);
        if (index1 < 0) { // can this happen ??
          f.format("  ** %s %s 0x%x (%s) not in %s %n", want2.getClass().getName(), want2, want2.hashCode(), name2, name1);
          ok = false;

        } else { // found it in both lists
          Object want = list1.get(index1);
          if (!want.equals(want1)) {
            f.format("  ** %s %s 0x%x (%s) not equal to %s 0x%x (%s) %n", want1.getClass().getName(), want1, want1.hashCode(), name1, want2, want2.hashCode(), name2);
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
      t.printStackTrace();
      f.format(" *** Throwable= %s %n", t.getMessage());
    }

    return ok;
  }

  private void compareVariableData(Variable var1, Variable var2, boolean showCompare, boolean justOne) throws IOException {
    Array data1 = var1.read();
    Array data2 = var2.read();

    if (showCompare)
      f.format(" compareArrays %s unlimited=%s size=%d%n", var1.getNameAndDimensions(), var1.isUnlimited(), data1.getSize());
    compareData(var1.getFullName(), data1, data2, justOne);
    if (showCompare) f.format("   ok%n");
  }

  public boolean compareData(String name, Array data1, Array data2, boolean justOne) {
    return compareData(name, data1, data2, TOL, justOne);
  }

  private boolean compareData(String name, Array data1, Array data2, double tol, boolean justOne) {
    boolean ok = true;
    if (data1.getSize() != data2.getSize()) {
      f.format(" DIFF %s: size %d !== %d%n", name, data1.getSize(), data2.getSize());
      ok = false;
    }

    if (data1.getElementType() != data2.getElementType()) {
      f.format(" DIFF %s: element type %s !== %s%n", name, data1.getElementType(), data2.getElementType());
      ok = false;
    }

    if (!ok) return false;

    DataType dt = DataType.getType(data1.getElementType());

    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    if (dt == DataType.DOUBLE) {
      while (iter1.hasNext() && iter2.hasNext()) {
        double v1 = iter1.getDoubleNext();
        double v2 = iter2.getDoubleNext();
        if (!Double.isNaN(v1) || !Double.isNaN(v2))
          if (!closeEnough(v1, v2, tol)) {
            f.format(" DIFF %s: %f != %f count=%s diff = %f pdiff = %f %n", name, v1, v2, iter1, diff(v1, v2), pdiff(v1, v2));
            ok = false;
            if (justOne) break;
          }
      }
    } else if (dt == DataType.FLOAT) {
      while (iter1.hasNext() && iter2.hasNext()) {
        float v1 = iter1.getFloatNext();
        float v2 = iter2.getFloatNext();
        if (!Float.isNaN(v1) || !Float.isNaN(v2))
          if (!closeEnough(v1, v2, (float) tol)) {
            f.format(" DIFF %s: %f != %f count=%s diff = %f pdiff = %f %n", name, v1, v2, iter1, diff(v1, v2), pdiff(v1, v2));
            ok = false;
            if (justOne) break;
          }
      }
    } else if (dt == DataType.INT) {
      while (iter1.hasNext() && iter2.hasNext()) {
        int v1 = iter1.getIntNext();
        int v2 = iter2.getIntNext();
        if (v1 != v2) {
          f.format(" DIFF %s: %d != %d count=%s diff = %f pdiff = %f %n", name, v1, v2, iter1, diff(v1, v2), pdiff(v1, v2));
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt == DataType.SHORT) {
      while (iter1.hasNext() && iter2.hasNext()) {
        short v1 = iter1.getShortNext();
        short v2 = iter2.getShortNext();
        if (v1 != v2) {
          f.format(" DIFF %s: %d != %d count=%s diff = %f pdiff = %f %n", name, v1, v2, iter1, diff(v1, v2), pdiff(v1, v2));
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt == DataType.BYTE) {
      while (iter1.hasNext() && iter2.hasNext()) {
        byte v1 = iter1.getByteNext();
        byte v2 = iter2.getByteNext();
        if (v1 != v2) {
          f.format(" DIFF %s: %d != %d count=%s diff = %f pdiff = %f %n", name, v1, v2, iter1, diff(v1, v2), pdiff(v1, v2));
          ok = false;
          if (justOne) break;
        }
      }
    } else if (dt == DataType.STRUCTURE) {
      while (iter1.hasNext() && iter2.hasNext()) {
        compareStructureData((StructureData) iter1.next(), (StructureData) iter2.next(), tol, justOne);
      }
    }

    return ok;
  }

  public boolean compareStructureData(StructureData sdata1, StructureData sdata2, double tol, boolean justOne) {
    boolean ok = true;

    StructureMembers sm1 = sdata1.getStructureMembers();
    StructureMembers sm2 = sdata2.getStructureMembers();
    if (sm1.getMembers().size() != sm2.getMembers().size()) {
      f.format(" size %d !== %d%n", sm1.getMembers().size(), sm2.getMembers().size());
      ok = false;
    }

    for (StructureMembers.Member m1 : sm1.getMembers()) {
      if (m1.getName().equals("time")) continue;
      StructureMembers.Member m2 = sm2.findMember(m1.getName());
      Array data1 = sdata1.getArray(m1);
      Array data2 = sdata2.getArray(m2);
      ok &= compareData(m1.getName(), data1, data2, tol, justOne);
    }

    return ok;
  }

  static private final double TOL = 1.0e-5;
  static private final float TOLF = 1.0e-5f;

  static public boolean closeEnoughP(double d1, double d2) {
    if (Math.abs(d1) < TOL) return Math.abs(d1 - d2) < TOL;
    return Math.abs((d1 - d2) / d1) < TOL;
  }

  static public boolean closeEnough(double d1, double d2) {
    return Math.abs(d1 - d2) < TOL;
  }

  static public boolean closeEnough(double d1, double d2, double tol) {
    return Math.abs(d1 - d2) < tol;
  }

  static public boolean closeEnoughP(double d1, double d2, double tol) {
    if (Math.abs(d1) < tol) return Math.abs(d1 - d2) < tol;
    return Math.abs((d1 - d2) / d1) < tol;
  }

  static public double diff(double d1, double d2) {
    return Math.abs(d1 - d2);
  }

  static public double pdiff(double d1, double d2) {
    return Math.abs((d1 - d2) / d1);
  }

  static public boolean closeEnough(float d1, float d2) {
    return Math.abs(d1 - d2) < TOLF;
  }

  static public boolean closeEnoughP(float d1, float d2) {
    if (Math.abs(d1) < TOLF) return Math.abs(d1 - d2) < TOLF;
    return Math.abs((d1 - d2) / d1) < TOLF;
  }


  public static void main(String arg[]) throws IOException {
    NetcdfFile ncfile1 = NetcdfDataset.openFile("dods://thredds.cise-nsf.gov:8080/thredds/dodsC/satellite/SFC-T/SUPER-NATIONAL_1km/20090516/SUPER-NATIONAL_1km_SFC-T_20090516_2200.gini", null);
    NetcdfFile ncfile2 = NetcdfDataset.openFile("dods://motherlode.ucar.edu:8080/thredds/dodsC/satellite/SFC-T/SUPER-NATIONAL_1km/20090516/SUPER-NATIONAL_1km_SFC-T_20090516_2200.gini", null);
    compareFiles(ncfile1, ncfile2, new Formatter(System.out), false, true, false);
  }
}
