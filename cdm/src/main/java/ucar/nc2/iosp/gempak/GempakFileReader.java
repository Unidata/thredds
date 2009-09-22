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


package ucar.nc2.iosp.gempak;


import ucar.unidata.io.RandomAccessFile;

import java.io.*;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;


/**
 * Read a Gempak grid file
 */
public class GempakFileReader implements GempakConstants {

    /** The file */
    protected RandomAccessFile rf;

    /** An error message */
    private String errorMessage;

    /** The label common param */
    protected DMLabel dmLabel;

    /** header info */
    protected List<DMFileHeaderInfo> fileHeaderInfo;

    /** headers */
    protected DMHeaders headers;

    /** key */
    protected DMKeys keys;

    /** part */
    protected List<DMPart> parts;

    /** the machine type byte order */
    protected int MTMACH = RandomAccessFile.BIG_ENDIAN;

    /** swap flag */
    protected boolean mvmst = false;

    /** swap flag */
    protected boolean needToSwap = false;

    /** file size */
    protected long fileSize = 0;

    /** masking pattern */
    //private static int mskpat = 0xFFFF;
    private static int mskpat = ~0;

    /**
     * Bean ctor
     */
    GempakFileReader() {}

    /**
     * Get a RandomAccessFile for the file location
     *
     * @param filename   filename to read.
     * @return RandomAccessFile
     *
     * @throws IOException   problem reading file
     */
    public static RandomAccessFile getFile(String filename)
            throws IOException {
        return new RandomAccessFile(filename, "r", 2048);
    }

    /**
     * Initialize the file, read in all the metadata (ala DM_OPEN)
     *
     * @param raf   RandomAccessFile to read.
     * @param fullCheck  if true, check entire structure
     *
     * @return a GempakFileReader
     * @throws IOException   problem reading file
     */
    public static GempakFileReader getInstance(RandomAccessFile raf,
            boolean fullCheck)
            throws IOException {
        GempakFileReader gfr = new GempakFileReader();
        gfr.init(raf, fullCheck);
        return gfr;
    }

    /**
     * Initialize the file, read in all the metadata (ala DM_OPEN)
     *
     * @param raf   RandomAccessFile to read.
     * @param fullCheck  if true, check entire structure
     *
     * @throws IOException   problem reading file
     */
    public final void init(RandomAccessFile raf, boolean fullCheck)
            throws IOException {
        setByteOrder();
        rf = raf;
        raf.seek(0);

        boolean ok = init(fullCheck);
        fileSize = rf.length();
        if ( !ok) {
            throw new IOException("Unable to open GEMPAK file: "
                                  + errorMessage);
        }
    }

    /**
     * Initialize this reader.  Read all the metadata
     *
     * @return true if successful
     *
     * @throws IOException  problem reading the data
     */
    protected boolean init() throws IOException {
        return init(true);
    }

    /**
     * Initialize the file, read in all the metadata (ala DM_OPEN)
     *
     * @param fullCheck  if true, check entire structure
     * @return  true if successful
     *
     * @throws IOException   problem reading file
     */
    protected boolean init(boolean fullCheck) throws IOException {
        if (rf == null) {
            throw new IOException("file has not been set");
        }

        dmLabel = new DMLabel();
        boolean labelOk = dmLabel.init();

        if ( !labelOk) {
            logError("not a GEMPAK file");
            return false;
        }

        // Read the keys  (DM_RKEY)
        readKeys();
        if (keys == null) {
            logError("Couldn't read keys");
            return false;
        }

        // Read the headers (DM_RHDA)
        readHeaders();
        if (headers == null) {
            logError("Couldn't read headers");
            return false;
        }

        // Read the parts (DM_RPRT)
        readParts();
        if (parts == null) {
            logError("Couldn't read parts");
            return false;
        }

        // Read the file header info (DM_RFIL)
        readFileHeaderInfo();
        if (fileHeaderInfo == null) {
            logError("Couldn't read file header info");
            return false;
        }
        return true;

    }

    /**
     * Get the file name.
     * @return  the name of the file
     */
    public String getFilename() {
        return (rf == null)
               ? null
               : rf.getLocation();
    }


    /**
     * Get initial file size
     * @return the file size when init  was called.
     */
    public long getInitFileSize() {
        return fileSize;
    }

    /**
     * Get the byte order for this system
     *
     * @return byte order
     */
    public int getByteOrder() {
        return MTMACH;
    }

    /**
     * Get the byte order for the machine type.
     *
     * @param kmachn   maching type
     *
     * @return  byte order
     */
    public int getByteOrder(int kmachn) {
        if ((kmachn == MTVAX) || (kmachn == MTULTX) || (kmachn == MTALPH)
                || (kmachn == MTLNUX) || (kmachn == MTIGPH)) {
            return RandomAccessFile.LITTLE_ENDIAN;
        }
        return RandomAccessFile.BIG_ENDIAN;
    }

    /**
     * Set the machine type for this system.
     * @see <a href="http://lopica.sourceforge.net/os.html">http://lopica.sourceforge.net/os.html</a>
     */
    private void setByteOrder() {
        String arch = System.getProperty("os.arch");
        if (arch.equals("x86") ||            // Windows, Linux
                arch.equals("arm") ||        // Window CE
                    arch.equals("alpha")) {  // Utrix, VAX, DECOS
            MTMACH = RandomAccessFile.LITTLE_ENDIAN;
        } else {
            MTMACH = RandomAccessFile.BIG_ENDIAN;
        }
    }

    /**
     * Read the file header info (DM_RFIL)
     *
     * @throws IOException problem reading file
     */
    protected void readFileHeaderInfo() throws IOException {
        if (dmLabel == null) {
            return;
        }
        int      iread      = dmLabel.kpfile;
        int      numheaders = dmLabel.kfhdrs;
        String[] names      = new String[numheaders];
        int[]    lens       = new int[numheaders];
        int[]    types      = new int[numheaders];
        for (int i = 0; i < numheaders; i++) {
            names[i] = DM_RSTR(iread++);
        }
        for (int i = 0; i < numheaders; i++) {
            lens[i] = DM_RINT(iread++);
        }
        for (int i = 0; i < numheaders; i++) {
            types[i] = DM_RINT(iread++);
        }
        fileHeaderInfo = new ArrayList<DMFileHeaderInfo>();

        for (int i = 0; i < numheaders; i++) {
            DMFileHeaderInfo ghi = new DMFileHeaderInfo();
            ghi.kfhnam = names[i];
            ghi.kfhlen = lens[i];
            ghi.kfhtyp = types[i];
            fileHeaderInfo.add(ghi);
        }
    }

    /**
     * Read in the row and column keys (DM_KEY)
     *
     * @throws IOException problem reading file
     */
    protected void readKeys() throws IOException {
        if (dmLabel == null) {
            return;
        }
        keys = new DMKeys();
        // read the row keys
        int       num   = dmLabel.krkeys;
        List<Key> rkeys = new ArrayList<Key>(num);
        for (int i = 0; i < num; i++) {
            String key = DM_RSTR(dmLabel.kprkey + i);
            rkeys.add(new Key(key, i, ROW));
        }
        keys.kkrow = rkeys;
        num        = dmLabel.kckeys;
        List<Key> ckeys = new ArrayList<Key>(num);
        for (int i = 0; i < num; i++) {
            String key = DM_RSTR(dmLabel.kpckey + i);
            ckeys.add(new Key(key, i, COL));
        }
        keys.kkcol = ckeys;
    }

    /** keys to swap */
    private static String[] swapKeys = {
        "STID", "STD2", "STAT", "COUN", "GPM1", "GVCD"
    };

    /** number  of words to swap */
    private static int[] swapNum = {
        1, 1, 1, 1, 3, 1
    };

    /**
     * Read the headers (DM_RHDA)
     *
     * @throws IOException problem reading file
     */
    protected void readHeaders() throws IOException {
        if (dmLabel == null) {
            return;
        }
        headers = new DMHeaders();
        List<int[]> rowHeaders = new ArrayList<int[]>(dmLabel.krow);
        int         istart     = dmLabel.kprowh;

        // first word is a valid flag so we have to add 1 to size
        int[] header;
        for (int i = 0; i < dmLabel.krow; i++) {
            header = new int[dmLabel.krkeys + 1];
            DM_RINT(istart, header);
            if (header[0] != IMISSD) {
                headers.lstrw = i;
            }
            rowHeaders.add(header);
            istart += header.length;
        }
        headers.rowHeaders = rowHeaders;
        List<int[]> colHeaders = new ArrayList<int[]>(dmLabel.kcol);
        istart = dmLabel.kpcolh;
        for (int i = 0; i < dmLabel.kcol; i++) {
            header = new int[dmLabel.kckeys + 1];
            DM_RINT(istart, header);
            if (header[0] != IMISSD) {
                headers.lstcl = i;
            }
            colHeaders.add(header);
            istart += header.length;
        }
        headers.colHeaders = colHeaders;


        // some of the words are characters
        if (needToSwap) {
            int[]    keyLoc  = new int[swapKeys.length];
            String[] keyType = new String[swapKeys.length];
            boolean  haveRow = false;
            boolean  haveCol = false;
            for (int i = 0; i < swapKeys.length; i++) {
                Key key = findKey(swapKeys[i]);
                keyLoc[i]  = (key != null)
                             ? key.loc + 1
                             : 0;
                keyType[i] = (key != null)
                             ? key.type
                             : "";
                if (keyType[i].equals(ROW)) {
                    haveRow = true;
                }
                if (keyType[i].equals(COL)) {
                    haveCol = true;
                }
            }
            if (haveRow) {
                for (int[] toCheck : headers.rowHeaders) {
                    for (int j = 0; j < swapKeys.length; j++) {
                        if (keyType[j].equals(ROW)) {
                            if (swapKeys[j].equals("GVCD")
                                    && !(toCheck[keyLoc[j]]
                                         > GempakUtil.vertCoords.length)) {
                                continue;
                            }
                            GempakUtil.swp4(toCheck, keyLoc[j], swapNum[j]);
                        }
                    }
                }
            }
            if (haveCol) {
                for (int[] toCheck : headers.colHeaders) {
                    for (int j = 0; j < swapKeys.length; j++) {
                        if (keyType[j].equals(COL)) {
                            if (swapKeys[j].equals("GVCD")
                                    && !(toCheck[keyLoc[j]]
                                         > GempakUtil.vertCoords.length)) {
                                continue;
                            }
                            GempakUtil.swp4(toCheck, keyLoc[j], swapNum[j]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Read the parts (DM_RPRT)
     *
     * @throws IOException problem reading file
     */
    protected void readParts() throws IOException {
        if (dmLabel == null) {
            return;
        }
        int      iread     = dmLabel.kppart;
        int      numParts  = dmLabel.kprt;
        DMPart[] partArray = new DMPart[numParts];
        // read the part names
        for (int i = 0; i < numParts; i++) {
            partArray[i] = new DMPart();
            String partName = DM_RSTR(iread++);
            partArray[i].kprtnm = partName;
        }
        // read the part header lengths
        for (int i = 0; i < numParts; i++) {
            int headerLen = DM_RINT(iread++);
            partArray[i].klnhdr = headerLen;
        }
        // read the part types
        for (int i = 0; i < numParts; i++) {
            int partType = DM_RINT(iread++);
            partArray[i].ktyprt = partType;
        }
        // get number of parameters/per part.
        for (int i = 0; i < numParts; i++) {
            partArray[i].kparms = DM_RINT(iread++);
        }
        // read parameter names
        for (int i = 0; i < numParts; i++) {
            int           numParms = partArray[i].kparms;
            List<DMParam> parms    = new ArrayList<DMParam>(numParms);
            for (int j = 0; j < numParms; j++) {
                DMParam dmp = new DMParam();
                parms.add(dmp);
                dmp.kprmnm = DM_RSTR(iread++);
            }
            partArray[i].params = parms;
        }
        // read the scale
        for (int i = 0; i < numParts; i++) {
            int  numParms = partArray[i].kparms;
            List parms    = partArray[i].params;
            for (int j = 0; j < numParms; j++) {
                DMParam dmp = (DMParam) parms.get(j);
                dmp.kscale = DM_RINT(iread++);
            }
        }
        // read the offset
        for (int i = 0; i < numParts; i++) {
            int  numParms = partArray[i].kparms;
            List parms    = partArray[i].params;
            for (int j = 0; j < numParms; j++) {
                DMParam dmp = (DMParam) parms.get(j);
                dmp.koffst = DM_RINT(iread++);
            }
        }
        // read the nbits
        for (int i = 0; i < numParts; i++) {
            int  numParms = partArray[i].kparms;
            List parms    = partArray[i].params;
            for (int j = 0; j < numParms; j++) {
                DMParam dmp = (DMParam) parms.get(j);
                dmp.kbits = DM_RINT(iread++);
            }
        }
        parts = new ArrayList<DMPart>(numParts);
        for (int i = 0; i < numParts; i++) {
            parts.add(partArray[i]);
        }
        for (DMPart part : parts) {
            if (part.ktyprt == MDRPCK) {
                part.packInfo = new PackingInfo(part);
            }
        }
    }

    /**
     * Get the byte offset in 0 based space from a 1 based 4 byte
     * FORTRAN word.
     * @param fortranWord    1 based word offset
     * @return byte offset to that word
     */
    public static long getOffset(int fortranWord) {
        return (fortranWord - 1) * 4l;
    }

    /**
     * Run the program
     *
     * @param args  filename
     *
     * @throws IOException problem reading the file
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("need to supply a GEMPAK grid file name");
            System.exit(1);
        }

        GempakFileReader gfr = getInstance(getFile(args[0]), true);
        gfr.printFileLabel();
        gfr.printKeys();
        gfr.printHeaders();
        gfr.printParts();

    }

    /**
     * Class to mimic the GEMPAK DMLABL common block
     */
    protected class DMLabel {

        /** File identifier */
        public static final String DMLABEL = "GEMPAK DATA MANAGEMENT FILE ";

        /** version number */
        public int kversn;

        /** # file headers */
        public int kfhdrs;

        /** ptr to file keys */
        public int kpfile;

        /** # of rows */
        public int krow;

        /** # row keys */
        public int krkeys;

        /** ptr to row keys */
        public int kprkey;

        /** ptr to row headers */
        public int kprowh;

        /** # of cols */
        public int kcol;

        /** # col keys */
        public int kckeys;

        /** ptr to col keys */
        public int kpckey;

        /** ptr to col headers */
        public int kpcolh;

        /** # of parts */
        public int kprt;

        /** # part info */
        public int kppart;

        /** ptr to dmg rec */
        public int kpdmgt;

        /** length of dmg rec */
        public int kldmgt;

        /** ptr to data */
        public int kpdata;

        /** file type */
        public int kftype;

        /** file source */
        public int kfsrce;

        /** machine data type */
        public int kmachn;

        /** int missing data values */
        public int kmissd;

        /** double missing data values */
        public double smissd;

        /** swap byte flags */
        public boolean kvmst;

        /** ieee flags */
        public boolean kieeet;

        /**
         * Create a new DMLabel for the GempakFileReader
         */
        public DMLabel() {}

        /**
         * Read in all the info based on the block of integer words.
         * Modeled after DM_RLBL.
         *
         * @return true if okay.
         *
         * @throws IOException problem reading the file
         */
        public boolean init() throws IOException {
            if (rf == null) {
                throw new IOException("File is null");
            }

            rf.order(RandomAccessFile.BIG_ENDIAN);
            int mmmm = DM_RINT(26);
            if (mmmm > 100) {
                mmmm       = GempakUtil.swp4(mmmm);
                needToSwap = true;
            }
            //System.out.println("needToSwap = "  + needToSwap);
            kmachn = mmmm;
            mvmst  = (getByteOrder() == RandomAccessFile.BIG_ENDIAN);
            kvmst = ((kmachn == MTVAX) || (kmachn == MTULTX)
                     || (kmachn == MTALPH) || (kmachn == MTLNUX)
                     || (kmachn == MTIGPH));

            //      Set the file values of the missing data values to the current
            //      system values so that random values will not be converted.
            kmissd = IMISSD;
            smissd = RMISSD;


            String label = DM_RSTR(1, 28);
            //System.out.println("label = " + label);
            if ( !label.equals(DMLABEL)) {
                return false;
            }

            int[] words = new int[23];
            DM_RINT(8, words);
            kversn = words[0];
            kfhdrs = words[1];
            kpfile = words[2];
            krow   = words[3];
            krkeys = words[4];
            kprkey = words[5];
            kprowh = words[6];
            kcol   = words[7];
            kckeys = words[8];
            kpckey = words[9];
            kpcolh = words[10];
            kprt   = words[11];
            kppart = words[12];
            kpdmgt = words[13];
            kldmgt = words[14];
            kpdata = words[15];
            kftype = words[16];
            kfsrce = words[17];
            //kmachn = words[18];  // set above
            kmissd = words[19];
            smissd = DM_RFLT(31);
            return true;

        }

        /**
         * Get a String representation of this.
         * @return a String representation of this.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();

            buf.append("GEMPAK file label:\n");
            buf.append("\tVersion: " + kversn + "\n");
            buf.append("\t# File keys: " + kfhdrs + "\n");
            buf.append("\tptr to file keys: " + kpfile + "\n");
            buf.append("\t# rows: " + krow + "\n");
            buf.append("\t# row keys: " + krkeys + "\n");
            buf.append("\tptr to row  keys: " + kprkey + "\n");
            buf.append("\tprt to row header: " + kprowh + "\n");
            buf.append("\t# cols: " + kcol + "\n");
            buf.append("\t# cols keys: " + kckeys + "\n");
            buf.append("\tptr to col keys: " + kpckey + "\n");
            buf.append("\tptr to col header: " + kpcolh + "\n");
            buf.append("\t# parts: " + kprt + "\n");
            buf.append("\tptr part info: " + kppart + "\n");
            buf.append("\tptr to data mgmt record: " + kpdmgt + "\n");
            buf.append("\tlen of data mgmt record: " + kldmgt + "\n");
            buf.append("\tdata pointer: " + kpdata + "\n");
            buf.append("\tfile type: " + kftype + "\n");
            buf.append("\tfile source: " + kfsrce + "\n");
            buf.append("\tmachine type: " + kmachn + "\n");
            buf.append("\tinteger missing value: " + kmissd + "\n");
            buf.append("\tfloat missing value: " + smissd + "\n");
            buf.append("\tswap? " + needToSwap);
            return buf.toString();

        }

    }

    /**
     * Class to hold the DM File header info
     */
    protected class DMFileHeaderInfo {

        /** file header name */
        public String kfhnam;

        /** file header length */
        public int kfhlen;

        /** file header type */
        public int kfhtyp;

        /**
         * Default ctor
         */
        public DMFileHeaderInfo() {}

        /**
         * Get a String representation of this object.
         * @return a String representation of this object.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("Name = ");
            buf.append(kfhnam);
            buf.append("; length = ");
            buf.append(kfhlen);
            buf.append("; type = ");
            buf.append(kfhtyp);
            return buf.toString();
        }
    }

    /**
     * Class to mimic the DMKEYS common block.
     */
    protected class DMPart {

        /** part name */
        public String kprtnm;

        /** header length */
        public int klnhdr;

        /** data type */
        public int ktyprt;

        /** number of params */
        public int kparms;

        /** list of params */
        public List<DMParam> params;

        /** packing number */
        public int kpkno;

        /** length of packed rec */
        public int kwordp;

        /** packing info */
        public PackingInfo packInfo = null;;

        /**
         * Default ctor
         */
        public DMPart() {}

        /**
         * Get a String representation of this object.
         * @return a String representation of this object.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("Part Name = ");
            buf.append(kprtnm);
            buf.append("; header length = ");
            buf.append(klnhdr);
            buf.append("; type = ");
            buf.append(ktyprt);
            buf.append("; packing num = ");
            buf.append(kpkno);
            buf.append("; packed rec len = ");
            buf.append(kwordp);
            buf.append("\nParameters: ");
            if ((params != null) && !params.isEmpty()) {
                for (int i = 0; i < params.size(); i++) {
                    buf.append("\n  " + params.get(i));
                }
            }
            return buf.toString();
        }
    }

    /**
     * Class to hold DM Parameter info
     */
    protected class DMParam {

        /** part name */
        public String kprmnm;

        /** scaling terms */
        public int kscale;

        /** offset terms */
        public int koffst;

        /** # bits */
        public int kbits;

        /**
         * Default ctor
         */
        public DMParam() {}

        /**
         * Get a String representation of this object.
         * @return a String representation of this object.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("Param name = ");
            buf.append(kprmnm);
            buf.append("; scale = ");
            buf.append(kscale);
            buf.append("; offset = ");
            buf.append(koffst);
            buf.append("; bits = ");
            buf.append(kbits);
            return buf.toString();
        }
    }

    /**
     * Class to hold DM Integer packing info
     */
    protected class PackingInfo {

        /** offsets */
        public int[] koffst;

        /** n bits */
        public int[] nbitsc;

        /** scaling */
        public double[] scalec;

        /** missing */
        public int[] imissc;

        /** missing */
        public int[] iswrdc;

        /** missing */
        public int[] isbitc;

        /**
         * Set the packing terms for this part
         *
         * @param part The part for the packing
         */
        public PackingInfo(DMPart part) {
            List<DMParam> params    = part.params;
            int           numParams = params.size();
            koffst = new int[numParams];
            nbitsc = new int[numParams];
            scalec = new double[numParams];
            imissc = new int[numParams];
            iswrdc = new int[numParams];
            isbitc = new int[numParams];
            int i      = 0;
            int itotal = 0;
            for (DMParam param : params) {
                koffst[i]   = param.koffst;
                nbitsc[i]   = param.kbits;
                scalec[i]   = Math.pow(10, param.kscale);
                imissc[i]   = mskpat >>> (32 - param.kbits);
                iswrdc[i]   = (itotal / 32);
                isbitc[i++] = (itotal % 32) + 1;
                itotal      += param.kbits;
            }
            part.kwordp = (itotal - 1) / 32 + 1;
        }

        /**
         * Get a String representation of this object.
         * @return a String representation of this object.
         * public String toString() {
         *   StringBuffer buf = new StringBuffer();
         *   buf.append("Param name = ");
         *   buf.append(kprmnm);
         *   buf.append("; scale = ");
         *   buf.append(kscale);
         *   buf.append("; offset = ");
         *   buf.append(koffst);
         *   buf.append("; bits = ");
         *   buf.append(kbits);
         *   return buf.toString();
         * }
         */
    }

    /**
     * Class to hold information about a key.
     */
    protected class Key {

        /** the key name */
        public String name;

        /** the key location */
        public int loc;

        /** the key type (ROW or COL) */
        public String type;

        /**
         * Create a new key
         *
         * @param name  the name
         * @param loc   the location
         * @param type  the type
         */
        public Key(String name, int loc, String type) {
            this.name = name;
            this.loc  = loc;
            this.type = type;
        }
    }

    /**
     * Class to mimic the DMKEYS common block.
     */
    protected class DMKeys {

        /** row keys */
        public List<Key> kkrow;

        /** col keys */
        public List<Key> kkcol;

        /**
         * Default ctor
         */
        public DMKeys() {}

        /**
         * Get a String representation of this object.
         * @return a String representation of this object.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("\nKeys:\n");
            buf.append("  row keys = ");
            for (Key key : keys.kkrow) {
                buf.append(key.name);
                buf.append(", ");
            }
            buf.append("\n");
            buf.append("  column keys = ");
            for (Key key : keys.kkcol) {
                buf.append(key.name);
                buf.append(", ");
            }
            return buf.toString();
        }
    }

    /**
     * Find a key with the given name
     *
     * @param name  the name of the key
     *
     * @return the key or null;
     */
    public Key findKey(String name) {
        if (keys == null) {
            return null;
        }
        // search rows
        for (Key key : keys.kkrow) {
            if (key.name.equals(name)) {
                return key;
            }
        }
        // search columns
        for (Key key : keys.kkcol) {
            if (key.name.equals(name)) {
                return key;
            }
        }
        return null;

    }

    /**
     * Class to mimic the DMHDRS common block.
     */
    protected class DMHeaders {

        /** last valid row */
        public int lstrw = 0;

        /** last valid column */
        public int lstcl = 0;

        /** row keys */
        public List<int[]> rowHeaders;

        /** column keys */
        public List<int[]> colHeaders;

        /**
         * Default ctor
         */
        public DMHeaders() {}

        /**
         * Get a String representation of this object.
         * @return a String representation of this object.
         */
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("\nHeaders:\n");
            if (rowHeaders != null) {
                buf.append("  num row headers = ");
                buf.append(rowHeaders.size());
                buf.append("\n");
            }
            buf.append("  last row = ");
            buf.append(lstrw);
            buf.append("\n");
            if (colHeaders != null) {
                buf.append("  num column headers = ");
                buf.append(colHeaders.size());
                buf.append("\n");
            }
            buf.append("  last column = ");
            buf.append(lstcl);
            return buf.toString();
        }
    }

    /**
     * Find the file header with this name
     * @param name   name of header
     * @return headerinfo or null if not found
     */
    public DMFileHeaderInfo findFileHeader(String name) {
        if ((fileHeaderInfo == null) || fileHeaderInfo.isEmpty()) {
            return null;
        }
        for (Iterator iter = fileHeaderInfo.iterator(); iter.hasNext(); ) {
            DMFileHeaderInfo fhi = (DMFileHeaderInfo) iter.next();
            if (name.equals(fhi.kfhnam)) {
                return fhi;
            }
        }
        return null;
    }

    /**
     * Read in the values for the file header
     * @param name   name of header
     * @return values or null if not found
     *
     * @throws IOException   problem reading file
     */
    public float[] getFileHeader(String name) throws IOException {
        DMFileHeaderInfo fh = findFileHeader(name);
        if ((fh == null) || (fh.kfhtyp != MDREAL)) {
            return null;
        }
        int knt   = fileHeaderInfo.indexOf(fh);  // 0 based
        int iread = dmLabel.kpfile + 3 * dmLabel.kfhdrs;
        for (int i = 0; i < knt; i++) {
            DMFileHeaderInfo fhi = fileHeaderInfo.get(i);
            iread = iread + fhi.kfhlen + 1;
        }
        int nword = DM_RINT(iread);
        if (nword <= 0) {
            logError("Invalid header length for " + name);
            return null;
        }
        iread++;
        float[] rheader = new float[nword];
        if (name.equals("NAVB") && needToSwap) {
            DM_RFLT(iread, 1, rheader, 0);
            needToSwap = false;
            iread++;
            DM_RFLT(iread, 1, rheader, 1);
            needToSwap = true;
            iread++;
            DM_RFLT(iread, nword - 2, rheader, 2);

        } else {
            DM_RFLT(iread, rheader);
        }
        return rheader;
    }


    /**
     * Log an error
     *
     * @param errMsg message to log
     */
    protected void logError(String errMsg) {
        errorMessage = errMsg;
    }

    /**
     * Print the file label
     */
    public void printFileLabel() {
        if (dmLabel == null) {
            return;
        }
        System.out.println(dmLabel);
    }

    /**
     * Print the row and column keys
     */
    public void printKeys() {
        if (keys == null) {
            return;
        }
        System.out.println(keys);
    }

    /**
     * Print the row and column keys
     */
    public void printHeaders() {
        if (headers == null) {
            return;
        }
        System.out.println(headers);
    }

    /**
     * Print the part information
     */
    public void printParts() {
        if (parts == null) {
            return;
        }
        for (int i = 0; i < parts.size(); i++) {
            System.out.println("\nParts[" + i + "]:");
            System.out.println(parts.get(i));
        }
    }

    /**
     * Find the part with the particular name.
     * @param name  name of part to find
     * @return part number.
     */
    public int getPartNumber(String name) {
        int part = 0;
        if ((parts != null) && !parts.isEmpty()) {
            for (int i = 0; i < parts.size(); i++) {
                String partName = ((DMPart) parts.get(i)).kprtnm;
                if (partName.equals(name)) {
                    // gotta add 1 because parts are 1 based
                    part = i + 1;
                    break;
                }
            }
        }
        return part;
    }

    /**
     * Find the part with the particular name.
     * @param name  name of part to find
     * @return part or null if not found
     */
    public DMPart getPart(String name) {
        if ((parts != null) && !parts.isEmpty()) {
            for (int i = 0; i < parts.size(); i++) {
                DMPart part     = (DMPart) parts.get(i);
                String partName = part.kprtnm;
                if (partName.equals(name)) {
                    return part;
                }
            }
        }
        return null;
    }

    /**
     * Get the pointer to the data.  Taken from DM_RDTR
     * @param  irow  row number
     * @param  icol  column number
     * @param  partName  name of the part
     * @return  word (1 based) of start of data.
     */
    public int getDataPointer(int irow, int icol, String partName) {
        int ipoint = -1;
        if ((irow < 1) || (irow > dmLabel.krow) || (icol < 1)
                || (icol > dmLabel.kcol)) {
            System.out.println("bad row or column number: " + irow + "/"
                               + icol);
            return ipoint;
        }
        int iprt = getPartNumber(partName);
        if (iprt == 0) {
            System.out.println("couldn't find part");
            return ipoint;
        }
        // gotta subtract 1 because parts are 1 but List is 0 based
        DMPart part = (DMPart) parts.get(iprt - 1);
        // check for valid data type
        if ((part.ktyprt != MDREAL) && (part.ktyprt != MDGRID)
                && (part.ktyprt != MDRPCK)) {
            System.out.println("Not a valid type");
            return ipoint;
        }
        int ilenhd = part.klnhdr;
        ipoint = dmLabel.kpdata + (irow - 1) * dmLabel.kcol * dmLabel.kprt
                 + (icol - 1) * dmLabel.kprt + (iprt - 1);
        return ipoint;
    }

    /**
     * Read an integer
     * @param word   word in file (1 based) to read
     *
     * @return  int read
     *
     * @throws IOException   problem reading file
     */
    public int DM_RINT(int word) throws IOException {
        if (rf == null) {
            throw new IOException("DM_RINT: no file to read from");
        }
        if (dmLabel == null) {
            throw new IOException("DM_RINT: reader not initialized");
        }
        rf.seek(getOffset(word));
        // set the order
        if (needToSwap) {
            //if ((dmLabel.kmachn != MTMACH) &&
            //   ((dmLabel.kvmst && ! mvmst) ||
            //   (mvmst && !dmLabel.kvmst))) {
            rf.order(RandomAccessFile.LITTLE_ENDIAN);  // swap
        } else {
            rf.order(RandomAccessFile.BIG_ENDIAN);
        }
        int idata = rf.readInt();
        if (IMISSD != dmLabel.kmissd) {
            if (idata == dmLabel.kmissd) {
                idata = IMISSD;
            }
        }
        rf.order(RandomAccessFile.BIG_ENDIAN);
        return idata;
    }

    /**
     * Convenience method to fully read into an array of ints
     * @param word word in file (1 based) to read
     * @param iarray   array to read into
     *
     *
     * @throws IOException   problem reading file
     */
    public void DM_RINT(int word, int[] iarray) throws IOException {
        DM_RINT(word, iarray.length, iarray, 0);
    }

    /**
     * Read into an array of ints.
     * @param word word in file (1 based) to read
     * @param iarray   array to read into
     * @param start  starting word in the array (0 based)
     * @param num    number of words to read
     *
     * @throws IOException   problem reading file
     */
    public void DM_RINT(int word, int num, int[] iarray, int start)
            throws IOException {

        for (int i = 0; i < num; i++) {
            if (start + i > iarray.length) {
                throw new IOException(
                    "DM_RINT: start+num exceeds iarray length");
            }
            iarray[start + i] = DM_RINT(word + i);
        }

    }

    /**
     * Read a float
     * @param word   word in file (1 based) to read
     *
     * @return float read
     *
     * @throws IOException   problem reading file
     */
    public float DM_RFLT(int word) throws IOException {
        if (rf == null) {
            throw new IOException("DM_RFLT: no file to read from");
        }
        if (dmLabel == null) {
            throw new IOException("DM_RFLT: reader not initialized");
        }
        rf.seek(getOffset(word));
        if (needToSwap) {
            // set the order
            //if ((dmLabel.kmachn != MTMACH) &&
            //   ((dmLabel.kvmst && ! mvmst) ||
            //   (mvmst && !dmLabel.kvmst))) {
            rf.order(RandomAccessFile.LITTLE_ENDIAN);  // swap
        } else {
            rf.order(RandomAccessFile.BIG_ENDIAN);
        }
        float rdata = rf.readFloat();
        if (RMISSD != dmLabel.smissd) {
            if (Math.abs(rdata - dmLabel.smissd) < RDIFFD) {
                rdata = RMISSD;
            }
        }
        // reset to read normally
        rf.order(RandomAccessFile.BIG_ENDIAN);
        return rdata;
    }

    /**
     * Convenience method to fully read into an array of floats
     * @param word word in file (1 based) to read
     * @param rarray   array to read into
     *
     *
     * @throws IOException   problem reading file
     */
    public void DM_RFLT(int word, float[] rarray) throws IOException {
        DM_RFLT(word, rarray.length, rarray, 0);
    }

    /**
     * Read into an array of ints.
     * @param word word in file (1 based) to read
     * @param num    number of words to read
     * @param rarray   array to read into
     * @param start  starting word in the array (0 based)
     *
     * @throws IOException   problem reading file
     */
    public void DM_RFLT(int word, int num, float[] rarray, int start)
            throws IOException {

        for (int i = 0; i < num; i++) {
            if (start + i > rarray.length) {
                throw new IOException(
                    "DM_RFLT: start+num exceeds rarray length");
            }
            rarray[start + i] = DM_RFLT(word + i);
        }

    }

    /**
     * Read a 4-byte String
     * @param isword   offset in file (1 based FORTRAN word)
     *
     * @return String read
     *
     * @throws IOException   problem reading file
     */
    public String DM_RSTR(int isword) throws IOException {
        return DM_RSTR(isword, 4);
    }

    /**
     * Read a String
     * @param isword   offset in file (1 based FORTRAN word)
     * @param nchar    number of characters to read
     *
     * @return String read
     *
     * @throws IOException   problem reading file
     */
    public String DM_RSTR(int isword, int nchar) throws IOException {
        if (rf == null) {
            throw new IOException("DM_RSTR: no file to read from");
        }
        rf.seek(getOffset(isword));
        return rf.readString(nchar);
    }


    /**
     * Read the data
     *
     * @param  irow  row to read
     * @param  icol  column to read
     * @param  partName  part name
     *
     * @return  the header and data array  or null;
     *
     * @throws IOException problem reading file
     */
    public RData DM_RDTR(int irow, int icol, String partName)
            throws IOException {
        return DM_RDTR(irow, icol, partName, 1);
    }

    /**
     * Read the real (float) data
     *
     * @param  irow  row to read
     * @param  icol  column to read
     * @param  partName  part name
     * @param  decimalScale scaling factor (power of 10);
     *
     * @return  the header and data array  or null;
     *
     * @throws IOException problem reading file
     */
    public RData DM_RDTR(int irow, int icol, String partName,
                         int decimalScale)
            throws IOException {

        int ipoint = -1;
        if ((irow < 1) || (irow > dmLabel.krow) || (icol < 1)
                || (icol > dmLabel.kcol)) {
            System.out.println("bad row/column number " + irow + "/" + icol);
            return null;
        }
        //System.out.println("reading row " + irow + ", column " + icol);
        int iprt = getPartNumber(partName);
        if (iprt == 0) {
            System.out.println("couldn't find part: " + partName);
            return null;
        }
        // gotta subtract 1 because parts are 1 but List is 0 based
        DMPart part = (DMPart) parts.get(iprt - 1);
        // check for valid real data type
        if ((part.ktyprt != MDREAL) && (part.ktyprt != MDGRID)
                && (part.ktyprt != MDRPCK)) {
            System.out.println("Not a valid type");
            return null;
        }
        int ilenhd = part.klnhdr;
        ipoint = dmLabel.kpdata + (irow - 1) * dmLabel.kcol * dmLabel.kprt
                 + (icol - 1) * dmLabel.kprt + (iprt - 1);

        float[] rdata  = null;
        int[]   header = null;
        int     istart = DM_RINT(ipoint);
        if (istart == 0) {
            return null;
        }
        // start catching up here because some files are incorrectly written
        try {
            int length = DM_RINT(istart);
            int isword = istart + 1;
            if (length <= ilenhd) {
                //System.out.println("length (" + length
                //                   + ") is less than header length (" + ilenhd
                //                   + ")");
                return null;
            } else if (Math.abs(length) > 10000000) {
                //System.out.println("length is huge");
                return null;
            }
            header = new int[ilenhd];
            DM_RINT(isword, header);
            int nword = length - ilenhd;
            isword += header.length;
            if (part.ktyprt == MDREAL) {
                rdata = new float[nword];
                DM_RFLT(isword, rdata);
            } else if (part.ktyprt == MDGRID) {
                rdata = DM_RPKG(isword, nword, decimalScale);
            } else {  //  packed ints
                int[] idata = new int[nword];
                DM_RINT(isword, idata);
                rdata = DM_UNPK(part, idata);
            }
        } catch (EOFException eof) {
            //System.err.println("reading off end of file");
            rdata = null;
        }
        RData rd = null;
        if (rdata != null) {
            rd = new RData(header, rdata);
        }

        return rd;

    }



    /**
     * Unpack an array of packed integers.
     *
     * @param part the part with packing info
     * @param ibitst packed integer bit string
     *
     * @return unpacked ints as floats
     */
    public float[] DM_UNPK(DMPart part, int[] ibitst) {
        int nparms = part.kparms;
        int nwordp = part.kwordp;
        int npack  = (int) (ibitst.length - 1) / nwordp + 1;
        if (npack * nwordp != ibitst.length) {
            //logError("number of packed records not correct");
            // System.out.println("number of packed records not correct: "
            //                   + npack * nwordp + " vs. " + ibitst.length);
            return null;
        }
        float[]     data  = new float[nparms * npack];
        PackingInfo pkinf = part.packInfo;
        int         ir    = 0;
        int         ii    = 0;
        for (int pack = 0; pack < npack; pack++) {
            //
            //  Move bitstring into internal words.  TODO: necessary?
            //
            int[] jdata = new int[nwordp];
            for (int i = 0; i < nwordp; i++) {
                jdata[i] = ibitst[ii + i];
            }

            //
            //  Extract each data value.
            //
            for (int idata = 0; idata < nparms; idata++) {

                //
                //  Extract correct bits from words using shift and mask
                //  operations.
                //
                int jbit   = pkinf.nbitsc[idata];
                int jsbit  = pkinf.isbitc[idata];
                int jshift = 1 - jsbit;
                int jsword = pkinf.iswrdc[idata];
                int jword  = jdata[jsword];
                // use >>> to shift avoid carrying sign along
                int mask   = mskpat >>> (32 - jbit);
                int ifield = jword >>> Math.abs(jshift);
                ifield = ifield & mask;
                if ((jsbit + jbit - 1) > 32) {
                    jword  = jdata[jsword + 1];
                    jshift = jshift + 32;
                    int iword = jword << jshift;
                    iword  = iword & mask;
                    ifield = ifield | iword;
                }
                //
                //  The integer data is now in ifield.  Use the scaling and
                //  offset terms to convert to REAL data.
                //
                if (ifield == pkinf.imissc[idata]) {
                    data[ir + idata] = RMISSD;
                } else {
                    data[ir + idata] = (ifield + pkinf.koffst[idata])
                                       * (float) pkinf.scalec[idata];
                }
            }
            ir += nparms;
            ii += nwordp;
        }
        return data;

    }

    /**
     * Get a bit string for an integer
     *
     * @param b  the integer
     * @return a bit string (e.g.: 01100001|11000000|10011010|10110100|)
     */
    protected static String getBits(int b) {
        String s = "";
        for (int i = 31; i >= 0; i--) {
            if ((b & (1 << i)) != 0) {
                s = s + "1";
            } else {
                s = s + "0";
            }
            if (i % 8 == 0) {
                s = s + "|";
            }
        }
        return s;
    }

    /**
     * subclass should implement
     *
     * @param isword starting word (1 based)
     * @param nword  number of words to read
     * @param decimalScale  decimal scale
     *
     * @return  returns null unless subclass overrides
     *
     * @throws IOException  problem reading data
     */
    public float[] DM_RPKG(int isword, int nword, int decimalScale)
            throws IOException {
        return null;
    }

    /**
     * A class to hold real (float) data
     *
     * @author Unidata Development Team
     */
    public class RData {

        /** the header */
        public int[] header;

        /** the data */
        public float[] data;

        /**
         * Create a new holder for the header and data
         *
         * @param header  the header
         * @param data    the data
         */
        public RData(int[] header, float[] data) {
            this.header = header;
            this.data   = data;
        }
    }


}

