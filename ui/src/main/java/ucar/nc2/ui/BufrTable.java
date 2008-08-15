/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.ui;

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.unidata.io.RandomAccessFile;
import ucar.bufr.Message;
import ucar.bufr.MessageScanner;
import ucar.bufr.Dump;
import ucar.bufr.DataDescriptor;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import thredds.ui.TextHistoryPane;
import thredds.ui.IndependentWindow;
import thredds.ui.BAMutil;
import thredds.ui.FileManager;

import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Class Description.
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class BufrTable extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted messageTable, obsTable, ddsTable;
  private JSplitPane split;

  private TextHistoryPane infoTA;
  private IndependentWindow infoWindow;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;
  private FileManager fileChooser;

  public BufrTable(PreferencesExt prefs) {
    this.prefs = prefs;

    messageTable = new BeanTableSorted(MessageBean.class, (PreferencesExt) prefs.node("MessageBean"), false);
    messageTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        MessageBean vb = (MessageBean) messageTable.getSelectedBean();
        java.util.List<DdsBean> beanList = new ArrayList<DdsBean>();
        try {
          setDataDescriptors(beanList, vb.m.getRootDataDescriptor(), 0);
        } catch (IOException e1) {
          e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        ddsTable.setBeans(beanList);
      }
    });

    obsTable = new BeanTableSorted(ObsBean.class, (PreferencesExt) prefs.node("ObsBean"), false);
    obsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        ObsBean csb = (ObsBean) obsTable.getSelectedBean();
      }
    });

    ddsTable = new BeanTableSorted(DdsBean.class, (PreferencesExt) prefs.node("DdsBean"), false);
    ddsTable.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        DdsBean csb = (DdsBean) ddsTable.getSelectedBean();
      }
    });

    thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(messageTable.getJTable(), "Options");
    varPopup.addAction("Show DDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean vb = (MessageBean) messageTable.getSelectedBean();
        infoTA.clear();
        Formatter f = new Formatter();
        try {
          new Dump().dumpDDS(f, vb.m);
        } catch (IOException e1) {
          e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        infoTA.appendLine(f.toString());
        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });
    varPopup.addAction("Data Table", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        try {
          NetcdfDataset ncd = getBufrMessageAsDataset(mb.m);
          Variable v = ncd.findVariable("obsRecord");
          if ((v != null) && (v instanceof Structure)) {
            if (dataTable == null) makeDataTable();
            dataTable.setStructure((Structure) v);
            dataWindow.showIfNotIconified();
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    varPopup.addAction("Bit Count", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        Message m = mb.m;

        try {
          DataDescriptor root = m.getRootDataDescriptor(); // make sure the dds has been formed
          int nbitsCounted = m.getTotalBits();
          int nbitsGiven = 8 * (m.dataSection.dataLength - 4);
          boolean ok = Math.abs(m.getCountedDataBytes() - m.dataSection.dataLength) <= 1; // radiosondes dataLen not even number

          infoTA.clear();
          Formatter out = new Formatter();
            if (!ok) out.format("*** BAD BIT COUNT %n");
            long last = m.dataSection.dataPos + m.dataSection.dataLength;
            out.format("Message nobs=%d compressed=%s vlen=%s countBits= %d givenBits=%d %n",
                    m.getNumberDatasets(), m.dds.isCompressed(), root.isVarLength(),
                    nbitsCounted, nbitsGiven);
            out.format(" countBits= %d givenBits=%d %n", nbitsCounted, nbitsGiven);
            out.format(" countBytes= %d dataSize=%d %n", m.getCountedDataBytes(), m.dataSection.dataLength);
            out.format("%n");
          infoTA.appendLine(out.toString());

        } catch (Exception ex) {
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          ex.printStackTrace( new PrintStream(bos));
          infoTA.appendLine(bos.toString());
        }

        infoTA.gotoTop();
        infoWindow.showIfNotIconified();
      }
    });
    varPopup.addAction("Write Message", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        try {
          String defloc = (location == null) ? "." : location;
          int pos = defloc.lastIndexOf(".");
          if (pos > 0)
            defloc = defloc.substring(0, pos);

          if (fileChooser == null)
            fileChooser = new FileManager(null);

          String filename = fileChooser.chooseFilenameToSave(defloc + ".save1.bufr");
          if (filename == null) return;

          File file = new File(filename);
          FileOutputStream fos = new FileOutputStream(file);
          WritableByteChannel wbc = fos.getChannel();
          wbc.write(ByteBuffer.wrap(mb.m.getHeader().getBytes()));

          byte[] raw = scan.getMessageBytes(mb.m);
          wbc.write(ByteBuffer.wrap(raw));
          wbc.close();
          JOptionPane.showMessageDialog(BufrTable.this, filename + " successfully written");

        } catch (Exception ex) {
          JOptionPane.showMessageDialog(BufrTable.this, "ERROR: " + ex.getMessage());
          ex.printStackTrace();
        }
      }
    });

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("netcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, messageTable, ddsTable);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  private void makeDataTable() {
    // the data Table
    dataTable = new StructureTable((PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("netcdfUI"), dataTable);
    dataWindow.setBounds((Rectangle) prefs.getBean("dataWindow", new Rectangle(50, 300, 1000, 600)));
  }

  public void save() {
    messageTable.saveState(false);
    ddsTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  private String location;
  private MessageScanner scan;

  public void setBufrFile(RandomAccessFile raf) throws IOException {
    this.location = raf.getLocation();
    long start = System.nanoTime();
    java.util.List<MessageBean> beanList = new ArrayList<MessageBean>();

    scan = new MessageScanner(raf);
    int count = 0;
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;

      beanList.add(new MessageBean(m));
      count++;
    }

    messageTable.setBeans(beanList);
  }

  private NetcdfDataset getBufrMessageAsDataset(Message m) throws IOException {
    byte[] mbytes = scan.getMessageBytes(m);
    NetcdfFile ncfile = NetcdfFile.openInMemory("test", mbytes);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);
    return ncd;
  }

  private int setDataDescriptors(java.util.List<DdsBean> beanList, DataDescriptor dds, int seqno) {
    for (DataDescriptor key : dds.subKeys) {
      beanList.add(new DdsBean(key, seqno++));
      if (key.getSubKeys() != null)
        seqno = setDataDescriptors(beanList, key, seqno);
    }
    return seqno;
  }

  public class MessageBean {
    Message m;

    // no-arg constructor
    public MessageBean() {
    }

    // create from a dataset
    public MessageBean(Message m) {
      this.m = m;
    }

    public String getCategory() throws IOException {
      return m.getCategoryFullName();
    }

    public String getCenter() {
      return m.getCenterName();
    }

    public String getTable() {
      return m.getTableName();
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

    public int getHash() {
      return m.hashCode();
    }

    public String getCompress() {
      return m.dds.isCompressed() ? "true" : "false";
    }

    public String getDate() {
      return m.ids.getReferenceTime();
    }

    public String getComplete() {
      try {
        return m.isTablesComplete() ? "true" : "false";
      } catch (IOException e) {
        return "exception";
      }
    }

    public String getBitsOk() {
      try {
        return m.isBitCountOk() ? "true" : "false";
      } catch (Exception e) {
        return "exception";
      }
    }

  }

  public class ObsBean {
    int n;

    // no-arg constructor
    public ObsBean() {
    }

    // create from a dataset
    public ObsBean(int n) {
      this.n = n;
    }

    public int getNobs() {
      return n;
    }
  }

  public class DdsBean {
    DataDescriptor dds;
    int seq;

    // no-arg constructor
    public DdsBean() {
    }

    // create from a dataset
    public DdsBean(DataDescriptor dds, int seq) {
      this.dds = dds;
      this.seq = seq;
    }

    public String getFxy() {
      return dds.getFxyName();
    }

    public String getName() {
      return dds.name;
    }

    public String getUnits() {
      return dds.units;
    }

    public int getBitWidth() {
      return dds.getBitWidth();
    }

    public int getScale() {
      return dds.scale;
    }

    public int getReference() {
      return dds.refVal;
    }

    public int getSeq() {
      return seq;
    }

  }

}
