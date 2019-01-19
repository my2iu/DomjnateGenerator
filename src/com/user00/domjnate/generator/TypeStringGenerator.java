package com.user00.domjnate.generator;

import java.util.Arrays;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.ArrayType;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.NullableType;
import com.user00.domjnate.generator.ast.PredefinedType;
import com.user00.domjnate.generator.ast.StringLiteralType;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.TypeReference;
import com.user00.domjnate.generator.ast.UnionType;

final class TypeStringGenerator extends Type.TypeVisitor<String>
{
   private final ApiGenerator apiGenerator;
   private final TypeStringGenerationContext ctx;

   TypeStringGenerator(ApiGenerator apiGenerator, TypeStringGenerationContext ctx)
   {
      this.apiGenerator = apiGenerator;
      this.ctx = ctx;
   }

   @Override
   public String visitPredefinedType(PredefinedType basicType)
   {
      String type = "java.lang.Object";
      switch(basicType.type)
      {
      case "any": type = "java.lang.Object"; break;
      case "number": type = (ctx.nullable || ctx.genericParameter) ? "Double" : "double"; break;
      case "string": type = "String"; break;
      case "boolean": type = (ctx.nullable || ctx.genericParameter) ? "Boolean" : "boolean"; break;
      case "void": type = (ctx.nullable || ctx.genericParameter) ? "Void" : "void"; break;
      case "symbol": type = "com.user00.domjnate.api.Symbol"; break;
      default: type = "unknown"; break;
      }
      if (ctx.typeDescription)
         return type + ".class";
      else
         return type;
   }

   @Override
   public String visitTypeReferenceType(TypeReference type)
   {
      // TODO: Should type aliases recurse?
      if (ctx.namespaceScope.typeAliases.containsKey(type.typeName))
      {
         return this.typeString(ctx.namespaceScope.typeAliases.get(type.typeName), ctx);
      }
      if (this.apiGenerator.topLevel.typeAliases.containsKey(type.typeName))
      {
         return this.typeString(this.apiGenerator.topLevel.typeAliases.get(type.typeName), ctx);
      }
      String typeArgs = "";
      if (type.typeArgs != null)
      {
         String ref = "";
         ref += "<";
         boolean isFirst = true;
         for (Type typeArg: type.typeArgs)
         {
            if (!isFirst) ref += ", ";
            isFirst = false;
            ref += typeString(typeArg, ctx.withGenericParameter(true));
         }
         ref += ">";
         typeArgs = ref;
      }
      if (type.typeName.equals("true"))
      {
         if (ctx.typeDescription)
            return "boolean.class";
         else
            return "boolean";
      }
      if (type.typeName.equals("undefined"))
      {
         if (ctx.typeDescription)
            return "Void.class";
         else
            return "Void";
      }
      if (type.typeName.equals("object"))
      {
         if (ctx.typeDescription)
            return "java.lang.Object.class";
         else
            return "java.lang.Object";
      }
      // See if we need to use a full package name here
      if (type.typeName.contains("."))
      {
         // We're referring to a type in another namespace, so show the
         // package name with the type name to be safe
         String[] parts = type.typeName.split("[.]");
         ApiDefinition api = apiGenerator.topLevel;
         for (String p: Arrays.copyOf(parts, parts.length - 1))
            api = api.namespaces.get(p);
         InterfaceDefinition referredIntf = api.interfaces.get(parts[parts.length - 1]);
         if (ctx.typeDescription)
            return apiGenerator.getFullPackageForInterface(api, referredIntf) + "." + referredIntf.name + ".class";
         else
            return apiGenerator.getFullPackageForInterface(api, referredIntf) + "." + referredIntf.name + typeArgs;
      }
      if (ctx.namespaceScope.interfaces.containsKey(type.typeName))
      {
         // We're currently in a namespace and are referring to another
         // type in the same namespace. We'll return the full type with
         // package name to be safe.
         // TODO: I'm ignoring this for now
         String otherPkg = apiGenerator.getFullPackageForInterface(ctx.namespaceScope, ctx.namespaceScope.interfaces.get(type.typeName));
         if (!otherPkg.equals(ctx.currentPackage))
         {
            if (ctx.typeDescription)
               return otherPkg + "." + ctx.namespaceScope.interfaces.get(type.typeName).name + ".class";
            else
               return otherPkg + "." + ctx.namespaceScope.interfaces.get(type.typeName).name + typeArgs; 
         }
      }
      else if (apiGenerator.topLevel.interfaces.containsKey(type.typeName))
      {
         ApiDefinition api = apiGenerator.topLevel;
         String otherPkg = apiGenerator.getFullPackageForInterface(apiGenerator.topLevel, apiGenerator.topLevel.interfaces.get(type.typeName));
         if (!otherPkg.equals(ctx.currentPackage))
         {
            if (ctx.typeDescription)
               return otherPkg + "." + apiGenerator.topLevel.interfaces.get(type.typeName).name + ".class";
            else
               return otherPkg + "." + apiGenerator.topLevel.interfaces.get(type.typeName).name + typeArgs; 
         }
      }
      if (ctx.typeDescription)
         return type.typeName + ".class";
      else
         return type.typeName + typeArgs;
   }

   @Override
   public String visitNullableType(NullableType type)
   {
      return typeString(type.subtype, ctx.withNullable(true));
   }

   @Override
   public String visitStringLiteralType(StringLiteralType type)
   {
      if (ctx.literalAsType)
         return "String";
      return "java.lang.Object";
   }
   
   @Override
   public String visitArrayType(ArrayType type)
   {
      if (ctx.stripArray)
         return typeString(type.type, ctx.withStripArray(false));
      String pkgPrefix = "";
      if (ctx.currentPackage != null && !ctx.currentPackage.equals(this.apiGenerator.pkg))
      {
         pkgPrefix = this.apiGenerator.pkg + ".";
      }
      if (ctx.typeDescription)
         return pkgPrefix + "Array.class";
      else
         return pkgPrefix + "Array<" + typeString(type.type, ctx.withGenericParameter(true))+ ">";
   }

   @Override
   public String visitUnionType(UnionType type)
   {
      if (type.isStringLiteralUnion() && ctx.literalAsType)
      {
         if (ctx.typeDescription)
            return "String.class";
         else
            return "String";
      }
      if (ctx.variant < 0)
         return super.visitUnionType(type);
      int variantCount = 0;
      for (Type t: type.subtypes)
      {
         int thisVariants = typeVariantCount(t, apiGenerator, ctx); 
         if (ctx.variant < variantCount + thisVariants)
            return typeString(t, ctx.withVariant(ctx.variant - variantCount));
         variantCount += thisVariants;
      }
      throw new IllegalArgumentException("Requesting variant beyond end of union");
   }

   @Override
   public String visitType(Type type)
   {
      if (ctx.typeDescription)
         return "java.lang.Object.class";
      else
         return "java.lang.Object";
   }

   String typeString(Type basicType, TypeStringGenerationContext ctx)
   {
      return typeString(apiGenerator, basicType, ctx);
   }

   static String typeString(ApiGenerator apiGenerator, Type basicType, TypeStringGenerationContext ctx)
   {
      if (basicType == null)
      {
         if (ctx.typeDescription)
            return "java.lang.Object.class";
         else
            return "java.lang.Object";
      }
      return basicType.visit(new TypeStringGenerator(apiGenerator, ctx));
   }
   
   static int typeVariantCount(Type type, ApiGenerator apiGenerator, TypeStringGenerationContext ctx)
   {
      if (type == null)
         return 1;
      return type.visit(new Type.TypeVisitor<Integer>() {
         @Override public Integer visitType(Type type)
         {
            return 1;
         }

         @Override public Integer visitNullableType(NullableType type)
         {
            return type.subtype.visit(this);
         }
         
         @Override public Integer visitUnionType(UnionType type)
         {
            if (type.isStringLiteralUnion() && ctx.literalAsType)
               return 1;
            
            int count = 0;
            for (Type t: type.subtypes)
            {
               count += t.visit(this);
            }
            return count;
         }
         
         @Override public Integer visitTypeReferenceType(TypeReference type)
         {
            // TODO: Should type aliases recurse?
            if (ctx.namespaceScope.typeAliases.containsKey(type.typeName))
            {
               return ctx.namespaceScope.typeAliases.get(type.typeName).visit(this);
            }
            if (apiGenerator.topLevel.typeAliases.containsKey(type.typeName))
            {
               return apiGenerator.topLevel.typeAliases.get(type.typeName).visit(this);
            }
            return 1;
         }

      });
   }
}