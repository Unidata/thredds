/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */


package ucar.nc2.iosp.gempak;


import ucar.unidata.io.RandomAccessFile;

import java.io.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Read a Gempak grid file
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
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

    /** key info */
    protected DMKeys keys;

    /** part info */
    protected List<DMPart> parts;

    /** the machine type byte order */
    protected int MTMACH = RandomAccessFile.BIG_ENDIAN;

    /** swap flag */
    protected boolean mvmst = false;

    /** swap flag */
    protected boolean needToSwap = false;

    /** file size */
    protected long fileSize = 0;

    /**
     * Bean ctor
     */
    public GempakFileReader() {}

    /**
     * Create a Gempak File Reader from the file
     *
     * @param filename  filename
     *
     * @throws IOException problem reading file
     */
    public GempakFileReader(String filename) throws IOException {
        this(new RandomAccessFile(filename, "r", 2048));
    }

    /**
     * Create a Gempak File Reader from the file
     *
     * @param raf file to read from
     *
     * @throws IOException problem reading file
     */
    public GempakFileReader(RandomAccessFile raf) throws IOException {
        init(raf, true);
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

        // TODO: Read the headers (DM_RPRT)

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
        int          num   = dmLabel.krkeys;
        List<String> rkeys = new ArrayList<String>(num);
        for (int i = 0; i < num; i++) {
            String key = DM_RSTR(dmLabel.kprkey + i);
            rkeys.add(key);
        }
        keys.kkrow = rkeys;
        num        = dmLabel.kckeys;
        List<String> ckeys = new ArrayList<String>(num);
        for (int i = 0; i < num; i++) {
            String key = DM_RSTR(dmLabel.kpckey + i);
            ckeys.add(key);
        }
        keys.kkcol = ckeys;
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
        //  TODO:  Set the packing?
        parts = new ArrayList<DMPart>(numParts);
        for (int i = 0; i < numParts; i++) {
            parts.add(partArray[i]);
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

        GempakFileReader gfr = new GempakFileReader(args[0]);
        gfr.printFileLabel();
        gfr.printKeys();
        gfr.printParts();

    }

    /**
     * Class to mimic the GEMPAK DMLABL common block
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
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

        /** data type */
        public int kparms;

        /** number  of parameters */
        public List<DMParam> params;

        /** packing number */
        public int kpkno;

        /** length of packed rec */
        public int kwordp;

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
     * Class to mimic the DMKEYS common block.
     */
    protected class DMKeys {

        /** row keys */
        public List<String> kkrow;

        /** col keys */
        public List<String> kkcol;

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
            buf.append(kkrow);
            buf.append("\n");
            buf.append("  column keys = ");
            buf.append(kkcol);
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
        if ((irow < 1) || (icol > dmLabel.krow) || (icol < 1)
                || (icol > dmLabel.kcol)) {
            System.out.println("bad row/column number");
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

}

