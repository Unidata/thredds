/*
 * CVS identifier:
 *
 * $Id: ForwardWT.java,v 1.60 2001/09/14 09:54:53 grosbois Exp $
 *
 * Class:                   ForwardWT
 *
 * Description:             This interface defines the specifics
 *                          of forward wavelet transforms
 *
 *
 *
 * COPYRIGHT:
 * 
 * This software module was originally developed by Rapha?l Grosbois and
 * Diego Santa Cruz (Swiss Federal Institute of Technology-EPFL); Joel
 * Askel?f (Ericsson Radio Systems AB); and Bertrand Berthelot, David
 * Bouchard, F?lix Henry, Gerard Mozelle and Patrice Onno (Canon Research
 * Centre France S.A) in the course of development of the JPEG2000
 * standard as specified by ISO/IEC 15444 (JPEG 2000 Standard). This
 * software module is an implementation of a part of the JPEG 2000
 * Standard. Swiss Federal Institute of Technology-EPFL, Ericsson Radio
 * Systems AB and Canon Research Centre France S.A (collectively JJ2000
 * Partners) agree not to assert against ISO/IEC and users of the JPEG
 * 2000 Standard (Users) any of their rights under the copyright, not
 * including other intellectual property rights, for this software module
 * with respect to the usage by ISO/IEC and Users of this software module
 * or modifications thereof for use in hardware or software products
 * claiming conformance to the JPEG 2000 Standard. Those intending to use
 * this software module in hardware or software products are advised that
 * their use may infringe existing patents. The original developers of
 * this software module, JJ2000 Partners and ISO/IEC assume no liability
 * for use of this software module or modifications thereof. No license
 * or right to this software module is granted for non JPEG 2000 Standard
 * conforming products. JJ2000 Partners have full right to use this
 * software module for his/her own purpose, assign or donate this
 * software module to any third party and to inhibit third parties from
 * using this software module for non JPEG 2000 Standard conforming
 * products. This copyright notice must be included in all copies or
 * derivative works of this software module.
 * 
 * Copyright (c) 1999/2000 JJ2000 Partners.
 * */
package ucar.jpeg.jj2000.j2k.wavelet.analysis;

import ucar.jpeg.jj2000.j2k.codestream.writer.*;
import ucar.jpeg.jj2000.j2k.codestream.*;
import ucar.jpeg.jj2000.j2k.wavelet.*;
import ucar.jpeg.jj2000.j2k.encoder.*;
import ucar.jpeg.jj2000.j2k.image.*;
import ucar.jpeg.jj2000.j2k.util.*;
import ucar.jpeg.jj2000.j2k.*;

import java.util.*;
import java.io.*;

/**
 * This abstract class represents the forward wavelet transform functional
 * block. The functional block may actually be comprised of several classes
 * linked together, but a subclass of this abstract class is the one that is
 * returned as the functional block that performs the forward wavelet
 * transform.
 *
 * <p>This class assumes that data is transferred in code-blocks, as defined
 * by the 'CBlkWTDataSrc' interface. The internal calculation of the wavelet
 * transform may be done differently but a buffering class should convert to
 * that type of transfer.</p>
 * */
public abstract class ForwardWT extends ImgDataAdapter
    implements ForwWT, CBlkWTDataSrc {

    /**
     * ID for the dyadic wavelet tree decomposition (also called "Mallat" in
     * JPEG 2000): 0x00.  
     * */
    public final static int WT_DECOMP_DYADIC = 0;

    /** The prefix for wavelet transform options: 'W' */
    public final static char OPT_PREFIX = 'W';

    /** The list of parameters that is accepted for wavelet transform. Options
     * for the wavelet transform start with 'W'. */
    private final static String [][] pinfo = {
        { "Wlev", "<number of decomposition levels>",
          "Specifies the number of wavelet decomposition levels to apply to "+
          "the image. If 0 no wavelet transform is performed. All components "+
          "and all tiles have the same number of decomposition levels.","5"},
        { "Wwt", "[full]",
          "Specifies the wavelet transform to be used. Possible value is: "+
          "'full' (full page). The value 'full' performs a normal DWT.",
          "full"},
        { "Wcboff", "<x y>", 
          "Code-blocks partition offset in the reference grid. Allowed for "+
          "<x> and <y> are 0 and 1.\n"+
          "Note: This option is defined in JPEG 2000 part 2 and may not"+
          " be supported by all JPEG 2000 decoders.", "0 0" }
    };

    /**
     * Initializes this object for the specified number of tiles 'nt' and
     * components 'nc'.
     *
     * @param src The source of ImgData
     * */
    protected ForwardWT(ImgData src) {
        super(src);
    }

    /**
     * Returns the parameters that are used in this class and implementing
     * classes. It returns a 2D String array. Each of the 1D arrays is for a
     * different option, and they have 3 elements. The first element is the
     * option name, the second one is the synopsis and the third one is a long
     * description of what the parameter is. The synopsis or description may
     * be 'null', in which case it is assumed that there is no synopsis or
     * description of the option, respectively. Null may be returned if no
     * options are supported.
     *
     * @return the options name, their synopsis and their explanation, or null
     * if no options are supported.
     * */
    public static String[][] getParameterInfo() {
        return pinfo;
    }

    /**
     * Creates a ForwardWT object with the specified filters, and with other
     * options specified in the parameter list 'pl'.
     *
     * @param src The source of data to be transformed
     *
     * @param pl The parameter list (or options).
     *
     * @param kers The encoder specifications.
     *
     * @return A new ForwardWT object with the specified filters and options
     * from 'pl'.
     *
     * @exception IllegalArgumentException If mandatory parameters are missing 
     * or if invalid values are given.
     * */
    public static ForwardWT createInstance(BlkImgDataSrc src,ParameterList pl,
                                           EncoderSpecs encSpec) {
        int defdec,deflev;
        String decompstr;
        String wtstr;
        String pstr;
        StreamTokenizer stok;
        StringTokenizer strtok;
        int prefx,prefy;        // Partitioning reference point coordinates

        // Check parameters
        pl.checkList(OPT_PREFIX,pl.toNameArray(pinfo));

        deflev = ((Integer)encSpec.dls.getDefault()).intValue();

        // Code-block partition origin
        String str = "";
        if(pl.getParameter("Wcboff")==null) {
            throw new Error("You must specify an argument to the '-Wcboff' "+
                            "option. See usage with the '-u' option");
        }
        StringTokenizer stk = new StringTokenizer(pl.getParameter("Wcboff"));
        if(stk.countTokens()!=2) {
            throw new IllegalArgumentException("'-Wcboff' option needs two"+
                                               " arguments. See usage with "+
                                               "the '-u' option.");
        }
        int cb0x = 0;
        str = stk.nextToken();
        try {
            cb0x = (new Integer(str)).intValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad first parameter for the "+
                                               "'-Wcboff' option: "+str);
        }
        if(cb0x<0 || cb0x>1) {
            throw new IllegalArgumentException("Invalid horizontal "+
                                               "code-block partition origin.");
        }
        int cb0y = 0;
        str = stk.nextToken();
        try {
            cb0y = (new Integer(str)).intValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad second parameter for the "+
                                               "'-Wcboff' option: "+str);
        }
        if(cb0y<0 || cb0y>1) {
            throw new IllegalArgumentException("Invalid vertical "+
                                               "code-block partition origin.");
        }
        if(cb0x!=0 || cb0y!=0) {
            FacilityManager.getMsgLogger().
                printmsg(MsgLogger.WARNING,"Code-blocks partition origin is "+
                         "different from (0,0). This is defined in JPEG 2000"+
                         " part 2 and may be not supported by all JPEG 2000 "+
                         "decoders.");
        }
        
        return new ForwWTFull(src,encSpec,cb0x,cb0y);
    }

}
