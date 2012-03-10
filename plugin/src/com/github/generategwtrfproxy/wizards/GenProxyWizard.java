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
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class GenProxyWizard extends Wizard implements INewWizard {

  private static String lineDelimiter = System.getProperty(
      "line.separator", "\n"); //$NON-NLS-N$

  private GenProxyWizardPage page;

  private boolean isDone;

  public GenProxyWizard() {
    setNeedsProgressMonitor(true);
    setForcePreviousAndNextButtons(true);
    setWindowTitle("New Proxy");
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    page = new GenProxyWizardPage();
    addPage(page);

    Object element = selection.getFirstElement();
    if (element instanceof ICompilationUnit) {
      page.setProxyFor(((ICompilationUnit) selection.getFirstElement()).findPrimaryType());
    } else if (element instanceof IPackageFragmentRoot) {
      page.setPackageFragmentRoot((IPackageFragmentRoot) element, true);
    } else if (element instanceof IPackageFragment) {
      IPackageFragment pkg = (IPackageFragment) element;
      IPackageFragmentRoot root = null;
      IJavaElement parent = pkg.getParent();
      while (null != parent) {
        if (parent instanceof IPackageFragmentRoot) {
          root = (IPackageFragmentRoot) parent;
          break;
        }
        parent = parent.getParent();
      }
      page.setPackageFragmentRoot(root, true);
      page.setPackageFragment(pkg, true);
    }
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
    ICompilationUnit cu = null;
    try {
      monitor.subTask("Creating proxy");

      IType proxyFor = page.getProxyFor();
      IPackageFragment pkg = page.getPackageFragment();
      String proxyName = page.getTypeName();
      boolean isAnnotProxyFor = page.isAnnotProxyFor();
      boolean entityProxy = page.isEntityProxy();
      List<Method> selectedMethods = page.getSelectedMethods();

      cu = pkg.createCompilationUnit(proxyName + ".java", "", false,
          new SubProgressMonitor(monitor, 1));

      cu.becomeWorkingCopy(new SubProgressMonitor(monitor, 1));

      ImportRewrite imports = CodeStyleConfiguration.createImportRewrite(cu,
          false);
      imports.addImport(proxyFor.getFullyQualifiedName());
      if (isAnnotProxyFor) {
        imports.addImport("com.google.web.bindery.requestfactory.shared.ProxyFor");
      } else {
        imports.addImport("com.google.web.bindery.requestfactory.shared.ProxyForName");
      }
      if (entityProxy) {
        imports.addImport("com.google.web.bindery.requestfactory.shared.EntityProxy");
      } else {
        imports.addImport("com.google.web.bindery.requestfactory.shared.ValueProxy");
      }

      cu.createPackageDeclaration(pkg.getElementName(), new SubProgressMonitor(
          monitor, 1));

      IBuffer buffer = cu.getBuffer();
      if (isAnnotProxyFor) {
        buffer.append("@ProxyFor(value=");
        buffer.append(proxyFor.getElementName());
        buffer.append(".class)");
      } else {
        buffer.append("@ProxyForName(value=\"");
        buffer.append(proxyFor.getFullyQualifiedName());
        buffer.append("\")");
      }
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
    } finally {
      if (null != cu) {
        try {
          cu.discardWorkingCopy();
        } catch (JavaModelException e) {
          e.printStackTrace();
        }
      }
    }

    monitor.done();
    return true;
  }
}
