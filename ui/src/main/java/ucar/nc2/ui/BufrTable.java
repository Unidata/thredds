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
import ucar.nc2.Attribute;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;
import ucar.ma2.DataType;

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
import java.util.List;
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
  private JSplitPane split, split2;

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
        ddsTable.setBeans(new ArrayList());
        obsTable.setBeans(new ArrayList());

        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        java.util.List<DdsBean> beanList = new ArrayList<DdsBean>();
        try {
          setDataDescriptors(beanList, mb.m.getRootDataDescriptor(), 0);
          setObs(mb.m);
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(BufrTable.this, e1.getMessage());
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
          if (!vb.m.isTablesComplete()) {
            f.format(" MISSING DATA DESCRIPTORS= ");
            vb.m.showMissingFields(f);
            f.format("%n%n");
          }

          new Dump().dump(f, vb.m);
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(BufrTable.this, e1.getMessage());
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
          JOptionPane.showMessageDialog(BufrTable.this, ex.getMessage());
          ex.printStackTrace();
        }
      }
    });
    varPopup.addAction("Bit Count", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
        Message m = mb.m;

        try {
          Formatter out = new Formatter();
          int nbitsCounted = m.calcTotalBits(out);
          int nbitsGiven = 8 * (m.dataSection.dataLength - 4);
          boolean ok = Math.abs(m.getCountedDataBytes() - m.dataSection.dataLength) <= 1; // radiosondes dataLen not even number

          infoTA.clear();
          if (!ok) out.format("*** BAD BIT COUNT %n");
          long last = m.dataSection.dataPos + m.dataSection.dataLength;
          DataDescriptor root = m.getRootDataDescriptor();
          out.format("Message nobs=%d compressed=%s vlen=%s countBits= %d givenBits=%d %n",
              m.getNumberDatasets(), m.dds.isCompressed(), root.isVarLength(),
              nbitsCounted, nbitsGiven);
          out.format(" countBits= %d givenBits=%d %n", nbitsCounted, nbitsGiven);
          out.format(" countBytes= %d dataSize=%d %n", m.getCountedDataBytes(), m.dataSection.dataLength);
          out.format("%n");
          infoTA.appendLine(out.toString());

        } catch (Exception ex) {
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          ex.printStackTrace(new PrintStream(bos));
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

    split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ddsTable, obsTable);
    split2.setDividerLocation(prefs.getInt("splitPos2", 800));

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, messageTable, split2);
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
    obsTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putInt("splitPos2", split2.getDividerLocation());
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
    obsTable.setBeans(new ArrayList());
    ddsTable.setBeans(new ArrayList());
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

  private void setObs(Message m) {
    
    java.util.List<ObsBean> beanList = new ArrayList<ObsBean>();
    try {
      NetcdfDataset ncd = getBufrMessageAsDataset(m);
      Variable v = ncd.findVariable("obsRecord");
      if ((v != null) && (v instanceof Structure)) {
        Structure obs = (Structure) v;
        StructureDataIterator iter = obs.getStructureIterator();
        while (iter.hasNext()) {
          beanList.add( new ObsBean(obs, iter.next())); 
        }
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(BufrTable.this, ex.getMessage());
      ex.printStackTrace();
    }
    obsTable.setBeans(beanList);
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


  public class ObsBean {
    double lat = Double.NaN, lon = Double.NaN, alt = Double.NaN;
    int year = -1, month = -1, day = -1, hour = -1, minute = -1, sec = -1;
    Date time;
    int wmo_block = -1, wmo_id = -1;
    String stn = null;

    // no-arg constructor
    public ObsBean() {
    }

    // create from a dataset
    public ObsBean(Structure obs, StructureData sdata) {
      // first choice
       for (Variable v : obs.getVariables()) {
         Attribute att = v.findAttribute("BUFR:TableB_descriptor");
         if (att == null) continue;
         String val = att.getStringValue();
         if (val.equals("0-5-1") && Double.isNaN(lat)) {
           lat = sdata.convertScalarDouble(v.getShortName());
         } else if (val.equals("0-6-1") && Double.isNaN(lon)) {
           lon = sdata.convertScalarDouble(v.getShortName());
         } else if (val.equals("0-7-30") && Double.isNaN(alt)) {

           alt = sdata.convertScalarDouble(v.getShortName());
         } else if (val.equals("0-4-1") && (year<0)) {
           year = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-2")&& (month<0)) {
           month = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-3")&& (day<0)) {
           day = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-4")&& (hour<0)) {
           hour = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-5")&& (minute<0)) {
           minute = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-4-6")&& (sec<0)) {
           sec = sdata.convertScalarInt(v.getShortName());

         } else if (val.equals("0-1-1")&& (wmo_block<0)) {
           wmo_block = sdata.convertScalarInt(v.getShortName());
         } else if (val.equals("0-1-2")&& (wmo_id<0)) {
           wmo_id = sdata.convertScalarInt(v.getShortName());

         } else if ((stn == null) &&
             (val.equals("0-1-7") || val.equals("0-1-194") || val.equals("0-1-11") || val.equals("0-1-18") )) {
           if (v.getDataType().isString())
             stn = sdata.getScalarString(v.getShortName());
           else
             stn = Integer.toString( sdata.convertScalarInt(v.getShortName()));
         }
       }

      // second choice
       for (Variable v : obs.getVariables()) {
         Attribute att = v.findAttribute("BUFR:TableB_descriptor");
         if (att == null) continue;
         String val = att.getStringValue();
         if (val.equals("0-5-2") && Double.isNaN(lat)) {
           lat = sdata.convertScalarDouble(v.getShortName());
         } else if (val.equals("0-6-2") && Double.isNaN(lon)) {
           lon = sdata.convertScalarDouble(v.getShortName());
         } else if (val.equals("0-7-1") && Double.isNaN(alt)) {
           alt = sdata.convertScalarDouble(v.getShortName());
         } else if ((val.equals("0-4-7")) && (sec<0)) {
           sec = sdata.convertScalarInt(v.getShortName());
         } 
       }

      // third choice
       for (Variable v : obs.getVariables()) {
         Attribute att = v.findAttribute("BUFR:TableB_descriptor");
         if (att == null) continue;
         String val = att.getStringValue();
         if (val.equals("0-7-10") && Double.isNaN(alt)) {
           alt = sdata.convertScalarDouble(v.getShortName());
         } else if (val.equals("0-7-2") && Double.isNaN(alt)) {
           alt = sdata.convertScalarDouble(v.getShortName());
         }

       }

    }

    public double getLat() {
      return lat;
    }
    public double getLon() {
      return lon;
    }
    public double getHeight() {
      return alt;
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

    public String getWmoId() {
      return wmo_block+"/"+wmo_id;
    }

    public String getStation() {
      return stn;
    }
  }

}
