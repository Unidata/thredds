/*****************************************************************************
 *
 * $Id: ChannelDefinitionBox.java,v 1.1 2002/07/25 14:50:46 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.colorspace.boxes;

import ucar.jpeg.colorspace .ColorSpaceException;
import ucar.jpeg.icc .ICCProfile;
import ucar.jpeg.jj2000.j2k.util.ParameterList;
import ucar.jpeg.jj2000.j2k.io.RandomAccessIO;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * This class maps the components in the codestream
 * to channels in the image.  It models the Component
 * Mapping box in the JP2 header.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public final class ChannelDefinitionBox extends JP2Box
{
    static { type = 0x63646566; }

    private int ndefs;
    private Hashtable definitions = new Hashtable();

    /**
     * Construct a ChannelDefinitionBox from an input image.
     *   @param in RandomAccessIO jp2 image
     *   @param boxStart offset to the start of the box in the image
     * @exception IOException, ColorSpaceException 
     */
    public ChannelDefinitionBox (RandomAccessIO in, int boxStart) 
        throws IOException, ColorSpaceException {
        super (in, boxStart);
        readBox(); }
    
    /** Analyze the box content. */
    private void readBox() throws IOException {
        
        byte [] bfr = new byte [8];

        in.seek(dataStart);
        in.readFully (bfr,0,2);
        ndefs = ICCProfile.getShort(bfr,0) & 0x0000ffff;

        int offset = dataStart+2;
        in.seek (offset);
        for (int i=0; i<ndefs; ++i) {
            in.readFully(bfr,0,6);
            int channel = ICCProfile.getShort(bfr,0);
            int [] channel_def = new int [3];
            channel_def[0] = getCn(bfr);
            channel_def[1] = getTyp(bfr);
            channel_def[2] = getAsoc(bfr);
            definitions.put (new Integer(channel_def[0]), channel_def); }}

    /* Return the number of channel definitions. */
    public int getNDefs () {
        return ndefs; }

    /* Return the channel association. */
    public int getCn (int asoc) {
        Enumeration keys = definitions.keys();
        while (keys.hasMoreElements()) {
            int [] bfr = (int []) definitions.get(keys.nextElement());
            if (asoc == getAsoc(bfr)) 
                return getCn(bfr); }
        return asoc; }

    /* Return the channel type. */
    public int getTyp (int channel) {
        int[] bfr = (int[]) definitions.get(new Integer (channel));
        return getTyp(bfr); }

    /* Return the associated channel of the association. */
    public int getAsoc (int channel) {
        int[] bfr = (int[]) definitions.get(new Integer (channel));
        return getAsoc(bfr); }


    /** Return a suitable String representation of the class instance. */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[ChannelDefinitionBox ").append(eol).append("  ");
        rep.append("ndefs= ").append(String.valueOf(ndefs));
        
        Enumeration keys= definitions.keys();
        while (keys.hasMoreElements()) {
            int[] bfr = (int[]) definitions.get(keys.nextElement());
            rep.append(eol).append("  ")
                .append("Cn= ").append(String.valueOf(getCn(bfr))).append(", ")
                .append("Typ= ").append(String.valueOf(getTyp(bfr))).append(", ")
                .append("Asoc= ").append(String.valueOf(getAsoc(bfr))); }
        
        rep.append ("]");
        return rep.toString(); }

    /** Return the channel from the record.*/
    private int getCn (byte [] bfr) {
        return ICCProfile.getShort(bfr,0); }

    /** Return the channel type from the record.*/
    private int getTyp (byte [] bfr) {
        return ICCProfile.getShort(bfr,2); }

    /** Return the associated channel from the record.*/
    private int getAsoc (byte [] bfr) {
        return ICCProfile.getShort(bfr,4); }

    private int getCn (int [] bfr) {
        return bfr[0]; }

    private int getTyp (int [] bfr) {
        return bfr[1]; }

    private int getAsoc (int [] bfr) {
        return bfr[2]; }

    /* end class ChannelDefinitionBox */ }











