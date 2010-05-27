/*****************************************************************************
 *
 * $Id: Resampler.java,v 1.2 2002/08/08 14:07:31 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/
package ucar.jpeg.colorspace;

import ucar.jpeg.jj2000.j2k.util.*;
import ucar.jpeg.jj2000.j2k.image.*;

/**
 * This class resamples the components of an image so that
 * all have the same number of samples.  The current implementation
 * only handles the case of 2:1 upsampling.
 * 
 * @see		jj2000.j2k.colorspace.ColorSpace
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class Resampler extends ColorSpaceMapper {
    private final int minCompSubsX;
    private final int minCompSubsY;
    private final int maxCompSubsX;
    private final int maxCompSubsY;

    final int wspan= 0, hspan= 0;

    /**
     * Factory method for creating instances of this class.
     *   @param src -- source of image data
     *   @param csMap -- provides colorspace info
     * @return Resampler instance
     */
    public static BlkImgDataSrc createInstance(BlkImgDataSrc src, 
					       ColorSpace csMap) 
        throws ColorSpaceException {
        return new Resampler (src, csMap); }

    /**
     * Ctor resamples a BlkImgDataSrc so that all components
     * have the same number of samples.
     *
     *  Note the present implementation does only two to one
     *  respampling in either direction (row, column).
     *
     *   @param src -- Source of image data
     *   @param csm -- provides colorspace info
     */
    protected Resampler (BlkImgDataSrc src, ColorSpace csMap)  
        throws ColorSpaceException {
        super (src, csMap);

        int c;

        // Calculate the minimum and maximum subsampling factor
        // across all channels.
 
        int minX= src.getCompSubsX(0);
        int minY= src.getCompSubsY(0);
        int maxX= minX;
        int maxY= minY;

        for (c = 1; c<ncomps; ++c) {
            minX = Math.min(minX, src.getCompSubsX(c)); 
            minY = Math.min(minY, src.getCompSubsY(c));
            maxX = Math.max(maxX, src.getCompSubsX(c)); 
            maxY = Math.max(maxY, src.getCompSubsY(c)); }

        // Throw an exception for other than 2:1 sampling.
        if ((maxX != 1 && maxX != 2) ||
            (maxY != 1 && maxY != 2)) {
            throw new ColorSpaceException("Upsampling by other than 2:1 not"+
					  " supported"); }
        
        minCompSubsX= minX;
        minCompSubsY= minY;
        maxCompSubsX= maxX;
        maxCompSubsY= maxY;
        
        /* end Resampler ctor */ }

    /**
     * Return a DataBlk containing the requested component
     * upsampled by the scale factor applied to the particular
     * scaling direction
     *
     * Returns, in the blk argument, a block of image data containing the
     * specifed rectangular area, in the specified component. The data is
     * returned, as a copy of the internal data, therefore the returned data
     * can be modified "in place".
     *
     * <P>The rectangular area to return is specified by the 'ulx', 'uly', 'w'
     * and 'h' members of the 'blk' argument, relative to the current
     * tile. These members are not modified by this method. The 'offset' of
     * the returned data is 0, and the 'scanw' is the same as the block's
     * width. See the 'DataBlk' class.
     *
     * <P>If the data array in 'blk' is 'null', then a new one is created. If
     * the data array is not 'null' then it is reused, and it must be large
     * enough to contain the block's data. Otherwise an 'ArrayStoreException'
     * or an 'IndexOutOfBoundsException' is thrown by the Java system.
     *
     * <P>The returned data has its 'progressive' attribute set to that of the
     * input data.
     *
     * @param blk Its coordinates and dimensions specify the area to
     * return. If it contains a non-null data array, then it must have the
     * correct dimensions. If it contains a null data array a new one is
     * created. The fields in this object are modified to return the data.
     *
     * @param c The index of the component from which to get the data. Only 0
     * and 3 are valid.
     *
     * @return The requested DataBlk
     *
     * @see #getCompData
     */
    public DataBlk getInternCompData (DataBlk outblk, int c) {

        // If the scaling factor of this channel is 1 in both
        // directions, simply return the source DataBlk.

        if (src.getCompSubsX(c)==1 && src.getCompSubsY(c)==1)
            return src.getInternCompData(outblk,c);

        int wfactor= src.getCompSubsX(c);
        int hfactor= src.getCompSubsY(c);
        if ((wfactor != 2 && wfactor != 1) || (hfactor != 2 && hfactor != 1))
            throw new IllegalArgumentException ("Upsampling by other than 2:1"+
						" not supported");
        
        int leftedgeOut= -1;     // offset to the start of the output scanline
        int rightedgeOut= -1;    // offset to the end of the output
				 // scanline + 1
        int leftedgeIn= -1;      // offset to the start of the input scanline  
        int rightedgeIn= -1;     // offset to the end of the input scanline + 1


        int y0In, y1In, y0Out, y1Out;
        int x0In, x1In, x0Out, x1Out;

        y0Out = outblk.uly;
        y1Out = y0Out + outblk.h - 1;

        x0Out = outblk.ulx;
        x1Out = x0Out + outblk.w - 1;

        y0In = y0Out/hfactor;
        y1In = y1Out/hfactor;

        x0In = x0Out/wfactor;
        x1In = x1Out/wfactor;

        
        // Calculate the requested height and width, requesting an extra
        // row and or for upsampled channels.
        int reqW = x1In - x0In + 1;
        int reqH = y1In - y0In + 1;

        // Initialize general input and output indexes
        int kOut= -1;
        int kIn=  -1;
        int yIn;

        switch (outblk.getDataType()) {

        case DataBlk.TYPE_INT:

            DataBlkInt inblkInt = new DataBlkInt(x0In, y0In, reqW,reqH);
            inblkInt = (DataBlkInt) src.getInternCompData(inblkInt, c);
            dataInt[c] =  inblkInt.getDataInt();

            // Reference the working array   
            int [] outdataInt = (int[])outblk.getData();
 
            // Create data array if necessary
            if (outdataInt == null || outdataInt.length!=outblk.w*outblk.h) {
                outdataInt = new int[outblk.h*outblk.w];
                outblk.setData(outdataInt); }

            // The nitty-gritty.

            for(int yOut=y0Out; yOut<=y1Out; ++yOut) {

                yIn = yOut/hfactor;
                

                leftedgeIn  = inblkInt.offset + (yIn-y0In)*inblkInt.scanw;
                rightedgeIn = leftedgeIn + inblkInt.w;
                leftedgeOut  = outblk.offset + (yOut-y0Out)*outblk.scanw;
                rightedgeOut = leftedgeOut + outblk.w;

                kIn=leftedgeIn;
                kOut=leftedgeOut;

                if ((x0Out&0x1) == 1) { // first is odd do the pixel once.
                    outdataInt[kOut++] = dataInt[c][kIn++]; }

                if ((x1Out&0x1) == 0) { // last is even adjust loop bounds
                    rightedgeOut--; }

                while(kOut<rightedgeOut) {
                    outdataInt[kOut++] = dataInt[c][kIn]; 
                    outdataInt[kOut++] = dataInt[c][kIn++]; }

                if ((x1Out&0x1) == 0) { // last is even do the pixel once.
                    outdataInt[kOut++] = dataInt[c][kIn]; }}

            outblk.progressive = inblkInt.progressive;
            break;

        case DataBlk.TYPE_FLOAT:

            DataBlkFloat inblkFloat = new DataBlkFloat(x0In, y0In, reqW,reqH);
            inblkFloat = (DataBlkFloat) src.getInternCompData(inblkFloat, c);
            dataFloat[c] =  inblkFloat.getDataFloat();

            // Reference the working array   
            float [] outdataFloat = (float[])outblk.getData();
 
            // Create data array if necessary
            if(outdataFloat==null || outdataFloat.length!=outblk.w*outblk.h) {
                outdataFloat = new float[outblk.h*outblk.w];
                outblk.setData(outdataFloat); }

            // The nitty-gritty.

            for(int yOut=y0Out; yOut<=y1Out; ++yOut) {

                yIn = yOut/hfactor;
                

                leftedgeIn  = inblkFloat.offset + (yIn-y0In)*inblkFloat.scanw;
                rightedgeIn = leftedgeIn + inblkFloat.w;
                leftedgeOut  = outblk.offset + (yOut-y0Out)*outblk.scanw;
                rightedgeOut = leftedgeOut + outblk.w;

                kIn=leftedgeIn;
                kOut=leftedgeOut;

                if ((x0Out&0x1) == 1) { // first is odd do the pixel once.
                    outdataFloat[kOut++] = dataFloat[c][kIn++]; }

                if ((x1Out&0x1) == 0) { // last is even adjust loop bounds
                    rightedgeOut--; }

                while(kOut<rightedgeOut) {
                    outdataFloat[kOut++] = dataFloat[c][kIn]; 
                    outdataFloat[kOut++] = dataFloat[c][kIn++]; }

                if ((x1Out&0x1) == 0) { // last is even do the pixel once.
                    outdataFloat[kOut++] = dataFloat[c][kIn]; }}

            outblk.progressive = inblkFloat.progressive;
            break;

        case DataBlk.TYPE_SHORT:
        case DataBlk.TYPE_BYTE:
        default:
            // Unsupported output type. 
            throw new IllegalArgumentException("invalid source datablock "+
					       "type");
        }

        return outblk; }


    /**
     * Return an appropriate String representation of this Resampler instance.
     */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[Resampler: ncomps= " + ncomps);
        StringBuffer body = new StringBuffer ("  ");
        for (int i=0;i<ncomps;++i) {
            body.append(eol);
            body.append("comp[");
            body.append(i);
            body.append("] xscale= ");
            body.append(imgdatasrc.getCompSubsX(i));
            body.append(", yscale= ");
            body.append(imgdatasrc.getCompSubsY(i)); }

        rep.append(ColorSpace.indent("  ",body));
        return rep.append("]").toString(); }



    /**
     * Returns, in the blk argument, a block of image data containing the
     * specifed rectangular area, in the specified component. The data is
     * returned, as a copy of the internal data, therefore the returned data
     * can be modified "in place".
     *
     * <P>The rectangular area to return is specified by the 'ulx', 'uly', 'w'
     * and 'h' members of the 'blk' argument, relative to the current
     * tile. These members are not modified by this method. The 'offset' of
     * the returned data is 0, and the 'scanw' is the same as the block's
     * width. See the 'DataBlk' class.
     *
     * <P>If the data array in 'blk' is 'null', then a new one is created. If
     * the data array is not 'null' then it is reused, and it must be large
     * enough to contain the block's data. Otherwise an 'ArrayStoreException'
     * or an 'IndexOutOfBoundsException' is thrown by the Java system.
     *
     * <P>The returned data has its 'progressive' attribute set to that of the
     * input data.
     *
     * @param blk Its coordinates and dimensions specify the area to
     * return. If it contains a non-null data array, then it must have the
     * correct dimensions. If it contains a null data array a new one is
     * created. The fields in this object are modified to return the data.
     *
     * @param c The index of the component from which to get the data. Only 0
     * and 3 are valid.
     *
     * @return The requested DataBlk
     *
     * @see #getInternCompData
     **/
    public DataBlk getCompData (DataBlk outblk, int c) {
        return getInternCompData(outblk, c); }


    /** 
     * Returns the height in pixels of the specified component in the
     * overall image.
     */
    public int getCompImgHeight(int c) { 
	return src.getCompImgHeight(c) * src.getCompSubsY(c); 
    }

    /**
     * Returns the width in pixels of the specified component in the
     * overall image.
     */
    public int getCompImgWidth(int c) {
	return src.getCompImgWidth(c) * src.getCompSubsX(c); 
    }

    /**
     * Returns the component subsampling factor in the horizontal
     * direction, for the specified component.
     */
    public int getCompSubsX(int c) { return 1; }

    /**
     * Returns the component subsampling factor in the vertical
     * direction, for the specified component.
     */
    public int getCompSubsY(int c)  { return 1; }

    /**
     * Returns the height in pixels of the specified tile-component.
     */
    public int getTileCompHeight(int t, int c)  { 
        return src.getTileCompHeight(t, c) * src.getCompSubsY(c); }

    /**
     * Returns the width in pixels of the specified tile-component..
     */
    public int getTileCompWidth(int t, int c)  { 
        return src.getTileCompWidth(t, c) * src.getCompSubsX(c); }

    /* end class Resampler */ }









