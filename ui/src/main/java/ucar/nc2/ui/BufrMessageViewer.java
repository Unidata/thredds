/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ui;

import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.SequenceDS;
import ucar.nc2.ft.point.bufr.BufrCdmIndex;
import ucar.nc2.ft.point.bufr.BufrCdmIndexProto;
import ucar.nc2.ft.point.bufr.StandardFields;
import ucar.nc2.iosp.bufr.*;
import ucar.nc2.iosp.bufr.tables.BufrTables;
import ucar.nc2.iosp.bufr.writer.Bufr2Xml;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.ui.widget.*;
import ucar.nc2.ui.widget.PopupMenu;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

//import ucar.util.GoogleDiff;

/**
 * ToolsUI/Iosp/Bufr
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class BufrMessageViewer extends JPanel {

  private PreferencesExt prefs;

  private BeanTable messageTable, obsTable, ddsTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA, infoTA2;
  private IndependentWindow infoWindow, infoWindow2;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;
  private FileManager fileChooser;
  private boolean seperateWindow = false;

  public BufrMessageViewer(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    AbstractButton tableButt = BAMutil.makeButtcon("Structure", "Data Table", false);
    tableButt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          NetcdfFile ncd = makeBufrDataset() ;
          Variable v = ncd.findVariable(BufrIosp2.obsRecord);
          if ((v != null) && (v instanceof Structure)) {
            if (dataTable == null) makeDataTable();
            dataTable.setStructure((Structure) v);
            dataWindow.show();
          }
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(BufrMessageViewer.this, ex.getMessage());
          ex.printStackTrace();
        }
      }
    });
    buttPanel.add(tableButt);

    AbstractButton showButt = BAMutil.makeButtcon("GetAll", "Read All Data", false);
    showButt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        readData(f);
        infoTA2.setText(f.toString());
        infoTA2.gotoTop();
        infoWindow2.show();
      }
    });
    buttPanel.add(showButt);

    AbstractAction seperateWindowAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        seperateWindow = (Boolean) getValue(BAMutil.STATE);
      }
    };
    BAMutil.setActionProperties(seperateWindowAction, "DrawVert", "seperate DDS window", true, 'C', -1);
    seperateWindowAction.putValue(BAMutil.STATE, Boolean.valueOf(seperateWindow));
    BAMutil.addActionToContainer(buttPanel, seperateWindowAction);

    AbstractButton distinctDdsButt = BAMutil.makeButtcon("dd", "Dump distinct DDS", false);
    distinctDdsButt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dumpDDS();
      }
    });
    buttPanel.add(distinctDdsButt);

    AbstractButton distinctMessButt = BAMutil.makeButtcon("Import", "Write distinct messages", false);
    distinctMessButt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        writeAll();
      }
    });
    buttPanel.add(distinctMessButt);

    AbstractButton configButt = BAMutil.makeButtcon("Dump", "Make BufrConfig", false);
    configButt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (raf == null) return;
        try {
          BufrConfig config = BufrConfig.scanEntireFile(raf);
          Formatter out = new Formatter();
          config.show(out);
          infoTA2.setText(out.toString());

        } catch (Exception ex) {
          ex.printStackTrace();
          StringWriter sw = new StringWriter(10000);
          ex.printStackTrace(new PrintWriter(sw));
          infoTA2.setText(sw.toString());
        }
        infoTA2.gotoTop();
        infoWindow2.show();
      }
    });
    buttPanel.add(configButt);

    AbstractButton writeButton = BAMutil.makeButtcon("V3", "Write index", false);
    writeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Formatter f = new Formatter();
        try {
          if (writeIndex(f)) {
            f.format("Index written");
            infoTA2.setText(f.toString());
          }

        } catch (Exception ex) {
          ex.printStackTrace();
          StringWriter sw = new StringWriter(10000);
          ex.printStackTrace(new PrintWriter(sw));
          infoTA2.setText(sw.toString());
        }
        infoTA2.gotoTop();
        infoWindow2.show();
      }
    });
    buttPanel.add(writeButton);

    ///////////////////////////////////////

    messageTable = new BeanTable(MessageBean.class, (PreferencesExt) prefs.node("GridRecordBean"), false);
    messageTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ddsTable.setBeans(new ArrayList());
        obsTable.setBeans(new ArrayList());

        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        if (mb == null) return;
        java.util.List<DdsBean> beanList = new ArrayList<>();
        try {
          setDataDescriptors(beanList, mb.m.getRootDataDescriptor(), 0);
          setObs(mb.m);
        } catch (Exception e1) {
          JOptionPane.showMessageDialog(BufrMessageViewer.this, e1.getMessage());
          e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        ddsTable.setBeans(beanList);
      }
    });

    obsTable = new BeanTable(ObsBean.class, (PreferencesExt) prefs.node("ObsBean"), false);
    /* obsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        obsTable.getSelectedBean();
      }
    }); */

    ddsTable = new BeanTable(DdsBean.class, (PreferencesExt) prefs.node("DdsBean"), false);
    /* ddsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ddsTable.getSelectedBean();
      }
    });  */

   ////////////////////////////////////////////////////////////

    ucar.nc2.ui.widget.PopupMenu varPopup = new PopupMenu(messageTable.getJTable(), "Options");
    varPopup.addAction("Show DDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean vb = (MessageBean) messageTable.getSelectedBean();
        if (vb == null) return;
        if (!seperateWindow) infoTA.clear();
        Formatter f = new Formatter();
        try {
          if (!vb.m.isTablesComplete()) {
            f.format(" MISSING DATA DESCRIPTORS= ");
            vb.m.showMissingFields(f);
            f.format("%n%n");
          }

          vb.m.dump(f);
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(BufrMessageViewer.this, e1.getMessage());
          e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if (seperateWindow) {
          TextHistoryPane ta = new TextHistoryPane();
          IndependentWindow info = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), ta);
          info.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));
          ta.appendLine(f.toString());
          ta.gotoTop();
          info.show();

        } else {
          infoTA.appendLine(f.toString());
          infoTA.gotoTop();
          infoWindow.show();
        }
      }
    });

    varPopup.addAction("Data Table", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        if (mb == null) return;
        try {
          NetcdfFile ncd = makeBufrMessageAsDataset(mb.m);
          Variable v = ncd.findVariable(BufrIosp2.obsRecord);
          if ((v != null) && (v instanceof Structure)) {
            if (dataTable == null) makeDataTable();
            dataTable.setStructure((Structure) v);
            dataWindow.show();
            mb.setReadOk(true);
          }
        } catch (Exception ex) {
          mb.setReadOk(false);
          JOptionPane.showMessageDialog(BufrMessageViewer.this, ex.getMessage());
          ex.printStackTrace();
        }
      }
    });

    varPopup.addAction("BitCount", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        if (mb == null) return;
        mb.checkBits();
      }
    });

    varPopup.addAction("Bit Count Details", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        if (mb == null) return;
        Message m = mb.m;

        Formatter out = new Formatter();
        try {
          infoTA2.clear();
          if (!m.dds.isCompressed()) {
            MessageUncompressedDataReader reader = new MessageUncompressedDataReader();
            reader.readData(null, m, raf, null, false, out);
          } else {
            MessageCompressedDataReader reader = new MessageCompressedDataReader();
            reader.readData(null, m, raf, null, out);
          }
          int nbitsGiven = 8 * (m.dataSection.getDataLength() - 4);
          DataDescriptor root = m.getRootDataDescriptor();
          out.format("Message nobs=%d compressed=%s vlen=%s countBits= %d givenBits=%d %n",
                  m.getNumberDatasets(), m.dds.isCompressed(), root.isVarLength(),
                  m.getCountedDataBits(), nbitsGiven);
          out.format(" countBits= %d givenBits=%d %n", m.getCountedDataBits(), nbitsGiven);
          out.format(" countBytes= %d dataSize=%d %n", m.getCountedDataBytes(), m.dataSection.getDataLength());
          out.format("%n");
          infoTA2.appendLine(out.toString());

        } catch (Exception ex) {
          StringWriter sw = new StringWriter(5000);
          ex.printStackTrace(new PrintWriter(sw));
          infoTA2.appendLine(out.toString());
          infoTA2.appendLine(sw.toString());
        }

        infoTA2.gotoTop();
        infoWindow2.show();
      }
    });

    varPopup.addAction("Read", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        if (mb == null) return;
        mb.read();
      }
    });

    varPopup.addAction("Write Message", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        if (mb == null) return;
        try {
          String defloc;
          String header = mb.m.getHeader();
          if (header != null) {
            header = header.split(" ")[0];
          }
          if (header == null) {
            defloc = (raf.getLocation() == null) ? "." : raf.getLocation();
            int pos = defloc.lastIndexOf(".");
            if (pos > 0)
              defloc = defloc.substring(0, pos);
          } else
            defloc = header;

          makeFileChooser();
          String filename = fileChooser.chooseFilenameToSave(defloc + ".bufr");
          if (filename == null) return;

          File file = new File(filename);
          FileOutputStream fos = new FileOutputStream(file);
          WritableByteChannel wbc = fos.getChannel();
          String headerS = mb.m.getHeader();
          if (headerS != null)
            wbc.write(ByteBuffer.wrap(headerS.getBytes(CDM.utf8Charset)));

          byte[] raw = scan.getMessageBytes(mb.m);
          wbc.write(ByteBuffer.wrap(raw));
          wbc.close();
          JOptionPane.showMessageDialog(BufrMessageViewer.this, filename + " successfully written");

        } catch (Exception ex) {
          JOptionPane.showMessageDialog(BufrMessageViewer.this, "ERROR: " + ex.getMessage());
          ex.printStackTrace();
        }
      }
    });

    varPopup.addAction("Show XML", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        if (mb == null) return;
        Message m = mb.m;

        ByteArrayOutputStream out = new ByteArrayOutputStream(1000 * 100);
        try {
          infoTA.clear();

          NetcdfFile ncd = makeBufrMessageAsDataset(mb.m);
          new Bufr2Xml(m, ncd, out, true);
          infoTA.setText(out.toString(CDM.UTF8));

        } catch (Exception ex) {
          StringWriter sw = new StringWriter();
          ex.printStackTrace(new PrintWriter(sw));
          try {
            infoTA.appendLine(out.toString(CDM.UTF8));
          } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
          }
          infoTA.appendLine(sw.toString());
        }

        infoTA.gotoTop();
        infoWindow.show();
      }
    });

      varPopup.addAction("Compare DDS", new AbstractAction() {
         public void actionPerformed(ActionEvent e) {
           List list = messageTable.getSelectedBeans();
           for (Object beano : list) {
             MessageBean bean = (MessageBean) beano;
             showDDS(bean.m);
           }
         }
       });


    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    // the info window 2
    infoTA2 = new TextHistoryPane();
    infoWindow2 = new IndependentWindow("Extra Information-2", BAMutil.getImage("netcdfUI"), infoTA2);
    infoWindow2.setBounds((Rectangle) prefs.getBean("InfoWindowBounds2", new Rectangle(300, 300, 500, 300)));

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ddsTable, obsTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, messageTable, split2);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  private void makeFileChooser() {
    if (fileChooser == null)
      fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));
  }

  public boolean writeIndex(Formatter f) throws IOException {
    //MFileCollectionManager dcm = scanCollection(spec, f);

    File bufrFile = new File(raf.getLocation());
    String name = bufrFile.getName();
    int pos = name.lastIndexOf('/');
    if (pos < 0) pos = name.lastIndexOf('\\');
    if (pos > 0) name = name.substring(pos + 1);
    File def = new File(bufrFile.getParent(), name + BufrCdmIndex.NCX_IDX);

    makeFileChooser();
    String filename = fileChooser.chooseFilename(def);
    if (filename == null) return false;
    if (!filename.endsWith(BufrCdmIndex.NCX_IDX))
      filename += BufrCdmIndex.NCX_IDX;
    File idxFile = new File(filename);

    BufrConfig config = BufrConfig.scanEntireFile(raf);
    return BufrCdmIndex.writeIndex(raf.getLocation(), config, idxFile);
  }


  private void makeDataTable() {
    // the data Table
    dataTable = new StructureTable((PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("netcdfUI"), dataTable);
    dataWindow.setBounds((Rectangle) prefs.getBean("dataWindow", new Rectangle(50, 300, 1000, 600)));
  }

  private void writeAll() {
    List<MessageBean> beans = messageTable.getBeans();
    HashMap<Integer, Message> map = new HashMap<>(2 * beans.size());

    for (MessageBean mb : beans) {
      map.put(mb.m.hashCode(), mb.m);
    }

    makeFileChooser();
    String defloc = (raf.getLocation() == null) ? "." : raf.getLocation();
    String dirName = fileChooser.chooseDirectory(defloc);
    if (dirName == null) return;

    try {
      int count = 0;
      for (Message m : map.values()) {
        String header = m.getHeader();
        if (header != null) {
          header = header.split(" ")[0];
        } else {
          // header is the non-negative hash code of m. Note that using Math.abs() for this operation is a bug:
          // http://findbugs.blogspot.com/2006/09/is-mathabs-broken.html
          header = Integer.toString(m.hashCode() & Integer.MAX_VALUE);
        }

        File file = new File(dirName + "/" + header + ".bufr");
        try (FileOutputStream fos = new FileOutputStream(file);
             WritableByteChannel wbc = fos.getChannel()) {
          String headerS = m.getHeader();
          if (headerS != null)
            wbc.write(ByteBuffer.wrap(headerS.getBytes(CDM.utf8Charset)));
          byte[] raw = scan.getMessageBytes(m);
          wbc.write(ByteBuffer.wrap(raw));
        }
        count++;
      }
      JOptionPane.showMessageDialog(BufrMessageViewer.this, count + " successfully written to " + dirName);

    } catch (IOException e1) {
      JOptionPane.showMessageDialog(BufrMessageViewer.this, e1.getMessage());
      e1.printStackTrace();
    }
  }

  private void readData(Formatter f) {
    List<MessageBean> beans = messageTable.getBeans();
    int count = 0;
    try {
      for (MessageBean bean : beans) {
        bean.read();
        count++;
      }
      f.format("Read %d messages", count);

    } catch (Exception e) {
       StringWriter sw = new StringWriter(10000);
       e.printStackTrace(new PrintWriter(sw));
       f.format("%s", sw.toString());
    }
  }

  private void dumpDDS() {
    List<MessageBean> beans = messageTable.getBeans();
    HashMap<Integer, Message> map = new HashMap<>(2 * beans.size());

    // unique DDS
    for (MessageBean mb : beans) {
      map.put(mb.m.hashCode(), mb.m);
    }

    makeFileChooser();
    String defloc = (raf.getLocation() == null) ? "." : raf.getLocation();
    int pos = defloc.lastIndexOf(".");
    if (pos > 0)
      defloc = defloc.substring(0, pos);
    String filename = fileChooser.chooseFilenameToSave(defloc + ".txt");
    if (filename == null) return;

    try (FileOutputStream out = new FileOutputStream(filename)) {
      OutputStreamWriter fout = new OutputStreamWriter(out, CDM.utf8Charset);
      BufferedWriter bw = new BufferedWriter(fout);
      Formatter f = new Formatter(bw);

      int count = 0;
      for (Message m : map.values()) {
        m.dump(f);
        f.flush();
        count++;
      }
      JOptionPane.showMessageDialog(BufrMessageViewer.this, count + " successfully written to " + filename);

    } catch (IOException e1) {
      JOptionPane.showMessageDialog(BufrMessageViewer.this, e1.getMessage());
      e1.printStackTrace();
    }
  }


 /*  private void compare(Message m1, Message m2, Formatter f) {
    Formatter f1 = new Formatter();
    Formatter f2 = new Formatter();
    m1.dump(f1);
    m1.dump(f2);

    TextHistoryPane ta = new TextHistoryPane();
    IndependentWindow info = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), ta);
    info.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));
    ta.appendLine(f.toString());
    ta.gotoTop();
    info.show();
  }  */

  private void showDDS(Message m1) {
    Formatter f1 = new Formatter();
    m1.dump(f1);
    TextHistoryPane ta = new TextHistoryPane();
    IndependentWindow info = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), ta);
    info.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));
    ta.appendLine(f1.toString());
    ta.gotoTop();
    info.show();
  }

  /* private void compare2(Message m1, Message m2, Formatter f) {
    Formatter f1 = new Formatter();
    Formatter f2 = new Formatter();
      m1.dump(f1);
      m1.dump(f2);
    GoogleDiff diff = new GoogleDiff();
    List<GoogleDiff.Diff> result = diff.diff_main(f1.toString(), f2.toString());
    for (GoogleDiff.Diff d : result)
      f.format("%s%n", d);
    //DataDescriptor root1 = m1.getRootDataDescriptor();
    //DataDescriptor root2 = m1.getRootDataDescriptor();
    //compare(root1.getSubKeys(), root2.getSubKeys(), f);
  }

  private void compare(List<DataDescriptor> dds1, List<DataDescriptor> dds2, Formatter f) throws IOException {

    int count = 0;
    for (DataDescriptor sub1 : dds1) {
      DataDescriptor sub2 = dds2.get(count);

    }
  } */

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void save() {
    messageTable.saveState(false);
    ddsTable.saveState(false);
    obsTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putBeanObject("InfoWindowBounds2", infoWindow2.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
    if (fileChooser != null) fileChooser.save();
  }

  private RandomAccessFile raf;
  private MessageScanner scan;
  int center;

  public void setBufrFile(RandomAccessFile raf) throws IOException {
    this.raf = raf;
    java.util.List<MessageBean> beanList = new ArrayList<>();
    center = -1;

    scan = new MessageScanner(raf, 0, true);
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;
      if (center == -1) center = m.ids.getCenterId();

      beanList.add(new MessageBean(m));
    }

    messageTable.setBeans(beanList);
    obsTable.setBeans(new ArrayList());
    ddsTable.setBeans(new ArrayList());
  }

  private NetcdfFile makeBufrMessageAsDataset(Message m) throws IOException {
    BufrIosp2 iosp = new BufrIosp2();
    NetcdfFileSubclass ncfile = new NetcdfFileSubclass(iosp, raf.getLocation());
    iosp.open(raf, ncfile, m);
    return ncfile;
  }

  private NetcdfFile makeBufrDataset() throws IOException {
    BufrIosp2 iosp = new BufrIosp2();
    NetcdfFileSubclass ncfile = new NetcdfFileSubclass(iosp, raf.getLocation());
    iosp.open(raf, ncfile, (CancelTask) null);
    return ncfile;
  }

  private int setDataDescriptors(java.util.List<DdsBean> beanList, DataDescriptor dds, int seqno) {
    for (DataDescriptor key : dds.getSubKeys()) {
      beanList.add(new DdsBean(key, seqno++));
      if (key.getSubKeys() != null)
        seqno = setDataDescriptors(beanList, key, seqno);
    }
    return seqno;
  }

  private void setObs(Message m) {

    java.util.List<ObsBean> beanList = new ArrayList<>();
    try {
      NetcdfFile ncd = makeBufrMessageAsDataset(m);
      Variable v = ncd.findVariable(BufrIosp2.obsRecord);
      if ((v != null) && (v instanceof Structure)) {
        Structure obs = (Structure) v;
        StandardFields.StandardFieldsFromStructure extract = new StandardFields.StandardFieldsFromStructure(center, obs);
        StructureDataIterator iter = obs.getStructureIterator();
        try {
          while (iter.hasNext()) {
            beanList.add(new ObsBean(extract, iter.next()));
          }
        } finally {
          iter.finish();
        }
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(BufrMessageViewer.this, ex.getMessage());
      ex.printStackTrace();
    }
    obsTable.setBeans(beanList);
  }

  public class MessageBean {
    Message m;
    int readOk = 0;
    int bitsOk = 0;

    // no-arg constructor

    public MessageBean() {
    }

    // create from a dataset

    public MessageBean(Message m) {
      this.m = m;
    }

    public String getCategory() throws IOException {
      return m.getLookup().getCategoryFullName();
    }

    public String getCenter() {
      return m.getLookup().getCenterName();
    }

    public String getTable() {
      return m.getLookup().getTableName();
    }

    public String getHeader() {
      return m.getHeader();
    }

    public int getEdition() {
      return m.is.getBufrEdition();
    }

    public int getNobs() {
      return m.getNumberDatasets();
    }

    public long getSize() {
      return m.getMessageSize();
    }

    public String getHash() {
      return Integer.toHexString(m.hashCode());
    }

    public String getDdsHash() {
      return Integer.toHexString(m.dds.getDataDescriptors().hashCode());
    }

    public String getCompress() {
      return m.dds.isCompressed() ? "true" : "false";
    }

    public String getDate() {
      return m.getReferenceTime().toString();
    }

    public String getComplete() {
      try {
        return m.isTablesComplete() ? "true" : "false";
      } catch (IOException e) {
        return "exception";
      }
    }

    public String getBitsOk() {
      if (bitsOk == 0) checkBits();
      switch (bitsOk) {
        default : return "N/A";
        case 1 : return "true";
        case 2 : return "false";
        case 3 : return "fail";
      }
    }

    void checkBits() {
      if (getNobs() == 0) return;
      try {
        boolean ok = m.isBitCountOk();
        setBitsOk(ok);
      } catch (Exception e) {
        bitsOk = 3;
      }
    }

    public String getLocal() {
      try {
        return m.usesLocalTable() ? "true" : "false";
      } catch (Exception e) {
        return "exception";
      }
    }

    public String getReadOk() {
      switch (readOk) {
        default : return "N/A";
        case 1 : return "true";
        case 2 : return "false";
      }
    }

    void setBitsOk(boolean ok) {
      bitsOk = ok ? 1 : 2;
    }

    void setReadOk(boolean ok) {
      readOk = ok ? 1 : 2;
    }

    private void read() {
      try {
        NetcdfFile ncd = makeBufrMessageAsDataset(m);
        SequenceDS v = (SequenceDS) ncd.findVariable(BufrIosp2.obsRecord);
        StructureDataIterator iter = v.getStructureIterator(-1);
        try {
          while (iter.hasNext())
            iter.next();

        } finally {
          iter.finish();
        }
        setReadOk(true);
      } catch (IOException e) {
        setReadOk(false);
      }
    }

  }


  public class DdsBean {
    DataDescriptor dds;
    int seq;

    // create from a dataset

    public DdsBean(DataDescriptor dds, int seq) {
      this.dds = dds;
      this.seq = seq;
    }

    public String getFxy() {
      return dds.getFxyName();
    }

    public String getName() {
      return dds.getName();
    }

    public String getUnits() {
      return dds.getUnits();
    }

    public String getDesc() {
      return dds.getDesc();
    }

    public int getBitWidth() {
      return dds.getBitWidth();
    }

    public int getScale() {
      return dds.getScale();
    }

    public int getReference() {
      return dds.getRefVal();
    }

    public String getSource() {
      return dds.getSource();
    }

    public int getSeq() {
      return seq;
    }

    public String getLocal() {
      if (dds.isLocalOverride()) return "override";
      return dds.isLocal() ? "true" : "false";
    }

  }


  public class ObsBean {
    double lat = Double.NaN, lon = Double.NaN, height = Double.NaN, heightOfStation = Double.NaN;
    int year = -1, month = -1, day = -1, hour = -1, minute = -1, sec = -1, doy = -1;
    CalendarDate date;
    int wmo_block = -1, wmo_id = -1;
    String stn = null;

    // no-arg constructor

    public ObsBean() {
    }

    // create from a dataset

    public ObsBean(StandardFields.StandardFieldsFromStructure extract, StructureData sdata) {
      extract.extract(sdata);
      this.stn = extract.getStationId();
      this.date = extract.makeCalendarDate();
      this.lat = extract.getFieldValueD(BufrCdmIndexProto.FldType.lat);
      this.lon = extract.getFieldValueD(BufrCdmIndexProto.FldType.lon);
      this.height = extract.getFieldValueD(BufrCdmIndexProto.FldType.height);
      this.heightOfStation = extract.getFieldValueD(BufrCdmIndexProto.FldType.heightOfStation);
      this.year = extract.getFieldValue(BufrCdmIndexProto.FldType.year);
      this.month = extract.getFieldValue(BufrCdmIndexProto.FldType.month);
      this.day = extract.getFieldValue(BufrCdmIndexProto.FldType.day);
      this.hour = extract.getFieldValue(BufrCdmIndexProto.FldType.hour);
      this.minute = extract.getFieldValue(BufrCdmIndexProto.FldType.minute);
      this.sec = extract.getFieldValue(BufrCdmIndexProto.FldType.sec);
      this.doy = extract.getFieldValue(BufrCdmIndexProto.FldType.doy);
      this.wmo_block = extract.getFieldValue(BufrCdmIndexProto.FldType.wmoBlock);
      this.wmo_id = extract.getFieldValue(BufrCdmIndexProto.FldType.wmoId);
    }

    public double getLat() {
      return lat;
    }

    public double getLon() {
      return lon;
    }

    public double getHeight() {
      return height;
    }

    public double getHeightOfStation() {
       return heightOfStation;
     }

     public int getYear() {
      return year;
    }

    public int getMonth() {
      return month;
    }

    public int getDay() {
      return day;
    }

    public int getHour() {
      return hour;
    }

    public int getMinute() {
      return minute;
    }

    public int getSec() {
      return sec;
    }

    public int getDoy() {
      return doy;
    }

    public String getWmoId() {
      return wmo_block + "/" + wmo_id;
    }

    public String getStation() {
      return stn;
    }

    public String getDate() {
      return date.toString();
    }


  }

 /* public static void main(String[] args) throws IOException {
    RandomAccessFile raf1 = new RandomAccessFile("G:/ldm/distinct/8.bufr", "r");
    RandomAccessFile raf2 = new RandomAccessFile("G:/ldm/distinct/9.bufr", "r");
    MessageScanner scan1 = new MessageScanner(raf1);
    MessageScanner scan2 = new MessageScanner(raf2);
    Message m1 = scan1.getFirstDataMessage();
    Message m2 = scan2.getFirstDataMessage();
    raf1.close();
    raf2.close();

    Formatter f1 = new Formatter();
    Formatter f2 = new Formatter();

    m1.dump(f1);
    m2.dump(f2);

    System.out.printf("%s%n", f1);
    System.out.printf("==========================%n");
    System.out.printf("%s%n", f2);
    System.out.printf("==========================%n");


    GoogleDiff diff = new GoogleDiff();
    LinkedList<GoogleDiff.Diff> result = diff.diff_main(f1.toString(), f2.toString());
    diff.diff_cleanupSemantic(result);
    for (GoogleDiff.Diff d : result)
      System.out.printf("%s%n", d);
  } */

}
