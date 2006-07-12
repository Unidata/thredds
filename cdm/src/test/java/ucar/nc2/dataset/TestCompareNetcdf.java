package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.*;

import java.io.IOException;
import java.util.*;

public class TestCompareNetcdf  {

  static public String compareSubset(NetcdfFile ncfile, NetcdfFile subset) throws IOException {

    Iterator s = subset.getDimensions().iterator();
    while (s.hasNext()) {
      Dimension ds = (Dimension) s.next();
      Dimension d = ncfile.findDimension( ds.getName());
      if (null == d)
        return "missing dimension "+ds.getName();
      if (d.getLength() != ds.getLength())
        return "wrong length, dimension "+ds.getName();
      if (d.isUnlimited() != ds.isUnlimited())
        return "wrong unlimited, dimension "+ds.getName();
    }

    Iterator atts = subset.getGlobalAttributes().iterator();
    while (atts.hasNext()) {
      Attribute ds = (Attribute) atts.next();
      Attribute d = ncfile.findGlobalAttribute( ds.getName());
      if (null == d)
        return "missing attribute "+ds.getName();
      if (!d.getDataType().equals(ds.getDataType()))
        return "wrong getDataType, attribute "+ds.getName();
      if (!d.getStringValue().equals(ds.getStringValue()))
        return "wrong Value, attribute "+ds.getName();
    }

    Iterator vs = subset.getVariables().iterator();
    while (vs.hasNext()) {
      Variable ds = (Variable) vs.next();
      Variable d = ncfile.findVariable( ds.getName());
      if (null == d)
        return "missing Variable "+ds.getName();
      String ret = compareVariable( d, ds);
      if ( null != ret)
        return ret;
    }


    return "ok";
  }

  static public String compareVariable(Variable v, Variable subset) throws IOException {

    List s = subset.getDimensions();
    for (int i=0; i<s.size(); i++) {
      Dimension ds = (Dimension) s.get(i);
      Dimension d = v.getDimension(i);
      if (!ds.equals(d))
        return "dimension not equals "+ds.getName()+" for variable "+subset.getName();
    }

    Iterator atts = subset.getAttributes().iterator();
    while (atts.hasNext()) {
      Attribute ds = (Attribute) atts.next();
      Attribute d = v.findAttribute( ds.getName());
      if (null == d)
        return "missing attribute "+ds.getName()+" for variable "+subset.getName();;
      if (!d.getDataType().equals(ds.getDataType()))
        return "wrong getDataType, attribute "+ds.getName()+" for variable "+subset.getName();
      if (!d.getStringValue().equals(ds.getStringValue()))
        return "wrong Value, attribute "+ds.getName()+" for variable "+subset.getName();;
    }

    Array vArray = v.read();
    Array sArray = subset.read();
    IndexIterator vIter = vArray.getIndexIterator();
    IndexIterator sIter = sArray.getIndexIterator();

    while (sIter.hasNext()) {
      if (sIter.getDoubleNext() != vIter.getDoubleNext())
         return "wrong data Value for variable "+subset.getName();

    }

    return null;
  }

}
