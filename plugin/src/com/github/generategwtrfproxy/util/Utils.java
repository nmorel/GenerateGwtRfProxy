package com.github.generategwtrfproxy.util;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Utils {

  private static List<String> VALUE_TYPES = Arrays.asList(
      String.class.getName(), Date.class.getName(), BigInteger.class.getName(),
      BigDecimal.class.getName(), Boolean.class.getName(),
      Byte.class.getName(), Character.class.getName(), Double.class.getName(),
      Float.class.getName(), Integer.class.getName(), Short.class.getName());

  public static String format(String source, int formatType) {

    TextEdit textEdit = null;
    textEdit = ToolFactory.createCodeFormatter(null).format(formatType, source,
        0, source.length(), 0, null);

    String formattedContent;
    if (textEdit != null) {
      Document document = new Document(source);
      try {
        textEdit.apply(document);
      } catch (MalformedTreeException e) {
        e.printStackTrace();
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
      formattedContent = document.get();
    } else {
      formattedContent = source;
    }

    return formattedContent;
  }

  public static boolean isPrefixedBy(String methodName, String prefix) {
    return methodName.startsWith(prefix)
        && methodName.length() > prefix.length()
        && Character.isUpperCase(methodName.charAt(prefix.length()));
  }

  public static boolean isTypeCanBeProxied(ITypeBinding type) {
    return !isValueType(type) && !type.isParameterizedType() && !type.isArray()
        && !type.isWildcardType() && Modifier.isPublic(type.getModifiers());
  }

  public static boolean isValueType(ITypeBinding type) {
    return type.isPrimitive() || type.isEnum()
        || VALUE_TYPES.contains(type.getQualifiedName());
  }

}
