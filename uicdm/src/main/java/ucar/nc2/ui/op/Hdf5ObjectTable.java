/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.hdf5.H5header;
import ucar.nc2.iosp.hdf5.H5iosp;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * ToolsUI/Iosp/Hdf5  raw file objects
 *
 * @author caron
 */
public class Hdf5ObjectTable extends JPanel {
    private PreferencesExt prefs;

    private BeanTable objectTable, messTable, attTable;
    private JSplitPane splitH, split, split2;

    private TextHistoryPane dumpTA, infoTA;
    private IndependentWindow infoWindow;

  private H5iosp iosp;
  private String location;

/**
 *
 */
    public Hdf5ObjectTable(PreferencesExt prefs) {
        this.prefs = prefs;
        PopupMenu varPopup;

        objectTable = new BeanTable(ObjectBean.class,
                                (PreferencesExt) prefs.node("Hdf5Object"), false,
                                "H5header.DataObject", "Level 2A data object header", null);
        objectTable.addListSelectionListener(e -> {
            messTable.setBeans(new ArrayList());

            ArrayList<Object> beans = new ArrayList<>();
            ObjectBean ob = (ObjectBean) objectTable.getSelectedBean();
            for ( H5header.HeaderMessage m : ob.m.getMessages()) {
                beans.add( new MessageBean(m));
            }
            messTable.setBeans(beans);

            ArrayList<Object> attBeans = new ArrayList<>();
            for ( H5header.MessageAttribute m : ob.m.getAttributes()) {
                attBeans.add( new AttributeBean(m));
            }
            attTable.setBeans(attBeans);
        });

        varPopup = new PopupMenu(objectTable.getJTable(), "Options");
        varPopup.addAction("show", new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            ObjectBean mb = (ObjectBean) objectTable.getSelectedBean();
            if (mb == null) { return; }
            dumpTA.clear();
            Formatter f = new Formatter();

            try {
                mb.show(f);
            }
            catch (IOException exc) {
                //To change body of catch statement use File | Settings | File Templates.
                exc.printStackTrace();
            }
            dumpTA.appendLine(f.toString());
            dumpTA.gotoTop();
          }
        });


        messTable = new BeanTable(MessageBean.class, (PreferencesExt) prefs.node("MessBean"), false,
                "H5header.HeaderMessage", "Level 2A1 and 2A2 (part of Data Object)", null);
        messTable.addListSelectionListener(e -> {
            MessageBean mb = (MessageBean) messTable.getSelectedBean();
            dumpTA.setText( mb.m.toString());
        });

        varPopup = new PopupMenu(messTable.getJTable(), "Options");
        varPopup.addAction("Show FractalHeap", new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            MessageBean mb = (MessageBean) messTable.getSelectedBean();
            if (mb == null) return;
            if (infoTA == null) makeInfoWindow();
            infoTA.clear();
            Formatter f = new Formatter();

            mb.m.showFractalHeap(f);
            infoTA.appendLine(f.toString());
            infoTA.gotoTop();
            infoWindow.show();
          }
        });

        attTable = new BeanTable(AttributeBean.class, (PreferencesExt) prefs.node("AttBean"), false,
                "H5header.HeaderAttribute", "Message Type 12/0xC : define an Atribute", null);
        attTable.addListSelectionListener(e -> {
            AttributeBean mb = (AttributeBean) attTable.getSelectedBean();
            Formatter f = new Formatter();
            mb.show(f);
            dumpTA.setText( f.toString());
        });

        // the info window
        dumpTA = new TextHistoryPane();

        splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, objectTable, dumpTA);
        splitH.setDividerLocation(prefs.getInt("splitPosH", 600));

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, splitH, messTable);
        split.setDividerLocation(prefs.getInt("splitPos", 500));

        split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, attTable);
        split2.setDividerLocation(prefs.getInt("splitPos2", 500));

        setLayout(new BorderLayout());
        add(split2, BorderLayout.CENTER);
    }

/**
 *
 */
    public void save() {
        objectTable.saveState(false);
        messTable.saveState(false);
        attTable.saveState(false);
        // prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
        prefs.putInt("splitPos", split.getDividerLocation());
        prefs.putInt("splitPos2", split2.getDividerLocation());
        prefs.putInt("splitPosH", splitH.getDividerLocation());
    }

/**
 *
 */
    private void makeInfoWindow() {
        infoTA = new TextHistoryPane();
        infoWindow = new IndependentWindow("Extra", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
        infoWindow.setBounds(new Rectangle(300, 300, 500, 800));
    }

/**
 *
 */
    public void getEosInfo(Formatter f) throws IOException {
        iosp.getEosInfo(f);
    }

/**
 *
 */
    public void closeOpenFiles() throws IOException {
        if (iosp != null) { iosp.close(); }
        iosp = null;
        attTable.clearBeans();
        messTable.clearBeans();
        objectTable.clearBeans();
        dumpTA.clear();
    }

/**
 *
 */
    public void setHdf5File(RandomAccessFile raf) throws IOException {
        closeOpenFiles();

        this.location = raf.getLocation();
        final List<ObjectBean> beanList = new ArrayList<>();

        iosp = new H5iosp();
        final NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);
        ncfile.sendIospMessage(H5iosp.IOSP_MESSAGE_INCLUDE_ORIGINAL_ATTRIBUTES);

        try {
            iosp.open(raf, ncfile, null);
        }
        catch (Throwable t) {
            final StringWriter sw = new StringWriter(20000);
            final PrintWriter s = new PrintWriter(sw);
            t.printStackTrace(s);
            dumpTA.setText(sw.toString());
        }

        final H5header header = (H5header) iosp.sendIospMessage("header");
        for (H5header.DataObject dataObj : header.getDataObjects()) {
            beanList.add(new ObjectBean(dataObj));
        }

        objectTable.setBeans(beanList);
    }

/**
 *
 */
    public void showInfo(Formatter f) throws IOException {
        if (iosp == null) { return; }

        final List<Object> objs = objectTable.getBeans();
        for (Object obj : objs) {
            final ObjectBean bean = (ObjectBean) obj;
            bean.m.show(f);
        }
    }

/**
 *
 */
    public void showInfo2(Formatter f) throws IOException {
        if (iosp == null) return;

        ByteArrayOutputStream os = new ByteArrayOutputStream(100 * 1000);
        PrintWriter pw = new PrintWriter( new OutputStreamWriter(os, CDM.utf8Charset));
        H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header H5header/headerDetails H5header/symbolTable H5header/memTracker"));
        H5header headerEmpty = (H5header) iosp.sendIospMessage("headerEmpty");
        headerEmpty.read(pw);
        H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl(""));
        pw.flush();
        f.format("%s", os.toString(CDM.utf8Charset.name()));
        H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl());
    }

/**
 *
 */
    public class ObjectBean {
        H5header.DataObject m;

    /**
     * no-arg constructor
     */
        public ObjectBean() {
        }

    /**
     * create from a dataset
     */
        public ObjectBean(H5header.DataObject m) {
          this.m = m;
        }

        public long getAddress(){
          return m.getAddress();
        }

        public String getName() {
          return m.getName();
        }

        void show(Formatter f) throws IOException {
            f.format("HDF5 object name '%s'%n", m.getName());
            //for ( H5header.HeaderMessage mess : m.getMessages()) {
            //  if (mess. instanceof  H5header.MessageDatatype)
            //}

            for ( H5header.MessageAttribute mess : m.getAttributes()) {
                final Attribute att = mess.getNcAttribute();
                f.format("  %s%n", att);
          }
        }
    }

/**
 *
 */
    public class MessageBean {
        H5header.HeaderMessage m;

    /**
     * no-arg constructor
     */
        public MessageBean() {
        }

    /**
     * create from a message
     */
        public MessageBean(H5header.HeaderMessage m) {
            this.m = m;
        }

        public String getMessageType(){
            return m.getMtype().toString();
        }

        public String getName(){
            return m.getName();
        }

        public int getSize() {
            return m.getSize();
        }

        public byte getFlags() {
            return m.getFlags();
        }

        public long getStart() {
            return m.getStart();
        }
  }

/**
 *
 */
    public class AttributeBean {
        H5header.MessageAttribute att;

    /**
     * no-arg constructor
     */
        public AttributeBean() {
        }

    /**
     * create from an attribute
     */
        public AttributeBean(H5header.MessageAttribute att) {
            this.att = att;
        }

        public byte getVersion() {
            return att.getVersion();
        }

        public String getAttributeName() {
            return att.getName();
        }

        public String getMdt() {
            return att.getMdt().toString();
        }

        public String getMds() {
            return att.getMds().toString();
        }

        public long getDataPos() {
            return att.getDataPosAbsolute();
        }

        void show(Formatter f) {
            f.format("hdf5 att = %s%n%n", att);
            try {
                f.format("netcdf attribute%n %s;%n", att.getNcAttribute());
            }
            catch (IOException e) {
                //To change body of catch statement use File | Settings | File Templates.
                e.printStackTrace();
            }
        }
    }
}
