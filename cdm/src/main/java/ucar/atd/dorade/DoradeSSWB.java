/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.atd.dorade;

import ucar.nc2.constants.CDM;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

class DoradeSSWB extends DoradeDescriptor {

    private static class KeyTable {
        public int offset;
        public int size;
        public int type;

        //
        // legal values for type
        //
        public static final int KEYED_BY_TIME = 1;
        public static final int KEYED_BY_ROT_ANG = 2;
        public static final int SOLO_EDIT_SUMMARY = 3;

        public KeyTable(int offset, int size, int type) {
            this.offset = offset;
            this.size = size;
            this.type = type;
        }

        private byte[] readTable(RandomAccessFile raf) throws IOException {
            raf.seek(this.offset);
            byte[] ret = new byte[this.size];
            if (raf.read(ret) != ret.length)
                throw new IOException("Error reading KeyTable");
            return ret;
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

    public DoradeSSWB(RandomAccessFile file, boolean littleEndianData) throws DescriptorException {
        byte[] data = readDescriptor(file, littleEndianData, "SSWB");

        //
        // unpack
        //

        if (data == null) {
            throw new IllegalStateException("SSWB data null");
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
            radarName = new String(data, 36, 8, CDM.utf8Charset).trim();

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
        StringBuilder s = new StringBuilder("SSWB\n");
        s.append("  last use: ").append(formatDate(lastUseTime)).append("\n");
        s.append("  start time: ").append(formatDate(startTime)).append("\n");
        s.append("  end time: ").append(formatDate(endTime)).append("\n");
        s.append("  file size: ").append(fileSize).append("\n");
        s.append("  compression flag: ").append(compressionFlag).append("\n");
        s.append("  volume time: ").append(formatDate(volumeTime)).append("\n");
        s.append("  number of params: ").append(nParams).append("\n");
        s.append("  radar name: ").append(radarName).append("\n");
        s.append("  SSWB version: ").append(version).append("\n");
        s.append("  status: ").append(status).append("\n");
        s.append("  number of key tables: ").append(nKeyTables).append("\n");
        for(KeyTable k: keyTables)
          s.append("  key table type contained: ").append(k.type).append("\n");
        return s.toString();
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