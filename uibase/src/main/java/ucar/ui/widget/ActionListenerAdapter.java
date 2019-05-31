package ucar.ui.widget;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Wrap an action listener to filter on event type.
 *  Prevent the  delegate from causing more events. */
public class ActionListenerAdapter implements ActionListener {
  private final String actionCommand;
  private final ActionListener delegate;

  /**
   * Recieve events only when event type equals actionCommand name.
   */
  public ActionListenerAdapter(String actionCommand, ActionListener delegate) {
    this.actionCommand = actionCommand;
    this.delegate = delegate;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals(actionCommand)) {
      delegate.actionPerformed(e);
    }
  }
}
