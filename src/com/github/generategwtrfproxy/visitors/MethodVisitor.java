package com.github.generategwtrfproxy.visitors;

import com.github.generategwtrfproxy.beans.Method;
import com.github.generategwtrfproxy.util.Utils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

public class MethodVisitor extends ASTVisitor {

  private TypeDeclaration rootType;

  private List<Method> methods = new ArrayList<Method>();

  public List<Method> getMethods() {
    return methods;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean visit(MethodDeclaration node) {

    int modifiers = node.getModifiers();
    if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers)) {
      return false;
    }

    Type returnType = node.getReturnType2();
    ITypeBinding returnTypeBinding = returnType.resolveBinding();
    List<SingleVariableDeclaration> parameters = node.parameters();

    Method method = null;
    method = getSetterMethod(node, returnTypeBinding, parameters);
    if (null == method) {
      method = getGetterMethod(node, returnType, returnTypeBinding, parameters);
    }

    if (null == method) {
      return false;
    }

    methods.add(method);
    return true;
  }

  public boolean visit(TypeDeclaration node) {
    if (null == rootType) {
      rootType = node;
      return true;
    } else {
      // don't take the methods from inner class
      return false;
    }
  }

  private Method getGetterMethod(MethodDeclaration method, Type returnType,
      ITypeBinding returnTypeBinding, List<SingleVariableDeclaration> parameters) {

    // a getter has zero parameter
    if (!parameters.isEmpty()) {
      return null;
    }

    // a getter never return void
    if (returnTypeBinding.getQualifiedName().equals("void")) {
      return null;
    }

    // if the return type is generic and different from Set or List, it is not
    // an acceptable getter
    if (returnType.isParameterizedType()) {
      ITypeBinding erasure = returnTypeBinding.getErasure();
      if (!(erasure.getQualifiedName().equals("java.util.List") || erasure.getQualifiedName().equals(
          "java.util.Set"))) {
        return null;
      }

      ITypeBinding genericParameter = returnTypeBinding.getTypeArguments()[0];
      if (!Utils.isValueType(genericParameter)
          && !Utils.isTypeCanBeProxied(genericParameter)) {
        return null;
      }
    } else if (!Utils.isValueType(returnTypeBinding)
        && !Utils.isTypeCanBeProxied(returnTypeBinding)) {
      return null;
    }

    String methodName = method.getName().toString();
    if (Utils.isPrefixedBy(methodName, "is")
        || Utils.isPrefixedBy(methodName, "has")) {
      if (!returnTypeBinding.getQualifiedName().equals("boolean")
          && !returnTypeBinding.getQualifiedName().equals("java.lang.Boolean")) {
        return null;
      }
    } else if (!Utils.isPrefixedBy(methodName, "get")) {
      return null;
    }

    return new Method(method, returnType, returnTypeBinding);
  }

  private Method getSetterMethod(MethodDeclaration method,
      ITypeBinding returnTypeBinding, List<SingleVariableDeclaration> parameters) {

    // a setter always return void
    if (!returnTypeBinding.getQualifiedName().equals("void")) {
      return null;
    }

    // a setter has one and only one parameter
    if (parameters.size() != 1) {
      return null;
    }

    // a setter is prefixed by set
    if (!Utils.isPrefixedBy(method.getName().toString(), "set")) {
      return null;
    }

    SingleVariableDeclaration parameter = parameters.get(0);
    Type parameterType = parameter.getType();
    ITypeBinding parameterTypeBinding = parameterType.resolveBinding();

    // if the return type is generic and different from Set or List, it is not
    // an acceptable getter
    if (parameterTypeBinding.isParameterizedType()) {
      ITypeBinding erasure = parameterTypeBinding.getErasure();
      if (!(erasure.getQualifiedName().equals("java.util.List") || erasure.getQualifiedName().equals(
          "java.util.Set"))) {
        return null;
      }

      ITypeBinding genericParameter = parameterTypeBinding.getTypeArguments()[0];
      if (!Utils.isValueType(genericParameter)
          && !Utils.isTypeCanBeProxied(genericParameter)) {
        return null;
      }
    } else if (!Utils.isValueType(parameterTypeBinding)
        && !Utils.isTypeCanBeProxied(parameterTypeBinding)) {
      return null;
    }

    return new Method(method, parameter.getName().toString(), parameterType, parameterTypeBinding);
  }

}
