// $Id$
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
package thredds.util;

import junit.framework.TestCase;

import java.util.*;

/**
 * @author john
 */
public class TestCollections extends TestCase {
  Random rand = new Random( System.currentTimeMillis());
  boolean show = false;

  String generate() {
    int n = 20 + rand.nextInt(40);
    byte[] b = new byte[n];
    for (int i = 0; i < b.length; i++) {
      b[i] = (byte) (32 + rand.nextInt(50));
    }
    return new String(b);
  }

  List makeCollection(int n) {
    List list = new ArrayList(n);
    for (int i = 0; i < n; i++) {
      list.add( generate());
    }
    return list;
  }

  void search(TreeSet tset, int n) {
    for (int i = 0; i < n; i++) {
      String want = generate();

      SortedSet head = tset.headSet( want);
      SortedSet tail = tset.tailSet( want);
      String before = head.isEmpty() ? "" :  (String) head.last();
      String after = tail.isEmpty() ? null :  (String) tail.first();
      assert want.compareTo( before) > 0;
      if (after != null) {
        assert want.compareTo( after) <= 0;
      } else {
        System.out.println(" last");
      }
      
      if (show) {
        System.out.println(" before = "+before);
        System.out.println(" want =   "+want);
        System.out.println(" after =  "+after);
        System.out.println("---");
      }
    }
  }

  void search(TreeMap tmap, int n) {
    for (int i = 0; i < n; i++) {
      String want = generate();

      SortedMap head = tmap.headMap( want);
      SortedMap tail = tmap.tailMap( want);
      String before = head.isEmpty() ? "" :  (String) head.lastKey();
      String after = tail.isEmpty() ? null :  (String) tail.firstKey();
      assert want.compareTo( before) > 0;
      assert want.compareTo( after) <= 0;
      if (show) {
        System.out.println(" before = "+before);
        System.out.println(" want =   "+want);
        System.out.println(" after =  "+after);
        System.out.println("---");
      }
    }
  }

  public void testTreeSet() {
    long start = System.currentTimeMillis();
    int n = 10000;
    TreeSet tset = new TreeSet( makeCollection( n));
    long took = System.currentTimeMillis() - start;
    System.out.println("making "+n+" TreeSet took "+took+" msec");

    n = 10000;
    start = System.currentTimeMillis();
    search(tset, n);
    double ftook = (float) (System.currentTimeMillis() - start) / n;
    System.out.println("searching "+n+" TreeSet took "+ftook+" msec/call");
  }


  public void testTreeMap() {
    TestCollections tc = new TestCollections();

    long start = System.currentTimeMillis();
    int n = 10000;
    List list = makeCollection( n);
    TreeMap tmap = new TreeMap( );
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      tmap.put( o, o);
    }
    long took = System.currentTimeMillis() - start;
    System.out.println("making "+n+" TreeMap took "+took+" msec");

    n = 10000;
    start = System.currentTimeMillis();
    search(tmap, n);
    double ftook = (float) (System.currentTimeMillis() - start) / n;
    System.out.println("searching "+n+" TreeMap took "+ftook+" msec/call");
  }
}

/* Change History:
   $Log: TestCollections.java,v $
   Revision 1.1  2005/11/03 19:30:20  caron
   no message

*/