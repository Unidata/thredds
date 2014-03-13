/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.core.util;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

abstract public class DapDump
{
    //////////////////////////////////////////////////
    // Provide a simple dump of binary data
    // (Static method)

    //////////////////////////////////////////////////
    // Constants

    static int MAXLIMIT = 20000;
    //////////////////////////////////////////////////
    // Provide a simple dump of binary data

    static public void
    dumpbytes(ByteBuffer buf0, boolean skipdmr)
    {
        int savepos = buf0.position();
        int limit0 = buf0.limit();
        int skipcount = 0;
        if(limit0 > MAXLIMIT) limit0 = MAXLIMIT;
        if(skipdmr) {
            skipcount = buf0.getInt(); //dmr count
            skipcount &= 0xFFFFFF; // mask off the flags to get true count
            skipcount += 4; // skip the count also
        }
        byte[] bytes = new byte[(limit0+8)-skipcount];
        Arrays.fill(bytes, (byte) 0);
        buf0.position(savepos+skipcount);
        buf0.get(bytes,0,limit0-skipcount);
        buf0.position(savepos);

        ByteBuffer buf = ByteBuffer.wrap(bytes).order(buf0.order());

        int i=0;
        int stop = bytes.length - 8;
        try {
            for(i=0;buf.position() < stop;i++) {
                savepos = buf.position();
                int iv = buf.getInt(); buf.position(savepos);
                byte b = buf.get();
                long uv = ((long)iv) & 0xFFFFFFFFL;
                int ib = (int) b;
                int ub = (iv & 0xFF);
                char c = (char)ub;
                String s = Character.toString(c);
                if(c == '\r') s = "\\r";
                else if(c == '\n') s = "\\n";
                else if(c < ' ') s = "?";
                System.out.printf("[%03d] %02x %4d '%s'\t%12d 0x%08x\n", i, ub, ib, s, iv, uv);
            }
        } catch (Exception e) {
            System.out.println("failure:" + e);
        } finally {
            System.out.flush();
        }
    }


    //////////////////////////////////////////////////
    // Standalone

    static public void main(String[] argv)
    {
        try {
            FileInputStream f = new FileInputStream(argv[0]);
            byte[] content = DapUtil.readbinaryfile(f);
            ByteBuffer buf = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN);
            DapDump.dumpbytes(buf, false);
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }
}
