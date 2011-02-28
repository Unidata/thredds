package ucar.nc2.ui;


import ucar.nc2.NetcdfFile;
import ucar.nc2.ft.point.remote.PointStream;
import ucar.nc2.ft.point.remote.PointStreamProto;
import ucar.nc2.iosp.grib.tables.GribTemplate;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamIosp;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * Cdmremote iosp
 *
 * @author caron
 * @since Oct 25, 2010
 */
public class CdmremotePanel extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted messTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA, infoPopup2, infoPopup3;
  private IndependentWindow infoWindow2, infoWindow3;

  private Map<String, GribTemplate> templates = null;

  private NetcdfFile ncd;

  public CdmremotePanel(PreferencesExt prefs) {
    this.prefs = prefs;

    PopupMenu varPopup;

    messTable = new BeanTableSorted(MessBean.class, (PreferencesExt) prefs.node("CdmMessage"), false);
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

  static private class MyNetcdfFile extends NetcdfFile {
  }

  public void setNcStream(String stream) throws IOException {
    if (stream.startsWith("http:"))
      setStream(stream);
    else
      setFile(stream);
  }

  public void setStream(String stream) throws IOException {
    if (ncd != null) ncd.close();

    long total = 0;
    List<MessBean> messages = new ArrayList<MessBean>();
    InputStream is = null;
    try {
      System.out.printf("open %s%n", stream);
      is = IO.getInputStreamFromUrl(stream);
      while (true) {
        Mess mess = new Mess();
        mess.magic = PointStream.readMagic(is);
        messages.add(new MessBean2(mess));

        if (mess.magic == PointStream.MessageType.Eos)
          break;

        total += 4;
        if ((mess.magic == PointStream.MessageType.Start) || (mess.magic == PointStream.MessageType.End))
          continue;

        mess.vlen = NcStream.readVInt(is);
        byte[] m = new byte[mess.vlen];
        NcStream.readFully(is, m);
        total += mess.vlen;

       // Start, Header, Data, End, Error,
       // StationList, PointFeatureCollection, PointFeature

        switch (mess.magic) {
          case Header:
            mess.obj = NcStreamProto.Header.parseFrom(m);
            break;
          case Data:
            mess.obj = NcStreamProto.Data.parseFrom(m);
            mess.dlen = NcStream.readVInt(is);
            is.skip(mess.dlen);
            total += mess.dlen;
            break;
          case Error:
            mess.obj = NcStreamProto.Error.parseFrom(m);
            break;
          case StationList:
            mess.obj = PointStreamProto.StationList.parseFrom(m);
            break;
          case PointFeatureCollection:
            mess.obj = PointStreamProto.PointFeatureCollection.parseFrom(m);
            break;
          case PointFeature:
            mess.obj = PointStreamProto.PointFeature.parseFrom(m);
            break;
          default:
            mess.obj = "unknown";
        }
      }
    } catch (IOException ioe) {

    } finally {
      if (is != null) is.close();
    }

    messTable.setBeans(messages);
    System.out.printf(" nmess = %d nbytes=%d%n", messages.size(), total);
  }
  
  class Mess {
    PointStream.MessageType magic;
    int vlen;
    int dlen;
    Object obj;
  }

  public void closeOpenFiles() throws IOException {
    if (ncd != null) ncd.close();
    ncd = null;
  }

  public void setFile(String filename) throws IOException {
    closeOpenFiles();

    List<MessBean> messages = new ArrayList<MessBean>();
    ncd = new MyNetcdfFile();
    NcStreamIosp iosp = new NcStreamIosp();
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(filename, "r");
      List<NcStreamIosp.NcsMess> ncm = iosp.open(raf, ncd);
      for (NcStreamIosp.NcsMess m : ncm) {
        messages.add(new MessBean(m));
      }

    } catch (Exception e) {
      if (raf != null) raf.close();
    }

    messTable.setBeans(messages);
    System.out.printf("mess = %d%n", messages.size());
  }

  public void showInfo(Formatter f) {
    if (ncd == null) return;
    f.format("%s", ncd.toString()); // CDL
  }

  public class MessBean  {
    private NcStreamIosp.NcsMess m;

    MessBean() {
    }

    MessBean(NcStreamIosp.NcsMess m) {
      this.m = m;
    }

    ///////////////

    public String getMagic() {
      return "";
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

    public int getDSize() {
      return 0;
    }
  }

  public class MessBean2 extends MessBean  {
    Mess mess;

    MessBean2() {
    }

    MessBean2(Mess m) {
      this.mess = m;
    }

    ///////////////

    public String getMagic() {
      return mess.magic.toString();
    }
    
    public String getObjClass() {
      return (mess.obj == null) ? "" : mess.obj.getClass().toString();
    }

    public String getDesc() {
      return (mess.obj == null) ? "" : mess.obj.toString();
    }

    public int getSize() {
      return mess.vlen;
    }

    public int getDSize() {
      return mess.dlen;
    }
  }

}
