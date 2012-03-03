package com.github.generategwtrfproxy.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

public class GenProxyWizard extends Wizard {

  private GenProxyWizardPage page;
  private IStructuredSelection selection;

  public GenProxyWizard(IStructuredSelection selection) {
    this.selection = selection;
  }

  public void addPages() {
    page = new GenProxyWizardPage(selection);
    addPage(page);
  }

  public boolean performFinish() {
    return false;
  }
}
