/*
 * Created by JFormDesigner on Sun Sep 26 17:14:16 MDT 2010
 */

package ucar.nc2.ui.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import ucar.grib.grib1.GribPDSParamTable;

/**
 * @author John Caron
 */
public class Grib1TableDialog extends JDialog {
  public Grib1TableDialog(Frame owner) {
    super(owner);
    initComponents();
  }

  public Grib1TableDialog(Dialog owner) {
    super(owner);
    initComponents();
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    dialogPane = new JPanel();
    contentPanel = new JPanel();
    label1 = new JLabel();
    center = new JTextField();
    label2 = new JLabel();
    label3 = new JLabel();
    subcenter = new JTextField();
    version = new JTextField();
    label4 = new JLabel();
    result = new JTextField();
    buttonBar = new JPanel();
    okButton = new JButton();
    cancelButton = new JButton();
    action1 = new OkAction();
    action2 = new cancelAction();

    //======== this ========
    setTitle("Get Grib1 Table Used");
    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    //======== dialogPane ========
    {
      dialogPane.setBorder(Borders.DIALOG_BORDER);
      dialogPane.setLayout(new BorderLayout());

      //======== contentPanel ========
      {

        //---- label1 ----
        label1.setText("center:");
        label1.setHorizontalAlignment(SwingConstants.RIGHT);

        //---- center ----
        center.setText("7");

        //---- label2 ----
        label2.setText("sub-center:");
        label2.setHorizontalAlignment(SwingConstants.RIGHT);

        //---- label3 ----
        label3.setText("table version:");
        label3.setHorizontalAlignment(SwingConstants.RIGHT);

        //---- subcenter ----
        subcenter.setText("4");

        //---- version ----
        version.setText("128");

        //---- label4 ----
        label4.setText("Table Used:");

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(GroupLayout.Alignment.TRAILING, contentPanelLayout.createSequentialGroup()
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                .addGroup(GroupLayout.Alignment.LEADING, contentPanelLayout.createSequentialGroup()
                  .addContainerGap()
                  .addComponent(label4)
                  .addGap(18, 18, 18)
                  .addComponent(result, GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE))
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(label3, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                    .addComponent(label2, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                    .addComponent(label1, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE))
                  .addGroup(contentPanelLayout.createParallelGroup()
                    .addGroup(contentPanelLayout.createSequentialGroup()
                      .addGap(12, 12, 12)
                      .addGroup(contentPanelLayout.createParallelGroup()
                        .addComponent(version, GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                        .addComponent(subcenter, GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)))
                    .addGroup(contentPanelLayout.createSequentialGroup()
                      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                      .addComponent(center, GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)))))
              .addContainerGap())
        );
        contentPanelLayout.setVerticalGroup(
          contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup()
              .addGap(21, 21, 21)
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                .addComponent(center, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(label1))
              .addGap(9, 9, 9)
              .addGroup(contentPanelLayout.createParallelGroup()
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addComponent(label2)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(label3, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
                .addGroup(contentPanelLayout.createSequentialGroup()
                  .addComponent(subcenter, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(version, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
              .addGap(42, 42, 42)
              .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(result, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(label4))
              .addContainerGap(117, Short.MAX_VALUE))
        );
      }
      dialogPane.add(contentPanel, BorderLayout.EAST);

      //======== buttonBar ========
      {
        buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);

        //---- okButton ----
        okButton.setAction(action1);

        //---- cancelButton ----
        cancelButton.setAction(action2);

        GroupLayout buttonBarLayout = new GroupLayout(buttonBar);
        buttonBar.setLayout(buttonBarLayout);
        buttonBarLayout.setHorizontalGroup(
          buttonBarLayout.createParallelGroup()
            .addGroup(buttonBarLayout.createSequentialGroup()
              .addGap(214, 214, 214)
              .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
              .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
              .addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
              .addContainerGap())
        );
        buttonBarLayout.setVerticalGroup(
          buttonBarLayout.createParallelGroup()
            .addComponent(okButton)
            .addGroup(buttonBarLayout.createSequentialGroup()
              .addComponent(cancelButton, GroupLayout.DEFAULT_SIZE, 19, Short.MAX_VALUE)
              .addContainerGap())
        );
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
  private JLabel label1;
  private JTextField center;
  private JLabel label2;
  private JLabel label3;
  private JTextField subcenter;
  private JTextField version;
  private JLabel label4;
  private JTextField result;
  private JPanel buttonBar;
  private JButton okButton;
  private JButton cancelButton;
  private OkAction action1;
  private cancelAction action2;
  // JFormDesigner - End of variables declaration  //GEN-END:variables

  private class OkAction extends AbstractAction {
    private OkAction() {
      // JFormDesigner - Action initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
      // Generated using JFormDesigner non-commercial license
      putValue(NAME, "OK");
      // JFormDesigner - End of action initialization  //GEN-END:initComponents
    }

    public void actionPerformed(ActionEvent e) {
      try {
        int center_id = Integer.parseInt( center.getText());
        int subcenter_id = Integer.parseInt( subcenter.getText());
        int version_id = Integer.parseInt( version.getText());
        GribPDSParamTable t = GribPDSParamTable.getParameterTable( center_id, subcenter_id, version_id);
        if (t == null)
          result.setText("NOT FOUND");
        else
          result.setText(t.getCenter_id()+" "+t.getSubcenter_id()+" "+t.getTable_number()+": "+t.getFilename());

      } catch (Exception ee) {

      }
    }
  }

  private class cancelAction extends AbstractAction {
    private cancelAction() {
      // JFormDesigner - Action initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
      // Generated using JFormDesigner non-commercial license
      putValue(NAME, "Cancel");
      // JFormDesigner - End of action initialization  //GEN-END:initComponents
    }

    public void actionPerformed(ActionEvent e) {
      setVisible(false);
    }
  }
}
