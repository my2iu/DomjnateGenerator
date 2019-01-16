package com.user00.domjnate.generator;

import com.user00.domjnate.generator.ast.ApiDefinition;

public class TypeStringGenerationContext
{
   public TypeStringGenerationContext(ApiDefinition namespaceScope, String currentPackage, GenericContext generics)
   {
      this.namespaceScope = namespaceScope;
      this.currentPackage = currentPackage;
      this.generics = generics;
   }
   public ApiDefinition namespaceScope;
   public GenericContext generics;
   public String currentPackage;
   public boolean nullable;
   public boolean typeDescription;
   public boolean genericParameter;
   public boolean stripArray;
   TypeStringGenerationContext copy()
   {
      TypeStringGenerationContext ctx = new TypeStringGenerationContext(namespaceScope, currentPackage, generics);
      ctx.nullable = nullable;
      ctx.typeDescription = typeDescription;
      ctx.genericParameter = genericParameter;
      ctx.stripArray = stripArray;
      return ctx;
   }
   TypeStringGenerationContext withNullable(boolean withNullable)
   {
      TypeStringGenerationContext ctx = copy();
      ctx.nullable = withNullable;
      return ctx;
   }
   TypeStringGenerationContext withTypeDescription(boolean withTypeDescription)
   {
      TypeStringGenerationContext ctx = copy();
      ctx.typeDescription = withTypeDescription;
      return ctx;
   }
   TypeStringGenerationContext withGenericParameter(boolean withGenericParameter)
   {
      TypeStringGenerationContext ctx = copy();
      ctx.genericParameter = withGenericParameter;
      return ctx;
   }
   TypeStringGenerationContext withStripArray(boolean withStripArray)
   {
      TypeStringGenerationContext ctx = copy();
      ctx.stripArray = withStripArray;
      return ctx;
   }
}