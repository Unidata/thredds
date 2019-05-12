/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.logs;

import com.google.common.base.MoreObjects;
import ucar.unidata.util.StringUtil2;

import java.io.Serializable;
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

  public static class Match {
    public String root;

    Match(String root) {
      this.root = root;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("root", root)
          .toString();
    }
  }

  public PathMatcher() {
    treeMap = new TreeMap<>(new PathComparator());
  }

  /**
   * Add an object to the collection to be searched by a String key.
   * @param root sort key
   */
  public void put(String root) {
    treeMap.put( root, new Match(root));
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
   * @param path find object with longest match where path.startsWith( key)
   * @return the value whose key is the longest that matches path, or null if none
   */
  public Match match( String path) {
    SortedMap<String, Match> tail = treeMap.tailMap( path);
    if (tail.isEmpty()) return null;
    String after = tail.firstKey();
    //System.out.println("  "+path+"; after="+afterPath);
    if (path.startsWith( after)) // common case
      return treeMap.get( after);

    // have to check more, until no common starting chars
    for (String key : tail.keySet()) {
      if (path.startsWith(key))
        return treeMap.get(key);
      // terminate when there's no match at all.
      if (StringUtil2.match(path, key) == 0)
        break;
    }

    return null;
  }


  private static class PathComparator implements Comparator<String>, Serializable {
    public int compare(String o1, String o2) {
      int compare = -1 * o1.compareTo(o2); // reverse sort
      if (debug) System.out.println(" compare "+o1+" to "+o2+" = "+compare);
      return compare;
    }
  }

  // testing
  private void doit( String s) {
    System.out.println(s+" == "+match(s));
  }

  private static boolean debug = false;
  public static void main( String[] args) {
    PathMatcher m = new PathMatcher();
    m.put("/thredds/dods/test/longer");
    m.put("/thredds/dods/test");
    m.put("/thredds/dods/tester");
    m.put("/thredds/dods/short");
    m.put("/actionable");
    m.put("myworld");
    m.put("mynot");
    m.put("ncmodels");
    m.put("ncmodels/bzipped");


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
