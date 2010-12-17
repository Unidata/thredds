/*****************************************************************************
 *
 * $Id: ImageHeaderBox.java,v 1.1 2002/07/25 14:50:47 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.colorspace.boxes;

import ucar.jpeg.colorspace .ColorSpaceException;
import ucar.jpeg.jj2000.j2k.util.ParameterList;
import ucar.jpeg.jj2000.j2k.io.RandomAccessIO;
import ucar.jpeg.icc .ICCProfile;

import java.io.IOException;

/**
 * This class models the Image Header box contained in a JP2
 * image.  It is a stub class here since for colormapping the
 * knowlege of the existance of the box in the image is sufficient.
 * 
 * @version	1.0
 * @author	Bruce A. Kern
 */
public final class ImageHeaderBox extends JP2Box
{
    static { type = 69686472; }

    long height;
    long width;
    int nc;
    short bpc;
    short c;
    boolean unk;
    boolean ipr;
    

    /**
     * Construct an ImageHeaderBox from an input image.
     *   @param in RandomAccessIO jp2 image
     *   @param boxStart offset to the start of the box in the image
     * @exception IOException, ColorSpaceException
     */
    public ImageHeaderBox (RandomAccessIO in, int boxStart)
        throws IOException, ColorSpaceException {
        super (in, boxStart);
        readBox(); }

    /** Return a suitable String representation of the class instance. */
    public String toString() {
        StringBuffer rep = new StringBuffer("[ImageHeaderBox ").append(eol).append("  ");
        rep.append("height= ").append(String.valueOf(height)).append(", ");
        rep.append("width= ").append(String.valueOf(width)).append(eol).append("  ");

        rep.append("nc= ").append(String.valueOf(nc)).append(", ");
        rep.append("bpc= ").append(String.valueOf(bpc)).append(", ");
        rep.append("c= ").append(String.valueOf(c)).append(eol).append("  ");

        rep.append("image colorspace is ").append(new String (unk==true?"known":"unknown"));
        rep.append(", the image ")
            .append(new String (ipr==true?"contains ":"does not contain "))
            .append("intellectual property").append("]");

    return rep.toString(); }

    /** Analyze the box content. */
    void readBox() throws IOException {
        byte [] bfr = new byte [14];
        in.seek(dataStart);
        in.readFully (bfr,0,14);

        height             = ICCProfile.getInt(bfr,0);
        width              = ICCProfile.getInt(bfr,4);
        nc                 = ICCProfile.getShort(bfr,8);
        bpc                = (short) (bfr[10] & 0x00ff);
        c                  = (short) (bfr[11] & 0x00ff);
        unk                = bfr[12]==0?true:false;
        ipr                = bfr[13]==1?true:false; }

    /* end class ImageHeaderBox */ }











