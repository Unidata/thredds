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
