/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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



package ucar.nc2.iosp.nowrad;

//~--- non-JDK imports --------------------------------------------------------

import ucar.ma2.*;
import ucar.nc2.Variable;
import ucar.nc2.iosp.AbstractIOServiceProvider;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Feb 10, 2010
 * Time: 11:22:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class NOWRadiosp extends AbstractIOServiceProvider {
    final static int DEF_WBITS  = 15;
    final static int Z_DEFLATED = 8;

    // used for writing
    protected int     fileUsed        = 0;    // how much of the file is written to ?
    protected int     recStart        = 0;    // where the record data starts
    protected boolean debug           = false,
                      debugSize       = false,
                      debugSPIO       = false;
    protected boolean showHeaderBytes = false;
    protected HashMap dimHash         = new HashMap(50);
    protected boolean fill;

    // private Nidsheader.Vinfo myInfo;
    protected NOWRadheader                   headerParser;
    private ucar.unidata.io.RandomAccessFile myRaf;
    private ucar.nc2.NetcdfFile              ncfile;
    private int                              pcode;
    protected boolean                        readonly;

    /**
     * checking the file
     *
     * @param raf
     * @return the valid of file checking
     */
    public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
        NOWRadheader localHeader = new NOWRadheader();

        return (localHeader.isValidFile(raf));
    }

    public String getFileTypeId() {
        return "NOWRAD";
    }

    public String getFileTypeDescription() {
        return "NOWRAD Products";
    }

    /**
     * Open the file and read the header part
     *
     * @param raf
     * @param file
     * @param cancelTask
     * @throws java.io.IOException
     */
    public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile file,
                     ucar.nc2.util.CancelTask cancelTask)
            throws IOException {
        ncfile       = file;
        myRaf        = raf;
        headerParser = new NOWRadheader();

        try {
            headerParser.read(myRaf, ncfile);
        } catch (Exception e) {}

        // myInfo = headerParser.getVarInfo();
        pcode = 0;
        ncfile.finish();
    }

    /**
     * Read the data for each variable passed in
     *
     * @param v2
     * @param section
     * @return output data
     * @throws IOException
     * @throws ucar.ma2.InvalidRangeException
     */
    public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {

        // subset
        Object             data;
        Array              outputData;
        byte[]             vdata = null;
        NOWRadheader.Vinfo vinfo;
        ByteBuffer         bos;
        List<Range>        ranges = section.getRanges();

        vinfo = (NOWRadheader.Vinfo) v2.getSPobject();

        try {
            vdata = headerParser.getData((int) vinfo.hoff);
        } catch (Exception e) {}

        bos        = ByteBuffer.wrap(vdata);
        data       = readOneScanData(bos, vinfo, v2.getName());
        outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
        outputData = outputData.flip(1);

        // outputData = outputData.flip(2);
        return (outputData.sectionNoReduce(ranges).copy());

        // return outputData;
    }

    /**
     * Read one scan radar data
     *
     * @param bos   Data buffer
     * @param vinfo variable info
     * @return the data object of scan data
     */

    // all the work is here, so can be called recursively
    public Object readOneScanData(ByteBuffer bos, NOWRadheader.Vinfo vinfo, String vName)
            throws IOException, InvalidRangeException {
        int    doff   = (int) vinfo.hoff;
        int    npixel = vinfo.yt * vinfo.xt;
        byte[] rdata  = null;
        byte[] ldata  = new byte[vinfo.xt];
        byte[] pdata  = new byte[npixel];
        byte[] b2     = new byte[2];

        bos.position(doff);

        // begining of image data
        if ((convertunsignedByte2Short(bos.get()) != 0xF0) || (bos.get() != 0x0C)) {
            return null;
        }

        int     ecode;
        int     color;
        int     datapos;
        int     offset  = 0;
        int     roffset = 0;
        boolean newline = true;
        int     linenum = 0;

        while (true) {

            // line number
            if (newline) {
                bos.get(b2);
                linenum = (convertunsignedByte2Short(b2[1]) << 8) + convertunsignedByte2Short(b2[0]);

                // System.out.println("Line Number = " + linenum);
            }

            // int linenum = bytesToInt(b2[0], b2[1], true);
            // System.out.println("Line Number = " + linenum);
           // if(linenum == 1225)
           //   System.out.println(" HHHHH");
            short b = convertunsignedByte2Short(bos.get());

            color   = b & 0xF;
            ecode   = b >> 4;
            datapos = bos.position();

            int datarun;

            if (ecode == 0xF) {
                byte bb1 = bos.get(datapos - 2);
                byte bb2 = bos.get(datapos);

                if ((color == 0x0) && (bb1 == 0x00) && (bb2 == 0x00)) {
                    datapos += 1;
                }

                bos.position(datapos);
                datarun = 0;
            } else if (ecode == 0xE) {
                byte b0 = bos.get(datapos);

                datarun = convertunsignedByte2Short(b0) + 1;
                datapos += 1;
                bos.position(datapos);
            } else if (ecode == 0xD) {
                b2[0]   = bos.get(datapos);
                b2[1]   = bos.get(datapos + 1);
                datarun = (convertunsignedByte2Short(b2[1]) << 8) + convertunsignedByte2Short(b2[0]) + 1;
                datapos += 2;
                bos.position(datapos);
            } else {
                datarun = ecode + 1;
            }

            // move the unpacked data in the data line
            rdata = new byte[datarun];

            for (int i = 0; i < datarun; i++) {
                rdata[i] = (byte) color;
            }

            System.arraycopy(rdata, 0, ldata, roffset, datarun);
            roffset = roffset + datarun;

            // System.out.println("run ecode = " + ecode + " and data run " + datarun + " and totalrun " + roffset);
            // check to see if the beginning of the next line or at the end of the file
            short c0 = convertunsignedByte2Short(bos.get());

            if (c0 == 0x00) {
                short c1 = convertunsignedByte2Short(bos.get());
                short c2 = convertunsignedByte2Short(bos.get());

                // System.out.println("c1 and c2 " + c1 + " " + c2);

                if ((c0 == 0x00) && (c1 == 0xF0) && (c2 == 0x0C)) {
                    // beginning of next line
                  //  System.out.println("linenum   " + linenum + "   and this line total " + roffset);
                  //  if (roffset != 3661) {
                  //      System.out.println("ERROR missing data, this line total only " + roffset);
                  //  }
                    System.arraycopy(ldata, 0, pdata, offset, roffset);
                    offset  = offset + vinfo.xt;
                    roffset = 0;
                    newline = true;
                    ldata  = new byte[vinfo.xt];
                } else if ((c1 == 0xF0) && (c2 == 0x02)) {
                    // end of the file
                    break;
                } else {
                    datapos = bos.position() - 3;
                    bos.position(datapos);
                    newline = false;
                }
            } else {
                newline = false;
                datapos = bos.position();
                bos.position(datapos - 1);
            }
        }

        return pdata;
    }

    int getUInt(byte[] b, int num) {
        int base = 1;
        int i;
        int word = 0;
        int bv[] = new int[num];

        for (i = 0; i < num; i++) {
            bv[i] = convertunsignedByte2Short(b[i]);
        }

        /*
         * Calculate the integer value of the byte sequence
         */
        for (i = num - 1; i >= 0; i--) {
            word += base * bv[i];
            base *= 256;
        }

        return word;
    }

    public static int bytesToInt(short a, short b, boolean swapBytes) {

        // again, high order bit is expressed left into 32-bit form
        if (swapBytes) {
            return (a & 0xff) + ((int) b << 8);
        } else {
            return ((int) a << 8) + (b & 0xff);
        }
    }

    public static int bytesToInt(byte a, byte b, boolean swapBytes) {

        // again, high order bit is expressed left into 32-bit form
        if (swapBytes) {
            return (a & 0xff) + ((int) b << 8);
        } else {
            return ((int) a << 8) + (b & 0xff);
        }
    }




    /**
     * Read data from encoded values and run len into regular data array
     *
     * @param ddata is encoded data values
     * @return the data array of row data
     */
    public byte[] readOneRowData(byte[] ddata, int rLen, int xt) throws IOException, InvalidRangeException {
        int    run;
        byte[] bdata = new byte[xt];
        int    nbin  = 0;
        int    total = 0;

        for (run = 0; run < rLen; run++) {
            int  drun   = convertunsignedByte2Short(ddata[run]) >> 4;
            byte dcode1 = (byte) (convertunsignedByte2Short(ddata[run]) & 0Xf);

            for (int i = 0; i < drun; i++) {
                bdata[nbin++] = dcode1;
                total++;
            }
        }

        if (total < xt) {
            for (run = total; run < xt; run++) {
                bdata[run] = 0;
            }
        }

        return bdata;
    }



     int getUInt(byte[] b, int offset, int num) {
        int base = 1;
        int i;
        int word = 0;
        int bv[] = new int[num];

        for (i = 0; i < num; i++) {
            bv[i] = convertunsignedByte2Short(b[offset + i]);
        }

        /*
         * Calculate the integer value of the byte sequence
         */
        for (i = num - 1; i >= 0; i--) {
            word += base * bv[i];
            base *= 256;
        }

        return word;
    }

    int getInt(byte[] b, int offset, int num) {
        int base = 1;
        int i;
        int word = 0;
        int bv[] = new int[num];

        for (i = 0; i < num; i++) {
            bv[i] = convertunsignedByte2Short(b[offset + i]);
        }

        if (bv[0] > 127) {
            bv[0] -= 128;
            base  = -1;
        }

        /*
         * Calculate the integer value of the byte sequence
         */

        for (i = num - 1; i >= 0; i--) {
            word += base * bv[i];
            base *= 256;
        }

        return word;
    }

    public short convertunsignedByte2Short(byte b) {
        return (short) ((b < 0)
                        ? (short) b + 256
                        : (short) b);
    }

    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    public void flush() throws java.io.IOException {
        myRaf.flush();
    }

    public void close() throws java.io.IOException {
        myRaf.close();
    }

    public static void main(String args[])
            throws Exception, IOException, InstantiationException, IllegalAccessException {
        String fileIn = "z:/nowrad/BREF_951207_2230";

        // String fileIn = "c:/data/image/Nids/n0r_20041013_1852";
        ucar.nc2.NetcdfFile.registerIOProvider(ucar.nc2.iosp.nowrad.NOWRadiosp.class);

        ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn);

        // List alist = ncf.getGlobalAttributes();
        ucar.nc2.Variable v      = ncf.findVariable("BaseReflectivity");
        int[]             origin = { 0, 0 };
        int[]             shape  = { 300, 36 };
        ArrayByte         data   = (ArrayByte) v.read(origin, shape);

        ncf.close();
    }
}
