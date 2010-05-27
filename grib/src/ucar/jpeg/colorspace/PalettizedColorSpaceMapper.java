/*****************************************************************************
 *
 * $Id: PalettizedColorSpaceMapper.java,v 1.2 2002/08/08 14:07:16 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/
package ucar.jpeg.colorspace;

import ucar.jpeg.jj2000.j2k.image.*;
import ucar.jpeg.jj2000.j2k.util.*;

import ucar.jpeg.colorspace.boxes.*;

/**
 * This class provides decoding of images with palettized colorspaces.
 * Here each sample in the input is treated as an index into a color
 * palette of triplet sRGB output values.
 * 
 * @see		jj2000.j2k.colorspace.ColorSpace
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class PalettizedColorSpaceMapper extends ColorSpaceMapper {
    int [] outShiftValueArray;
    int srcChannel = 0;

    /** Access to the palette box information. */ 
    private PaletteBox /*final*/ pbox; 

    /**
     * Factory method for creating instances of this class.
     *   @param src -- source of image data
     *   @param csMap -- provides colorspace info
     * @return PalettizedColorSpaceMapper instance
     */
    public static BlkImgDataSrc createInstance(BlkImgDataSrc src, 
					       ColorSpace csMap) 
        throws ColorSpaceException {
        return new PalettizedColorSpaceMapper (src, csMap); 
    }

    /**
     * Ctor which creates an ICCProfile for the image and initializes
     * all data objects (input, working, and output).
     *
     *   @param src -- Source of image data
     *   @param csm -- provides colorspace info
     */
    protected PalettizedColorSpaceMapper(BlkImgDataSrc src, ColorSpace csMap)  
        throws ColorSpaceException {
        super (src, csMap);
        pbox = csMap.getPaletteBox();
        initialize();
    }

    /** General utility used by ctors */
    private void initialize() throws ColorSpaceException {
        if(ncomps!=1 && ncomps!= 3) 
            throw new ColorSpaceException 
                ("wrong number of components ("+ncomps+
		 ") for palettized image"); 

        int outComps = getNumComps();
        outShiftValueArray = new int[outComps];

        for (int i=0; i<outComps; i++) {
            outShiftValueArray[i] = 1<<(getNomRangeBits(i)-1); }}


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
    public DataBlk getCompData (DataBlk out, int c) {

        if (pbox==null) return src.getCompData(out,c);

        if (ncomps != 1) {
            String msg = "PalettizedColorSpaceMapper: color palette "+
		"_not_ applied, incorrect number ("
                +String.valueOf(ncomps) + ") of components";
            FacilityManager.getMsgLogger().printmsg(MsgLogger.WARNING,msg);
            return src.getCompData(out, c); }
        
        // Initialize general input and output indexes
        int leftedgeOut= -1;     // offset to the start of the output scanline
        int rightedgeOut= -1;    // offset to the end of the output
				 // scanline + 1
        int leftedgeIn= -1;      // offset to the start of the input scanline  
        int rightedgeIn= -1;     // offset to the end of the input
				 // scanline + 1
        int kOut= -1; 
        int kIn=  -1;

        // Assure a properly sized data buffer for output.
        setInternalBuffer (out);

        switch (out.getDataType()) { // Int and Float data only

            case DataBlk.TYPE_INT:

                copyGeometry (inInt[0], out);
                
                // Request data from the source.        
                inInt [0] = (DataBlkInt) src.getInternCompData(inInt[0],0);
                dataInt[0] = (int[]) inInt[0].getData();
                int [] outdataInt = ((DataBlkInt) out).getDataInt();

                // The nitty-gritty.

                for(int row=0; row<out.h; ++row) {
                    leftedgeIn  = inInt[0].offset + row*inInt[0].scanw;
                    rightedgeIn = leftedgeIn + inInt[0].w;
                    leftedgeOut  = out.offset + row*out.scanw;
                    rightedgeOut = leftedgeOut + out.w;
                    
                    for(kOut=leftedgeOut, kIn=leftedgeIn; kIn<rightedgeIn; 
			++kIn, ++kOut) {                             
                        outdataInt[kOut] = 
                            pbox.getEntry(c,dataInt[0][kIn]+shiftValueArray[0])
                            -outShiftValueArray[c]; }}
                out.progressive = inInt[0].progressive;
                break;
                    
            case DataBlk.TYPE_FLOAT:

                copyGeometry (inFloat[0], out);
                
                // Request data from the source.        
                inFloat[0] = (DataBlkFloat)src.getInternCompData(inFloat[0],0);
                dataFloat[0] = (float[]) inFloat[0].getData();
                float [] outdataFloat = ((DataBlkFloat) out).getDataFloat();

                // The nitty-gritty.

                for(int row=0; row<out.h; ++row) {
                    leftedgeIn  = inFloat[0].offset + row*inFloat[0].scanw;
                    rightedgeIn = leftedgeIn + inFloat[0].w;
                    leftedgeOut  = out.offset + row*out.scanw;
                    rightedgeOut = leftedgeOut + out.w;

                    for(kOut=leftedgeOut, kIn=leftedgeIn; kIn<rightedgeIn; 
			++kIn, ++kOut) {                     
                        outdataFloat[kOut] = 
                            pbox.getEntry(c,(int)dataFloat[0][kIn]+
					  shiftValueArray[0])
			    -outShiftValueArray[c]; }}
                out.progressive = inFloat[0].progressive;
                break;
                    
            case DataBlk.TYPE_SHORT:
            case DataBlk.TYPE_BYTE:
            default:
                // Unsupported output type. 
                throw new IllegalArgumentException("invalid source datablock"+
						   " type"); }

        // Initialize the output block geometry and set the profiled
        // data into the output block.
        out.offset = 0;
        out.scanw = out.w;
        return out; } 


    /** Return a suitable String representation of the class instance, e.g.
     *  <p>
     *  [PalettizedColorSpaceMapper 
     *    ncomps= 3, scomp= 1, nentries= 1024
     *    column=0, 7 bit signed entry
     *    column=1, 7 bit unsigned entry
     *    column=2, 7 bit signed entry]
     *  <p>
     **/
    public String toString () {

        int c;
        StringBuffer rep= new StringBuffer("[PalettizedColorSpaceMapper ");
        StringBuffer body = new StringBuffer("  "+eol);
        
        if (pbox!=null) {
            body .append ("ncomps= ") .append (getNumComps()) 
                .append (", scomp= ") .append (srcChannel);
            for (c=0; c<getNumComps(); ++c) {
                body .append(eol)
                    .append("column= ") .append(c) 
                    .append(", ") .append(pbox.getBitDepth(c)) .append(" bit ")
                    .append(pbox.isSigned(c)?"signed entry":"unsigned entry"); 
	    }
	} else { 
            body.append ("image does not contain a palette box"); }
        
        rep.append(ColorSpace.indent("  ",body));
        return rep.append("]").toString(); }


    /**
     * Returns, in the blk argument, a block of image data containing the
     * specifed rectangular area, in the specified component. The data is
     * returned, as a reference to the internal data, if any, instead of as a
     * copy, therefore the returned data should not be modified.
     *
     * <P>The rectangular area to return is specified by the 'ulx', 'uly', 'w'
     * and 'h' members of the 'blk' argument, relative to the current
     * tile. These members are not modified by this method. The 'offset' and
     * 'scanw' of the returned data can be arbitrary. See the 'DataBlk' class.
     *
     * <P>This method, in general, is more efficient than the 'getCompData()'
     * method since it may not copy the data. However if the array of returned
     * data is to be modified by the caller then the other method is probably
     * preferable.
     *
     * <P>If possible, the data in the returned 'DataBlk' should be the
     * internal data itself, instead of a copy, in order to increase the data
     * transfer efficiency. However, this depends on the particular
     * implementation (it may be more convenient to just return a copy of the
     * data). This is the reason why the returned data should not be modified.
     *
     * <P>If the data array in <tt>blk</tt> is <tt>null</tt>, then a new one
     * is created if necessary. The implementation of this interface may
     * choose to return the same array or a new one, depending on what is more
     * efficient. Therefore, the data array in <tt>blk</tt> prior to the
     * method call should not be considered to contain the returned data, a
     * new array may have been created. Instead, get the array from
     * <tt>blk</tt> after the method has returned.
     *
     * <P>The returned data may have its 'progressive' attribute set. In this
     * case the returned data is only an approximation of the "final" data.
     *
     * @param blk Its coordinates and dimensions specify the area to return,
     * relative to the current tile. Some fields in this object are modified
     * to return the data.
     *
     * @param c The index of the component from which to get the data.
     *
     * @return The requested DataBlk
     * 
     * @see #getCompData
     */
    public DataBlk getInternCompData (DataBlk out, int c) {
	return getCompData(out, c);
    }

    /**
     * Returns the number of bits, referred to as the "range bits",
     * corresponding to the nominal range of the image data in the specified
     * component. If this number is <i>n</b> then for unsigned data the
     * nominal range is between 0 and 2^b-1, and for signed data it is between
     * -2^(b-1) and 2^(b-1)-1. In the case of transformed data which is not in
     * the image domain (e.g., wavelet coefficients), this method returns the
     * "range bits" of the image data that generated the coefficients.
     *
     * @param c The index of the component.
     *
     * @return The number of bits corresponding to the nominal range of the
     * image data (in the image domain).
     */
    public int getNomRangeBits(int c) {
        return pbox==null?
            src.getNomRangeBits(c):
            pbox.getBitDepth(c); }

    /**
     * Returns the number of components in the image.
     *
     * @return The number of components in the image.
     */
    public int getNumComps() {
        return pbox==null? src.getNumComps(): pbox.getNumColumns(); }


    /**
     * Returns the component subsampling factor in the horizontal direction,
     * for the specified component. This is, approximately, the ratio of
     * dimensions between the reference grid and the component itself, see the
     * 'ImgData' interface desription for details.
     *
     * @param c The index of the component (between 0 and N-1)
     *
     * @return The horizontal subsampling factor of component 'c'
     *
     * @see jj2000.j2k.image.ImgData
     * */
    public int getCompSubsX(int c) {
        return imgdatasrc.getCompSubsX(srcChannel); }

    /**
     * Returns the component subsampling factor in the vertical direction, for
     * the specified component. This is, approximately, the ratio of
     * dimensions between the reference grid and the component itself, see the
     * 'ImgData' interface desription for details.
     *
     * @param c The index of the component (between 0 and N-1)
     *
     * @return The vertical subsampling factor of component 'c'
     *
     * @see jj2000.j2k.image.ImgData
     * */
    public int getCompSubsY(int c) {
        return imgdatasrc.getCompSubsY(srcChannel); }

    /**
     * Returns the width in pixels of the specified tile-component
     *
     * @param t Tile index
     *
     * @param c The index of the component, from 0 to N-1.
     *
     * @return The width in pixels of component <tt>c</tt> in tile<tt>t</tt>.
     * */
    public int getTileCompWidth(int t,int c) {
        return imgdatasrc.getTileCompWidth(t, srcChannel); }

    /**
     * Returns the height in pixels of the specified tile-component.
     *
     * @param t The tile index.
     *
     * @param c The index of the component, from 0 to N-1.
     *
     * @return The height in pixels of component <tt>c</tt> in tile
     * <tt>t</tt>.
     * */
    public int getTileCompHeight(int t,int c) {
        return imgdatasrc.getTileCompHeight(t, srcChannel); }

    /**
     * Returns the width in pixels of the specified component in the overall
     * image.
     *
     * @param c The index of the component, from 0 to N-1.
     *
     * @return The width in pixels of component <tt>c</tt> in the overall
     * image.
     * */
    public int getCompImgWidth(int c) {
        return imgdatasrc.getCompImgWidth(srcChannel); }


    /**
     * Returns the number of bits, referred to as the "range bits",
     * corresponding to the nominal range of the image data in the specified
     * component. If this number is <i>n</b> then for unsigned data the
     * nominal range is between 0 and 2^b-1, and for signed data it is between
     * -2^(b-1) and 2^(b-1)-1. In the case of transformed data which is not in
     * the image domain (e.g., wavelet coefficients), this method returns the
     * "range bits" of the image data that generated the coefficients.
     *
     * @param c The index of the component.
     *
     * @return The number of bits corresponding to the nominal range of the
     * image data (in the image domain).
     * */
    public int getCompImgHeight(int c) {
        return imgdatasrc.getCompImgHeight(srcChannel); }

    /**
     * Returns the horizontal coordinate of the upper-left corner of the
     * specified component in the current tile.
     *
     * @param c The index of the component.
     * */
    public int getCompULX(int c) {
        return imgdatasrc.getCompULX(srcChannel); }

    /**
     * Returns the vertical coordinate of the upper-left corner of the
     * specified component in the current tile.
     *
     * @param c The index of the component.
     * */
    public int getCompULY(int c) {
        return imgdatasrc.getCompULY(srcChannel); }

    /* end class PalettizedColorSpaceMapper */ }









