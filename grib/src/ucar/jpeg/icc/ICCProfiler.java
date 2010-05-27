/*****************************************************************************
 *
 * $Id: ICCProfiler.java,v 1.2 2002/08/08 14:08:27 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/
package ucar.jpeg.icc;

import java.io.*;

import ucar.jpeg.jj2000.j2k.decoder.*;
import ucar.jpeg.jj2000.j2k.image.*;
import ucar.jpeg.jj2000.j2k.util.*;
import ucar.jpeg.jj2000.j2k.io.*;

import ucar.jpeg.colorspace.*;
import ucar.jpeg.icc.lut.*;

/**
 * This class provides ICC Profiling API for the jj2000.j2k imaging chain
 * by implementing the BlkImgDataSrc interface, in particular the getCompData
 * and getInternCompData methods.
 * 
 * @see		jj2000.j2k.icc.ICCProfile
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class ICCProfiler extends ColorSpaceMapper {

    /** The prefix for ICC Profiler options */
    public final static char OPT_PREFIX = 'I';

    /** Platform dependant end of line String. */
    protected final static String eol = System.getProperty("line.separator");

    // Renamed for convenience:
    private static final int GRAY  = RestrictedICCProfile.GRAY;
    private static final int RED   = RestrictedICCProfile.RED;
    private static final int GREEN = RestrictedICCProfile.GREEN;
    private static final int BLUE  = RestrictedICCProfile.BLUE;

    // ICCProfiles.
    RestrictedICCProfile ricc = null;
    ICCProfile icc = null;

    // Temporary variables needed during profiling.
    private DataBlkInt[]    tempInt; // Holds the results of the transform.
    private DataBlkFloat [] tempFloat; // Holds the results of the transform.

    private Object  xform = null;

    /** The image's ICC profile. */
    private RestrictedICCProfile iccp = null;

    /**
     * Factory method for creating instances of this class.
     *   @param src -- source of image data
     *   @param csMap -- provides colorspace info
     * @return ICCProfiler instance
     * @exception IOException profile access exception
     * @exception ICCProfileException profile content exception
     */
    public static BlkImgDataSrc createInstance(BlkImgDataSrc src, 
					       ColorSpace csMap) 
        throws IOException, ICCProfileException, ColorSpaceException {
        return new ICCProfiler (src, csMap); 
    }

    /**
     * Ctor which creates an ICCProfile for the image and initializes
     * all data objects (input, working, output).
     *
     *   @param src -- Source of image data
     *   @param csm -- provides colorspace info
     *
     * @exception IOException
     * @exception ICCProfileException
     * @exception IllegalArgumentException
     */
    protected ICCProfiler (BlkImgDataSrc src, ColorSpace csMap)  
        throws ColorSpaceException, IOException, ICCProfileException, 
	       IllegalArgumentException {
        super (src, csMap);
        initialize ();

        iccp = getICCProfile(csMap);
        if(ncomps==1) {
	    xform = new MonochromeTransformTosRGB(iccp,maxValueArray[0], 
						  shiftValueArray[0]);
	} else {
            xform = new MatrixBasedTransformTosRGB(iccp,maxValueArray,
						   shiftValueArray);
	}

        /* end ICCProfiler ctor */ }
    
    /** General utility used by ctors */
    private void initialize() { 

        tempInt     = new DataBlkInt [ncomps];
        tempFloat   = new DataBlkFloat [ncomps];

        /* For each component, get the maximum data value, a reference
         * to the pixel data and set up working and temporary DataBlks
         * for both integer and float output.
         */
        for (int i=0;i<ncomps;++i) {
            tempInt [i] = new DataBlkInt();
            tempFloat [i] = new DataBlkFloat(); }}

   /**
    * Get the ICCProfile information JP2 ColorSpace
    *   @param csm provides all necessary info about the colorspace
    * @return ICCMatrixBasedInputProfile for 3 component input and
    * ICCMonochromeInputProfile for a 1 component source.  Returns
    * null if exceptions were encountered.
    * @exception ColorSpaceException
    * @exception ICCProfileException
    * @exception IllegalArgumentException
    */
    private RestrictedICCProfile getICCProfile (ColorSpace csm) 
        throws ColorSpaceException, ICCProfileException, 
	       IllegalArgumentException {

        switch (ncomps) {
        case 1:
            icc=ICCMonochromeInputProfile.createInstance  (csm);
            ricc = icc.parse();
            if (ricc.getType() != RestrictedICCProfile.kMonochromeInput) 
                throw new IllegalArgumentException("wrong ICCProfile type"+
						   " for image");
            break;
        case 3:
            icc=ICCMatrixBasedInputProfile.createInstance  (csm);
            ricc =  icc.parse();
            if (ricc.getType() != RestrictedICCProfile.kThreeCompInput) 
                throw new IllegalArgumentException("wrong ICCProfile type"+
						   " for image");
            break;
        default:
            throw new IllegalArgumentException ("illegal number of "+
						"components ("+ncomps+
						") in image");
        }
        return ricc; 
    }

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
     * @param out Its coordinates and dimensions specify the area to
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

        try {
            if (ncomps != 1 && ncomps != 3) {
                String msg = "ICCProfiler: icc profile _not_ applied to " +
                    ncomps + " component image";
                FacilityManager.getMsgLogger().printmsg(MsgLogger.WARNING,msg);
                return src.getCompData(outblk, c); }

            int type = outblk.getDataType();
            
            int leftedgeOut= -1;  // offset to the start of the output scanline
            int rightedgeOut= -1; // offset to the end of the output
				  // scanline + 1
            int leftedgeIn= -1; // offset to the start of the input scanline  
            int rightedgeIn= -1; // offset to the end of the input
				 // scanline + 1

            // Calculate all components:
            for (int i=0; i<ncomps; ++i) {

                int fixedPtBits  = src.getFixedPoint(i);
                int shiftVal     = shiftValueArray[i];
                int maxVal       = maxValueArray[i];

                // Initialize general input and output indexes
                int kOut= -1; 
                int kIn=  -1;

                switch (type) { // Int and Float data only

                case DataBlk.TYPE_INT:

                    // Set up the DataBlk geometry
                    copyGeometry (workInt[i],   outblk);
                    copyGeometry (tempInt[i],   outblk);
                    copyGeometry (inInt[i],     outblk);
                    setInternalBuffer (outblk);

                    // Reference the output array
                    workDataInt[i] = (int[])workInt[i].getData();
                
                    // Request data from the source.    
                    inInt[i] = (DataBlkInt) src.getInternCompData(inInt[i], i);
                    dataInt[i] = inInt[i].getDataInt();

                    // The nitty-gritty.

                    for(int row=0; row<outblk.h; ++row) {
                        leftedgeIn  = inInt[i].offset + row*inInt[i].scanw;
                        rightedgeIn = leftedgeIn + inInt[i].w;
                        leftedgeOut  = outblk.offset + row*outblk.scanw;
                        rightedgeOut = leftedgeOut + outblk.w;

                        for(kOut=leftedgeOut,kIn=leftedgeIn; kIn<rightedgeIn; 
			    ++kIn, ++kOut) {
                            int tmpInt = 
				(dataInt[i][kIn] >>fixedPtBits)+shiftVal;
                            workDataInt[i][kOut] = 
				((tmpInt<0) ? 0 : 
				 ((tmpInt>maxVal)?maxVal:tmpInt)); 
			}
		    }
                    break;
                    
                case DataBlk.TYPE_FLOAT:

                    // Set up the DataBlk geometry
                    copyGeometry (workFloat[i], outblk);  
                    copyGeometry (tempFloat[i], outblk); 
                    copyGeometry (inFloat[i],   outblk);
                    setInternalBuffer (outblk);
 
                    // Reference the output array
                    workDataFloat[i] = (float[])workFloat[i].getData();

                    // Request data from the source.    
                    inFloat[i] = 
			(DataBlkFloat) src.getInternCompData(inFloat[i], i);
                    dataFloat[i] = inFloat[i].getDataFloat();

                    // The nitty-gritty.

                    for(int row=0; row<outblk.h; ++row) {
                        leftedgeIn  = inFloat[i].offset + row*inFloat[i].scanw;
                        rightedgeIn = leftedgeIn + inFloat[i].w;
                        leftedgeOut  = outblk.offset + row*outblk.scanw;
                        rightedgeOut = leftedgeOut + outblk.w;

                        for(kOut=leftedgeOut, kIn=leftedgeIn; kIn<rightedgeIn; 
			    ++kIn, ++kOut) {
                            float tmpFloat = dataFloat[i][kIn] / 
				(1<<fixedPtBits) + shiftVal;
                            workDataFloat[i][kOut] = 
				((tmpFloat<0) ? 0 :
				 ((tmpFloat>maxVal)?maxVal:tmpFloat)); 
			}
		    }
                    break;
                    
                case DataBlk.TYPE_SHORT:
                case DataBlk.TYPE_BYTE:
                default:
                    // Unsupported output type. 
                    throw new IllegalArgumentException ("Invalid source "+
							"datablock type"); 
		}
	    }
            
            switch (type) { // Int and Float data only

            case DataBlk.TYPE_INT:
                    
                if(ncomps == 1) {
                    ((MonochromeTransformTosRGB) xform).apply(workInt[c],
							      tempInt[c]); 
		} else { // ncomps == 3
                    ((MatrixBasedTransformTosRGB) xform).apply(workInt, 
							       tempInt); 
		}

                outblk.progressive = inInt[c].progressive;
                outblk.setData(tempInt[c].getData());
                break;

            case DataBlk.TYPE_FLOAT:
                    
                if(ncomps==1) {
                    ((MonochromeTransformTosRGB) xform).apply(workFloat[c],
							      tempFloat[c]); 
		} else { // ncomps == 3
                    ((MatrixBasedTransformTosRGB) xform).apply(workFloat,
							       tempFloat); 
		}

                outblk.progressive = inFloat[c].progressive;
                outblk.setData(tempFloat[c].getData());
                break;

            case DataBlk.TYPE_SHORT:
            case DataBlk.TYPE_BYTE:
            default:
                // Unsupported output type. 
                throw new IllegalArgumentException ("invalid source datablock"+
						    " type"); 
	    }

            // Initialize the output block geometry and set the profiled
            // data into the output block.
            outblk.offset = 0;
            outblk.scanw = outblk.w; 
	} catch (MatrixBasedTransformException e) { 
            FacilityManager.getMsgLogger().
		printmsg(MsgLogger.ERROR,"matrix transform problem:\n"+
			 e.getMessage());
            if(pl.getParameter("debug").equals("on")) {
		e.printStackTrace();
	    } else { 
		FacilityManager.getMsgLogger().
		    printmsg(MsgLogger.ERROR,
			     "Use '-debug' option for more details");
	    }
	    return null; 
	} catch (MonochromeTransformException e) { 
            FacilityManager.getMsgLogger().
		printmsg(MsgLogger.ERROR,
			 "monochrome transform problem:\n"+e.getMessage());
            if(pl.getParameter("debug").equals("on")) {
		e.printStackTrace();
	    } else {
		FacilityManager.getMsgLogger().
		    printmsg(MsgLogger.ERROR,
			     "Use '-debug' option for more details");
	    }
            return null; 
	}

        return outblk; 
    }

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
     **/
    public DataBlk getInternCompData(DataBlk out, int c) {
	return getCompData(out, c);
    }

    /** Return a suitable String representation of the class instance. */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[ICCProfiler:");
        StringBuffer body = new StringBuffer ();
        if(icc!=null)     
	    body.append(eol).append(ColorSpace.indent("  ", icc.toString()));
        if(xform!=null)
	    body.append(eol).append(ColorSpace.indent("  ", xform.toString()));
        rep.append(ColorSpace.indent("  ", body));
        return rep.append("]").toString(); 
    }

    /* end class ICCProfiler */ }
