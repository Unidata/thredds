// $Id: TestCompare.java 51 2006-07-12 17:13:13Z caron $
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

import ucar.nc2.dataset.VariableEnhanced;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.DataType;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

/**
 * @author john
 */
public class TestCompare {

  static boolean showCompare = false;
  static boolean showEach = false;
  static boolean compareData = false;

   static public void compareFiles(NetcdfFile org, NetcdfFile copy) {
     compareFiles( org,  copy, false, false, false);
   }
  
  static public void compareFiles(NetcdfFile org, NetcdfFile copy, boolean _compareData, boolean _showCompare, boolean _showEach) {
    System.out.println(" Original= "+org.getLocation());
    System.out.println(" Copy= "+copy.getLocation());
    showCompare = _showCompare;
    showEach = _showEach;
    compareData = _compareData;

    //System.out.println("Original= "+org);
    //System.out.println("Copy= "+copy);

    if ((org.getId() != null) || (copy.getId() != null))
      assert org.getId().equals( copy.getId());
    if ((org.getTitle() != null) || (copy.getTitle() != null))
      assert org.getTitle().equals( copy.getTitle());

    // assert org.getLocation().equals( ncml.getLocation());

    compareGroups( org.getRootGroup(), copy.getRootGroup());
  }

  static private void compareGroups(Group org, Group copy) {
    if (showCompare) System.out.println("compareGroups  "+org.getName()+" "+copy.getName());
    assert org.getName().equals( copy.getName());

    // dimensions
    checkAll( org.getDimensions(), copy.getDimensions());

    // attributes
    checkAll( org.getAttributes(), copy.getAttributes());

    // variables
    //List vars = checkAll( org.getVariables(), copy.getVariables());
    List<Variable> varsOrg = org.getVariables();
    for (Variable orgV : varsOrg) {
      Variable copyVar = copy.findVariable(orgV.getShortName());
      assert copyVar != null : orgV.getShortName();
      compareVariables(orgV, copyVar);
    }

    List<Variable> varsCopy = copy.getVariables();
    for (Variable copyV : varsCopy) {
      Variable orgV = org.findVariable(copyV.getShortName());
      assert orgV != null;
    }

    // nested groups
    List groups = checkAll( org.getGroups(), copy.getGroups());
    for (int i = 0; i < groups.size(); i+=2) {
      Group orgGroup =  (Group) groups.get(i);
      Group ncmlGroup =  (Group) groups.get(i+1);
      compareGroups(orgGroup, ncmlGroup);
    }

  }


  static void compareVariables(Variable org, Variable copy) {
    if (showCompare) System.out.println("compareVariables  "+org.getName()+" "+copy.getName());
    assert org.getName().equals( copy.getName());

    // dimensions
    checkAll( org.getDimensions(), copy.getDimensions());

    // attributes
    checkAll( org.getAttributes(), copy.getAttributes());

    // coord sys
    if (org instanceof VariableEnhanced) {
      VariableEnhanced orge = (VariableEnhanced) org;
      VariableEnhanced copye = (VariableEnhanced) copy;
      checkAll( orge.getCoordinateSystems(), copye.getCoordinateSystems());
    }

    // data !!
    if (compareData) {
      try {
        compareVariableData(org, copy);
      } catch (IOException e) {
        assert false;
      }
    }

    // nested variables
    if (org instanceof Structure)  {
      assert (copy instanceof Structure);
      Structure orgS = (Structure) org;
      Structure ncmlS = (Structure) copy;

      List vars = checkAll( orgS.getVariables(), ncmlS.getVariables());
      for (int i = 0; i < vars.size(); i+=2) {
        Variable orgV =  (Variable) vars.get(i);
        Variable ncmlV =  (Variable) vars.get(i+1);
        compareVariables(orgV, ncmlV);
      }
    }

  }

  // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paited objects.
  static public ArrayList checkAll(List list1, List list2) {
    ArrayList result = new ArrayList();

    Iterator iter1 = list1.iterator();
    while ( iter1.hasNext()) {
      checkEach(iter1.next(), list1, list2, result);
    }

    Iterator iter2 = list2.iterator();
    while ( iter2.hasNext()) {
      checkEach(iter2.next(), list2, list1, null);
    }

    return result;
  }

  static public void checkEach(Object want1, List list1, List list2, List result) {
    int index2 = list2.indexOf( want1);
    if (index2 < 0)
      System.out.println(); // grab in debugger

    assert (index2 >= 0) : want1.getClass().getName() +" "+want1 + " not in list 2";
    Object want2 = list2.get( index2);

    int index1 = list1.indexOf( want2);
    assert (index1 >= 0) :  want2.getClass().getName() +" "+want2 + " not in list 1";
    Object want = list1.get( index1);
    assert want == want1: want1 + " not == "+ want;

    if (showEach) System.out.println("  OK <"+want1+". equals <"+want2+">");
    if (result != null) {
      result.add(want1);
      result.add(want2);
    }
  }

  static public void compareVariableData(Variable var1, Variable var2) throws IOException {
    Array data1 = var1.read();
    Array data2 = var2.read();

    if (showCompare) System.out.print("compareArrays  "+var1.getName()+" "+var1.isUnlimited()+ " size = "+data1.getSize());
    compareData(data1, data2);
    if (showCompare) System.out.println(" ok");
  }

  static public void compareData(Array data1, Array data2) {
    assert data1.getSize() == data2.getSize();
    assert data1.getElementType() == data2.getElementType() : data1.getElementType()+"!="+ data2.getElementType();
    DataType dt = DataType.getType( data1.getElementType());

    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    if (dt == DataType.DOUBLE) {
      while (iter1.hasNext()) {
          double v1 = iter1.getDoubleNext();
          double v2 = iter2.getDoubleNext();
          if (!Double.isNaN(v1) || !Double.isNaN(v2))
            assert v1 == v2 : v1 + " != "+ v2+" count="+iter1;
      }
    }

    else if (dt == DataType.FLOAT) {
      while (iter1.hasNext()) {
          float v1 = iter1.getFloatNext();
          float v2 = iter2.getFloatNext();
          if (!Float.isNaN(v1) || !Float.isNaN(v2))
            assert v1 == v2 : v1 + " != "+ v2+" count="+iter1;
      }
    }

    else if (dt == DataType.INT) {
      while (iter1.hasNext()) {
          int v1 = iter1.getIntNext();
          int v2 = iter2.getIntNext();
          assert v1 == v2 : v1 + " != "+ v2+" count="+iter1;
      }
    }

    else if (dt == DataType.SHORT) {
      while (iter1.hasNext()) {
          short v1 = iter1.getShortNext();
          short v2 = iter2.getShortNext();
          assert v1 == v2 : v1 + " != "+ v2+" count="+iter1;
      }
    }

    else if (dt == DataType.BYTE) {
      while (iter1.hasNext()) {
          byte v1 = iter1.getByteNext();
          byte v2 = iter2.getByteNext();
          assert v1 == v2 : v1 + " != "+ v2+" count="+iter1;
      }
    }
  }


}
