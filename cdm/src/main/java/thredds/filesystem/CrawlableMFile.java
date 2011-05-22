package thredds.filesystem;

import net.jcip.annotations.Immutable;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.inventory.MCollection;
import thredds.inventory.MController;
import thredds.inventory.MFile;
import thredds.inventory.MFileFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * An adapter of cached MFiles to CrawlableDataset
 *
 * @author caron
 * @since 5/22/11
 */
@Immutable
public class CrawlableMFile implements MFile, thredds.crawlabledataset.CrawlableDataset {
  private final MController controller;
  private final MFile mfile;

  CrawlableMFile(MController controller, MFile mfile) {
    this.controller = controller;
    this.mfile = mfile;
  }

  CrawlableMFile(MController controller, String path) {
    this.controller = controller;
    this.mfile = null; // controller.getFromPath(path);  LOOK
  }

  // MFile

  @Override
  public long getLastModified() {
    return mfile.getLastModified();
  }

  @Override
  public long getLength() {
    return mfile.getLength();
  }

  @Override
  public boolean isDirectory() {
    return  mfile.isDirectory();
  }

  @Override
  public int compareTo(MFile o) {
    return  mfile.compareTo(o);
  }

  @Override
  public Object getAuxInfo() {
    return mfile.getAuxInfo();
  }

  @Override
  public void setAuxInfo(Object info) {
    mfile.setAuxInfo(info);
  }

  // CrawlableDataset

  @Override
  public Object getConfigObject() {
    return null;
  }

  @Override
  public String getPath() {
    return mfile.getPath();
  }

  @Override
  public String getName() {
    return mfile.getName();
  }

  @Override
  public CrawlableDataset getParentDataset() {
    String path = mfile.getPath();
    int pos = path.lastIndexOf("/");
    return new CrawlableMFile(controller, path.substring(0,pos));
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public boolean isCollection() {
    return mfile.isDirectory();
  }

  @Override
  public CrawlableDataset getDescendant(String relativePath) {
    return new CrawlableMFile(controller, mfile.getPath() + "/" + relativePath);
  }

  @Override
  public List<CrawlableDataset> listDatasets() throws IOException {
    return listDatasets(null);
  }

  @Override
  public List<CrawlableDataset> listDatasets(CrawlableDatasetFilter filter) throws IOException {
    MCollection mc = new MCollection(mfile.getPath(), mfile.getPath(), false, (MFileFilter) null, null);
    Iterator<MFile> iter = controller.getInventory(mc);
    List<CrawlableDataset> result = new ArrayList<CrawlableDataset>();
    while (iter.hasNext()) {
      CrawlableMFile crf = new CrawlableMFile(controller, iter.next());
      if (filter == null || filter.accept(crf))
        result.add(crf);
    }
    return result;
  }

  @Override
  public long length() {
    return getLength();
  }

  @Override
  public Date lastModified() {
    return new Date(getLastModified());
  }
}
