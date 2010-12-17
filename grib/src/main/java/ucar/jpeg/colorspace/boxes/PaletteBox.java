/*****************************************************************************
 *
 * $Id: PaletteBox.java,v 1.1 2002/07/25 14:50:47 grosbois Exp $
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

/**
 * This class models the palette box contained in a JP2
 * image.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public final class PaletteBox extends JP2Box
{
    static { type = 0x70636c72; }

    private int nentries;
    private int ncolumns;
    private short [] bitdepth;
    private int [][] entries;

    /**
     * Construct a PaletteBox from an input image.
     *   @param in RandomAccessIO jp2 image
     *   @param boxStart offset to the start of the box in the image
     * @exception IOException, ColorSpaceException 
     */
    public PaletteBox (RandomAccessIO in, int boxStart) 
        throws IOException, ColorSpaceException {
        super (in, boxStart);
        readBox(); }

    /** Analyze the box content. */
    void readBox() 
        throws IOException, ColorSpaceException {
        byte [] bfr = new byte [4];
        int i,j,b,m;
        int entry;

        // Read the number of palette entries and columns per entry.
        in.seek((int)dataStart);
        in.readFully (bfr,0,3);
        nentries = ICCProfile.getShort(bfr,0) & 0x0000ffff;
        ncolumns = bfr[2] & 0x0000ffff;

        // Read the bitdepths for each column
        bitdepth = new short [ncolumns];
        bfr = new byte [ncolumns];
        in.readFully (bfr,0,ncolumns);
        for (i=0; i<ncolumns; ++i) {
            bitdepth [i] = (short)(bfr[i] & 0x00fff); }

        entries = new int [nentries*ncolumns] [];

        bfr = new byte [2];
        for (i=0; i<nentries; ++i) {
            entries [i] = new int [ncolumns];

            for (j=0; j<ncolumns; ++j) {

                int bd = getBitDepth(j);
                boolean signed = isSigned(j);

                switch (getEntrySize(j)) {
                case 1:  // 8 bit entries
                    in.readFully (bfr,0,1);
                    b = bfr[0];
                    break;

                case 2:  // 16 bits
                    in.readFully (bfr,0,2);
                    b = ICCProfile.getShort (bfr,0);
                    break;

                default:
                    throw new ColorSpaceException ("palettes greater than 16 bits deep not supported");
                }

                if (signed) {
                    // Do sign extension if high bit is set.
                    if ((b & (1<<(bd-1))) == 0) {
                        // high bit not set.
                        m = (1 << bd)-1;
                        entries [i][j] = m & b ; }
                    else {
                        // high bit set.
                        m = 0xffffffff << bd;
                        entries [i][j] = m | b; }}
                else {
                    // Clear all high bits.
                    m = (1 << bd)-1;
                    entries [i][j] = m &  b; }}}}

    /** Return the number of palette entries. */
    public int getNumEntries () {
        return nentries; }

    /** Return the number of palette columns. */
    public int getNumColumns () {
        return ncolumns; }

    /** Are entries signed predicate. */
    public boolean isSigned (int column) {
        return (bitdepth[column] & 0x80) == 1; }

    /** Are entries unsigned predicate. */
    public boolean isUnSigned (int column) {
        return !isSigned(column); }

    /** Return the bitdepth of palette entries. */
    public short getBitDepth (int column) {
        return (short) ((bitdepth[column] & 0x7f) +1); }

    /** Return an entry for a given index and column. */
    public int getEntry(int column, int entry) {
    return entries[entry][column]; }

    /** Return a suitable String representation of the class instance. */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[PaletteBox ")
            .append("nentries= ").append(String.valueOf(nentries))
            .append(", ncolumns= ").append(String.valueOf(ncolumns))
            .append(", bitdepth per column= (");
        for (int i=0; i<ncolumns; ++i) 
            rep.append(getBitDepth(i)).append(isSigned(i)?"S":"U")
                .append(i<ncolumns-1? ", ": "");
        return rep.append(")]").toString(); }

    private int getEntrySize(int column) {
        int bd = getBitDepth (column); 
        return bd/8 + (bd%8)==0 ? 0: 1; }

    /* end class PaletteBox */ }

