/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.iosp.hdf4;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.StringTokenizer;

/**
 * @author caron
 * @since Jul 18, 2007
 */
public class TagEnum {
  private static java.util.Map<Short, TagEnum> hash = new java.util.HashMap<Short, TagEnum>(100);

  public static int SPECIAL_LINKED = 1;    /* Fixed-size Linked blocks */
  public static int SPECIAL_EXT = 2;       /* External */
  public static int SPECIAL_COMP = 3;      /* Compressed */
  public static int SPECIAL_VLINKED = 4;   /* Variable-length linked blocks */
  public static int SPECIAL_CHUNKED = 5;   /* chunked element */
  public static int SPECIAL_BUFFERED = 6;  /* Buffered element */
  public static int SPECIAL_COMPRAS = 7;   /* Compressed Raster element */

  /*
typedef enum
  {
      COMP_CODE_NONE = 0,       /* don't encode at all, just store
      COMP_CODE_RLE,            /* for simple RLE encoding
      COMP_CODE_NBIT,           /* for N-bit encoding
      COMP_CODE_SKPHUFF,        /* for Skipping huffman encoding
      COMP_CODE_DEFLATE,        /* for gzip 'deflate' encoding
      COMP_CODE_SZIP,		        /* for szip encoding
      COMP_CODE_INVALID,        /* invalid last code, for range checking
      COMP_CODE_JPEG            /* _Ugly_ hack to allow JPEG images to be created with GRsetcompress
  }  */
  public static int COMP_CODE_NONE = 0;    // don't encode at all, just store
  public static int COMP_CODE_RLE = 1;     // for simple RLE encoding
  public static int COMP_CODE_NBIT = 2;    // for N-bit encoding
  public static int COMP_CODE_SKPHUFF = 3; // for Skipping huffman encoding
  public static int COMP_CODE_DEFLATE = 4; // for gzip 'deflate' encoding
  public static int COMP_CODE_SZIP = 5;    // for szip encoding

  public final static TagEnum NONE = new TagEnum("NONE", "", (short) 0);
  public final static TagEnum NULL = new TagEnum("NULL", "", (short) 1);
  public final static TagEnum RLE = new TagEnum("RLE", "Run length encoding", (short) 11);
  public final static TagEnum IMC = new TagEnum("IMC", "IMCOMP compression alias", (short) 12);
  public final static TagEnum IMCOMP = new TagEnum("IMCOMP", "IMCOMP compression", (short) 12);
  public final static TagEnum JPEG = new TagEnum("JPEG", "JPEG compression (24-bit data)", (short) 13);
  public final static TagEnum GREYJPEG = new TagEnum("GREYJPEG", "JPEG compression (8-bit data)", (short) 14);
  public final static TagEnum JPEG5 = new TagEnum("JPEG5", "JPEG compression (24-bit data)", (short) 15);
  public final static TagEnum GREYJPEG5 = new TagEnum("GREYJPEG5", "JPEG compression (8-bit data)", (short) 16);

  public final static TagEnum LINKED = new TagEnum("LINKED", "Linked-block special element", (short) 20);
  public final static TagEnum VERSION = new TagEnum("VERSION", "Version", (short) 30);
  public final static TagEnum COMPRESSED = new TagEnum("COMPRESSED", "Compressed special element", (short) 40); // 0x28
  public final static TagEnum VLINKED = new TagEnum("VLINKED", "Variable-len linked-block header", (short) 50);
  public final static TagEnum VLINKED_DATA = new TagEnum("VLINKED_DATA", "Variable-len linked-block data", (short) 51);
  public final static TagEnum CHUNKED = new TagEnum("CHUNKED", "Chunked special element header", (short) 60);
  public final static TagEnum CHUNK = new TagEnum("CHUNK", "Chunk element", (short) 61);  // 0x3d

  public final static TagEnum FID = new TagEnum("FID", "File identifier", (short) 100);
  public final static TagEnum FD = new TagEnum("FD", "File description", (short) 101);
  public final static TagEnum TID = new TagEnum("TID", "Tag identifier", (short) 102);
  public final static TagEnum TD = new TagEnum("TD", "Tag descriptor", (short) 103);
  public final static TagEnum DIL = new TagEnum("DIL", "Data identifier label", (short) 104);
  public final static TagEnum DIA = new TagEnum("DIA", "Data identifier annotation", (short) 105);
  public final static TagEnum NT = new TagEnum("NT", "Number type", (short) 106);
  public final static TagEnum MT = new TagEnum("MT", "Machine type", (short) 107);
  public final static TagEnum FREE = new TagEnum("FREE", "Free space in the file", (short) 108);

  public final static TagEnum ID8 = new TagEnum("ID8", "8-bit Image dimension", (short) 200); // obsolete
  public final static TagEnum IP8 = new TagEnum("IP8", "8-bit Image palette", (short) 201); // obsolete
  public final static TagEnum RI8 = new TagEnum("RI8", "Raster-8 image", (short) 202); // obsolete
  public final static TagEnum CI8 = new TagEnum("CI8", "RLE compressed 8-bit image", (short) 203); // obsolete
  public final static TagEnum II8 = new TagEnum("II8", "IMCOMP compressed 8-bit image", (short) 204); // obsolete

  public final static TagEnum ID = new TagEnum("ID", "Image DimRec", (short) 300);
  public final static TagEnum LUT = new TagEnum("LUT", "Image Palette", (short) 301);
  public final static TagEnum RI = new TagEnum("RI", "Raster Image", (short) 302);
  public final static TagEnum CI = new TagEnum("CI", "Compressed Image", (short) 303);
  public final static TagEnum NRI = new TagEnum("NRI", "New-format Raster Image", (short) 304);
  public final static TagEnum RIG = new TagEnum("RIG", "Raster Image Group", (short) 306);
  public final static TagEnum LD = new TagEnum("LD", "Palette DimRec", (short) 307);
  public final static TagEnum MD = new TagEnum("MD", "Matte DimRec", (short) 308);
  public final static TagEnum MA = new TagEnum("MA", "Matte Data", (short) 309);
  public final static TagEnum CCN = new TagEnum("CCN", "Color correction", (short) 310);
  public final static TagEnum CFM = new TagEnum("CFM", "Color format", (short) 311);
  public final static TagEnum AR = new TagEnum("AR", "Cspect ratio", (short) 312);

  public final static TagEnum DRAW = new TagEnum("DRAW", "Draw these images in sequence", (short) 400);
  public final static TagEnum RUN = new TagEnum("RUN", "Cun this as a program/script", (short) 401);

  public final static TagEnum XYP = new TagEnum("XYP", "X-Y position", (short) 500);
  public final static TagEnum MTO = new TagEnum("MTO", "Machine-type override", (short) 501);

  public final static TagEnum T14 = new TagEnum("T14", "TEK 4014 data", (short) 602);
  public final static TagEnum T105 = new TagEnum("T105", "TEK 4105 data", (short) 603);

  public final static TagEnum SDG = new TagEnum("SDG", "Scientific Data Group", (short) 700);  // obsolete
  public final static TagEnum SDD = new TagEnum("SDD", "Scientific Data DimRec", (short) 701);
  public final static TagEnum SD = new TagEnum("SD", "Scientific Data", (short) 702);
  public final static TagEnum SDS = new TagEnum("SDS", "Scales", (short) 703);
  public final static TagEnum SDL = new TagEnum("SDL", "Labels", (short) 704);
  public final static TagEnum SDU = new TagEnum("SDU", "Units", (short) 705);
  public final static TagEnum SDF = new TagEnum("SDF", "Formats", (short) 706);
  public final static TagEnum SDM = new TagEnum("SDM", "Max/Min", (short) 707);
  public final static TagEnum SDC = new TagEnum("SDC", "Coord sys", (short) 708);
  public final static TagEnum SDT = new TagEnum("SDT", "Transpose", (short) 709); // obsolete
  public final static TagEnum SDLNK = new TagEnum("SDLNK", "Links related to the dataset", (short) 710);
  public final static TagEnum NDG = new TagEnum("NDG", "Numeric Data Group", (short) 720);
  public final static TagEnum CAL = new TagEnum("CAL", "Calibration information", (short) 731);
  public final static TagEnum FV = new TagEnum("FV", "Fill Value information", (short) 732);
  public final static TagEnum BREQ = new TagEnum("BREQ", "Beginning of required tags", (short) 799);
  public final static TagEnum SDRAG = new TagEnum("SDRAG", "List of ragged array line lengths", (short) 781);
  public final static TagEnum EREQ = new TagEnum("EREQ", "Current end of the range", (short) 780);

  public final static TagEnum VG = new TagEnum("VG", "Vgroup", (short) 1965);
  public final static TagEnum VH = new TagEnum("VH", "Vdata Header", (short) 1962);
  public final static TagEnum VS = new TagEnum("VS", "Vdata Storage", (short) 1963);

  private String name, desc;
  private short code;

  private TagEnum(String name, String desc, short code) {
    this.name = name;
    this.desc = desc;
    this.code = code;
    hash.put(code, this);
  }

  public String getDesc() { return desc; }
  public String getName() { return name; }
  public short getCode() { return code; }
  public String toString() { return name+" ("+code+") "+desc; }

  /**
   * Find the Tag that matches the code.
   *
   * @param code find Tag with this code.
   * @return Tag or null if no match.
   */
  public static TagEnum getTag(short code) {
    TagEnum te = hash.get(code);
    if (te == null) te = new TagEnum("UNKNOWN", "UNKNOWN", code);
    return te;
  }

  static public void main( String args[]) throws IOException {
    FileInputStream ios = new FileInputStream("C:/dev/hdf4/HDF4.2r1/hdf/src/htags.h");
    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;
      if (line.startsWith("#define")) {
        StringTokenizer stoker = new StringTokenizer(line," ()");
        stoker.nextToken(); // skip define
        String name = stoker.nextToken();
        if (!stoker.hasMoreTokens()) continue;
        //System.out.println(line);

        if (name.startsWith("DFTAG_"))
          name = name.substring(6);

        String code = stoker.nextToken();
        if (code.startsWith("u"))
          code = stoker.nextToken();

        int pos = line.indexOf("/*");
        String desc = "";
        if (pos > 0) {
          int pos2 = line.indexOf("*/");
          desc = (pos2 > 0) ? line.substring(pos+3, pos2) : line.substring(pos+3);
          desc = desc.trim();
        }

        System.out.println("  public final static Tags "+name+" = new Tags(\""+name+"\", \""+desc+"\", (short) "+code+");");
      }
    }
  }

}
