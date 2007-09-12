package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

/** Test reading variable data */

public class TestReadStrides extends TestCase {

  public TestReadStrides( String name) {
    super(name);
  }

  public void testReadStridesCached() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = TestLocalNC2.openFile("ncml/nc/time0.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("T")));

    // read entire array
    Array A = temp.read("0:2,0:3");
    assert (A.getRank() == 2);

    Index ima = A.getIndex();
    int[] shape = A.getShape();
    assert shape[0] == 3;
    assert shape[1] == 4;

    for (int i=0; i<shape[0]; i++) {
      for (int j=0; j<shape[1]; j++) {
        double dval = A.getDouble(ima.set(i,j));
        assert(  dval == (double) (i*10+j)) : dval;
      }
    }

    A = temp.read("0:2:1,0:3:1");
    assert (A.getRank() == 2);

    ima = A.getIndex();
    shape = A.getShape();
    assert shape[0] == 3;
    assert shape[1] == 4;

    for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        double dval = A.getDouble(ima.set(i, j));
        assert(dval == (double) (i * 10 + j)):dval;
      }
    }

    A = temp.read("0:2:2,0:3:2");
    assert (A.getRank() == 2);

    ima = A.getIndex();
    shape = A.getShape();
    assert shape[0] == 2;
    assert shape[1] == 2;

    for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        double dval = A.getDouble(ima.set(i, j));
        assert(dval == (double) (i * 20 + j*2)):dval;
      }
    }

    A = temp.read(":,0:3:2");
    assert (A.getRank() == 2);

    ima = A.getIndex();
    shape = A.getShape();
    assert shape[0] == 3;
    assert shape[1] == 2;

    for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        double dval = A.getDouble(ima.set(i, j));
        assert(dval == (double) (i * 10 + j*2)):dval;
      }
    }

    A = temp.read("0:2:2,:");
    assert (A.getRank() == 2);

    ima = A.getIndex();
    shape = A.getShape();
    assert shape[0] == 2;
    assert shape[1] == 4;

    for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        double dval = A.getDouble(ima.set(i, j));
        assert(dval == (double) (i * 20 + j)):dval;
      }
    }

    ncfile.close();
  }

  public void testReadStridesNoCache() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = TestLocalNC2.openFile("ncml/nc/time0.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("T")));
    temp.setCaching(false);

    Array A;
    Index ima;
    int shape[];

    A = temp.read("0:2:1,0:3:1");
    assert (A.getRank() == 2);

    ima = A.getIndex();
    shape = A.getShape();
    assert shape[0] == 3;
    assert shape[1] == 4;

    for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        double dval = A.getDouble(ima.set(i, j));
        assert(dval == (double) (i * 10 + j)):dval;
      }
    }

    A = temp.read("0:2:2,0:3:2");
    assert (A.getRank() == 2);

    ima = A.getIndex();
    shape = A.getShape();
    assert shape[0] == 2;
    assert shape[1] == 2;

    for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        double dval = A.getDouble(ima.set(i, j));
        assert(dval == (double) (i * 20 + j*2)):dval;
      }
    }

    A = temp.read(":,0:3:2");
    assert (A.getRank() == 2);

    ima = A.getIndex();
    shape = A.getShape();
    assert shape[0] == 3;
    assert shape[1] == 2;

    for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        double dval = A.getDouble(ima.set(i, j));
        assert(dval == (double) (i * 10 + j*2)):dval;
      }
    }

    A = temp.read("0:2:2,:");
    assert (A.getRank() == 2);

    ima = A.getIndex();
    shape = A.getShape();
    assert shape[0] == 2;
    assert shape[1] == 4;

    for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[1]; j++) {
        double dval = A.getDouble(ima.set(i, j));
        assert(dval == (double) (i * 20 + j)):dval;
      }
    }

    ncfile.close();
  }

  public void testReadStridesAll() throws IOException, InvalidRangeException {
    //testReadStrides("test/data/ncml/nc/time0.nc");
    testReadStrides("gini/HI-NATIONAL_14km_IR_20050918_2000.gini");
  }

  private void testReadStrides(String filename) throws IOException, InvalidRangeException {
    NetcdfFile ncfile = TestLocalNC2.openFile(filename);

    List vars = ncfile.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      if (v.getRank() == 0 ) continue;
      if (!v.hasCachedData())
        v.setCaching(false);

      //if (v.getRank() == 1 )
        testVariableReadStrides(v);
    }
     ncfile.close();
  }

   private void testVariableReadStrides(Variable v) throws IOException, InvalidRangeException {
     Array allData = v.read();

     int[] shape = v.getShape();
     if(shape.length < 5)
         return;
     for (int first=0;first<3;first++) {
       for (int stride=2;stride<5;stride++) {

         ArrayList ranges = new ArrayList();
         for (int i = 0; i < shape.length; i++) {
           int last = shape[i] - 1;
           Range r = new Range(first, last, stride);
           ranges.add(r);
         }

         System.out.println( v.getName()+" test range= "+Range.makeSectionSpec(ranges));

         Array sectionRead = v.read(ranges);
         Array sectionMake = allData.sectionNoReduce( ranges);

         TestMA2.testEquals(sectionRead, sectionMake);
       }
     }



   }

}
