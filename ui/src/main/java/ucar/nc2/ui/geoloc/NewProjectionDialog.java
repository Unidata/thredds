/*
 * Created by JFormDesigner on Thu Oct 04 18:36:31 MDT 2012
 */

package ucar.nc2.ui.geoloc;

import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import javax.swing.*;
import javax.swing.GroupLayout;
import javax.swing.border.*;

import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.Format;
import ucar.util.prefs.ui.*;

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

  public void setProjectionTypes(Collection<Object> types) {
    cbProjectionType.setItemList(types);
  }

  public NPController getNPController() {
    return navPanel;
  }

  private static final int min_sigfig = 6;
  private void wire() {
    NavigatedPanel mapEditPanel = navPanel.getNavigatedPanel();
    mapEditPanel.addNewMapAreaListener(new NewMapAreaListener() {
      @Override
      public void actionPerformed(NewMapAreaEvent e) {
        ProjectionRect rect = e.getMapArea();
        //System.out.printf("%s%n", rect.toString2());
        minx.setText(Format.d(rect.getMinX(), min_sigfig));
        maxx.setText(Format.d(rect.getMaxX(), min_sigfig));
        miny.setText(Format.d(rect.getMinY(), min_sigfig));
        maxy.setText(Format.d(rect.getMaxY(), min_sigfig));
      }
    });

          //enable event listeners when we're done constructing the UI
    cbProjectionType.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ProjectionManager.ProjectionClass pc = (ProjectionManager.ProjectionClass) cbProjectionType.getSelectedItem();
        projectionParamPanel1.setProjection(pc);
        revalidate();
      }
    });


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
    textField1 = new JTextField();
    label1 = new JLabel();
    label2 = new JLabel();
    cbProjectionType = new ComboBox();
    cancelButton = new JButton();
    okButton = new JButton();
    button1 = new JButton();
    panel3 = new JPanel();
    maxy = new JTextField();
    label3 = new JLabel();
    minx = new JTextField();
    maxx = new JTextField();
    miny = new JTextField();
    label4 = new JLabel();
    label5 = new JLabel();
    label6 = new JLabel();
    panel2 = new JPanel();
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

          //---- label1 ----
          label1.setText("Name:");

          //---- label2 ----
          label2.setText("Type:");

          //---- cbProjectionType ----
          cbProjectionType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              comboBox1ItemStateChanged(e);
            }
          });

          //---- cancelButton ----
          cancelButton.setText("Cancel");

          //---- okButton ----
          okButton.setText("Preview");

          //---- button1 ----
          button1.setText("Save");

          //======== panel3 ========
          {
            panel3.setBorder(new TitledBorder(null, "Map Area", TitledBorder.CENTER, TitledBorder.TOP));

            //---- label3 ----
            label3.setText("max y");

            //---- label4 ----
            label4.setText("min y");

            //---- label5 ----
            label5.setText("min x");

            //---- label6 ----
            label6.setText("max x");

            GroupLayout panel3Layout = new GroupLayout(panel3);
            panel3.setLayout(panel3Layout);
            panel3Layout.setHorizontalGroup(
              panel3Layout.createParallelGroup()
                .addGroup(panel3Layout.createSequentialGroup()
                  .addComponent(minx, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(maxx))
                .addGroup(panel3Layout.createSequentialGroup()
                  .addGroup(panel3Layout.createParallelGroup()
                    .addGroup(panel3Layout.createSequentialGroup()
                      .addGap(84, 84, 84)
                      .addComponent(label3))
                    .addGroup(panel3Layout.createSequentialGroup()
                      .addGap(89, 89, 89)
                      .addComponent(label4)))
                  .addGap(0, 0, Short.MAX_VALUE))
                .addGroup(panel3Layout.createSequentialGroup()
                  .addContainerGap()
                  .addGroup(panel3Layout.createParallelGroup()
                    .addGroup(panel3Layout.createSequentialGroup()
                      .addComponent(label5)
                      .addGap(18, 18, 18)
                      .addComponent(maxy)
                      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                      .addComponent(label6)
                      .addContainerGap())
                    .addGroup(GroupLayout.Alignment.TRAILING, panel3Layout.createSequentialGroup()
                      .addGap(0, 0, Short.MAX_VALUE)
                      .addComponent(miny, GroupLayout.PREFERRED_SIZE, 121, GroupLayout.PREFERRED_SIZE)
                      .addGap(44, 44, 44))))
            );
            panel3Layout.setVerticalGroup(
              panel3Layout.createParallelGroup()
                .addGroup(panel3Layout.createSequentialGroup()
                  .addContainerGap()
                  .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addGroup(panel3Layout.createParallelGroup()
                      .addGroup(panel3Layout.createSequentialGroup()
                        .addComponent(label3)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(maxy, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                      .addComponent(label6, GroupLayout.Alignment.TRAILING))
                    .addComponent(label5))
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(maxx, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(minx, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(miny, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(label4)
                  .addContainerGap(14, Short.MAX_VALUE))
            );
          }

          //======== panel2 ========
          {
            panel2.setBorder(new TitledBorder(null, "Parameters", TitledBorder.CENTER, TitledBorder.TOP));
            panel2.setLayout(new BorderLayout());
            panel2.add(projectionParamPanel1, BorderLayout.CENTER);
          }

          GroupLayout panel1Layout = new GroupLayout(panel1);
          panel1.setLayout(panel1Layout);
          panel1Layout.setHorizontalGroup(
            panel1Layout.createParallelGroup()
              .addGroup(GroupLayout.Alignment.TRAILING, panel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(button1)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelButton))
              .addGroup(panel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel1Layout.createParallelGroup()
                  .addGroup(panel1Layout.createSequentialGroup()
                    .addGroup(panel1Layout.createParallelGroup()
                      .addGroup(panel1Layout.createSequentialGroup()
                        .addComponent(label2)
                        .addGap(9, 9, 9))
                      .addGroup(GroupLayout.Alignment.TRAILING, panel1Layout.createSequentialGroup()
                        .addComponent(label1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)))
                    .addGroup(panel1Layout.createParallelGroup()
                      .addComponent(cbProjectionType, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                      .addComponent(textField1)))
                  .addComponent(panel3, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(panel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
          );
          panel1Layout.setVerticalGroup(
            panel1Layout.createParallelGroup()
              .addGroup(panel1Layout.createSequentialGroup()
                .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                  .addComponent(textField1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                  .addComponent(label1))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                  .addComponent(label2)
                  .addComponent(cbProjectionType, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panel2, GroupLayout.PREFERRED_SIZE, 268, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 48, Short.MAX_VALUE)
                .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                  .addComponent(cancelButton)
                  .addComponent(okButton)
                  .addComponent(button1))
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
  private JTextField textField1;
  private JLabel label1;
  private JLabel label2;
  private ComboBox cbProjectionType;
  private JButton cancelButton;
  private JButton okButton;
  private JButton button1;
  private JPanel panel3;
  private JTextField maxy;
  private JLabel label3;
  private JTextField minx;
  private JTextField maxx;
  private JTextField miny;
  private JLabel label4;
  private JLabel label5;
  private JLabel label6;
  private JPanel panel2;
  private ProjectionParamPanel projectionParamPanel1;
  private NPController navPanel;
  private JPanel buttonBar;
  // JFormDesigner - End of variables declaration  //GEN-END:variables
}
