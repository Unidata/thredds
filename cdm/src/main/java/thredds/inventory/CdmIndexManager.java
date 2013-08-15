package thredds.inventory;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * abstract superclass for managing index files (ncx)
 *
 * @author caron
 * @since 8/14/13
 */
public abstract class CdmIndexManager {

    public abstract CollectionManager.ChangeChecker getChangeChecker();

    public abstract boolean readIndex(String location, long dataModified, CollectionManager.Force force) throws IOException;

    public abstract boolean makeIndex(String location, RandomAccessFile dataRaf) throws IOException;

}
