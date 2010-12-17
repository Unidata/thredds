/*****************************************************************************
 *
 * $Id: ICCTagTable.java,v 1.1 2002/07/25 14:56:37 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.icc.tags;

import java.io.PrintStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import ucar.jpeg.colorspace .ColorSpace;
import ucar.jpeg.icc .ICCProfile;
import ucar.jpeg.icc .types.ICCProfileHeader;

/**
 * This class models an ICCTagTable as a HashTable which maps 
 * ICCTag signatures (as Integers) to ICCTags.
 *
 * On disk the tag table exists as a byte array conventionally aggragted into a
 * structured sequence of types (bytes, shorts, ints, and floats.  The first four bytes
 * are the integer count of tags in the table.  This is followed by an array of triplets,
 * one for each tag. The triplets each contain three integers, which are the tag signature,
 * the offset of the tag in the byte array and the length of the tag in bytes.
 * The tag data follows.  Each tag consists of an integer (4 bytes) tag type, a reserved integer
 * and the tag data, which varies depending on the tag.
 * 
 * @see	jj2000.j2k.icc.tags.ICCTag
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class ICCTagTable extends Hashtable
{
    private static final String eol = System.getProperty("line.separator");
    private static final int offTagCount = ICCProfileHeader.size;
    private static final int offTags     = offTagCount + ICCProfile.int_size;

    private final Vector trios = new Vector ();

    private int tagCount;

    
    static private class Triplet {
        /** Tag identifier              */ private int signature;
        /** absolute offset of tag data */ private int offset;
        /** length of tag data          */ private int count;
        /** size of an entry            */ public static final int size = 3*ICCProfile.int_size;


        Triplet (int signature, int offset, int count) {
            this.signature = signature;
            this.offset = offset;
            this.count = count; }}

    /**
     * Representation of a tag table
     * @return String
     */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[ICCTagTable containing " + tagCount + " tags:");
        StringBuffer body = new StringBuffer ("  ");
        Enumeration keys = keys();
        while (keys.hasMoreElements()) {
            Integer key = (Integer) keys.nextElement();
            ICCTag tag = (ICCTag) get(key);
            body.append(eol).append(tag.toString()); }
        rep.append(ColorSpace.indent("  ",body));
    return rep.append("]") .toString(); }


    /**
     * Factory method for creating a tag table from raw input.
     *   @param byte array of unstructured data representing a tag
     * @return ICCTagTable
     */
    public static ICCTagTable createInstance (byte [] data) {
        ICCTagTable tags = new ICCTagTable(data);
        return tags; }


    /**
     * Ctor used by factory method.
     *   @param byte raw tag data
     */
    protected ICCTagTable (byte [] data) {
        tagCount = ICCProfile.getInt(data,offTagCount);

        int offset=offTags;
        for (int i=0; i<tagCount; ++i) {
            int signature = ICCProfile.getInt(data, offset);
            int tagOffset = ICCProfile.getInt(data, offset+ICCProfile.int_size);
            int length    = ICCProfile.getInt(data, offset+2*ICCProfile.int_size);
            trios.addElement (new Triplet (signature, tagOffset, length));
            offset+=3*ICCProfile.int_size; }


        Enumeration enumeration = trios.elements();
        while (enumeration.hasMoreElements()) {
            Triplet trio = (Triplet) enumeration.nextElement();
            ICCTag tag = ICCTag.createInstance (trio.signature, data, trio.offset, trio.count);
            put (new Integer(tag.signature), tag); }
    }


    /**
     * Output the table to a disk
     *   @param raf RandomAccessFile which receives the table.
     * @exception IOException
     */
    public void write (RandomAccessFile raf) throws IOException {

        int ntags = trios.size();

        int countOff = ICCProfileHeader.size;
        int tagOff   = countOff + ICCProfile.int_size;
        int dataOff = tagOff + 3*ntags*ICCProfile.int_size;

        raf.seek (countOff);
        raf.writeInt (ntags);

        int currentTagOff  = tagOff;
        int currentDataOff = dataOff;

        Enumeration enumeration = trios.elements();
        while (enumeration.hasMoreElements()) {
            Triplet trio = (Triplet) enumeration.nextElement();
            ICCTag tag = (ICCTag) get (new Integer(trio.signature));
            
            raf.seek (currentTagOff);
            raf.writeInt (tag.signature);
            raf.writeInt (currentDataOff);
            raf.writeInt (tag.count);
            currentTagOff += 3*trio.size;

            raf.seek (currentDataOff);
            raf.write (tag.data, tag.offset, tag.count);
            currentDataOff += tag.count; }}
    
    /* end class ICCTagTable */ }









