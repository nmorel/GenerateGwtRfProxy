package com.github.generategwtrfproxy.beans;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;

public class Method {

  private Type paramOrReturnType;

  private ITypeBinding paramOrReturnTypeBinding;

  private String parameterName;

  private boolean getter;

  private String methodName;

  public Method(MethodDeclaration method, String parameterName,
      Type parameterType, ITypeBinding parameterTypeBinding) {
    this.getter = false;
    this.methodName = method.getName().toString();
    this.paramOrReturnType = parameterType;
    this.paramOrReturnTypeBinding = parameterTypeBinding;
    this.parameterName = parameterName;
  }

  public Method(MethodDeclaration method, Type returnType,
      ITypeBinding returnTypeBinding) {
    this.getter = true;
    this.methodName = method.getName().toString();
    this.paramOrReturnType = returnType;
    this.paramOrReturnTypeBinding = returnTypeBinding;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getParameterName() {
    return parameterName;
  }

  public Type getParamOrReturnType() {
    return paramOrReturnType;
  }

  public ITypeBinding getParamOrReturnTypeBinding() {
    return paramOrReturnTypeBinding;
  }

  public boolean isGetter() {
    return getter;
  }

  public void setGetter(boolean getter) {
    this.getter = getter;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public void setParameterName(String parameterName) {
    this.parameterName = parameterName;
  }

  public void setParamOrReturnType(Type paramOrReturnType) {
    this.paramOrReturnType = paramOrReturnType;
  }

  public void setParamOrReturnTypeBinding(ITypeBinding paramOrReturnTypeBinding) {
    this.paramOrReturnTypeBinding = paramOrReturnTypeBinding;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (getter) {
      builder.append(paramOrReturnTypeBinding.getName());
    } else {
      builder.append("void");
    }

    builder.append(" ");
    builder.append(methodName);
    builder.append("(");
    if (!getter) {
      builder.append(paramOrReturnTypeBinding.getName());
      builder.append(" ");
      builder.append(parameterName);
    }
    builder.append(")");
    return builder.toString();
  }

}
