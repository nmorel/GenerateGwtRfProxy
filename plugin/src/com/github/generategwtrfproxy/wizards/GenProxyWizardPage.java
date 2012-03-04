package com.github.generategwtrfproxy.wizards;

import com.github.generategwtrfproxy.beans.Method;
import com.github.generategwtrfproxy.visitors.MethodVisitor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import java.util.ArrayList;
import java.util.List;

public class GenProxyWizardPage extends NewTypeWizardPage {

  private class MethodProvider implements IStructuredContentProvider {

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getElements(Object inputElement) {
      return new Object[] {inputElement};
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

  }

  private static final String PAGE_NAME = "GenProxyWizardPage";

  private IType proxyFor;

  private Button entityProxy;
  private Button valueProxy;
  private CheckboxTableViewer methodsTable;

  protected GenProxyWizardPage(IType selectedPojo) {
    super(false, PAGE_NAME);
    setTitle("Generate Proxy");
    setDescription("Select the getter/setter you want in your proxy");

    this.proxyFor = selectedPojo;

    initContainerPage(selectedPojo);
    initTypePage(selectedPojo);
  }

  public void createControl(Composite parent) {
    initializeDialogUnits(parent);

    Composite container = new Composite(parent, SWT.NULL);
    container.setFont(parent.getFont());

    int nbColumns = 4;

    GridLayout layout = new GridLayout();
    layout.numColumns = nbColumns;
    layout.verticalSpacing = 5;
    container.setLayout(layout);

    createContainerControls(container, nbColumns);
    createPackageControls(container, nbColumns);
    createTypeNameControls(container, nbColumns);

    createProxyTypeControls(container, nbColumns);

    createGettersSettersSelectionControls(container, nbColumns);

    setControl(container);
    setFocus();
    setDefaultValues();

    Dialog.applyDialogFont(container);
  }

  public IType getProxyFor() {
    return proxyFor;
  }

  public List<Method> getSelectedMethods() {
    List<Method> methods = new ArrayList<Method>();
    for (Object obj : methodsTable.getCheckedElements()) {
      methods.add((Method) obj);
    }
    return methods;
  }

  public boolean isEntityProxy() {
    return entityProxy.getSelection();
  }

  @Override
  protected void handleFieldChanged(String fieldName) {
    doStatusUpdate();
    super.handleFieldChanged(fieldName);
  }

  private void createGettersSettersSelectionControls(Composite container,
      int nbColumns) {
    Label label = new Label(container, SWT.NULL);
    label.setText("Methods:");
    label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

    methodsTable = CheckboxTableViewer.newCheckList(container, SWT.BORDER
        | SWT.V_SCROLL | SWT.H_SCROLL);
    methodsTable.setLabelProvider(new LabelProvider());
    methodsTable.setContentProvider(new MethodProvider());
    methodsTable.setSelection(new StructuredSelection(), true);

    Table table = methodsTable.getTable();
    GridData gd = new GridData();
    gd.horizontalAlignment = GridData.FILL;
    gd.horizontalSpan = nbColumns - 2;
    gd.heightHint = 300;
    table.setLayoutData(gd);

    TableLayout layout = new TableLayout();
    layout.addColumnData(new ColumnWeightData(100, false));
    table.setLayout(layout);
    table.setHeaderVisible(false);

    TableColumn columnName = new TableColumn(table, SWT.LEFT);
    columnName.setText("Name");

    Composite buttons = new Composite(container, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 1;
    buttons.setLayout(gridLayout);
    buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

    Button selectAllMethods = new Button(buttons, SWT.PUSH);
    selectAllMethods.setText("Select All");
    selectAllMethods.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    selectAllMethods.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        methodsTable.setAllChecked(true);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        methodsTable.setAllChecked(true);
      }
    });

    Button deselectAllMethods = new Button(buttons, SWT.PUSH);
    deselectAllMethods.setText("Deselect All");
    deselectAllMethods.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    deselectAllMethods.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        methodsTable.setAllChecked(false);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        methodsTable.setAllChecked(false);
      }
    });

    Button selectGetters = new Button(buttons, SWT.PUSH);
    selectGetters.setText("Select Getters");
    selectGetters.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    selectGetters.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        selectGetterOrSetter(true);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        selectGetterOrSetter(true);
      }
    });

    Button selectSetters = new Button(buttons, SWT.PUSH);
    selectSetters.setText("Select Setters");
    selectSetters.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    selectSetters.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        selectGetterOrSetter(false);
      }

      @Override
      public void widgetSelected(SelectionEvent e) {
        selectGetterOrSetter(false);
      }
    });
  }

  private void createProxyTypeControls(Composite container, int nbColumns) {
    Label label = new Label(container, SWT.NULL);
    label.setText("Proxy type:");

    GridData gd = new GridData(GridData.FILL);
    gd.horizontalSpan = nbColumns - 1;

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.verticalSpacing = 5;

    Composite checks = new Composite(container, SWT.NULL);
    checks.setLayoutData(gd);
    checks.setLayout(layout);

    entityProxy = new Button(checks, SWT.RADIO);
    entityProxy.setText("EntityProxy");

    valueProxy = new Button(checks, SWT.RADIO);
    valueProxy.setText("ValueProxy");
  }

  // ------ validation --------
  private void doStatusUpdate() {
    // status of all used components
    IStatus[] status = new IStatus[] {
        fContainerStatus, fPackageStatus, fTypeNameStatus};

    // the mode severe status will be displayed and the OK button
    // enabled/disabled.
    updateStatus(status);
  }

  private CompilationUnit parse(ICompilationUnit unit) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setSource(unit);
    parser.setResolveBindings(true);
    return (CompilationUnit) parser.createAST(null); // parse
  }

  private void selectGetterOrSetter(boolean getters) {
    for (int i = 0; i < methodsTable.getTable().getItemCount(); i++) {
      Method method = (Method) methodsTable.getElementAt(i);
      if (method.isGetter()) {
        methodsTable.setChecked(method, getters);
      } else {
        methodsTable.setChecked(method, !getters);
      }
    }
  }

  private void setDefaultValues() {
    setTypeName(proxyFor.getElementName() + "Proxy", true);
    entityProxy.setSelection(true);

    CompilationUnit parse = parse(proxyFor.getCompilationUnit());
    MethodVisitor visitor = new MethodVisitor();
    parse.accept(visitor);

    for (Method method : visitor.getMethods()) {
      methodsTable.add(method);
    }
    methodsTable.setAllChecked(true);

    doStatusUpdate();
  }
}
