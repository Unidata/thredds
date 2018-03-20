/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

import java.util.TreeMap;
import java.util.Iterator;

/**
 * Statistics utilities.
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */
public class Stat {
  private TreeMap store = new TreeMap();

  public Stat() {
  }

  public void avg( String what, long val) {
     if (store.containsKey( what)) {
       Node n = (Node) store.get(what);
       n.count++;
       n.accum += val;
     } else {
       Node n = new Node();
       n.count = 1;
       n.accum = val;
       store.put(what, n);
     }
  }

  public void print() {
    Iterator iter = store.keySet().iterator();
    while (iter.hasNext()) {
      String what = (String) iter.next();
      Node n = (Node) store.get(what);

      int avg = (int) (n.accum / n.count);
      System.out.println(what+ " "+n.accum+ "total msec "+avg+ "avg msec "+" count= "+n.count);
    }
    System.out.println("\n");

  }

  class Node {
    int   count;
    double accum;
  }
}