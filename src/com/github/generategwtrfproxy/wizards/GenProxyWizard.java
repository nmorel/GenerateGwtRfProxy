package com.github.generategwtrfproxy.wizards;

import com.github.generategwtrfproxy.Activator;
import com.github.generategwtrfproxy.beans.Method;
import com.github.generategwtrfproxy.util.Utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class GenProxyWizard extends Wizard {

  private static String lineDelimiter = System.getProperty(
      "line.separator", "\n"); //$NON-NLS-N$

  private GenProxyWizardPage page;
  private IType primaryProxyFor;

  private boolean isDone;

  public GenProxyWizard(IStructuredSelection selection) {

    primaryProxyFor = ((ICompilationUnit) selection.getFirstElement()).findPrimaryType();

    setNeedsProgressMonitor(true);
    setForcePreviousAndNextButtons(true);
    setWindowTitle("New Proxy");
  }

  public void addPages() {
    page = new GenProxyWizardPage(primaryProxyFor);
    addPage(page);
  }

  @Override
  public boolean performFinish() {
    try {
      super.getContainer().run(false, false, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
          isDone = finish(monitor);
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return isDone;
  }

  private boolean finish(IProgressMonitor desiredMonitor) {
    IProgressMonitor monitor = desiredMonitor;
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }

    try {
      monitor.subTask("Creating proxy");

      IType proxyFor = page.getProxyFor();
      IPackageFragment pkg = page.getPackageFragment();
      String proxyName = page.getTypeName();
      boolean entityProxy = page.isEntityProxy();
      List<Method> selectedMethods = page.getSelectedMethods();

      ICompilationUnit cu = pkg.createCompilationUnit(proxyName + ".java", "",
          false, new SubProgressMonitor(monitor, 1));

      cu.becomeWorkingCopy(new SubProgressMonitor(monitor, 1));

      ImportRewrite imports = CodeStyleConfiguration.createImportRewrite(cu,
          false);
      imports.addImport(proxyFor.getFullyQualifiedName());
      imports.addImport("com.google.web.bindery.requestfactory.shared.ProxyFor");
      if (entityProxy) {
        imports.addImport("com.google.web.bindery.requestfactory.shared.EntityProxy");
      } else {
        imports.addImport("com.google.web.bindery.requestfactory.shared.ValueProxy");
      }

      cu.createPackageDeclaration(pkg.getElementName(), new SubProgressMonitor(
          monitor, 1));

      IBuffer buffer = cu.getBuffer();
      buffer.append("@ProxyFor(");
      buffer.append(proxyFor.getElementName());
      buffer.append(".class)");
      buffer.append(lineDelimiter);
      buffer.append("public interface ");
      buffer.append(proxyName);
      buffer.append(" extends ");
      if (entityProxy) {
        buffer.append("EntityProxy");
      } else {
        buffer.append("ValueProxy");
      }
      buffer.append(" {");
      buffer.append(lineDelimiter);

      for (Method method : selectedMethods) {
        buffer.append(method.toString());
        buffer.append(";");
        buffer.append(lineDelimiter);

        imports.addImport(method.getParamOrReturnTypeBinding());
      }

      buffer.append("}");

      cu.applyTextEdit(
          imports.rewriteImports(new SubProgressMonitor(monitor, 1)),
          new SubProgressMonitor(monitor, 1));

      String formattedContent = Utils.format(buffer.getContents(),
          CodeFormatter.K_COMPILATION_UNIT);
      buffer.setContents(formattedContent);

      cu.commitWorkingCopy(false, new SubProgressMonitor(monitor, 1));

      monitor.worked(1);
    } catch (Exception e) {
      e.printStackTrace();

      IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
          "An unexpected error has happened. Close the wizard and retry.", e);

      ErrorDialog.openError(getShell(), null, null, status);

      return false;
    }

    monitor.done();
    return true;
  }
}
