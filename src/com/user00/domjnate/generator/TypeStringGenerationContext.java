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
   public boolean nullable;          // nullable version of types (i.e. primitives are autoboxed so that null will be accepted as a paramter)
   public boolean typeDescription;   // output objects (i.e. Class<T>) describing the type
   public boolean genericParameter;  // type that can be used as a generic parameter (i.e. primitives are autoboxed)
   public boolean stripArray;        // remove the top-most array type so we get the type of the contents of the array
   public boolean rawJavaScriptName; // output the raw name of the JS type (i.e. remove any generic parameters)
   public int variant = -1;
   public boolean literalAsType = true;  // if the type is a literal string constant or numeric constant, treat that as the type of that constant
   TypeStringGenerationContext copy()
   {
      TypeStringGenerationContext ctx = new TypeStringGenerationContext(namespaceScope, currentPackage, generics);
      ctx.nullable = nullable;
      ctx.typeDescription = typeDescription;
      ctx.genericParameter = genericParameter;
      ctx.stripArray = stripArray;
      ctx.rawJavaScriptName = rawJavaScriptName;
      ctx.variant = variant;
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
   TypeStringGenerationContext withRawJavaScriptName(boolean withJsName)
   {
      TypeStringGenerationContext ctx = copy();
      ctx.rawJavaScriptName = withJsName;
      return ctx;
   }
   TypeStringGenerationContext withVariant(int withVariant)
   {
      TypeStringGenerationContext ctx = copy();
      ctx.variant = withVariant;
      return ctx;
   }
}