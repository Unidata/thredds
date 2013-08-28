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

package thredds.logs;

import ucar.unidata.util.StringUtil2;

import java.util.TreeMap;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.Comparator;

/**
 * Duplicate thredds.servlet.PathMatcher
 *
 * @author caron
 * @since Mar 24, 2009
 */
public class PathMatcher {

  private final TreeMap<String, Match> treeMap;

  public class Match {
    public String root;
    public String dir;

    Match(String root, String dir) {
      this.root = root;
      this.dir = dir;
    }
  }

  public PathMatcher() {
    treeMap = new TreeMap<String, Match>( new PathComparator());
  }

  /**
   * Add an object to the collection to be searched by a String key.
   * @param root sort key
   * @param dir add this object to the list to be searched.
   */
  public void put(String root, String dir) {
    treeMap.put( root, new Match(root, dir));
  }

  /**
   * See if this object already exists in the collection, using equals().
   * @param key find object that has this key
   * @return existing object, else null.
   */
  public Match get(String  key) {
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
   * @param path find object with longesh match where path.startsWith( key)
   * @return the value whose key is the longest that matches path, or null if none
   */
  public Match match( String path) {
    SortedMap<String, Match> tail = treeMap.tailMap( path);
    if (tail.isEmpty()) return null;
    String after = (String) tail.firstKey();
    //System.out.println("  "+path+"; after="+afterPath);
    if (path.startsWith( after)) // common case
      return treeMap.get( after);

    // have to check more, until no common starting chars
    for (String key : tail.keySet()) {
      if (path.startsWith(key))
        return treeMap.get(key);
      // terminate when theres no match at all.
      if (StringUtil2.match(path, key) == 0)
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