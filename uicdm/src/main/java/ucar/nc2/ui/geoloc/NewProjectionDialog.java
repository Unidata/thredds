/*
 * Created by JFormDesigner on Thu Oct 04 18:36:31 MDT 2012
 */

package ucar.nc2.ui.geoloc;

import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import javax.swing.*;
import javax.swing.border.*;

import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.Format;
import ucar.ui.prefs.*;

/**
 * @author John Caron
 */
public class NewProjectionDialog extends JDialog {
  public NewProjectionDialog(Frame owner) {
    super(owner);
    initComponents();
    wire();
  }

  public NewProjectionDialog(Dialog owner) {
    super(owner);
    initComponents();
    wire();
  }

  public void setProjectionManager(ProjectionManager pm, Collection<Object> types) {
    this.pm = pm;
    cbProjectionType.setItemList(types);
  }

  public NPController getNPController() {
    return navPanel;
  }

  private static final int min_sigfig = 6;
  private ProjectionManager pm;

  private void wire() {
    NavigatedPanel mapEditPanel = navPanel.getNavigatedPanel();
    mapEditPanel.addNewMapAreaListener(new NewMapAreaListener() {
      // nav panel moved
      public void actionPerformed(NewMapAreaEvent e) {
        ProjectionRect rect = e.getMapArea();
        //System.out.printf("%s%n", rect.toString2());
        minx.setText(Format.d(rect.getMinX(), min_sigfig));
        maxx.setText(Format.d(rect.getMaxX(), min_sigfig));
        miny.setText(Format.d(rect.getMinY(), min_sigfig));
        maxy.setText(Format.d(rect.getMaxY(), min_sigfig));
      }
    });

    // projection class was chosen
    cbProjectionType.addActionListener(e -> {
        ProjectionManager.ProjectionClass pc = (ProjectionManager.ProjectionClass) cbProjectionType.getSelectedItem();
        projectionParamPanel1.setProjection(pc);
        pc.makeDefaultProjection();
        pc.putParamIntoDialog(pc.projInstance);
        navPanel.setProjection(pc.projInstance);
        invalidate();
        validate();
    });

    // apply button was pressed
    applyButton.addActionListener(e -> {
        ProjectionManager.ProjectionClass pc = (ProjectionManager.ProjectionClass) cbProjectionType.getSelectedItem();
        pc.setProjFromDialog(pc.projInstance);
        System.out.printf("Projection = %s%n", pc.projInstance);

        ProjectionRect mapArea = getMapAreaFromDialog();
        if (mapArea != null) {
          pc.projInstance.setDefaultMapArea(mapArea);
          System.out.printf("mapArea = %s%n", mapArea.toString2(4));
        }

        projectionParamPanel1.setProjection(pc);
        pc.putParamIntoDialog(pc.projInstance);
        navPanel.setProjection(pc.projInstance);
        if (mapArea != null)
          navPanel.getNavigatedPanel().setMapArea(mapArea);

        invalidate();
        validate();
    });

  }

  ProjectionRect getMapAreaFromDialog() {

    try {
      double minxv = Double.parseDouble(minx.getText());
      double maxxv = Double.parseDouble(maxx.getText());
      double minyv = Double.parseDouble(miny.getText());
      double maxyv = Double.parseDouble(maxy.getText());
      return new ProjectionRect(minxv, minyv, maxxv, maxyv);
    } catch (Exception e) {
      System.out.printf("Illegal value %s%n", e);
      return null;
    }
  }


  private void comboBox1ItemStateChanged(ItemEvent e) {
    System.out.printf("%s%n", e);
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    dialogPane = new JPanel();
    contentPanel = new JPanel();
    panel1 = new JPanel();
    cancelButton = new JButton();
    okButton = new JButton();
    applyButton = new JButton();
    MapArePanel = new JPanel();
    maxy = new JTextField();
    label3 = new JLabel();
    minx = new JTextField();
    maxx = new JTextField();
    miny = new JTextField();
    label4 = new JLabel();
    label5 = new JLabel();
    label6 = new JLabel();
    ProjPanel = new JPanel();
    cbProjectionType = new ComboBox();
    projectionParamPanel1 = new ProjectionParamPanel();
    navPanel = new NPController();
    buttonBar = new JPanel();

    //======== this ========
    setTitle("Projection Manager");
    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    //======== dialogPane ========
    {
      dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
      dialogPane.setLayout(new BorderLayout());

      //======== contentPanel ========
      {
        contentPanel.setLayout(new BorderLayout());

        //======== panel1 ========
        {

          //---- cancelButton ----
          cancelButton.setText("Cancel");

          //---- okButton ----
          okButton.setText("Save");

          //---- applyButton ----
          applyButton.setText("Apply");

          //======== MapArePanel ========
          {
            MapArePanel.setBorder(new TitledBorder(null, "Map Area", TitledBorder.CENTER, TitledBorder.TOP));

            //---- label3 ----
            label3.setText("max y");

            //---- label4 ----
            label4.setText("min y");

            //---- label5 ----
            label5.setText("min x");

            //---- label6 ----
            label6.setText("max x");

            GroupLayout MapArePanelLayout = new GroupLayout(MapArePanel);
            MapArePanel.setLayout(MapArePanelLayout);
            MapArePanelLayout.setHorizontalGroup(
              MapArePanelLayout.createParallelGroup()
                .addGroup(MapArePanelLayout.createSequentialGroup()
                  .addComponent(minx, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(maxx))
                .addGroup(MapArePanelLayout.createSequentialGroup()
                  .addGroup(MapArePanelLayout.createParallelGroup()
                    .addGroup(MapArePanelLayout.createSequentialGroup()
                      .addGap(84, 84, 84)
                      .addComponent(label3))
                    .addGroup(MapArePanelLayout.createSequentialGroup()
                      .addGap(89, 89, 89)
                      .addComponent(label4)))
                  .addGap(0, 0, Short.MAX_VALUE))
                .addGroup(MapArePanelLayout.createSequentialGroup()
                  .addContainerGap()
                  .addGroup(MapArePanelLayout.createParallelGroup()
                    .addGroup(MapArePanelLayout.createSequentialGroup()
                      .addComponent(label5)
                      .addGap(18, 18, 18)
                      .addComponent(maxy)
                      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                      .addComponent(label6)
                      .addContainerGap())
                    .addGroup(GroupLayout.Alignment.TRAILING, MapArePanelLayout.createSequentialGroup()
                      .addGap(0, 0, Short.MAX_VALUE)
                      .addComponent(miny, GroupLayout.PREFERRED_SIZE, 121, GroupLayout.PREFERRED_SIZE)
                      .addGap(44, 44, 44))))
            );
            MapArePanelLayout.setVerticalGroup(
              MapArePanelLayout.createParallelGroup()
                .addGroup(MapArePanelLayout.createSequentialGroup()
                  .addContainerGap()
                  .addGroup(MapArePanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addGroup(MapArePanelLayout.createParallelGroup()
                      .addGroup(MapArePanelLayout.createSequentialGroup()
                        .addComponent(label3)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxy, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                      .addComponent(label6, GroupLayout.Alignment.TRAILING))
                    .addComponent(label5))
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(MapArePanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(maxx, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(minx, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(miny, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(label4)
                  .addContainerGap(14, Short.MAX_VALUE))
            );
          }

          //======== ProjPanel ========
          {
            ProjPanel.setBorder(new TitledBorder(null, "Projection Class", TitledBorder.CENTER, TitledBorder.TOP));
            ProjPanel.setLayout(new BoxLayout(ProjPanel, BoxLayout.X_AXIS));

            //---- cbProjectionType ----
            cbProjectionType.addItemListener(this::comboBox1ItemStateChanged);
            ProjPanel.add(cbProjectionType);
          }

          //---- projectionParamPanel1 ----
          projectionParamPanel1.setBorder(new TitledBorder(null, "Parameters", TitledBorder.CENTER, TitledBorder.TOP));

          GroupLayout panel1Layout = new GroupLayout(panel1);
          panel1.setLayout(panel1Layout);
          panel1Layout.setHorizontalGroup(
            panel1Layout.createParallelGroup()
              .addGroup(GroupLayout.Alignment.TRAILING, panel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(applyButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelButton))
              .addGroup(panel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel1Layout.createParallelGroup()
                  .addComponent(ProjPanel, GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
                  .addComponent(MapArePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(projectionParamPanel1, GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE))
                .addContainerGap())
          );
          panel1Layout.setVerticalGroup(
            panel1Layout.createParallelGroup()
              .addGroup(panel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ProjPanel, GroupLayout.PREFERRED_SIZE, 44, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(projectionParamPanel1, GroupLayout.PREFERRED_SIZE, 252, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(MapArePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 72, Short.MAX_VALUE)
                .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                  .addComponent(cancelButton)
                  .addComponent(okButton)
                  .addComponent(applyButton))
                .addContainerGap())
          );
        }
        contentPanel.add(panel1, BorderLayout.EAST);

        //---- navPanel ----
        navPanel.setPreferredSize(new Dimension(500, 250));
        contentPanel.add(navPanel, BorderLayout.CENTER);
      }
      dialogPane.add(contentPanel, BorderLayout.CENTER);

      //======== buttonBar ========
      {
        buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
        buttonBar.setLayout(new FlowLayout());
      }
      dialogPane.add(buttonBar, BorderLayout.SOUTH);
    }
    contentPane.add(dialogPane, BorderLayout.CENTER);
    pack();
    setLocationRelativeTo(getOwner());
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JPanel dialogPane;
  private JPanel contentPanel;
  private JPanel panel1;
  private JButton cancelButton;
  private JButton okButton;
  private JButton applyButton;
  private JPanel MapArePanel;
  private JTextField maxy;
  private JLabel label3;
  private JTextField minx;
  private JTextField maxx;
  private JTextField miny;
  private JLabel label4;
  private JLabel label5;
  private JLabel label6;
  private JPanel ProjPanel;
  private ComboBox cbProjectionType;
  private ProjectionParamPanel projectionParamPanel1;
  private NPController navPanel;
  private JPanel buttonBar;
  // JFormDesigner - End of variables declaration  //GEN-END:variables
}
