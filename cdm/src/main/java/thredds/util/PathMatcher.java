// $Id: PathMatcher.java,v 1.4 2006/06/06 16:17:08 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

import java.util.*;

/**
 * A Collection of (String key, Object) which is sorted on key.
 * match( String path) returns the Object whose key is the longest that matches path.
 * Match means that path.startsWith( key).
 */
public class PathMatcher {

  private TreeMap treeMap;

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
    if (path.startsWith( after))
      return treeMap.get( after);
    return null;
  }

  private class PathComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      return -1 * o1.toString().compareTo( o2.toString());
    }
  }

  // testing
  private void doit( String s) {
    System.out.println(s+" == "+match(s));
  }

  static public void main( String[] args) {
    PathMatcher m = new PathMatcher();
    m.put("/thredds/dods/test/longer", null);
    m.put("/thredds/dods/test", null);
    m.put("/thredds/dods/tester", null);
    m.put("/thredds/dods/short", null);
    m.put("/actionable", null);
    m.put("myworld", null);
    m.put("mynot", null);

    m.doit("nope");
    m.doit("/thredds/dods/test");
    m.doit("/thredds/dods/test/lo");
    m.doit("/thredds/dods/test/longer/donger");
    m.doit("myworldly");
    m.doit("/my");
    m.doit("mysnot");
  }

}

/* Change History:
   $Log: PathMatcher.java,v $
   Revision 1.4  2006/06/06 16:17:08  caron
   *** empty log message ***

   Revision 1.3  2006/05/08 02:47:19  caron
   cleanup code for 1.5 compile
   modest performance improvements
   dapper reading, deal with coordinate axes as structure members
   improve DL writing
   TDS unit testing

   Revision 1.2  2006/04/20 22:13:15  caron
   improve DL record extraction
   CatalogCrawler improvements

   Revision 1.1  2005/11/03 19:30:20  caron
   no message

*/