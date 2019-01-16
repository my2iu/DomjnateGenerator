package com.user00.domjnate.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.ArrayType;
import com.user00.domjnate.generator.ast.CallSignatureDefinition;
import com.user00.domjnate.generator.ast.PredefinedType;
import com.user00.domjnate.generator.ast.ProblemTracker;
import com.user00.domjnate.generator.ast.CallSignatureDefinition.CallParameter;
import com.user00.domjnate.generator.ast.GenericParameter;
import com.user00.domjnate.generator.ast.IndexSignatureDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.NullableType;
import com.user00.domjnate.generator.ast.ObjectType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.Type.TypeVisitor;

import com.user00.domjnate.generator.ast.TypeQueryType;
import com.user00.domjnate.generator.ast.TypeReference;
import com.user00.domjnate.generator.ast.UnionType;

public class ApiGenerator
{
   String outputDir = "apigen";
   String pkg = "com.user00.domjnate.api";
   ApiDefinition topLevel;
   
   FileOutputManager files = new FileOutputManager();
   
   static class FileOutputManager
   {
      void makeFile(String outputDir, String pkg, String name, Consumer<PrintWriter> worker) throws IOException
      {
         String pkgDir = Paths.get(outputDir, pkg.replace('.', File.separatorChar)).toString();
         new File(pkgDir).mkdirs();
         try (FileOutputStream outStream = new FileOutputStream(pkgDir + File.separatorChar + name + ".java");
               Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
               PrintWriter out = new PrintWriter(writer))
         {
            worker.accept(out);
         }         
      }
   }
   
   String getterName(String propName)
   {
      return "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
   }

   String setterName(String propName)
   {
      return "set" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
   }
   
   String methodName(String name)
   {
      if ("catch".equals(name))
         return "_catch";
      return name;
   }
   
   public static class TypeStringGenerationContext
   {
      public TypeStringGenerationContext(ApiDefinition namespaceScope, String currentPackage)
      {
         this.namespaceScope = namespaceScope;
         this.currentPackage = currentPackage;
      }
      public ApiDefinition namespaceScope;
      public String currentPackage;
      public boolean nullable;
      public boolean typeDescription;
      public boolean genericParameter;
      TypeStringGenerationContext copy()
      {
         TypeStringGenerationContext ctx = new TypeStringGenerationContext(namespaceScope, currentPackage);
         ctx.nullable = nullable;
         ctx.typeDescription = typeDescription;
         ctx.genericParameter = genericParameter;
         return ctx;
      }
      TypeStringGenerationContext withNullable(boolean nullable)
      {
         TypeStringGenerationContext ctx = copy();
         ctx.nullable = nullable;
         return ctx;
      }
      TypeStringGenerationContext withTypeDescription(boolean typeDescription)
      {
         TypeStringGenerationContext ctx = copy();
         ctx.typeDescription = typeDescription;
         return ctx;
      }
      TypeStringGenerationContext withGenericParameter(boolean genericParameter)
      {
         TypeStringGenerationContext ctx = copy();
         ctx.genericParameter = genericParameter;
         return ctx;
      }
   }
   
   String typeString(Type basicType, TypeStringGenerationContext ctx)
   {
      if (basicType == null) return "java.lang.Object";
      return basicType.visit(new TypeVisitor<String>() {
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
               return typeString(ctx.namespaceScope.typeAliases.get(type.typeName), ctx);
            }
            if (topLevel.typeAliases.containsKey(type.typeName))
            {
               return typeString(topLevel.typeAliases.get(type.typeName), ctx);
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
            // See if we need to use a full package name here
            if (type.typeName.contains("."))
            {
               // We're referring to a type in another namespace, so show the
               // package name with the type name to be safe
               String[] parts = type.typeName.split("[.]");
               ApiDefinition api = topLevel;
               for (String p: Arrays.copyOf(parts, parts.length - 1))
                  api = api.namespaces.get(p);
               InterfaceDefinition referredIntf = api.interfaces.get(parts[parts.length - 1]);
               if (ctx.typeDescription)
                  return getFullPackageForInterface(api, referredIntf) + "." + referredIntf.name + ".class";
               else
                  return getFullPackageForInterface(api, referredIntf) + "." + referredIntf.name + typeArgs;
            }
            if (ctx.namespaceScope.interfaces.containsKey(type.typeName))
            {
               // We're currently in a namespace and are referring to another
               // type in the same namespace. We'll return the full type with
               // package name to be safe.
               // TODO: I'm ignoring this for now
               String otherPkg = getFullPackageForInterface(ctx.namespaceScope, ctx.namespaceScope.interfaces.get(type.typeName));
               if (!otherPkg.equals(ctx.currentPackage))
               {
                  if (ctx.typeDescription)
                     return otherPkg + "." + ctx.namespaceScope.interfaces.get(type.typeName).name + ".class";
                  else
                     return otherPkg + "." + ctx.namespaceScope.interfaces.get(type.typeName).name + typeArgs; 
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
         public String visitArrayType(ArrayType type)
         {
            String pkgPrefix = "";
            if (ctx.currentPackage != null && !ctx.currentPackage.equals(pkg))
            {
               pkgPrefix = pkg + ".";
            }
            if (ctx.typeDescription)
               return pkgPrefix + "Array.class";
            else
               return pkgPrefix + "Array<" + typeString(type.type, ctx.withGenericParameter(true))+ ">";
         }
         
         @Override
         public String visitUnionType(UnionType type)
         {
            return super.visitUnionType(type);
         }
         
         @Override
         public String visitType(Type type)
         {
            return "java.lang.Object";
         }
      });
   }

   void generateFunctionInterface(InterfaceDefinition intf, ApiDefinition api) throws IOException
   {
      Set<String> imports = new HashSet<>();
      String fullPkg = getFullPackageForInterface(api, intf);

      String name = intf.name;
      files.makeFile(outputDir, fullPkg, name, (outmain) -> {
         String body;
         try (StringWriter stringWriter = new StringWriter();
               PrintWriter out = new PrintWriter(stringWriter)) {
            intf.problems.dump(out);
            
            imports.add("jsinterop.annotations.JsFunction");
            out.println("@JsFunction");
            out.print(String.format("public interface %1$s", name));
            if (intf.extendsTypes != null)
            {
               out.println("Unhandled extends on a function interface");
            }
            out.println();
            out.println("{");
            
            for (CallSignatureDefinition call: intf.callSignatures)
            {
               if (call.genericTypeParameters != null)
                  out.println("Unhandled type parameters on function interface call signature");
               for (int n = 0; n <= call.optionalParams.size(); n++)
               {
                  generateMethodWithOptionals(out, "accept", call, api, null, fullPkg, null, n, false);
               }
   
            }
            
            out.println("}");
            body = stringWriter.toString();
         }
         catch (IOException e)
         {
            throw new IllegalArgumentException(e);
         }
         
         outmain.println(String.format("package %1$s;", fullPkg));
         outmain.println();
         List<String> importList = new ArrayList<>(imports);
         Collections.sort(importList);
         for (String imported: importList)
         {
            outmain.println(String.format("import %1$s;", imported));
         }
         outmain.println();

         outmain.print(body);
      });
      
   }
   
   InterfaceDefinition lookupInterfaceAmbient(String name, Map<String, Type> ambientVars)
   {
      Type type = ambientVars.get(name);
      if (type instanceof ObjectType)
      {
         ambientVars.remove(name);
         return ((ObjectType)type).intf;
      }
      else if (type instanceof TypeReference && topLevel.interfaces.containsKey(((TypeReference)type).typeName))
      {
         if (!topLevel.interfaces.get(((TypeReference)type).typeName).doNotGenerateJava)
         {
            if (name.equals(((TypeReference)type).typeName))
               topLevel.interfaces.get(((TypeReference)type).typeName).isStaticOnly = true;
            else
               System.err.println("static ambient " + name + " with " + ((TypeReference)type).typeName);
         }
         ambientVars.remove(name);
         return topLevel.interfaces.get(((TypeReference)type).typeName); 
      }
      return null;
   }
   
   void generateInterface(ApiDefinition api, InterfaceDefinition intf) throws IOException
   {
      if (intf.doNotGenerateJava) return;
      if (intf.isFunction())
      {
         generateFunctionInterface(intf, api);
         return;
      }
      String name = intf.name;
      InterfaceDefinition intfAmbient = null;
      if (api.ambientVars.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, api.ambientVars);
      else if (api.ambientConsts.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, api.ambientConsts);
      else if (topLevel.ambientVars.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, topLevel.ambientVars);
      else if (topLevel.ambientConsts.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, topLevel.ambientConsts);
      InterfaceDefinition staticIntf = intfAmbient;
      String fullPkg = getFullPackageForInterface(api, intf);
      
      files.makeFile(outputDir, fullPkg, name, (outmain) -> {
         String intfBody;
         Set<String> imports = new HashSet<>();
         
         try (StringWriter stringWriter = new StringWriter();
               PrintWriter out = new PrintWriter(stringWriter)) {
            intf.problems.dump(out);
            
            imports.add("jsinterop.annotations.JsType");
            out.println(String.format("@JsType(isNative=true,name=\"%1$s\")", name));
            out.print(String.format("public interface %1$s", name));
            if (intf.genericTypeParams != null)
            {
               generateGenericTypeParams(out, intf.genericTypeParams);
            }
            if (intf.extendsTypes != null)
            {
               out.print(" extends ");
               boolean isFirst = true;
               for (TypeReference typeRef: intf.extendsTypes)
               {
                  if (!isFirst)
                     out.print(", ");
                  isFirst = false;
                  out.print(typeString(typeRef, new TypeStringGenerationContext(api, fullPkg)));
                  typeRef.problems.dump(out);
               }
            }
            out.println();
            out.println("{");
            
            if (!intf.isStaticOnly)
            {
               for (PropertyDefinition prop: intf.properties)
               {
                  makeProperty(out, name, prop, api, fullPkg, false, imports);
               }
               for (PropertyDefinition method: intf.methods)
               {
                  imports.add("jsinterop.annotations.JsMethod");
                  generateMethod(out, method, api, intf, fullPkg);
               }
               for (IndexSignatureDefinition idxSig: intf.indexSignatures)
               {
                  String returnType = typeString(idxSig.returnType, new TypeStringGenerationContext(api, fullPkg));
                  String returnTypeDescription = typeString(idxSig.returnType, new TypeStringGenerationContext(api, fullPkg).withTypeDescription(true));
                  imports.add("jsinterop.annotations.JsOverlay");
                  if (idxSig.indexType instanceof PredefinedType && ((PredefinedType)idxSig.indexType).type.equals("number"))
                  {
                     out.println("@JsOverlay");
                     out.println(String.format("public default %1$s get(double %2$s) {", returnType, idxSig.indexName));
                     out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getIndex(this, %2$s, %3$s);", returnType, idxSig.indexName, returnTypeDescription));
                     out.println("}");
                     if (!idxSig.readOnly) 
                     {
                        out.println("@JsOverlay");
                        out.println(String.format("public default void set(double %2$s, %1$s val) {", returnType, idxSig.indexName));
                        out.println(String.format("  com.user00.domjnate.util.Js.setIndex(this, %2$s, val);", returnType, idxSig.indexName));
                        out.println("}");
                     }
                  }
                  else
                     out.println("Unhandled index signature on interface");
               }
               for (CallSignatureDefinition construct: intf.constructSignatures)
               {
                  makeConstructor(out, construct, api, fullPkg, false, imports);
               }
               for (CallSignatureDefinition call: intf.callSignatures)
               {
                  out.println("Unhandled call signature on interface");
               }
            }
            
            if (staticIntf != null)
            {
               staticIntf.problems.dump(out);
               for (PropertyDefinition prop: staticIntf.properties)
               {
                  makeProperty(out, name, prop, api, fullPkg, true, imports);
               }
               for (PropertyDefinition method: staticIntf.methods)
               {
                  imports.add("jsinterop.annotations.JsOverlay");
                  generateStaticMethod(out, name, method, api, fullPkg);
               }
               for (CallSignatureDefinition call: staticIntf.callSignatures)
               {
                  makeStaticCallOnInterface(out, intf.name, call, api, fullPkg, imports);
               }
               for (CallSignatureDefinition construct: staticIntf.constructSignatures)
               {
                  makeConstructor(out, construct, api, fullPkg, true, imports);
               }
               for (IndexSignatureDefinition idxSig: staticIntf.indexSignatures)
               {
                  out.println("Unhandled static index");
               }
            }
            
            out.println("}");

            intfBody = stringWriter.toString();
         }
         catch (IOException e) 
         {
            throw new IllegalArgumentException(e);
         }
         
         outmain.println(String.format("package %1$s;", fullPkg));
         outmain.println();
         List<String> importList = new ArrayList<>(imports);
         Collections.sort(importList);
         for (String imported: importList)
         {
            outmain.println(String.format("import %1$s;", imported));
         }
         outmain.println();

         outmain.print(intfBody);

      });
   }

   private void makeStaticCallOnInterface(PrintWriter out, String name, CallSignatureDefinition sig, ApiDefinition api, String currentPackage, Set<String> imports)
   {
      sig.problems.dump(out);
      imports.add("jsinterop.annotations.JsOverlay");
      if (sig.genericTypeParameters != null && sig.genericTypeParameters.size() > 0)
      {
         out.println("Unhandled call on interface with generic type parameters");
         return;
      }
      for (int n = 0; n <= sig.optionalParams.size(); n++)
      {
         makeStaticCallOnInterfaceWithOptionals(out, name, sig, api, currentPackage, n);
      }

   }

   private void makeStaticCallOnInterfaceWithOptionals(PrintWriter out, String name, CallSignatureDefinition callSigType, ApiDefinition api, String currentPackage, int numOptionals)
   {
      String returnType = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage));
      String returnTypeDescription = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage).withTypeDescription(true));
      out.println("@JsOverlay");
      out.print(String.format("public static %1$s call(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win", returnType));

      generateMethodParameters(out, callSigType, api, currentPackage, numOptionals, false, true);
      out.println(") {");
      out.print(String.format("  return com.user00.domjnate.util.Js.callMethod(_win, \"%2$s\", %1$s", returnTypeDescription, name));
      generateMethodParameters(out, callSigType, api, currentPackage, numOptionals, false, false);
      out.println(");");
      out.println("}");

      callSigType.problems.dump(out);
      for (CallParameter param: callSigType.params)
         param.problems.dump(out);
   }

   
   private void makeConstructor(PrintWriter out, CallSignatureDefinition construct, ApiDefinition api, String currentPackage, boolean isStatic, Set<String> imports)
   {
      construct.problems.dump(out);
      imports.add("jsinterop.annotations.JsOverlay");
      if (construct.genericTypeParameters != null && construct.genericTypeParameters.size() > 0)
      {
         out.println("Unhandled constructor with generic type parameters");
         return;
      }
      for (int n = 0; n <= construct.optionalParams.size(); n++)
      {
         generateConstructWithOptionals(out, construct, api, currentPackage, n, isStatic);
      }

   }

   private void generateConstructWithOptionals(PrintWriter out, CallSignatureDefinition callSigType, ApiDefinition api, String currentPackage, int numOptionals, boolean isStatic)
   {
      String returnType = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage));
      String returnTypeDescription = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage).withTypeDescription(true));
      out.println("@JsOverlay");
      out.print(String.format("public %2$s %1$s _new(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win", returnType, isStatic ? "static" : "default"));

//      if (callSigType.genericTypeParameters != null)
//      {
//         generateGenericTypeParams(out, callSigType.genericTypeParameters);
//      }
      generateMethodParameters(out, callSigType, api, currentPackage, numOptionals, false, true);
      out.println(") {");
      out.println(String.format("  java.lang.Object constructor = com.user00.domjnate.util.Js.getConstructor(_win, \"%1$s\");", returnType));
      out.print(String.format("  return com.user00.domjnate.util.Js.construct(_win, constructor, %1$s", returnTypeDescription));
      generateMethodParameters(out, callSigType, api, currentPackage, numOptionals, false, false);
      out.println(");");
      out.println("}");

      callSigType.problems.dump(out);
      for (CallParameter param: callSigType.params)
         param.problems.dump(out);
   }

   private void generateMethodParameters(PrintWriter out, CallSignatureDefinition callSigType, 
         ApiDefinition api, String currentPackage, 
         int numOptionals, boolean isFirst, boolean withTypes)
   {
      for (CallParameter param: callSigType.params)
      {
         if (!isFirst) out.print(", ");
         isFirst = false;
         if (withTypes) 
         {
            String paramType = typeString(param.type, new TypeStringGenerationContext(api, currentPackage));
            out.print(paramType + " ");
         }
         out.print(param.name);
      }
      for (int n = 0; n < numOptionals; n++)
      {
         CallParameter param = callSigType.optionalParams.get(n);
         if (!isFirst) out.print(", ");
         isFirst = false;
         if (withTypes) 
         {
            String paramType = typeString(param.type, new TypeStringGenerationContext(api, currentPackage));
            out.print(paramType + " ");
         }
         out.print(param.name);
      }
      if (callSigType.restParameter != null)
      {
         CallParameter param = callSigType.restParameter;
         if (!isFirst) out.print(", ");
         isFirst = false;
         if (withTypes) 
         {
            String paramType = typeString(param.type, new TypeStringGenerationContext(api, currentPackage));
            out.print(paramType);
            out.print("... ");
         }
         out.print(param.name);
      }
   }

   private void makeProperty(PrintWriter out, String className, PropertyDefinition prop, ApiDefinition api, String currentPackage, boolean isStatic, Set<String> imports)
   {
      if (prop.type != null && prop.type instanceof TypeQueryType)
      {
         out.println("// TODO: Suppressing property with type query type " + prop.name);
         return;
      }
      String type = "java.lang.Object";
      String typeDescription = "java.lang.Object";
      if (prop.type != null)
      {
         type = typeString(prop.type, new TypeStringGenerationContext(api, currentPackage).withNullable(prop.optional));
         typeDescription = typeString(prop.type, new TypeStringGenerationContext(api, currentPackage).withTypeDescription(true));
         prop.type.problems.dump(out);
      }
      if (!isStatic)
      {
         imports.add("jsinterop.annotations.JsProperty");
         out.println(String.format("@JsProperty(name=\"%1$s\")", prop.name));
         out.println(String.format("%2$s %1$s();", getterName(prop.name), type));
         
         if (!prop.readOnly)
         {
            out.println(String.format("@JsProperty(name=\"%1$s\")", prop.name));
            out.println(String.format("void %1$s(%2$s val);", setterName(prop.name), type));
         }
      }
      else
      {
         imports.add("jsinterop.annotations.JsOverlay");
         out.println("@JsOverlay");
         out.println(String.format("public static %2$s %1$s(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win) {", getterName(prop.name), type));
         out.println(String.format("  com.user00.domjnate.util.EmptyInterface obj = com.user00.domjnate.util.Js.getMember(_win, \"%1$s\", com.user00.domjnate.util.EmptyInterface.class);", className));
         out.println(String.format("  return com.user00.domjnate.util.Js.getMember(obj, \"%1$s\", %2$s);", prop.name, typeDescription));
         out.println("}");
         
         if (!prop.readOnly)
         {
            out.println("@JsOverlay");
            out.println(String.format("public static void %1$s(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win, %2$s val) {", setterName(prop.name), type));
            out.println(String.format("  com.user00.domjnate.util.EmptyInterface obj = com.user00.domjnate.util.Js.getMember(_win, \"%1$s\", com.user00.domjnate.util.EmptyInterface.class);", className));
            out.println(String.format("  com.user00.domjnate.util.Js.setMember(obj, \"%1$s\", val);", prop.name));
            out.println("}");
         }
      }
   }

   private void generateGenericTypeParams(PrintWriter out, List<GenericParameter> genericTypeParameters)
   {
      out.print("<");
      boolean isFirst = true;
      for (GenericParameter generic: genericTypeParameters)
      {
         if (generic.simpleExtendsKeyOf != null)
            throw new IllegalArgumentException("generic keyof parameters should have been filtered out previously");
         if (!isFirst)
            out.print(",");
         isFirst = false;
         out.print(generic.name);
         if (generic.simpleExtends != null)
         {
            out.print(" extends ");
            out.print(generic.simpleExtends);
         }
      }         
      out.print("> ");

   }
   
   private void generateMethod(PrintWriter out, PropertyDefinition method, ApiDefinition api, InterfaceDefinition sourceIntf, String currentPackage)
   {
//      if ("addEventListener".equals(method.name) && method.callSigType.genericTypeParameters != null)
//      {
//         out.println("// TODO: Suppressing addEventListener with typed events");
//         return;
//      }
//      if ("removeEventListener".equals(method.name) && method.callSigType.genericTypeParameters != null)
//      {
//         out.println("// TODO: Suppressing removeEventListener with typed events");
//         return;
//      }
      if (method.callSigType.genericTypeParameters != null)
      {
         // Ignore keyof generic type parameters for now
         for (GenericParameter generic: method.callSigType.genericTypeParameters)
         {
            if (generic.simpleExtendsKeyOf != null)
            {
               out.println("// TODO: Suppressing generic keyof type parameter for " + method.name);
               return;
            }
         }
      }
      for (int n = 0; n <= method.callSigType.optionalParams.size(); n++)
      {
         generateMethodWithOptionals(out, method.name, method.callSigType, api, sourceIntf, currentPackage, method.problems, n, true);
      }
   }
   
   private void generateMethodWithOptionals(PrintWriter out, String methodName, CallSignatureDefinition callSigType, ApiDefinition api, InterfaceDefinition sourceIntf, String currentPackage, ProblemTracker methodProblems, int numOptionals, boolean withJsMethodAnnotation)
   {
      String returnType = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage));
      if (returnType.equals("this") && sourceIntf != null)
         // TODO: Add generic type arguments
         returnType = sourceIntf.name;

      if (withJsMethodAnnotation)
         out.println(String.format("@JsMethod(name=\"%1$s\")", methodName));
      if (callSigType.genericTypeParameters != null)
      {
         generateGenericTypeParams(out, callSigType.genericTypeParameters);
      }
      boolean isFirst = true;
      out.print(String.format("%1$s %2$s(", returnType, methodName(methodName)));
      generateMethodParameters(out, callSigType, api, currentPackage, numOptionals, isFirst, true);
      out.println(");");
      if (methodProblems != null)
         methodProblems.dump(out);
      callSigType.problems.dump(out);
      for (CallParameter param: callSigType.params)
         param.problems.dump(out);
   }

   private void generateStaticMethod(PrintWriter out, String className, PropertyDefinition method, ApiDefinition api, String currentPackage)
   {
//      if ("addEventListener".equals(method.name) && method.callSigType.genericTypeParameters != null)
//      {
//         out.println("// TODO: Suppressing addEventListener with typed events");
//         return;
//      }
//      if ("removeEventListener".equals(method.name) && method.callSigType.genericTypeParameters != null)
//      {
//         out.println("// TODO: Suppressing removeEventListener with typed events");
//         return;
//      }
      if (method.callSigType.genericTypeParameters != null)
      {
         // Ignore keyof generic type parameters for now
         for (GenericParameter generic: method.callSigType.genericTypeParameters)
         {
            if (generic.simpleExtendsKeyOf != null)
            {
               out.println("// TODO: Suppressing generic keyof type parameter for " + method.name);
               return;
            }
         }
      }
      for (int n = 0; n <= method.callSigType.optionalParams.size(); n++)
      {
         generateStaticMethodWithOptionals(out, className, method.name, method.callSigType, api, currentPackage, method.problems, n);
      }
   }
   

   private void generateStaticMethodWithOptionals(PrintWriter out, String className, String methodName, CallSignatureDefinition callSigType, ApiDefinition api, String currentPackage, ProblemTracker methodProblems, int numOptionals)
   {
      String returnType = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage));
      String returnTypeDescription = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage).withTypeDescription(true));

      out.println("@JsOverlay");
      out.print("public static ");
      if (callSigType.genericTypeParameters != null)
      {
         generateGenericTypeParams(out, callSigType.genericTypeParameters);
      }
      boolean isFirst = true;
      out.print(String.format("%1$s %2$s(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win", returnType, methodName(methodName)));
      isFirst = false;
      generateMethodParameters(out, callSigType, api, currentPackage, numOptionals, isFirst, true);
      out.println(") {");
      out.print("  ");
      if (!returnType.equals("void"))
         out.print("return ");
      out.print(String.format("com.user00.domjnate.util.Js.callStaticMethod(_win, \"%1$s\", \"%2$s\", %3$s", className, methodName, returnTypeDescription));
      generateMethodParameters(out, callSigType, api, currentPackage, numOptionals, false, false);
      out.println(");");
      out.println("}");
      if (methodProblems != null)
         methodProblems.dump(out);
      callSigType.problems.dump(out);
      for (CallParameter param: callSigType.params)
         param.problems.dump(out);
   }

   public String getFullPackageForInterface(ApiDefinition namespace, InterfaceDefinition intf)
   {
      if (intf.remapPackage != null)
         return pkg + "." + intf.remapPackage;
      String subpkg = null;
      while (namespace.parent != null)
      {
         ApiDefinition parent = namespace.parent;
         if (namespace.remapName != null)
         {
            if (subpkg == null)
               subpkg = namespace.remapName;
            else
               subpkg = namespace.remapName + "." + subpkg;
            break;
         }
         String levelName = null;
         for (Map.Entry<String, ApiDefinition> entry: parent.namespaces.entrySet())
         {
            if (entry.getValue() == namespace)
               levelName = entry.getKey();
         }
         if (levelName == null) throw new IllegalArgumentException("Cannot find namespace");
         if (subpkg == null)
            subpkg = levelName;
         else
            subpkg = levelName + "." + subpkg;
         namespace = parent;
      }
      if (subpkg != null)
         return pkg + "." + subpkg;
      else
         return pkg;
   }
   
   public void generate(String namespaceName, ApiDefinition api) throws IOException
   {
//      String levelName = namespaceName;
//      String levelJavaPkg;
//      if (api.remapName == null)
//         levelJavaPkg = namespaceName;
//      else
//         levelJavaPkg = api.remapName;
      for (InterfaceDefinition intf: api.interfaces.values())
      {
//         String intfPkg = levelJavaPkg;
//         if (intf.remapPackage != null)
//            intfPkg = intf.remapPackage;
         generateInterface(api, intf);
      }
      for (Map.Entry<String, Type> entry: api.ambientVars.entrySet())
      {
         System.err.println("Unhandled ambient var " + entry.getKey());
         entry.getValue().problems.dump(System.err);
      }
      for (Map.Entry<String, Type> entry: api.ambientConsts.entrySet())
      {
         System.err.println("Unhandled ambient const " + entry.getKey());
         entry.getValue().problems.dump(System.err);
      }
      for (Map.Entry<String, ApiDefinition> namespace: api.namespaces.entrySet())
      {
         String nextName;
         if (namespaceName == null)
            nextName = namespace.getKey();
         else
            nextName = namespaceName + "." + namespace.getKey();
         generate(nextName, namespace.getValue());
      }

      api.problems.dump(System.err);
   }
}
