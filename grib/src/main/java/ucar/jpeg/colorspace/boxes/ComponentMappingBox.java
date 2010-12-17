/*****************************************************************************
 *
 * $Id: ComponentMappingBox.java,v 1.1 2002/07/25 14:50:46 grosbois Exp $
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
import java.util.Enumeration;
import java.util.Vector;

/**
 * This class maps the components in the codestream
 * to channels in the image.  It models the Component
 * Mapping box in the JP2 header.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public final class ComponentMappingBox extends JP2Box
{
    static { type = 0x636d6170; }

    private int nChannels;
    private Vector map = new Vector();

    /**
     * Construct a ComponentMappingBox from an input image.
     *   @param in RandomAccessIO jp2 image
     *   @param boxStart offset to the start of the box in the image
     * @exception IOException, ColorSpaceException 
     */
    public ComponentMappingBox (RandomAccessIO in, int boxStart)
        throws IOException, ColorSpaceException {
        super (in, boxStart); 
        readBox(); }

    /** Analyze the box content. */
    void readBox() throws IOException {
        nChannels = (boxEnd-dataStart) / 4;
        in.seek(dataStart);
        for (int offset=dataStart; offset<boxEnd; offset += 4) {
            byte [] mapping = new byte[4];
            in.readFully(mapping,0,4);
            map.addElement(mapping); }}

    /* Return the number of mapped channels. */
    public int getNChannels () {
        return nChannels; }

    /* Return the component mapped to the channel. */
    public int getCMP (int channel) {
        byte [] mapping = (byte[]) map.elementAt(channel);
        return ICCProfile.getShort(mapping, 0) & 0x0000ffff; }

    /** Return the channel type. */
    public short getMTYP (int channel) {
        byte [] mapping = (byte[]) map.elementAt(channel);
        return (short) (mapping[2] & 0x00ff); }

    /** Return the palette index for the channel. */
    public short getPCOL (int channel) {
        byte [] mapping = (byte[]) map.elementAt(channel);
        return (short) (mapping[3] & 0x000ff); }

    /** Return a suitable String representation of the class instance. */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[ComponentMappingBox ").append("  ");
        rep.append("nChannels= ").append(String.valueOf(nChannels));
        Enumeration enumeration = map.elements();
        while (enumeration.hasMoreElements()) {
            byte [] bfr = (byte[]) enumeration.nextElement();
            rep.append(eol).append("  ").append("CMP= ").append(String.valueOf(getCMP(bfr))).append(", ");
            rep.append("MTYP= ").append(String.valueOf(getMTYP(bfr))).append(", ");
            rep.append("PCOL= ").append(String.valueOf(getPCOL(bfr))); }
        rep.append ("]");
        return rep.toString(); }

    private int getCMP (byte [] mapping) {
        return ICCProfile.getShort(mapping, 0) & 0x0000ffff; }

    private short getMTYP (byte [] mapping) {
        return (short) (mapping[2] & 0x00ff); }

    private short getPCOL (byte [] mapping) {
        return (short) (mapping[3] & 0x000ff); }

    /* end class ComponentMappingBox */ }









