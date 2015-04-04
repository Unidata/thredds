/* Copyright */
package thredds.core;

import ucar.unidata.util.StringUtil2;

import java.util.*;

/**
 * Find the dataRoot path from the request, by getting the longest match.
 * Use a TreeSet for minimum memory use
 *
 * @author caron
 * @since 4/1/2015
 */
public class DataRootPathMatcher<T> {
  static private final boolean debug = false;
  private final TreeSet<String> treeSet;    // this should be in-memory for speed
  private final Map<String, T> map;         // this could be turned into an off-heap cache if needed

  public DataRootPathMatcher() {
    treeSet = new TreeSet<>( new PathComparator());
    map = new HashMap<>();
  }

  /**
   * Add a dataRoot path.
   * @param path dataRoot path
   * @return true if not already exist
   */
  public boolean put(String path, T value) {
    map.put(path, value);
    return treeSet.add(path);
  }

  /**
   * See if this object already exists in the collection
   * @param path find object that has this key
   * @return true if already contains the key
   */
  public boolean contains(String  path) {
    return treeSet.contains(path);
  }

  public T get(String  path) {
    return map.get( path);
  }

  /**
   * Get an iterator over the dataRoot paths, in sorted order.
   * @return iterator
   */
  public Iterable<String> getKeys() {
    return treeSet;
  }

  /**
   * Get an iterator over the dataRoot keys and values
   * @return iterator
   */
  public Set<Map.Entry<String, T>> getValues() {
    return map.entrySet();
  }

  /**
   * Find the longest path match.
   * @param reqPath find object with longest match where reqPath.startsWith( key)
   * @return the value whose key is the longest that matches path, or null if none
   */
  public String findLongestPathMatch( String reqPath) {
    SortedSet<String> tail = treeSet.tailSet( reqPath);
    if (tail.isEmpty()) return null;
    String after = tail.first();
    if (reqPath.startsWith( after)) // common case
      return tail.first();

    // have to check more, until no common starting chars
    for (String key : tail) {
      if (reqPath.startsWith(key))
        return key;

      // terminate when there's no match at all.
      if (StringUtil2.match(reqPath, key) == 0)
        break;
    }

    return null;
  }

  /**
   * Find the longest DataRoot match.
   * @param reqPath find object with longest match where reqPath.startsWith( key)
   * @return the value whose key is the longest that matches path, or null if none
   */
  public T findLongestMatch( String reqPath) {
    String path =  findLongestPathMatch(reqPath);
    if (path == null) return null;
    return map.get(path);
  }

  private static class PathComparator implements Comparator<String> {
    public int compare(String s1, String s2) {
      int compare = s2.compareTo( s1); // reverse sort
      if (debug) System.out.println(" compare "+s1+" to "+s2+" = "+compare);
      return compare;
    }
  }
}
