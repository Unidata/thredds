// $Id:PathMatcher.java 63 2006-07-12 21:50:51Z edavis $
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

import ucar.unidata.util.StringUtil;

import java.util.*;

/**
 * A Collection of (String key, Object) which is sorted on key.
 * match( String path) returns the Object whose key is the longest that matches path.
 * Match means that path.startsWith( key).
 *
 * Matching is thread-safe, as long as put() is no longer being called.
 */
public class PathMatcher {

  private final TreeMap treeMap;

  public PathMatcher() {
    treeMap = new TreeMap( new PathComparator());
  }

  /**
   * Add an object to the collection to be searched by a String key.
   * @param key sort key
   * @param value add this object to the list to be searched.
   */
  public void put(String key, Object value) {
    treeMap.put( key, value == null ? key : value);
  }

  /**
   * See if this object already exists in the collection, using equals().
   * @return existing object, else null.
   */
  public Object get(String  key) {
    return treeMap.get( key);
  }

  /**
   * Get an iterator over the values, in sorted order.
   * @return iterator
   */
  public Iterator iterator() {
    return treeMap.values().iterator();
  }

  /**
   * Find the longest match.
   * @param path
   * @return the value whose key is the longest that matches path, or null if none
   */
  public Object match( String path) {
    SortedMap tail = treeMap.tailMap( path);
    if (tail.isEmpty()) return null;
    String after = (String) tail.firstKey();
    //System.out.println("  "+path+"; after="+afterPath);
    if (path.startsWith( after)) // common case
      return treeMap.get( after);

    // have to check more, until no common starting chars
    Iterator iter = tail.keySet().iterator();
    while (iter.hasNext()) {
      String key =  (String) iter.next();
      if (path.startsWith( key))
        return treeMap.get( key);
      // terminate when theres no match at all.
      if (StringUtil.match(path, key) == 0)
        break;
    }

    return null;
  }

  private class PathComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      int compare = -1 * o1.toString().compareTo( o2.toString()); // reverse sort
      if (debug) System.out.println(" compare "+o1+" to "+o2+" = "+compare);
      return compare;
    }
  }

  // testing
  private void doit( String s) {
    System.out.println(s+" == "+match(s));
  }

  static private boolean debug = false;
  static public void main( String[] args) {
    PathMatcher m = new PathMatcher();
    m.put("/thredds/dods/test/longer", null);
    m.put("/thredds/dods/test", null);
    m.put("/thredds/dods/tester", null);
    m.put("/thredds/dods/short", null);
    m.put("/actionable", null);
    m.put("myworld", null);
    m.put("mynot", null);
    m.put("ncmodels", null);
    m.put("ncmodels/bzipped", null);


    m.doit("nope");
    m.doit("/thredds/dods/test");
    m.doit("/thredds/dods/test/lo");
    m.doit("/thredds/dods/test/longer/donger");
    m.doit("myworldly");
    m.doit("/my");
    m.doit("mysnot");

    debug = true;
    m.doit("ncmodels/canonical");

  }

}