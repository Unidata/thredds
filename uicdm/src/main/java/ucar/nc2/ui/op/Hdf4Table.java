/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.iosp.hdf4.H4header;
import ucar.nc2.iosp.hdf4.H4iosp;
import ucar.ui.widget.TextHistoryPane;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * ToolsUI/Iosp/Hdf4
 *
 * @author caron
 */
public class Hdf4Table extends JPanel {
    private PreferencesExt prefs;

    private BeanTable tagTable;
    // private BeanTable messTable, attTable;
    private JSplitPane split;

    private TextHistoryPane dumpTA;
    // private TextHistoryPane infoTA;
    // private IndependentWindow infoWindow;

    private H4iosp iosp;
    private H4header header;
    private String location;

/**
 *
 */
    public Hdf4Table(PreferencesExt prefs) {
        this.prefs = prefs;

        tagTable = new BeanTable(TagBean.class, (PreferencesExt) prefs.node("Hdf4Object"), false);
        tagTable.addListSelectionListener(e -> {
            final TagBean bean = (TagBean) tagTable.getSelectedBean();
            dumpTA.setText("Tag=\n ");
            dumpTA.appendLine(bean.tag.detail());
            dumpTA.appendLine("\nVinfo=");
            dumpTA.appendLine(bean.tag.getVinfo());
        });

        /* messTable = new BeanTable(MessageBean.class, (PreferencesExt) prefs.node("MessBean"), false);
        messTable.addListSelectionListener(e -> {
            MessageBean mb = (MessageBean) messTable.getSelectedBean();
            dumpTA.setText( mb.m.toString());
        });

        thredds.ui.PopupMenu varPopup = new thredds.ui.PopupMenu(messTable.getJTable(), "Options");
        varPopup.addAction("Show FractalHeap", new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            MessageBean mb = (MessageBean) messTable.getSelectedBean();

            if (infoTA == null) {
              infoTA = new TextHistoryPane();
              infoWindow = new IndependentWindow("Extra", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
              infoWindow.setBounds(new Rectangle(300, 300, 500, 800));
            }
            infoTA.clear();
            Formatter f = new Formatter();
            mb.m.showFractalHeap(f);

            infoTA.appendLine(f.toString());
            infoTA.gotoTop();
            infoWindow.showIfNotIconified();
          }
        });

        attTable = new BeanTable(AttributeBean.class, (PreferencesExt) prefs.node("AttBean"), false);
        attTable.addListSelectionListener(e -> {
            AttributeBean mb = (AttributeBean) attTable.getSelectedBean();
            dumpTA.setText( mb.att.toString());
        }); */

        // the info window
        dumpTA = new TextHistoryPane();

        //splitH = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, objectTable, dumpTA);
        //splitH.setDividerLocation(prefs.getInt("splitPosH", 600));

        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, tagTable, dumpTA);
        split.setDividerLocation(prefs.getInt("splitPos", 500));

        //split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, split, attTable);
        //split2.setDividerLocation(prefs.getInt("splitPos2", 500));

        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
    }

/**
 *
 */
    public void save() {
        tagTable.saveState(false);
        //messTable.saveState(false);
        //attTable.saveState(false);
        // prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
        prefs.putInt("splitPos", split.getDividerLocation());
        //prefs.putInt("splitPos2", split2.getDividerLocation());
        //prefs.putInt("splitPosH", splitH.getDividerLocation());
    }

/**
 *
 */
    public void closeOpenFiles() throws IOException {
        if (iosp != null) { iosp.close(); }
        iosp = null;
    }

/**
 *
 */
    public void setHdf4File(RandomAccessFile raf) throws IOException {
        closeOpenFiles();

        this.location = raf.getLocation();
        final List<TagBean> beanList = new ArrayList<>();

        iosp = new H4iosp();
        final NetcdfFile ncfile = new NetcdfFileSubclass(iosp, location);

        try {
            iosp.open(raf, ncfile, null);
        }
        catch (Throwable t) {
            final StringWriter sw = new StringWriter(20000);
            t.printStackTrace(new PrintWriter(sw));
            dumpTA.setText(sw.toString());
        }

        header = (H4header) iosp.sendIospMessage("header");
        for (H4header.Tag tag : header.getTags ()) {
            beanList.add(new TagBean(tag));
        }

        tagTable.setBeans(beanList);
    }

/**
 *
 */
    public void getEosInfo(Formatter f) throws IOException {
        header.getEosInfo(f);
    }

/**
 *
 */
    public class TagBean {
        H4header.Tag tag;

    /**
     * no-arg constructor
     */
        public TagBean() {
        }

    /**
     * create from a tag
     */
        public TagBean(H4header.Tag tag) {
            this.tag = tag;
        }

        public short getCode(){
            return tag.getCode();
        }

        public String getType() {
            return tag.getType();
        }

        public short getRefno() {
            return tag.getRefno();
        }

        public boolean isExtended() {
            return tag.isExtended();
        }

        public String getVClass() {
            return tag.getVClass();
        }

        public int getOffset() {
            return tag.getOffset();
        }

        public int getLength() {
            return tag.getLength();
        }

        public boolean isUsed() {
            return tag.isUsed();
        }

        public String getVinfo() {
            return tag.getVinfo();
        }
    }
}
