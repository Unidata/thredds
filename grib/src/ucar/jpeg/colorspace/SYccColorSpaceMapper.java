/*****************************************************************************
 *
 * $Id: SYccColorSpaceMapper.java,v 1.1 2002/07/25 14:52:01 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.colorspace;

import ucar.jpeg.jj2000.j2k.util.ParameterList;
import ucar.jpeg.jj2000.j2k.image.BlkImgDataSrc;
import ucar.jpeg.jj2000.j2k.image.DataBlk;
import ucar.jpeg.jj2000.j2k.image.DataBlkInt;
import ucar.jpeg.jj2000.j2k.image.DataBlkFloat;
import ucar.jpeg.jj2000.j2k.image.ImgDataAdapter;
import ucar.jpeg.jj2000.j2k.util.FacilityManager;
import ucar.jpeg.jj2000.j2k.util.MsgLogger;


/**
 * This decodes maps which are defined in the sYCC 
 * colorspace into the sRGB colorspadce.
 * 
 * @see		jj2000.j2k.colorspace.ColorSpace
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class SYccColorSpaceMapper extends ColorSpaceMapper
{
    /* sYCC colorspace matrix */
        
    /** Matrix component for ycc transform. */ static protected float Matrix00 = 1;
    /** Matrix component for ycc transform. */ static protected float Matrix01 = 0;
    /** Matrix component for ycc transform. */ static protected float Matrix02 = (float) 1.402;
    /** Matrix component for ycc transform. */ static protected float Matrix10 = 1;
    /** Matrix component for ycc transform. */ static protected float Matrix11 = (float) -0.34413;
    /** Matrix component for ycc transform. */ static protected float Matrix12 = (float) -0.71414;
    /** Matrix component for ycc transform. */ static protected float Matrix20 = 1;
    /** Matrix component for ycc transform. */ static protected float Matrix21 = (float) 1.772;
    /** Matrix component for ycc transform. */ static protected float Matrix22 = 0; 

       
    /**
     * Factory method for creating instances of this class.
     *   @param src -- source of image data
     *   @param csMap -- provides colorspace info
     * @return SYccColorSpaceMapper instance
     */
    public static BlkImgDataSrc createInstance (BlkImgDataSrc src, ColorSpace csMap) 
        throws ColorSpaceException {
        return new SYccColorSpaceMapper (src, csMap); }

    /**
     * Ctor which creates an ICCProfile for the image and initializes
     * all data objects (input, working, and output).
     *
     *   @param src -- Source of image data
     *   @param csm -- provides colorspace info
     */
    protected SYccColorSpaceMapper (BlkImgDataSrc src, ColorSpace csMap)  
        throws ColorSpaceException {
        super (src, csMap);
        initialize();
        /* end SYccColorSpaceMapper ctor */ }

    /** General utility used by ctors */
    private void initialize() throws ColorSpaceException {

        if (ncomps != 1 && ncomps != 3) {
            String msg = "SYccColorSpaceMapper: ycc transformation _not_ applied to " +
                ncomps + " component image";
            FacilityManager.getMsgLogger().printmsg(MsgLogger.ERROR,msg);
            throw new ColorSpaceException(msg); }}


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
     * @see #getInternCompData
     **/
    public DataBlk getCompData (DataBlk outblk, int c) {

        int type = outblk.getDataType();

        int i,j;

        // Calculate all components:
        for (i=0; i<ncomps; ++i) {

            // Set up the working DataBlk geometry.
            copyGeometry (workInt[i],   outblk);
            copyGeometry (workFloat[i], outblk);  
            copyGeometry (inInt[i],     outblk);  
            copyGeometry (inFloat[i],   outblk);
                
            // Request data from the source.
            inInt[i] = (DataBlkInt) src.getInternCompData(inInt[i], i);
        }

        if (type==DataBlk.TYPE_INT) {
            if (ncomps == 1) workInt[c] = inInt[c];
            else             workInt    = mult (inInt);
            outblk.progressive = inInt[c].progressive;
            outblk.setData(workInt[c].getData());
        }

        if (type==DataBlk.TYPE_FLOAT) {
            if (ncomps == 1) workFloat[c] = inFloat[c];
            else             workFloat    = mult (inFloat);
            outblk.progressive = inFloat[c].progressive;
            outblk.setData(workFloat[c].getData());
        }
                                                                       

        // Initialize the output block geometry and set the profiled
        // data into the output block.
        outblk.offset = 0;
        outblk.scanw = outblk.w;

        return outblk; }

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
        return getCompData(out, c);}


    /**
     * Output a DataBlkFloat array where each sample in each component
     * is the product of the YCC matrix * the vector of samples across 
     * the input components.
     *   @param inblk input DataBlkFloat array
     * @return output DataBlkFloat array
     */
    private static DataBlkFloat[] mult (DataBlkFloat[] inblk) {

        if (inblk.length!=3) throw new IllegalArgumentException ("bad input array size");

        int i,j;
        int length = inblk[0].h*inblk[0].w;
        DataBlkFloat [] outblk = new DataBlkFloat [3];
        float [][] out = new float [3][];
        float [][] in  = new float [3][];

        for (i=0;i<3;++i) {
            in[i]  = inblk [i] .getDataFloat();
            outblk[i] = new DataBlkFloat ();
            copyGeometry (outblk[i],inblk[i]);
            outblk[i].offset = inblk[i].offset;
            out[i] = new float [length];
            outblk[i].setData (out[i]); }

        for (j=0; j<length; ++j) {
                out[0][j]= 
                    ( Matrix00*in[0][inblk[0].offset+j] + 
                      Matrix01*in[1][inblk[1].offset+j] + 
                      Matrix02*in[2][inblk[2].offset+j] );
                               
                out[1][j]= 
                    ( Matrix10*in[0][inblk[0].offset+j] + 
                      Matrix11*in[1][inblk[1].offset+j] + 
                      Matrix12*in[2][inblk[2].offset+j] );
                                             
                out[2][j]=             
                    ( Matrix20*in[0][inblk[0].offset+j] + 
                      Matrix21*in[1][inblk[1].offset+j] + 
                      Matrix22*in[2][inblk[2].offset+j] ); }

        return outblk; }    



    /**
     * Output a DataBlkInt array where each sample in each component
     * is the product of the YCC matrix * the vector of samples across 
     * the input components.
     *   @param inblk input DataBlkInt array
     * @return output DataBlkInt array
     */
    private static DataBlkInt[] mult (DataBlkInt[] inblk) {

        if (inblk.length!=3) throw new IllegalArgumentException ("bad input array size");


        int i,j;
        int length = inblk[0].h*inblk[0].w;
        DataBlkInt [] outblk = new DataBlkInt [3];
        int [][] out = new int [3][];
        int [][] in  = new int [3][];

        for (i=0;i<3;++i) {
            in[i]  = inblk [i] .getDataInt();
            outblk[i] = new DataBlkInt ();
            copyGeometry (outblk[i],inblk[i]);
            outblk[i].offset = inblk[i].offset;
            out[i] = new int [length];
            outblk[i].setData (out[i]); }

        for (j=0; j<length; ++j) {
                out[0][j]= (int) 
                    ( Matrix00*in[0][inblk[0].offset+j] + 
                      Matrix01*in[1][inblk[1].offset+j] + 
                      Matrix02*in[2][inblk[2].offset+j] );
                               
                out[1][j]= (int)
                    ( Matrix10*in[0][inblk[0].offset+j] + 
                      Matrix11*in[1][inblk[1].offset+j] + 
                      Matrix12*in[2][inblk[2].offset+j] );
                                             
                out[2][j]= (int)              
                    ( Matrix20*in[0][inblk[0].offset+j] + 
                      Matrix21*in[1][inblk[1].offset+j] + 
                      Matrix22*in[2][inblk[2].offset+j] ); }

        return outblk; }


    /** Return a suitable String representation of the class instance. */
    public String toString () {
        int i;

        StringBuffer rep_nComps     = new StringBuffer("ncomps= ").append(String.valueOf(ncomps));
        StringBuffer rep_comps      = new StringBuffer();

        for (i=0; i<ncomps; ++i) {
            rep_comps
                .append("  ")
                .append("component[")
                .append(String.valueOf(i))
                .append("] height, width = (")
                .append(src.getCompImgHeight(i))
                .append(", ")
                .append(src.getCompImgWidth(i))
                .append(")")
                .append(eol); }

        StringBuffer rep = new StringBuffer ("[SYccColorSpaceMapper ");
        rep.append(rep_nComps)    .append(eol);
        rep.append(rep_comps)     .append("  ");

        return rep.append("]").toString(); }
 

    /* end class SYccColorSpaceMapper */ }










