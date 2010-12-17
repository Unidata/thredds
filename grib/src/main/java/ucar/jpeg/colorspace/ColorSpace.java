/*****************************************************************************
 *
 * $Id: ColorSpace.java,v 1.2 2002/07/25 16:31:11 grosbois Exp $
 *
 * Copyright Eastman Kodak Company, 343 State Street, Rochester, NY 14650
 * $Date $
 *****************************************************************************/

package ucar.jpeg.colorspace;

import java.io.IOException;
import ucar.jpeg.jj2000.j2k.fileformat.FileFormatBoxes;
import ucar.jpeg.jj2000.j2k.util.ParameterList;
import ucar.jpeg.jj2000.j2k.codestream.reader.HeaderDecoder;
import ucar.jpeg.jj2000.j2k.io.RandomAccessIO;
import ucar.jpeg.icc .ICCProfile;
import ucar.jpeg.colorspace .boxes.PaletteBox;
import ucar.jpeg.colorspace .boxes.ComponentMappingBox;
import ucar.jpeg.colorspace .boxes.ColorSpecificationBox;
import ucar.jpeg.colorspace .boxes.ChannelDefinitionBox;
import ucar.jpeg.colorspace .boxes.ImageHeaderBox;
import ucar.jpeg.colorspace .boxes.JP2Box;

/**
 * This class analyzes the image to provide colorspace
 * information for the decoding chain.  It does this by
 * examining the box structure of the JP2 image.
 * It also provides access to the parameter list information,
 * which is stored as a public final field.
 * 
 * @see		jj2000.j2k.icc.ICCProfile
 * @version	1.0
 * @author	Bruce A. Kern
 */
public class ColorSpace {
    public static final String eol = System.getProperty("line.separator");

    // Renamed for convenience:
    static final int GRAY  = 0;
    static final int RED   = 1;
    static final int GREEN = 2;
    static final int BLUE  = 3;

    /** Parameter Specs */
    public ParameterList pl;

    /** Parameter Specs */
    public HeaderDecoder hd;

    /* Image box structure as pertains to colorspacees. */
    private PaletteBox            pbox = null;
    private ComponentMappingBox   cmbox = null;
    private ColorSpecificationBox csbox = null;
    private ChannelDefinitionBox  cdbox = null;
    private ImageHeaderBox        ihbox = null;

    /** Input image */
    private RandomAccessIO in=null;

    /**
     * Retrieve the ICC profile from the images as
     * a byte array.
     * @return the ICC Profile as a byte [].
     */
    public byte [] getICCProfile () {
        return csbox.getICCProfile(); 
    }
    
    /** Indent a String that contains newlines. */
    public static String indent(String ident, StringBuffer instr) {
        return indent(ident, instr.toString()); 
    }
    
    /** Indent a String that contains newlines. */
    public static String indent(String ident, String instr) {
        StringBuffer tgt = new StringBuffer (instr);
        char eolChar = eol.charAt(0);
        int i = tgt.length();
        while (--i > 0) {
            if (tgt.charAt(i) == eolChar) tgt.insert(i+1,ident); }
        return ident + tgt.toString(); }

    /**
     * public constructor which takes in the image, parameterlist and the
     * image header decoder as args.
     *   @param in input RandomAccess image file.
     *   @param hd provides information about the image header.
     *   @param pl provides parameters from the default and commandline lists. 
     * @exception IOException, ColorSpaceException
     */
    public ColorSpace (RandomAccessIO in, HeaderDecoder hd, ParameterList pl) 
        throws IOException, ColorSpaceException {
        this.pl = pl;
        this.in = in;
        this.hd = hd;
        getBoxes(); 
    }

    /**
     * Retrieve the various boxes from the JP2 file.
     * @exception ColorSpaceException, IOException
     */
    final protected void getBoxes () throws ColorSpaceException, IOException {
        byte [] data;
        int type;
        long len = 0;
        int boxStart  = 0;
        byte [] boxHeader = new byte [16];
        int i=0;

        // Search the toplevel boxes for the header box
        while (true) {
            in.seek(boxStart);
            in.readFully(boxHeader,0,16);
            len = (long) ICCProfile.getInt(boxHeader,0);
            if(len==1) len = ICCProfile.getLong(boxHeader,8);  // Extended
							       // length
            type =  ICCProfile.getInt(boxHeader,4);

            // Verify the contents of the file so far.
            if(i==0 && type!=FileFormatBoxes.JP2_SIGNATURE_BOX) {
                throw new ColorSpaceException("first box in image not "+
					      "signature");
	    } else if(i==1 && type!=FileFormatBoxes.FILE_TYPE_BOX) {
                throw new ColorSpaceException("second box in image not file");
	    } else if(type==FileFormatBoxes.CONTIGUOUS_CODESTREAM_BOX) {
                throw new ColorSpaceException("header box not found in image");
	    } else if (type==FileFormatBoxes.JP2_HEADER_BOX) {
		break;
	    }

            // Progress to the next box.
            ++i;
            boxStart += len; }
        
        // boxStart indexes the start of the JP2_HEADER_BOX,
        // make headerBoxEnd index the end of the box.
        long headerBoxEnd = boxStart+len;

        if(len==1) boxStart += 8;  // Extended length header

        for(boxStart += 8; boxStart<headerBoxEnd;  boxStart += len) {
            in.seek(boxStart);
            in.readFully(boxHeader,0,16);
            len = (long) ICCProfile.getInt(boxHeader,0);
            if(len==1) throw new ColorSpaceException("Extended length boxes "+
						     "not supported");
            type = (int) ICCProfile.getInt(boxHeader,4);

            switch (type) {
            case FileFormatBoxes.IMAGE_HEADER_BOX:
                ihbox = new ImageHeaderBox (in,boxStart);
                break;
            case FileFormatBoxes.COLOUR_SPECIFICATION_BOX:
                csbox = new ColorSpecificationBox (in,boxStart);
                break;
            case FileFormatBoxes.CHANNEL_DEFINITION_BOX:
                cdbox = new ChannelDefinitionBox (in,boxStart);
                break;
            case FileFormatBoxes.COMPONENT_MAPPING_BOX:
                cmbox = new ComponentMappingBox (in,boxStart);
                break;
            case FileFormatBoxes.PALETTE_BOX:
                pbox = new PaletteBox (in,boxStart);
                break;
            default:
                break;
            }}

        if (ihbox==null) 
            throw new ColorSpaceException ("image header box not found");

        if ((pbox==null && cmbox!=null) || (pbox!=null && cmbox==null))
            throw new ColorSpaceException ("palette box and component "+
					   "mapping box inconsistency"); 
    }


    /** Return the channel definition of the input component. */
    public int getChannelDefinition (int c) {
        if (cdbox==null) return c;
        else return cdbox.getCn(c+1); }


    /** Return the colorspace method (Profiled, enumerated, or palettized). */
    public MethodEnum getMethod () {
        return csbox.getMethod(); }

    /** Return the colorspace (sYCC, sRGB, sGreyScale). */
    public CSEnum getColorSpace () {
        return csbox.getColorSpace(); }

    /** Return number of channels in the palette. */
    public  /*final*/ PaletteBox getPaletteBox() {
        return pbox; }

    /** Return number of channels in the palette. */
    public int getPaletteChannels() {
        return pbox == null?
            0:
            pbox.getNumColumns(); }

    /** Return bitdepth of the palette entries. */
    public int getPaletteChannelBits(int c) {
        return pbox == null?
            0:
            pbox.getBitDepth(c); }

    /**
     * Return a palettized sample
     *   @param channel requested 
     *   @param index of entry
     * @return palettized sample
     */
    public int getPalettizedSample(int channel, int index) {
        return pbox == null?
            0:
            pbox.getEntry (channel,index); }

    /** Is palettized predicate. */
    public boolean isPalettized() {
        return pbox != null; }

    /** Signed output predicate. */
    public boolean isOutputSigned (int channel) {
        return (pbox!=null) ?
            pbox.isSigned(channel):
            hd.isOriginalSigned(channel); }

    /** Return a suitable String representation of the class instance. */
    public String toString () {
        StringBuffer rep = new StringBuffer ("[ColorSpace is ")
            .append(csbox.getMethodString())
            .append(isPalettized()? "  and palettized ": " ")
            .append(getMethod() == ENUMERATED?csbox.getColorSpaceString(): "");
        if(ihbox!=null) rep.append(eol).
			    append(indent("    ",ihbox.toString()));
        if(cdbox!=null) rep.append(eol).
			    append(indent("    ",cdbox.toString()));
        if(csbox!=null) rep.append(eol).
			    append(indent("    ",csbox.toString()));
        if(pbox!=null) rep.append(eol).append(indent("    ",pbox.toString()));
        if(cmbox!=null) rep.append(eol).
			     append(indent("    ",cmbox.toString()));
        return rep.append("]").toString(); 
    }

    /**
     * Are profiling diagnostics turned on
     * @return yes or no
     */
    public boolean debugging() {
        return pl.getProperty("colorspace_debug")!=null && 
            pl.getProperty("colorspace_debug").equalsIgnoreCase("on"); }

    /* Enumeration Class */
    /** method enumeration */ 
    public final static MethodEnum ICC_PROFILED = new MethodEnum ("profiled");
    /** method enumeration */ 
    public final static MethodEnum ENUMERATED = new MethodEnum ("enumerated");
    
    /** colorspace enumeration */ 
    public final static CSEnum sRGB       = new CSEnum("sRGB");
    /** colorspace enumeration */ 
    public final static CSEnum GreyScale  = new CSEnum("GreyScale");
    /** colorspace enumeration */ 
    public final static CSEnum sYCC       = new CSEnum("sYCC");
    /** colorspace enumeration */ 
    public final static CSEnum Illegal    = new CSEnum("Illegal");
    /** colorspace enumeration */
    public final static CSEnum Unknown    = new CSEnum("Unknown");

    /**
     * Typesafe enumeration class
     * @version	1.0
     * @author	Bruce A Kern
     */
    public static class Enumeration {
        public final String value;
        public Enumeration (String value) { 
	    this.value=value; 
	}
        public String toString () { 
	    return value; 
	}
    }

    
    /**
     * Method enumeration class
     * @version	1.0
     * @author	Bruce A Kern
     */
    public static class MethodEnum extends Enumeration { 
	public MethodEnum (String value) {super(value); 
	}
    }

    /**
     * Colorspace enumeration class
     * @version	1.0
     * @author	Bruce A Kern
     */
    public static class CSEnum extends Enumeration { 
	public CSEnum (String value) {super(value); 
	}
    }

    /* end class ColorSpace */ }







