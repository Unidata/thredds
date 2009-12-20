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

import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.BeanTableSorted;
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.iosp.bufr.*;
import ucar.nc2.iosp.bufr.writer.Bufr2Xml;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.SequenceDS;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;

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
 * ToolsUI/Iosp/Bufr
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class BufrMessageViewer extends JPanel {
  private PreferencesExt prefs;

  private BeanTableSorted messageTable, obsTable, ddsTable;
  private JSplitPane split, split2;

  private TextHistoryPane infoTA, infoTA2;
  private IndependentWindow infoWindow, infoWindow2;

  private StructureTable dataTable;
  private IndependentWindow dataWindow;
  private FileManager fileChooser;
  private DateFormatter df = new DateFormatter();
  private boolean seperateWindow = false, doRead=false;

  public BufrMessageViewer(final PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    AbstractAction useReaderAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        doRead = state.booleanValue();
      }
    };
    BAMutil.setActionProperties(useReaderAction, "addCoords", "read data", true, 'C', -1);
    useReaderAction.putValue(BAMutil.STATE, new Boolean(doRead));
    BAMutil.addActionToContainer(buttPanel, useReaderAction);

    AbstractAction seperateWindowAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Boolean state = (Boolean) getValue(BAMutil.STATE);
        seperateWindow = state.booleanValue();
      }
    };
    BAMutil.setActionProperties(seperateWindowAction, "addCoords", "seperate DDS window", true, 'C', -1);
    seperateWindowAction.putValue(BAMutil.STATE, new Boolean(seperateWindow));
    BAMutil.addActionToContainer(buttPanel, seperateWindowAction);

    messageTable = new BeanTableSorted(MessageBean.class, (PreferencesExt) prefs.node("GridRecordBean"), false);
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
          JOptionPane.showMessageDialog(BufrMessageViewer.this, e1.getMessage());
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
        try {
          NetcdfDataset ncd = getBufrMessageAsDataset(mb.m);
          Variable v = ncd.findVariable(BufrIosp.obsRecord);
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

    varPopup.addAction("Bit Count", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
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
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          ex.printStackTrace(new PrintStream(bos));
          infoTA2.appendLine(out.toString());
          infoTA2.appendLine(bos.toString());
        }

        infoTA2.gotoTop();
        infoWindow2.show();
      }
    });

    varPopup.addAction("Write Message", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        MessageBean mb = (MessageBean) messageTable.getSelectedBean();
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

          if (fileChooser == null)
            fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

          String filename = fileChooser.chooseFilenameToSave(defloc + ".bufr");
          if (filename == null) return;

          File file = new File(filename);
          FileOutputStream fos = new FileOutputStream(file);
          WritableByteChannel wbc = fos.getChannel();
          wbc.write(ByteBuffer.wrap(mb.m.getHeader().getBytes()));

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
    
    varPopup.addAction("Dump distinct DDS", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        dumpDDS();
      }
    });

    varPopup.addAction("Write all distinct messages", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        writeAll();
      }
    });

    varPopup.addAction("Show XML", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         MessageBean mb = (MessageBean) messageTable.getSelectedBean();
         Message m = mb.m;

         ByteArrayOutputStream out = new ByteArrayOutputStream(1000 * 100);
         try {
           infoTA.clear();

            NetcdfDataset ncd = getBufrMessageAsDataset(mb.m);
            new Bufr2Xml(m, ncd, out, true);
            infoTA.setText( out.toString());

         } catch (Exception ex) {
           ByteArrayOutputStream bos = new ByteArrayOutputStream();
           ex.printStackTrace(new PrintStream(bos));
           infoTA.appendLine(out.toString());
           infoTA.appendLine(bos.toString());
         }

         infoTA.gotoTop();
         infoWindow.show();
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

  private void makeDataTable() {
    // the data Table
    dataTable = new StructureTable((PreferencesExt) prefs.node("structTable"));
    dataWindow = new IndependentWindow("Data Table", BAMutil.getImage("netcdfUI"), dataTable);
    dataWindow.setBounds((Rectangle) prefs.getBean("dataWindow", new Rectangle(50, 300, 1000, 600)));
  }

  private void writeAll() {
    List<MessageBean> beans = messageTable.getBeans();
    HashMap<Integer, Message> map = new HashMap<Integer, Message>(2 * beans.size());

    for (MessageBean mb : beans) {
      map.put(mb.m.hashCode(), mb.m);
    }

    if (fileChooser == null)
      fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

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
          header = Integer.toString( Math.abs(m.hashCode()));
        }

        File file = new File(dirName+"/"+header+".bufr");
        FileOutputStream fos = new FileOutputStream(file);
        WritableByteChannel wbc = fos.getChannel();
        wbc.write(ByteBuffer.wrap(m.getHeader().getBytes()));
        byte[] raw = scan.getMessageBytes(m);
        wbc.write(ByteBuffer.wrap(raw));
        wbc.close();
        count++;
      }
      JOptionPane.showMessageDialog(BufrMessageViewer.this, count + " successfully written to " + dirName);

    } catch (IOException e1) {
      JOptionPane.showMessageDialog(BufrMessageViewer.this, e1.getMessage());
      e1.printStackTrace();
    }
  }

  private void dumpDDS() {
    List<MessageBean> beans = messageTable.getBeans();
    HashMap<Integer, Message> map = new HashMap<Integer, Message>(2 * beans.size());

    for (MessageBean mb : beans) {
      map.put(mb.m.hashCode(), mb.m);
    }

    if (fileChooser == null)
      fileChooser = new FileManager(null, null, null, (PreferencesExt) prefs.node("FileManager"));

    String defloc = (raf.getLocation() == null) ? "." : raf.getLocation();
    int pos = defloc.lastIndexOf(".");
    if (pos > 0)
      defloc = defloc.substring(0, pos);
    String filename = fileChooser.chooseFilenameToSave(defloc + ".txt");
    if (filename == null) return;

    try {
      File file = new File(filename);
      FileOutputStream fos = new FileOutputStream(file);

      int count = 0;
      for (Message m : map.values()) {
        Formatter f = new Formatter(fos);
        m.dump(f);
        f.flush();
        count++;
      }
      fos.close();
      JOptionPane.showMessageDialog(BufrMessageViewer.this, count + " successfully written to " + filename);

    } catch (IOException e1) {
      JOptionPane.showMessageDialog(BufrMessageViewer.this, e1.getMessage());
      e1.printStackTrace();
    }
  }

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

  public void setBufrFile(RandomAccessFile raf) throws IOException {
    this.raf = raf;
    java.util.List<MessageBean> beanList = new ArrayList<MessageBean>();

    scan = new MessageScanner(raf);
    while (scan.hasNext()) {
      Message m = scan.next();
      if (m == null) continue;

      beanList.add(new MessageBean(m));
    }

    messageTable.setBeans(beanList);
    obsTable.setBeans(new ArrayList());
    ddsTable.setBeans(new ArrayList());
  }

  private NetcdfDataset getBufrMessageAsDataset(Message m) throws IOException {
    byte[] mbytes = scan.getMessageBytes(m);
    NetcdfFile ncfile = null;
    try {
      ncfile = NetcdfFile.openInMemory("test", mbytes, "ucar.nc2.iosp.bufr.BufrIosp");
    } catch (Exception e) {
      throw new IOException(e);
    }
    return new NetcdfDataset(ncfile);
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

    java.util.List<ObsBean> beanList = new ArrayList<ObsBean>();
    try {
      NetcdfDataset ncd = getBufrMessageAsDataset(m);
      Variable v = ncd.findVariable(BufrIosp.obsRecord);
      if ((v != null) && (v instanceof Structure)) {
        Structure obs = (Structure) v;
        StructureDataIterator iter = obs.getStructureIterator();
        while (iter.hasNext()) {
          beanList.add(new ObsBean(obs, iter.next()));
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

    public String getHash() {
      return  Integer.toHexString(m.dds.getDataDescriptors().hashCode());
      // return Integer.toHexString(m.hashCode());
    }

    public String getCompress() {
      return m.dds.isCompressed() ? "true" : "false";
    }

    public String getDate() {
      return df.toDateTimeString(m.getReferenceTime());
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
        if (!getComplete().equals("true")) return "incomplete";
        return m.isBitCountOk() ? "true" : "false"; // LOOK using bit count 1
      } catch (Exception e) {
        return "exception";
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
      if (!getComplete().equals("true")) return "false";
      if (!getBitsOk().equals("true")) return "false";
      if (!doRead) return "N/A";
      if (readOk == 0)
        try {
          NetcdfDataset ncd = getBufrMessageAsDataset(m);
          SequenceDS v = (SequenceDS) ncd.findVariable(BufrIosp.obsRecord);
          StructureDataIterator iter = v.getStructureIterator(-1);
          while (iter.hasNext()) {
            iter.next();
          }
          readOk = 1;
        } catch (Exception e) {
          readOk = 2;
        }
      return readOk == 1 ? "true" : "false";
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
      return dds.getName();
    }

    public String getUnits() {
      return dds.getUnits();
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

    public int getSeq() {
      return seq;
    }

    public String getLocal() {
      if (dds.isLocalOverride()) return "override";
      return dds.isLocal() ? "true" : "false";
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
        } else if (val.equals("0-4-1") && (year < 0)) {
          year = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-2") && (month < 0)) {
          month = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-3") && (day < 0)) {
          day = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-4") && (hour < 0)) {
          hour = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-5") && (minute < 0)) {
          minute = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-4-6") && (sec < 0)) {
          sec = sdata.convertScalarInt(v.getShortName());

        } else if (val.equals("0-1-1") && (wmo_block < 0)) {
          wmo_block = sdata.convertScalarInt(v.getShortName());
        } else if (val.equals("0-1-2") && (wmo_id < 0)) {
          wmo_id = sdata.convertScalarInt(v.getShortName());

        } else if ((stn == null) &&
                (val.equals("0-1-7") || val.equals("0-1-194") || val.equals("0-1-11") || val.equals("0-1-18"))) {
          if (v.getDataType().isString())
            stn = sdata.getScalarString(v.getShortName());
          else
            stn = Integer.toString(sdata.convertScalarInt(v.getShortName()));
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
        } else if ((val.equals("0-4-7")) && (sec < 0)) {
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
      return wmo_block + "/" + wmo_id;
    }

    public String getStation() {
      return stn;
    }
  }

}
