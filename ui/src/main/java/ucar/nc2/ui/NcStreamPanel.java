package ucar.nc2.ui;

import ucar.nc2.NetcdfFile;
import ucar.nc2.stream.NcStreamIosp;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;

/**
 * Class Description.
 *
 * @author caron
 * @since 7/8/12
 */
public class NcStreamPanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted messTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow2, infoWindow3;

  private RandomAccessFile raf;
  private NetcdfFile ncd;

  public NcStreamPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    PopupMenu varPopup;

    messTable = new BeanTableSorted(MessBean.class, (PreferencesExt) prefs.node("NcStreamPanel"), false);
    messTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        MessBean bean = (MessBean) messTable.getSelectedBean();
        if (bean == null) return;
        infoTA.setText(bean.getDesc());
      }
    });
    varPopup = new PopupMenu(messTable.getJTable(), "Options");
    varPopup.addAction("Show record -> variable data assignments", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessBean bean = (MessBean) messTable.getSelectedBean();
        if (bean == null) return;
        infoTA.setText(bean.getDesc());
      }
    });

    // the info windows
    infoTA = new TextHistoryPane();

    infoPopup2 = new TextHistoryPane();
    infoWindow2 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    infoPopup3 = new TextHistoryPane();
    infoWindow3 = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoPopup3);
    infoWindow3.setBounds((Rectangle) prefs.getBean("InfoWindowBounds3", new Rectangle(300, 300, 500, 300)));

    setLayout(new BorderLayout());

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, messTable, infoTA);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    add(split2, BorderLayout.CENTER);
  }

  public void save() {
    messTable.saveState(false);
    //prefs.putBeanObject("InfoWindowBounds3", infoWindow3.getBounds());
    if (split != null) prefs.putInt("splitPos", split.getDividerLocation());
    if (split2 != null) prefs.putInt("splitPos2", split2.getDividerLocation());
  }

  public void closeOpenFiles() throws IOException {
    if (ncd != null) ncd.close();
    ncd = null;
    raf = null;
  }

  public void showInfo(Formatter f) {
    if (ncd == null) return;
    try {
      f.format("%s length = %d%n", raf.getLocation(), raf.length()); // CDL
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    f.format("%s", ncd.toString()); // CDL
  }

  public void setNcStreamFIle(String filename) throws IOException {
    closeOpenFiles();

    java.util.List<MessBean> messages = new ArrayList<MessBean>();
    ncd = new MyNetcdfFile();
    NcStreamIosp iosp = new NcStreamIosp();
    try {
      raf = new RandomAccessFile(filename, "r");
      java.util.List<NcStreamIosp.NcsMess> ncm = iosp.open(raf, ncd);
      for (NcStreamIosp.NcsMess m : ncm) {
        messages.add(new MessBean(m));
      }

    } catch (Exception e) {
      e.printStackTrace();
      if (raf != null) raf.close();
    }

    messTable.setBeans(messages);
    //System.out.printf("mess = %d%n", messages.size());
  }


  static private class MyNetcdfFile extends NetcdfFile {
  }

  public class MessBean {
    private NcStreamIosp.NcsMess m;

    MessBean() {
    }

    MessBean(NcStreamIosp.NcsMess m) {
      this.m = m;
    }

    public String getObjClass() {
      return m.what.getClass().toString();
    }

    public String getDesc() {
      return m.what.toString();
    }

    public int getSize() {
      return m.len;
    }

    public int getNelems() {
      return m.nelems;
    }

    public long getFilePos() {
      return m.filePos;
    }

  }

}
