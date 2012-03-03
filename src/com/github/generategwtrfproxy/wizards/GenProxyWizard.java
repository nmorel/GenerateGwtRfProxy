package com.github.generategwtrfproxy.wizards;

import com.github.generategwtrfproxy.Activator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class GenProxyWizard extends Wizard {

  private static List<String> getElementSignatures(String typeName) {
    List<String> list = new ArrayList<String>();
    if (Signature.getTypeSignatureKind(typeName) == Signature.ARRAY_TYPE_SIGNATURE) {
      list.add(Signature.getElementType(typeName));
    } else {
      String[] names = Signature.getTypeArguments(typeName);
      list.add(Signature.getTypeErasure(typeName));
      if (names.length > 0) {
        list.add(names[0]);
      }
    }
    return list;
  }
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
      IPackageFragmentRoot srcFolder = page.getPackageFragmentRoot();
      IPackageFragment pkg = page.getPackageFragment();
      String proxyName = page.getTypeName();
      boolean entityProxy = page.isEntityProxy();
      List<IMethod> selectedMethods = page.getSelectedMethods();

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
      buffer.append("public interface ");
      buffer.append(proxyName);
      buffer.append(" extends ");
      if (entityProxy) {
        buffer.append("EntityProxy");
      } else {
        buffer.append("ValueProxy");
      }
      buffer.append(" {");

      for (IMethod method : selectedMethods) {
        String methodName = method.getElementName();
        if (methodName.startsWith("get") || methodName.startsWith("is") || methodName.startsWith("has")) { //$NON-NLS-N$           

          String returnType = Signature.toString(method.getReturnType());
          System.out.println(returnType);
          for(IImportDeclaration importD : proxyFor.getCompilationUnit().getImports()){
            String importName = importD.getElementName();
            if(importName.endsWith(returnType)){
              imports.addImport(importName);
              break;
            }
          }
//         System.out.println(getElementSignatures(method.getReturnType()));
//          System.out.println(methodName);
//          System.out.println(method.getSignature());
//          System.out.println(Signature.toString(method.getReturnType()));
//          System.out.println(Signature.getSignatureQualifier(method.getReturnType()));
//          System.out.println(Signature.getSignatureSimpleName(method.getReturnType()));
          
//          System.out.println(Signature.getReturnType(method.getSignature()));
//          imports.addImport(Signature.getSignatureQualifier(method.getReturnType())+"."+returnType);
          buffer.append(returnType);
          buffer.append(" ");
          buffer.append(methodName);
          buffer.append("();");
          
        } else if (methodName.startsWith("set")) { 
          buffer.append("void ");
          buffer.append(methodName);
          buffer.append("(");
         ILocalVariable parameter = method.getParameters()[0];
         String paramType = Signature.toString(parameter.getTypeSignature());
         for(IImportDeclaration importD : proxyFor.getCompilationUnit().getImports()){
           String importName = importD.getElementName();
           if(importName.endsWith(paramType)){
             imports.addImport(importName);
             break;
           }
         }
         buffer.append(paramType);
         buffer.append(" ");
         buffer.append(parameter.getElementName());         
          buffer.append(");");
//          int index = signature.lastIndexOf(entityName);
//          signature = signature.substring(0, index) + entityName + "Proxy" //$NON-NLS-N$
//              + signature.substring(index + entityName.length());
        }
      }

      buffer.append("}");

      cu.applyTextEdit(
          imports.rewriteImports(new SubProgressMonitor(monitor, 1)),
          new SubProgressMonitor(monitor, 1));

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
