package ucar.nc2.ft.point.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.ft.point.PointIteratorEmpty;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.units.DateUnit;
import ucar.units.UnitException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract superclass for creating a {@link ucar.nc2.ft.PointFeatureCollection} from a point stream.
 * Subclasses must implement {@link #getInputStream()}.
 *
 * @author cwardgar
 * @since 2014/10/02
 */
public abstract class PointCollectionStreamAbstract extends PointCollectionImpl {
    private static final Logger logger = LoggerFactory.getLogger(PointCollectionStreamAbstract.class);

    // True if we must retrieve values for timeUnit and altUnits from the stream.
    private boolean needUnits;

    /**
     * Creates a feature collection with the specified name. {@link #getTimeUnit} and {@link #getAltUnits} will return
     * default values until {@link #getPointFeatureIterator} is called for the first time.
     *
     * @param name  the name of this feature collection. May be null.
     */
    public PointCollectionStreamAbstract(String name) {
        super(name, DateUnit.getUnixDateUnit(), null);  // Default values. altUnits can be null; timeUnit must not be.
        this.needUnits = true;  // Client did not provide explicit values for timeUnit and altUnits.
    }

    /**
     * Creates a feature collection with the specified name, time unit, and altitude unit.
     *
     * @param name      the name of this feature collection. May be null.
     * @param timeUnit  the time unit. May not be null.
     * @param altUnits  the UDUNITS altitude unit string. May be null.
     */
    public PointCollectionStreamAbstract(String name, DateUnit timeUnit, String altUnits) {
        super(name, timeUnit, altUnits);
        this.needUnits = false;  // Client provided explicit values for timeUnit and altUnits.
    }

    /**
     * Returns the input stream from which to read point data.
     *
     * @return the input stream from which to read point data.
     * @throws IOException if an I/O error occurs.
     */
    public abstract InputStream getInputStream() throws IOException;

    @Override
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
        InputStream in = getInputStream();
        boolean leaveStreamOpen = false;

        try {
            PointStream.MessageType mtype = PointStream.readMagic(in);

            if (mtype == PointStream.MessageType.PointFeatureCollection) {
                int len = NcStream.readVInt(in);
                byte[] data = new byte[len];
                NcStream.readFully(in, data);

                PointStreamProto.PointFeatureCollection pfc = PointStreamProto.PointFeatureCollection.parseFrom(data);

                if (needUnits) {
                    try {
                        this.altUnits = pfc.hasAltUnit() ? pfc.getAltUnit() : null;
                        this.timeUnit = new DateUnit(pfc.getTimeUnit());
                    } catch (UnitException e) {
                        String message = String.format("Invalid time unit found in stream (%s). Using default (%s).",
                                pfc.getTimeUnit(), this.timeUnit.getUnitsString());
                        logger.error(message, e);
                        // Default value for timeUnit will remain.
                    }

                    needUnits = false;
                }

                PointFeatureIterator iter = new PointIteratorStream(in, new PointStream.ProtobufPointFeatureMaker(pfc));
                iter.setCalculateBounds(this);

                leaveStreamOpen = true;  // It is now iter's responsiblity to close the stream.
                return iter;
            } else if (mtype == PointStream.MessageType.End) {
                return new PointIteratorEmpty(); // nothing in the iteration
            } else if (mtype == PointStream.MessageType.Error) {
                int len = NcStream.readVInt(in);
                byte[] data = new byte[len];
                NcStream.readFully(in, data);

                NcStreamProto.Error proto = NcStreamProto.Error.parseFrom(data);
                throw new IOException(NcStream.decodeErrorMessage(proto));
            } else {
                throw new IOException("Illegal pointstream message type= " + mtype); // maybe kill the socket ?
            }
        } finally {
            if (!leaveStreamOpen) {
                in.close();
            }
        }
    }
}
