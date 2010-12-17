/*****************************************************************************
 *
 * $Id: ChannelDefinitionMapper.java,v 1.2 2002/08/08 14:06:53 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/
package ucar.jpeg.colorspace;

import ucar.jpeg.jj2000.j2k.image.*;
import ucar.jpeg.jj2000.j2k.util.*;

/**
 * This class is responsible for the mapping between
 * requested components and image channels.
 * 
 * @see		jj2000.j2k.colorspace.ColorSpace
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class ChannelDefinitionMapper extends ColorSpaceMapper {
    /**
     * Factory method for creating instances of this class.
     *   @param src -- source of image data
     *   @param csMap -- provides colorspace info
     * @return ChannelDefinitionMapper instance
     * @exception ColorSpaceException
     */
    public static BlkImgDataSrc createInstance(BlkImgDataSrc src, 
					       ColorSpace csMap) 
        throws ColorSpaceException {
        
        return new ChannelDefinitionMapper (src, csMap); }

    /**
     * Ctor which creates an ICCProfile for the image and initializes
     * all data objects (input, working, and output).
     *
     *   @param src -- Source of image data
     *   @param csm -- provides colorspace info
     */
    protected ChannelDefinitionMapper (BlkImgDataSrc src, ColorSpace csMap) 
        throws ColorSpaceException {
        super (src, csMap);
        /* end ChannelDefinitionMapper ctor */ }


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
        return src.getCompData (out, csMap.getChannelDefinition(c)); }

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
     * <P>This method, in general, is less efficient than the
     * 'getInternCompData()' method since, in general, it copies the
     * data. However if the array of returned data is to be modified by the
     * caller then this method is preferable.
     *
     * <P>If the data array in 'blk' is 'null', then a new one is created. If
     * the data array is not 'null' then it is reused, and it must be large
     * enough to contain the block's data. Otherwise an 'ArrayStoreException'
     * or an 'IndexOutOfBoundsException' is thrown by the Java system.
     *
     * <P>The returned data may have its 'progressive' attribute set. In this
     * case the returned data is only an approximation of the "final" data.
     *
     * @param blk Its coordinates and dimensions specify the area to return,
     * relative to the current tile. If it contains a non-null data array,
     * then it must be large enough. If it contains a null data array a new
     * one is created. Some fields in this object are modified to return the
     * data.
     *
     * @param c The index of the component from which to get the data.
     *
     * @see #getCompData
     * */
    public DataBlk getInternCompData (DataBlk out, int c) {
        return src.getInternCompData(out, csMap.getChannelDefinition(c)); }

    /**
     * Returns the number of bits, referred to as the "range bits",
     * corresponding to the nominal range of the data in the specified
     * component. If this number is <i>b</b> then for unsigned data the
     * nominal range is between 0 and 2^b-1, and for signed data it is between
     * -2^(b-1) and 2^(b-1)-1. For floating point data this value is not
     * applicable.
     *
     * @param c The index of the component.
     *
     * @return The number of bits corresponding to the nominal range of the
     * data. Fro floating-point data this value is not applicable and the
     * return value is undefined.
     */
    public int getFixedPoint(int c) {
        return src.getFixedPoint(csMap.getChannelDefinition(c));}

    public int getNomRangeBits(int c) {
        return src.getNomRangeBits(csMap.getChannelDefinition(c)); }

    public int getCompImgHeight(int c) {
        return src.getCompImgHeight(csMap.getChannelDefinition(c)); }

    public int getCompImgWidth(int c)  {
        return src.getCompImgWidth(csMap.getChannelDefinition(c)); }

    public int getCompSubsX(int c)  {
        return src.getCompSubsX(csMap.getChannelDefinition(c)); }

    public int getCompSubsY(int c)  {
        return src.getCompSubsY(csMap.getChannelDefinition(c)); }

    public int getCompULX(int c)  {
        return src.getCompULX(csMap.getChannelDefinition(c)); }

    public int getCompULY(int c)  {
        return src.getCompULY(csMap.getChannelDefinition(c)); }

    public int getTileCompHeight(int t, int c)  {
        return src.getTileCompHeight(t, csMap.getChannelDefinition(c)); }

    public int getTileCompWidth(int t, int c)  {
        return src.getTileCompWidth(t, csMap.getChannelDefinition(c)); }

    public String toString () {
        int i;
        StringBuffer rep = 
            new StringBuffer ("[ChannelDefinitionMapper nchannels= ")
            .append(ncomps);

        for (i=0;i<ncomps;++i) {
            rep.append(eol).append("  component[").append(i)
                .append("] mapped to channel[")
                .append(csMap.getChannelDefinition(i)).append("]"); }

    return rep.append("]").toString();}

    /* end class ChannelDefinitionMapper */ }









