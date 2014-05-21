package thredds.server.ncss.view.dsg;

import thredds.server.ncss.exception.UnsupportedResponseFormatException;
import thredds.server.ncss.format.SupportedFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cwardgar on 2014/05/21.
 */
public abstract class DsgSubsetWriterFactory {
    private static final Map<SupportedFormat, DsgSubsetWriter> writerMap = new HashMap<>();
    static {
        // writerMap.put(SupportedFormat.XML_Stream, DsgSubsetWriterXml);
        // ...etc
    }

    public static DsgSubsetWriter getInstance(SupportedFormat format) throws UnsupportedResponseFormatException {
        DsgSubsetWriter writer = writerMap.get(format);
        if (writer == null) {
            throw new UnsupportedResponseFormatException("Unknown result type = " + format.getFormatName());
        } else {
            return writer;
        }
    }

    private DsgSubsetWriterFactory() { }
}
