package ucar.nc2.stream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by rmay on 8/10/15.
 */
public class NcStreamCompression {
    NcStreamProto.Compress type;
    Object compressInfo;

    private NcStreamCompression(NcStreamProto.Compress type, Object info) {
        this.type = type;
        this.compressInfo = info;
    }

    private NcStreamCompression(NcStreamProto.Compress type) {
        this(type, null);
    }

    public static NcStreamCompression none() {
        return new NcStreamCompression(NcStreamProto.Compress.NONE);
    }

    public static NcStreamCompression deflate() {
        return deflate(-1);
    }

    public static NcStreamCompression deflate(int level) {
        return new NcStreamCompression(NcStreamProto.Compress.DEFLATE, level);
    }

    public OutputStream setupStream(OutputStream out, int size)
            throws IOException
    {
        switch (type) {
            // For compression (currently deflate) we compress the data, then
            // will write the block size, and then data, when the stream is closed.
            case DEFLATE:
                // limit level to range [-1, 9], where -1 is default deflate setting.
                int level = Math.min(Math.max((Integer)compressInfo, -1), 9);
                int bufferSize = Math.min(size / 2, 512 * 1024 * 1024);
                return new NcStreamCompressedOutputStream(out, bufferSize, level);

            default:
                System.out.printf(" Unknown compression type %s. Defaulting to none.%n", type);

            // In the case of no compression, go ahead and write the block
            // size so that the stream is ready for data
            case NONE:
                NcStream.writeVInt(out, size);
                return out;
        }
    }
}