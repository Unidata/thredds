package ucar.atd.dorade;

import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>Title: DoradeSSWB</p>
 * <p>Description: DORADE "super" sweep information block</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: University Corporation for Atmospheric Research</p>
 * @author Chris Burghart
 * @version $Revision: 1.1 $ $Date: 2004/12/09 20:18:52 $
 */
/* $Id: DoradeSSWB.java,v 1.1 2004/12/09 20:18:52 jeffmc Exp $ */

class DoradeSSWB extends DoradeDescriptor {

    private class KeyTable {
        public int offset;
        public int size;
        public int type;

        //
        // legal values for type
        //
        public final int KEYED_BY_TIME = 1;
        public final int KEYED_BY_ROT_ANG = 2;
        public final int SOLO_EDIT_SUMMARY = 3;

        public KeyTable(int offset, int size, int type) {
            this.offset = offset;
            this.size = size;
            this.type = type;
        }
    }

    private Date lastUseTime;
    private Date startTime;
    private Date endTime;
    private int fileSize;
    private int compressionFlag;
    private Date volumeTime;
    private int nParams;
    private String radarName;
    private int version;
    private int status;
    private int nKeyTables;
    private KeyTable[] keyTables;

    public DoradeSSWB(RandomAccessFile file, boolean littleEndianData)
            throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "SSWB");

        //
        // unpack
        //

        if (data == null) {
            System.out.println("SSWB data null");
            System.exit(1);
        }

        int intTime = grabInt(data, 8);
        lastUseTime = new Date((long)intTime * 1000);

        intTime = grabInt(data, 12);
        startTime = new Date((long)intTime * 1000);

        intTime = grabInt(data, 16);
        endTime = new Date((long)intTime * 1000);

        fileSize = grabInt(data, 20);
        compressionFlag = grabInt(data, 24);

        intTime = grabInt(data, 28);
        volumeTime = new Date((long)intTime * 1000);

        nParams = grabInt(data, 32);

        //
        // Old SSWB stops after the param count, new one has more stuff
        //
        if (data.length == 36) {
            radarName = "";
            version = 0;
            status = 0;
            nKeyTables = 0;
            keyTables = null;
        } else {
            radarName = new String(data, 36, 8).trim();

            //
            // AUUGGGGGHHHHHH!  Everything after this is sometimes offset
            // by an additional 4 bytes!  If the length of the descriptor is
            // 200, it's the additional offset version.  (EDITORIAL NOTE: It
            // may be easy to just use structures and compiler alignment when
            // writing these files, but it makes it really hard to define a
            // consistent data format.  Here we have a case in point.)
            //
            int optOffset = (data.length == 200) ? 4 : 0;

            //
            // more precise start and end times (to millisecond) may be here
            //
            double doubleTime = grabDouble(data, 44 + optOffset);
            if (doubleTime != 0.0)
                startTime.setTime((long)(doubleTime * 1000));

            doubleTime = grabDouble(data, 52 + optOffset);
            if (doubleTime != 0.0)
                endTime.setTime((long)(doubleTime * 1000));

            //
            // SSWB version, status, and key table count
            //
            version = grabInt(data, 60 + optOffset);
            nKeyTables = grabInt(data, 64 + optOffset);  // up to 8 key tables
            status = grabInt(data, 68 + optOffset);

            // 28 unused bytes after number of key tables

            //
            // key tables start at byte 100 (or 104, see above),
            // each is 12 bytes
            //
            keyTables = (nKeyTables > 0) ? new KeyTable[nKeyTables] : null;
            for (int i = 0; i < nKeyTables; i++) {
                int entrystart = 100 + 12 * i + optOffset;
                keyTables[i] = new KeyTable(grabInt(data, entrystart),
                                            grabInt(data, entrystart + 4),
                                            grabInt(data, entrystart + 8));
            }
        }

        //
        // debugging output
        //
        if (verbose)
            System.out.println(this);
    }

    public String toString() {
        String s = "SSWB\n";
        s += "  last use: " + formatDate(lastUseTime) + "\n";
        s += "  start time: " + formatDate(startTime) + "\n";
        s += "  end time: " + formatDate(endTime) + "\n";
        s += "  file size: " + fileSize + "\n";
        s += "  compression flag: " + compressionFlag + "\n";
        s += "  volume time: " + formatDate(volumeTime) + "\n";
        s += "  number of params: " + nParams + "\n";
        s += "  radar name: " + radarName + "\n";
        s += "  SSWB version: " + version + "\n";
        s += "  status: " + status + "\n";
        s += "  number of key tables: " + nKeyTables;
        return s;
    }

    /**
     * Get the start time for this sweep.
     * @return <code>Date</code> of the start of this sweep
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Get the end time for this sweep.
     * @return <code>Date</code> of the end of this sweep
     */
    public Date getEndTime() {
        return endTime;
    }
}