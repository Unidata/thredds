package ucar.nc2.ft.point.remote;

import java.io.*;

/**
 * Creates a {@link ucar.nc2.ft.PointFeatureCollection} by reading point stream data from a local file.
 *
 * @author cwardgar
 * @since 2014/10/02
 */
public class PointCollectionStreamLocal extends PointCollectionStreamAbstract {
    private final File file;

    public PointCollectionStreamLocal(File file) {
        super(file.getAbsolutePath());
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }
}
