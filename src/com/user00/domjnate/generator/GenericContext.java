package com.user00.domjnate.generator;

import java.util.List;

import com.user00.domjnate.generator.ast.GenericParameter;

/**
 * Keeps track of which generic are currently active in the current
 * interface/method/etc.
 */
public class GenericContext
{
   public GenericContext(List<GenericParameter> genericTypeParams)
   {
      this(genericTypeParams, null);
   }
   public GenericContext(List<GenericParameter> genericTypeParams, GenericContext parent)
   {
      if (genericTypeParams != null)
      {
         params = genericTypeParams;
      }
      this.parent = parent;
   }

   public GenericContext parent;
   public List<GenericParameter> params;
}
