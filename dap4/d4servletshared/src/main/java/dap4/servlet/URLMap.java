/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.servlet;

import dap4.core.util.DapException;

/**
 * Provide a map from url path -> file system path.
 * The idea is that the space of url paths forms a tree
 * with the paths in the tree being of varying length.
 * Each leaf in the tree is associated with
 * a file path prefix. It is assumed that if the suffix
 * of the url (the part after the prefix from the map)
 * is appended to the path associated with the leaf, then
 * we will have a full file system path leading to
 * the file (or directory) to which we the full url refers.
 */

public interface URLMap
{

    //////////////////////////////////////////////////
    // Type Decls

    // The result of a map is to produce multiple values as defined here.
    public class Result
    {
        public String prefix = null;
        public String suffix = null;
        public String toString() {return prefix + "->" + suffix;}
    }

    //////////////////////////////////////////////////
    // API

    /**
     * Use some maximal prefix of the url path
     * to locate the associated file path prefix.
     * Return a Pair specifying:
     * 1. Pair.prefix: path specifying the file path associated with
     * the url prefix
     * 2. Pair.suffix: the suffix of the url path that was not used.
     *
     * @param urlpath a string specifying the url path to be mapped
     * @return Pair returning items 1 an 2 above.
     * @throws DapException if the map fails
     *                      <p/>
     *                      Note that this map does not deal with the whole url, only the path part
     *                      because as a rule, the host+port is unknown at the time the map is built
     *                      and also because there might be multiple hosts using the same map.
     */
    public Result mapURL(String urlpath) throws DapException;

    /**
     * Inverse of mapURL; returns a url path and suffix: U,S
     * such that mapURL(U)+S = path
     * Return a Pair specifying:
     * 1. Pair.prefix: path specifying the url path associated with
     * the file prefix
     * 2. Pair.suffix: the suffix of the file path that was not used.
     *
     * @param path a string specifying the file path to be mapped
     * @return Pair returning items 1 an 2 above.
     * @throws DapException if the map fails
     *                      <p/>
     *                      This is an optional operation and if not supported,
     *                      throw UnsupportedOperationException.
     */
    public Result mapPath(String path) throws DapException;

    /**
     * Add an entry into the map. Any trailing / on urlprefix
     * or leading / on file prefix will be removed.
     *
     * @param urlprefix
     * @param fileprefix
     */
    public void addEntry(String urlprefix, String fileprefix) throws DapException;

}

