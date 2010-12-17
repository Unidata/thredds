/*****************************************************************************
 *
 * $Id: JP2Box.java,v 1.1 2002/07/25 14:50:47 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.colorspace.boxes;

import ucar.jpeg.colorspace .ColorSpaceException;
import ucar.jpeg.jj2000.j2k.fileformat.FileFormatBoxes;
import ucar.jpeg.icc .ICCProfile;
import ucar.jpeg.jj2000.j2k.util.ParameterList;
import ucar.jpeg.jj2000.j2k.io.RandomAccessIO;

import java.io.IOException;

/**
 * The abstract super class modeling the aspects of
 * a JP2 box common to all such boxes.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public abstract class JP2Box
{
    /** Platform dependant line terminator */ public final static String eol = System.getProperty ("line.separator");
    /** Box type                           */ public static int type;

    /** Return a String representation of the Box type. */
    public static String getTypeString (int t) {
        return BoxType.get(t); }

    /** Length of the box.             */ public     int length;
    /** input file                     */ protected RandomAccessIO in;
    /** offset to start of box         */ protected int boxStart;
    /** offset to end of box           */ protected int boxEnd;
    /** offset to start of data in box */ protected int dataStart;

    public JP2Box ()
        throws ColorSpaceException {
            try { throw new ColorSpaceException ("JP2Box empty ctor called!!"); }
            catch (ColorSpaceException e) {e.printStackTrace(); throw e;}}

    /**
     * Construct a JP2Box from an input image.
     *   @param in RandomAccessIO jp2 image
     *   @param boxStart offset to the start of the box in the image
     * @exception IOException, ColorSpaceException 
     */
    public JP2Box (RandomAccessIO in, int boxStart) 
        throws IOException, ColorSpaceException {
        byte [] boxHeader = new byte [16];

        this.in = in;
        this.boxStart  = boxStart;

        this.in.seek(this.boxStart);
        this.in.readFully(boxHeader,0,8);

        this.dataStart = boxStart+8;
        this.length    = ICCProfile.getInt(boxHeader,0);
        this.boxEnd    = boxStart+length;
        if (length==1) throw new ColorSpaceException("extended length boxes not supported"); }


    /** Return the box type as a String. */
    public String getTypeString () {
        return BoxType.get(this.type); }


    /** JP2 Box structure analysis help */
    protected static class BoxType extends java.util.Hashtable {

        private static java.util.Hashtable map = new java.util.Hashtable();

        static {
            put (FileFormatBoxes.BITS_PER_COMPONENT_BOX,"BITS_PER_COMPONENT_BOX");
            put (FileFormatBoxes.CAPTURE_RESOLUTION_BOX,"CAPTURE_RESOLUTION_BOX");
            put (FileFormatBoxes.CHANNEL_DEFINITION_BOX,"CHANNEL_DEFINITION_BOX");
            put (FileFormatBoxes.COLOUR_SPECIFICATION_BOX,"COLOUR_SPECIFICATION_BOX");
            put (FileFormatBoxes.COMPONENT_MAPPING_BOX,"COMPONENT_MAPPING_BOX");
            put (FileFormatBoxes.CONTIGUOUS_CODESTREAM_BOX,"CONTIGUOUS_CODESTREAM_BOX");
            put (FileFormatBoxes.DEFAULT_DISPLAY_RESOLUTION_BOX,"DEFAULT_DISPLAY_RESOLUTION_BOX");
            put (FileFormatBoxes.FILE_TYPE_BOX,"FILE_TYPE_BOX");
            put (FileFormatBoxes.IMAGE_HEADER_BOX,"IMAGE_HEADER_BOX");
            put (FileFormatBoxes.INTELLECTUAL_PROPERTY_BOX,"INTELLECTUAL_PROPERTY_BOX");
            put (FileFormatBoxes.JP2_HEADER_BOX,"JP2_HEADER_BOX");
            put (FileFormatBoxes.JP2_SIGNATURE_BOX,"JP2_SIGNATURE_BOX");
            put (FileFormatBoxes.PALETTE_BOX,"PALETTE_BOX");
            put (FileFormatBoxes.RESOLUTION_BOX,"RESOLUTION_BOX");
            put (FileFormatBoxes.URL_BOX,"URL_BOX");
            put (FileFormatBoxes.UUID_BOX,"UUID_BOX");
            put (FileFormatBoxes.UUID_INFO_BOX,"UUID_INFO_BOX");
            put (FileFormatBoxes.UUID_LIST_BOX,"UUID_LIST_BOX");
            put (FileFormatBoxes.XML_BOX,"XML_BOX"); }

        private static void put (int type, String desc) {
            map.put (new Integer (type), desc); }

        public static String get (int type) {
            return (String) map.get (new Integer(type));}

        /* end class BoxType */ }

    /* end class JP2Box */ }









