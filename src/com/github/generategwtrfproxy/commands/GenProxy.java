package com.github.generategwtrfproxy.commands;

import com.github.generategwtrfproxy.wizards.GenProxyWizard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class GenProxy extends AbstractHandler {
  /**
   * The constructor.
   */
  public GenProxy() {
  }

  /**
   * the command has been executed, so extract the needed information from the
   * application context.
   */
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell();
    ISelection selection = HandlerUtil.getCurrentSelection(event);

    if (selection instanceof IStructuredSelection) {
      GenProxyWizard wizard = new GenProxyWizard(
          (IStructuredSelection) selection);
      WizardDialog dialog = new WizardDialog(shell, wizard);
      dialog.create();
      dialog.open();
    }

    return null;
  }
}
