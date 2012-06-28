package ucar.nc2.iosp.hdf5;

import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 6/27/12
 */
public class H5deconstructor {

  public void deflate(Formatter f, Variable v) throws IOException {
    H5header.Vinfo vinfo = (H5header.Vinfo) v.getSPobject();
    DataBTree btree = vinfo.btree;
    if (btree == null || vinfo.useFillValue) {
      f.format("%s not chunked%n", v.getShortName());
      return;
    }
  }

  public void deflate(Formatter f, DataBTree btree) throws IOException {

    int count = 0;
    long total = 0;
    DataBTree.DataChunkIterator iter = btree.getDataChunkIteratorFilter(null);
    while (iter.hasNext()) {
      DataBTree.DataChunk dc = iter.next();
      total += dc.size;
      count++;
    }

  }
}
