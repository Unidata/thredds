/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.util;

import dap4.core.dmr.*;

import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;

/**
 * Misc. Utility methods
 */

abstract public class DapUtil // Should only contain static methods
{
    //////////////////////////////////////////////////
    // Constants

    static final public BigInteger BIG_UMASK64 = new BigInteger("FFFFFFFFFFFFFFFF", 16);
    static final public Charset UTF8 = Charset.forName("UTF-8");

    // Define the Serialization Constants common to servlet and client

    static final public ByteOrder NETWORK_ORDER = ByteOrder.BIG_ENDIAN;
    static final public ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();

    // Use Bit flags to avoid heavyweight enumset
    static public final int CHUNK_DATA = 0;  // bit 0 : value 0
    static final public int CHUNK_END = 1;   // bit 0 : value 1
    static public final int CHUNK_ERROR = 2; // bit 1 : value 1
    static public final int CHUNK_LITTLE_ENDIAN = 4; // bit 2: value 1
    // Construct the union of all flags
    static final public int CHUNK_ALL
            = CHUNK_DATA | CHUNK_ERROR | CHUNK_END | CHUNK_LITTLE_ENDIAN;

    static final public String LF = "\n";
    static final public String CRLF = "\r\n";
    static final public int CRLFSIZE = 2;

    //static final public int CHECKSUMSIZE = 16; // bytes if MD5
    //static final public String DIGESTOR = "MD5";

    static final public int CHECKSUMSIZE = 4; // bytes if CRC32
    static final public String DIGESTER = "CRC32";

    static final public String DRIVELETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    //////////////////////////////////////////////////
    // return last name part of an fqn; result will be escaped.

    static public String fqnSuffix(String fqn)
    {
        int structindex = fqn.lastIndexOf('.');
        int groupindex = fqn.lastIndexOf('/');
        if(structindex >= 0)
            return fqn.substring(structindex + 1, fqn.length());
        else
            return fqn.substring(groupindex + 1, fqn.length());
    }

    // return prefix name part of an fqn; result will be escaped.
    static public String fqnPrefix(String fqn)
    {
        int structindex = fqn.lastIndexOf('.');
        int groupindex = fqn.lastIndexOf('/');
        if(structindex >= 0)
            return fqn.substring(0, structindex);
        else
            return fqn.substring(0, groupindex);
    }

    /**
     * Split a string with respect to a separator
     * character and taking backslashes into consideration.
     *
     * @param s   The string to split
     * @param sep The character on which to split
     * @return a List of strings (all with escaping still intact)
     *         representing s split at unescaped instances of sep.
     */
    static public List<String>
    backslashSplit(String s, char sep)
    {
        List<String> path = new ArrayList<String>();
        int len = s.length();
        StringBuilder piece = new StringBuilder();
        int i = 0;
        for(; i <= len - 1; i++) {
            char c = s.charAt(i);
            if(c == '\\' && i < (len - 1)) {
                piece.append(c); // keep escapes in place
                piece.append(s.charAt(++i));
            } else if(c == sep) {
                path.add(piece.toString());
                piece.setLength(0);
            } else
                piece.append(c);
        }
        path.add(piece.toString());
        return path;
    }

    static public boolean hasSequence(DapNode node)
    {
        switch (node.getSort()) {
        case SEQUENCE:
            return true;

        case STRUCTURE:
            DapStructure container = (DapStructure) node;
            for(int i = 0; i < container.getFields().size(); i++) {
                if(hasSequence(container.getFields().get(i)))
                    return true;
            }
            break;

        case GROUP:
            DapGroup group = (DapGroup) node;
            for(int i = 0; i < group.getDecls().size(); i++) {
                if(hasSequence(group.getDecls().get(i)))
                    return true;
            }
            break;

        // Following can never have/be sequence
        case GRID:
        case ENUMERATION:
        case ATTRIBUTE:
        case DIMENSION:
        case ATOMICVARIABLE:
        case XML:
        default: /* ignore */
            break;
        }
        return false;
    }

    /**
     * Walk the specified subtree dir tree to try to locate file|dir named filename.
     * Use breadth first search.
     *
     * @param filename Name of the file|dir to locate
     * @param abspath  Absolute path from which to start search
     * @param wantdir  True if we are looking for a directory,
     *                 false if we are looking for a file
     * @return absolute path of the file or null
     */
    static public String
    locateFile(String filename, String abspath, boolean wantdir)
    {
        Deque<String> q = new ArrayDeque<String>();
        // clean up the path and filename
        filename = filename.trim().replace('\\', '/');
        abspath = abspath.trim().replace('\\', '/');
        if(filename.charAt(0) == '/') filename = filename.substring(1);
        if(filename.endsWith("/")) filename = filename.substring(0, filename.length() - 1);
        if(abspath.endsWith("/")) abspath = abspath.substring(0, abspath.length() - 1);
        q.addFirst(abspath);  // prime the search queue

        for(; ; ) {  // breadth first search
            String currentpath = q.poll();
            if(currentpath == null) break; // done searching
            File current = new File(currentpath);
            File[] contents = current.listFiles();
            if(contents != null) {
                for(File subfile : contents) {
                    if(!subfile.getName().equals(filename)) continue;
                    if((wantdir && subfile.isDirectory())
                            || (!wantdir && subfile.isFile())) {
                        // Assume this is it
                        return DapUtil.canonicalpath(subfile.getAbsolutePath());
                    }
                }
                for(File subfile : contents) {
                    if(subfile.isDirectory())
                        q.addFirst(currentpath + "/" + subfile.getName());
                }
            }
        }
        return null;
    }

    /**
     * Walk the specified dir tree to locate file specified by relative path.
     * Use breadth first search.
     *
     * @param relpath Name of the file|dir to locate
     * @param abspath Absolute path from which to start search
     * @param wantdir True if we are looking for a directory,
     *                false if we are looking for a file
     * @return absolute path of the file|dir wrt to abspath
     */
    static public String
    locateRelative(String relpath, String abspath, boolean wantdir)
    {
        // clean up the path and filename
        relpath = relpath.trim().replace('\\', '/');
        if(relpath.charAt(0) == '/') relpath = relpath.substring(1);
        if(relpath.endsWith("/")) relpath = relpath.substring(0, relpath.length() - 1);
        String[] pieces = relpath.split("[/]");
        String partial = abspath;
        for(int i = 0; i < pieces.length - 1; i++) {
            String nextdir = locateFile(pieces[i], abspath, true);
            if(nextdir == null) return null;
            partial = nextdir;
        }
        // See if the final file|dir exists in this dir
        String finalpath = locateFile(pieces[pieces.length - 1], partial, wantdir);
        return finalpath;
    }

    /**
     * Convert path to:
     * 1. use '/' consistently
     * 2. remove any trailing '/'
     * 3. trim blanks
     *
     * @param path convert this path
     * @return canonicalized version
     */
    static public String
    canonicalpath(String path)
    {
        if(path == null) return null;
        path = path.trim();
        path = path.replace('\\', '/');
        if(path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        // As a last step, lowercase the drive letter, if any
        if(hasDriveLetter(path))
            path = path.substring(0, 1).toLowerCase() + path.substring(1);
        return path;
    }

    static public String
    relativize(String path)
    {
        if(path != null) {
            if(path.startsWith("/"))
                path = path.substring(1);
            else if(hasDriveLetter(path))
                path = path.substring(2);
        }
        return path;
    }

    static public String
    absolutize(String path)
    {
        if(path != null && !path.startsWith("/") && !hasDriveLetter(path))
            path = "/" + path;
        return path;
    }

    static public boolean
    checkFixedSize(DapVariable var)
    {
        switch (var.getSort()) {
        case ATOMICVARIABLE:
            DapType dt = var.getBaseType();
            return dt.isFixedSize();

        case STRUCTURE:
        case SEQUENCE:
        case GRID:
            for(DapVariable field : ((DapStructure) var).getFields()) {
                if(!checkFixedSize(field)) return false;
            }
            break;

        default:
            break;
        }
        return true;
    }

    /**
     * Properly extract the byte contents of a ByteBuffer
     *
     * @param buf The buffer whose content is to be extracted
     *            as defined by the buffer limit.
     * @return The byte array contents of the buffer
     */
    static public byte[] extract(ByteBuffer buf)
    {
        int len = buf.limit();
        byte[] bytes = new byte[len];
        buf.rewind();
        buf.get(bytes);
        return bytes;
    }

    static public byte[]
    readbinaryfile(InputStream stream)
            throws IOException
    {
        // Extract the stream into a bytebuffer
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] tmp = new byte[1 << 16];
        for(; ; ) {
            int cnt;
            try {
                cnt = stream.read(tmp);
                if(cnt <= 0) break;
                bytes.write(tmp, 0, cnt);
            } catch (IOException ioe) {
                throw new DapException(ioe);
            }
        }
        return bytes.toByteArray();
    }

    static public String
    readtextfile(InputStream stream)
            throws IOException
    {
        StringBuilder buf = new StringBuilder();
        InputStreamReader rdr = new InputStreamReader(stream, UTF8);
        for(; ; ) {
            int c = rdr.read();
            if(c < 0) break;
            buf.append((char) c);
        }
        return buf.toString();
    }

    /**
     * Given a dap variable, get the path  from the
     * top-level variable to and including the given variable
     * such that all but the last element is a structure.
     */
    static public List<DapVariable>
    getStructurePath(DapVariable var)
    {
        List<DapNode> path = var.getPath();
        List<DapVariable> structpath = new ArrayList<DapVariable>();
        for(int i = 0; i < path.size(); i++) {
            DapNode node = path.get(i);
            switch (node.getSort()) {
            case DATASET:
            case GROUP:
                break;
            case ATOMICVARIABLE:
            case STRUCTURE:
                structpath.add((DapVariable) node);
                break;
            default:
                assert false : "Internal error";
            }
        }
        return structpath;
    }

    /**
     * Convert null paths to ""
     *
     * @param path
     * @return path or ""
     */
    static public String
    denullify(String path)
    {
        return (path == null  ? "" : path);
    }

    /**
     * Convert "" paths to null
     *
     * @param path
     * @return path or null
     */
    static public String
    nullify(String path)
    {
        return (path != null && path.length() == 0 ? null : path);
    }

    static public long dimProduct(List<DapDimension> dimset) // dimension crossproduct
    {
        long count = 1;
        for(DapDimension dim : dimset) {
            count *= dim.getSize();
        }
        return count;
    }

    /**
     * Given a view, get the universal
     * View either from a DapDataset.
     */
/*
    static public View
    createView(DapDataset dataset)
        throws DapException
    {
        View u = new View(dataset);
        List<DapNode> nodes = dataset.getNodeList();
        for(DapNode node : nodes) {
            switch (node.getSort()) {
            case ATOMICVARIABLE:
            case STRUCTURE:
            case SEQUENCE:
                u.put((DapVariable) node);
                break;
            default:
                break;
            }
        }
        u.finish(!View.EXPAND);
        return u;
    }
*/
    static public List<Slice>
    dimsetSlices(List<DapDimension> dimset)
            throws DapException
    {
        List<Slice> slices = new ArrayList<Slice>(dimset.size());
        for(int i = 0; i < dimset.size(); i++) {
            DapDimension dim = dimset.get(i);
            Slice s = new Slice(dim);
            slices.add(s);
        }
        return slices;
    }

    /**
     * Test a List<Slice> against set of DapDimensions
     * to see if the list is whole wrt the dimensions
     *
     * @param slices the set of slices
     * @param dimset the list of DapDimension
     * @result true if slices is whole wrt dimset; false otherwise
     */
    static public boolean
    isWhole(List<Slice> slices, List<DapDimension> dimset)
    {
        if(slices.size() != dimset.size())
            return false;
        for(int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            DapDimension dim = dimset.get(i);
            if(slice.getStride() != 1 || slice.getFirst() != 0 || slice.getCount() != dim.getSize())
                return false;
        }
        return true;
    }


    static public long
    sliceProduct(List<Slice> slices) // another crossproduct
    {
        long count = 1;
        for(Slice slice : slices) {
            count *= slice.getCount();
        }
        return count;
    }

    static public boolean
    hasStrideOne(List<Slice> slices)
    {
        for(Slice slice : slices) {
            if(slice.getStride() != 1)
                return false;
        }
        return true;
    }

    /**
     * Given an Array of Strings and a separator and a count,
     * concat the first count elements of an array with separator
     * between them. A null string is treated like "".
     *
     * @param array the array to concat
     * @param sep   the separator
     * @param from  start point for join (inclusive)
     * @param upto  end point for join (exclusive)
     * @return the join
     */
    static public String
    join(String[] array, String sep, int from, int upto)
    {
        if(sep == null) sep = "";
        if(from < 0 || upto <= from || upto > array.length)
            throw new IndexOutOfBoundsException();
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(int i = from; i < upto; i++, first = false) {
            if(!first) result.append(sep);
            result.append(array[i]);
        }
        return result.toString();
    }

    /**
     * Relativizing a path =>  remove any leading '/' and cleaning it
     *
     * @param path
     * @return
     */
    static public String
    xrelpath(String path)
    {
        return DapUtil.canonicalpath(path);
    }

    /**
     * return true if this path appears to start with a windows drive letter
     *
     * @param path
     * @return
     */
    static public boolean
    hasDriveLetter(String path)
    {
        if(path != null && path.length() >= 2) {
            return (DRIVELETTERS.indexOf(path.charAt(0)) >= 0 && path.charAt(1) == ':');
        }
        return false;
    }

    /**
     * Return the set of leading protocols for a url; may be more than one.
     *
     * @param url the url whose protocols to return
     * @return list of leading protocols without the trailing :
     */
    static public List<String>
    getProtocols(String url)
    {
        // break off any leading protocols;
        // there may be more than one.
        // Watch out for Windows paths starting with a drive letter.
        // Each protocol does not have trailing :

        List<String> allprotocols = new ArrayList<>(); // all leading protocols upto path or host

        // Note, we cannot use split because of the context sensitivity
        StringBuilder buf = new StringBuilder(url);
        for(; ; ) {
            int index = buf.indexOf(":");
            if(index < 0) break; // no more protocols
            String protocol = buf.substring(0, index);
            // Check for windows drive letter
            if(index == 1 //=>|protocol| == 1
                    && "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    .indexOf(buf.charAt(0)) >= 0) break;
            allprotocols.add(protocol);
            buf.delete(0, index + 1); // remove the leading protocol
            if(buf.indexOf("/") == 0)
                break; // anything after this is not a protocol
        }
        return allprotocols;
    }

    static public String
    merge(String[] pieces, String sep)
    {
        if(pieces == null) return "";
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < pieces.length; i++) {
            if(i > 0) buf.append(sep);
            buf.append(pieces[i]);
        }
        return buf.toString();
    }

    static String
    multiSliceString(List<List<Slice>> slices)
    {
        if(slices == null || slices.size() == 0)
            return "[]";
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < slices.size(); i++) {
            List<Slice> set = slices.get(i);
            buf.append("[");
            for(int j = 0; j < set.size(); j++) {
                if(i > 0)
                    buf.append(",");
                buf.append(set.get(j));
            }
            buf.append("]");
        }
        buf.append("]");
        return buf.toString();
    }

    static public boolean
    isContiguous(List<Slice> slices)
    {
        if(slices == null)
            return false;
        for(Slice s : slices) {
            if(!s.isContiguous())
                return false;
        }
        return true;
    }


    /**
     * Re-throw run-time exceptions
     */
    static public void checkruntime(Exception e)
    {
	if(e instanceof RuntimeException)
	    throw (RuntimeException)e;
    }

} // class DapUtil

