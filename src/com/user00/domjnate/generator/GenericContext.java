package com.user00.domjnate.generator;

import java.util.List;

import com.user00.domjnate.generator.ast.GenericParameter;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.TypeReference;

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
   public boolean isGeneric(Type type)
   {
      if (!(type instanceof TypeReference)) return false;
      TypeReference ref = (TypeReference)type;
      if (params != null)
      {
         for (GenericParameter param: params)
         {
            if (ref.typeName.equals(param.name))
               return true;
         }
      }
      if (parent == null) return false;
      return parent.isGeneric(type);
   }
}
