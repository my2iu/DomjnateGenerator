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
import com.user00.domjnate.generator.ast.CallSignatureDefinition;
import com.user00.domjnate.generator.ast.PredefinedType;
import com.user00.domjnate.generator.ast.ProblemTracker;
import com.user00.domjnate.generator.ast.CallSignatureDefinition.CallParameter;
import com.user00.domjnate.generator.ast.GenericParameter;
import com.user00.domjnate.generator.ast.IndexSignatureDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.ObjectType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.TypeQueryType;
import com.user00.domjnate.generator.ast.TypeReference;

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
   
   String getterName(String propName, boolean isReadOnly)
   {
      if (isReadOnly)
      {
         // Check if property is all upper case (and contains at least one upper-case)
         boolean allNotLowerCase = propName.codePoints().allMatch(code -> !Character.isLowerCase(code));
         boolean someUpperCase = propName.codePoints().anyMatch(code -> Character.isUpperCase(code));
         if (someUpperCase && allNotLowerCase)
         {
            return propName;
         }
      }
      return "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
   }

   String setterName(String propName)
   {
      return "set" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
   }
   
   String methodName(String name)
   {
      switch(name)
      {
      case "catch": return "_catch";
      case "assert": return "_assert";
      case "continue": return "_continue";
      case "for": return "_for";
      default: break;
      }
      return name;
   }
   
   String argName(String name)
   {
      switch(name)
      {
      case "this": return "_this";
      default: break;
      }
      return name;
   }

   String typeString(Type basicType, TypeStringGenerationContext ctx)
   {
      return TypeStringGenerator.typeString(this, basicType, ctx);
   }

   static interface CallVariant
   {
      void handleVariant(int numOptional, int[] variant);
   }
   
   private void iterateOverCallVariants(CallSignatureDefinition callSigType, ApiDefinition namespaceScope, String currentPackage, GenericContext generics, CallVariant call)
   {
      int numArguments = callSigType.params.size();
      numArguments += callSigType.optionalParams.size();
      if (callSigType.restParameter != null)
         numArguments += 1;

      // Figure out the number of variants for each argument
      int [] argumentTypeVariantCounts = new int[numArguments];
      for (int n = 0; n < callSigType.params.size(); n++)
      {
         argumentTypeVariantCounts[n] = TypeStringGenerator.typeVariantCount(callSigType.params.get(n).type, this, new TypeStringGenerationContext(namespaceScope, currentPackage, generics));
      }
      for (int n = 0; n < callSigType.optionalParams.size(); n++)
      {
         argumentTypeVariantCounts[n + callSigType.params.size()] = TypeStringGenerator.typeVariantCount(callSigType.optionalParams.get(n).type, this, new TypeStringGenerationContext(namespaceScope, currentPackage, generics));
      }
      if (callSigType.restParameter != null)
         argumentTypeVariantCounts[callSigType.params.size() + callSigType.optionalParams.size()] = TypeStringGenerator.typeVariantCount(callSigType.restParameter.type, this, new TypeStringGenerationContext(namespaceScope, currentPackage, generics));
      
      // Vary the number of optional arguments
      for (int n = 0; n <= callSigType.optionalParams.size(); n++)
      {
         int totalParams = callSigType.params.size();
         totalParams += n;
         if (callSigType.restParameter != null)
            totalParams += 1;
         
         int [] variants = new int[numArguments];
         // Walk through all the variants of the arguments
         while(true)
         {
            call.handleVariant(n, variants);
            boolean exhaustedVariants = true;
            for (int argIdx = 0; argIdx < totalParams; argIdx++)
            {
               variants[argIdx]++;
               if (variants[argIdx] < argumentTypeVariantCounts[argIdx])
               {
                  exhaustedVariants = false;
                  break;
               }
               else
               {
                  variants[argIdx] = 0;
               }
            }
            if (exhaustedVariants) break;
         }
      }
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
            GenericContext generics = new GenericContext(intf.genericTypeParams);
            if (intf.genericTypeParams != null)
            {
               generateGenericTypeParams(out, intf.genericTypeParams, api, fullPkg, generics);
            }
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
               int numArguments = call.params.size();
               numArguments += call.optionalParams.size();
               if (call.restParameter != null)
                  numArguments += 1;
               int [] variants = new int[numArguments];
               Arrays.fill(variants, -1);
               int numOptional = call.optionalParams.size();
               // TODO: Use @JsOptional on optional parameters
               // TODO: better handling of unions (don't just fallback to java.lang.Object)
               generateMethodWithOptionals(out, "accept", call, api, null, fullPkg, generics, null, numOptional, variants, false);
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
            GenericContext generics = new GenericContext(intf.genericTypeParams);
            if (intf.genericTypeParams != null)
            {
               generateGenericTypeParams(out, intf.genericTypeParams, api, fullPkg, generics);
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
                  out.print(typeString(typeRef, new TypeStringGenerationContext(api, fullPkg, generics)));
                  typeRef.problems.dump(out);
               }
            }
            out.println();
            out.println("{");
            
            if (!intf.isStaticOnly)
            {
               for (PropertyDefinition prop: intf.properties)
               {
                  makeProperty(out, name, prop, api, fullPkg, generics, false, imports);
               }
               for (PropertyDefinition method: intf.methods)
               {
                  imports.add("jsinterop.annotations.JsMethod");
                  generateMethod(out, method, api, intf, fullPkg, generics);
               }
               for (IndexSignatureDefinition idxSig: intf.indexSignatures)
               {
                  // TODO: Just a temporary handling of wrapping generics to get things to compile
                  // (this should eventually be handled better)
                  boolean isReturnGeneric = generics.isGeneric(idxSig.returnType);
                  
                  String returnType = typeString(idxSig.returnType, new TypeStringGenerationContext(api, fullPkg, generics));
                  String returnTypeDescription = typeString(idxSig.returnType, new TypeStringGenerationContext(api, fullPkg, generics).withTypeDescription(true));
                  imports.add("jsinterop.annotations.JsOverlay");
                  if (idxSig.indexType instanceof PredefinedType && ((PredefinedType)idxSig.indexType).type.equals("number"))
                  {
                     out.println("@JsOverlay");
                     if (isReturnGeneric)
                     {
                        out.println(String.format("public default %1$s get(double %2$s, Class<%1$s> _type) {", returnType, idxSig.indexName));
                        out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getIndex(this, %2$s, _type);", returnType, idxSig.indexName));
                     }
                     else
                     {
                        out.println(String.format("public default %1$s get(double %2$s) {", returnType, idxSig.indexName));
                        out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getIndex(this, %2$s, %3$s);", returnType, idxSig.indexName, returnTypeDescription));
                     }
                     out.println("}");
                     if (!idxSig.readOnly) 
                     {
                        out.println("@JsOverlay");
                        out.println(String.format("public default void set(double %2$s, %1$s val) {", returnType, idxSig.indexName));
                        out.println(String.format("  com.user00.domjnate.util.Js.setIndex(this, %2$s, val);", returnType, idxSig.indexName));
                        out.println("}");
                     }
                  }
                  else if (idxSig.indexType instanceof PredefinedType && ((PredefinedType)idxSig.indexType).type.equals("string"))
                  {
                     out.println("@JsOverlay");
                     if (isReturnGeneric)
                     {
                        out.println(String.format("public default %1$s get(String %2$s, Class<%1$s> _type) {", returnType, idxSig.indexName));
                        out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getMember(this, %2$s, _type);", returnType, idxSig.indexName));
                     }
                     else
                     {
                        out.println(String.format("public default %1$s get(String %2$s) {", returnType, idxSig.indexName));
                        out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getMember(this, %2$s, %3$s);", returnType, idxSig.indexName, returnTypeDescription));
                     }
                     out.println("}");
                     if (!idxSig.readOnly) 
                     {
                        out.println("@JsOverlay");
                        out.println(String.format("public default void set(String %2$s, %1$s val) {", returnType, idxSig.indexName));
                        out.println(String.format("  com.user00.domjnate.util.Js.setMember(this, %2$s, val);", returnType, idxSig.indexName));
                        out.println("}");
                     }
                  }
                  else
                     out.println("Unhandled index signature on interface");
               }
               for (CallSignatureDefinition construct: intf.constructSignatures)
               {
                  makeConstructor(out, construct, api, fullPkg, generics, false, imports);
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
                  makeProperty(out, name, prop, api, fullPkg, generics, true, imports);
               }
               for (PropertyDefinition method: staticIntf.methods)
               {
                  imports.add("jsinterop.annotations.JsOverlay");
                  generateStaticMethod(out, name, method, api, fullPkg, generics);
               }
               for (CallSignatureDefinition call: staticIntf.callSignatures)
               {
                  makeStaticCallOnInterface(out, intf.name, call, api, fullPkg, generics, imports);
               }
               for (CallSignatureDefinition construct: staticIntf.constructSignatures)
               {
                  makeConstructor(out, construct, api, fullPkg, generics, true, imports);
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

   private void makeStaticCallOnInterface(PrintWriter out, String name, CallSignatureDefinition sig, ApiDefinition api, String currentPackage, GenericContext generics, Set<String> imports)
   {
      sig.problems.dump(out);
      imports.add("jsinterop.annotations.JsOverlay");
      iterateOverCallVariants(sig, api, currentPackage, generics, (numOptional, variants) -> {
         makeStaticCallOnInterfaceWithOptionals(out, name, sig, api, currentPackage, generics, numOptional, variants);
      });
   }

   private void makeStaticCallOnInterfaceWithOptionals(PrintWriter out, String name, CallSignatureDefinition callSigType, ApiDefinition api, String currentPackage, GenericContext generics, int numOptionals, int[] variants)
   {
      String returnType = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage, generics));
      String returnTypeDescription = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage, generics).withTypeDescription(true));
      String typeArgs = createMethodTypeArgs(out, callSigType, api, currentPackage, generics);
      out.println("@JsOverlay");
      out.print(String.format("public static %2$s%1$s call(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win", returnType, typeArgs));

      generateMethodParameters(out, callSigType, api, currentPackage, generics, numOptionals, variants, false, true, generics.getGenericParamAsType(callSigType.returnType));
      out.println(") {");
      out.print(String.format("  return com.user00.domjnate.util.Js.callMethod(_win, \"%2$s\", %1$s", returnTypeDescription, name));
      generateMethodParameters(out, callSigType, api, currentPackage, generics, numOptionals, variants, false, false, null);
      out.println(");");
      out.println("}");

      callSigType.problems.dump(out);
      for (CallParameter param: callSigType.params)
         param.problems.dump(out);
   }

   
   private void makeConstructor(PrintWriter out, CallSignatureDefinition construct, ApiDefinition api, String currentPackage, GenericContext generics, boolean isStatic, Set<String> imports)
   {
      construct.problems.dump(out);
      imports.add("jsinterop.annotations.JsOverlay");
      iterateOverCallVariants(construct, api, currentPackage, generics, (numOptional, variants) -> {
         generateConstructWithOptionals(out, construct, api, currentPackage, generics, numOptional, variants, isStatic);
      });
   }

   private String createMethodTypeArgs(PrintWriter out, CallSignatureDefinition callSigType, ApiDefinition api, String currentPackage, GenericContext generics)
   {
      String typeArgs = "";
      if (callSigType.genericTypeParameters != null && callSigType.genericTypeParameters.size() > 0)
      {
         typeArgs = "<";
         boolean isFirst = true;
         for (GenericParameter generic: callSigType.genericTypeParameters)
         {
            if (!isFirst)
               typeArgs += ",";
            isFirst = false;
            typeArgs += generic.name;
            if (generic.simpleExtendsKeyOf != null)
            {
               out.println("Unhandled generic keyof in constuctor");
               continue;
            }
            if (generic.simpleExtends != null)
               typeArgs += " extends " + typeString(generic.simpleExtends, new TypeStringGenerationContext(api, currentPackage, generics).withGenericParameter(true));
            generic.problems.dump(out);
         }
         typeArgs += "> ";
      }
      return typeArgs;
   }
   
   private void generateConstructWithOptionals(PrintWriter out, CallSignatureDefinition callSigType, ApiDefinition api, String currentPackage, GenericContext generics, int numOptionals, int[] variants, boolean isStatic)
   {
      String returnType = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage, generics));
      String jsClassName = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage, generics).withRawJavaScriptName(true));
      String returnTypeDescription = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage, generics).withTypeDescription(true));
      String typeArgs = createMethodTypeArgs(out, callSigType, api, currentPackage, generics);
      out.println("@JsOverlay");
      out.print(String.format("public %2$s %3$s%1$s _new(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win", returnType, isStatic ? "static" : "default", typeArgs));

      generateMethodParameters(out, callSigType, api, currentPackage, generics, numOptionals, variants, false, true, generics.getGenericParamAsType(callSigType.returnType));
      out.println(") {");
      out.println(String.format("  java.lang.Object constructor = com.user00.domjnate.util.Js.getConstructor(_win, \"%1$s\");", jsClassName));
      out.print(String.format("  return com.user00.domjnate.util.Js.construct(_win, constructor, %1$s", returnTypeDescription));
      generateMethodParameters(out, callSigType, api, currentPackage, generics, numOptionals, variants, false, false, null);
      out.println(");");
      out.println("}");

      callSigType.problems.dump(out);
      for (CallParameter param: callSigType.params)
         param.problems.dump(out);
   }

   private void generateMethodParameters(PrintWriter out, CallSignatureDefinition callSigType, 
         ApiDefinition api, String currentPackage, GenericContext generics, 
         int numOptionals, int[] variant, boolean isFirst, boolean withTypes, String genericParamAsType)
   {
      if (genericParamAsType != null && withTypes)
      {
         if (!isFirst)
            out.print(", ");
         isFirst = false;
         if (withTypes)
            out.print("Class<" + genericParamAsType + "> ");
         out.print("_type");
      }
      int paramIdx = 0;
      for (CallParameter param: callSigType.params)
      {
         if (!isFirst) out.print(", ");
         isFirst = false;
         if (withTypes) 
         {
            String paramType = typeString(param.type, new TypeStringGenerationContext(api, currentPackage, generics).withVariant(variant[paramIdx]));
            out.print(paramType + " ");
         }
         out.print(argName(param.name));
         paramIdx++;
      }
      for (int n = 0; n < numOptionals; n++)
      {
         CallParameter param = callSigType.optionalParams.get(n);
         if (!isFirst) out.print(", ");
         isFirst = false;
         if (withTypes) 
         {
            String paramType = typeString(param.type, new TypeStringGenerationContext(api, currentPackage, generics).withVariant(variant[paramIdx]));
            out.print(paramType + " ");
         }
         out.print(argName(param.name));
         paramIdx++;
      }
      if (callSigType.restParameter != null)
      {
         CallParameter param = callSigType.restParameter;
         if (!isFirst) out.print(", ");
         isFirst = false;
         if (withTypes) 
         {
            String paramType = typeString(param.type, new TypeStringGenerationContext(api, currentPackage, generics).withVariant(variant[paramIdx]).withStripArray(true));
            out.print(paramType);
            out.print("... ");
         }
         out.print(argName(param.name));
         paramIdx++;
      }
   }

   private void makeProperty(PrintWriter out, String className, PropertyDefinition prop, ApiDefinition api, String currentPackage, GenericContext generics, boolean isStatic, Set<String> imports)
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
         type = typeString(prop.type, new TypeStringGenerationContext(api, currentPackage, generics).withNullable(prop.optional));
         typeDescription = typeString(prop.type, new TypeStringGenerationContext(api, currentPackage, generics).withTypeDescription(true));
         prop.type.problems.dump(out);
      }
      if (!isStatic)
      {
         imports.add("jsinterop.annotations.JsProperty");
         out.println(String.format("@JsProperty(name=\"%1$s\")", prop.name));
         out.println(String.format("%2$s %1$s();", getterName(prop.name, prop.readOnly), type));
         
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
         out.println(String.format("public static %2$s %1$s(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win) {", getterName(prop.name, prop.readOnly), type));
         out.println(String.format("  java.lang.Object obj = com.user00.domjnate.util.Js.getMember(_win, \"%1$s\", com.user00.domjnate.util.EmptyInterface.class);", className));
         out.println(String.format("  return com.user00.domjnate.util.Js.getMember(obj, \"%1$s\", %2$s);", prop.name, typeDescription));
         out.println("}");
         
         if (!prop.readOnly)
         {
            out.println("@JsOverlay");
            out.println(String.format("public static void %1$s(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win, %2$s val) {", setterName(prop.name), type));
            out.println(String.format("  java.lang.Object obj = com.user00.domjnate.util.Js.getMember(_win, \"%1$s\", com.user00.domjnate.util.EmptyInterface.class);", className));
            out.println(String.format("  com.user00.domjnate.util.Js.setMember(obj, \"%1$s\", val);", prop.name));
            out.println("}");
         }
      }
   }

   private void generateGenericTypeParams(PrintWriter out, List<GenericParameter> genericTypeParameters, ApiDefinition api, String currentPackage, GenericContext generics)
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
            out.print(typeString(generic.simpleExtends, new TypeStringGenerationContext(api, currentPackage, generics).withGenericParameter(true)));
         }
      }
      out.print("> ");

   }
   
   private void generateMethod(PrintWriter out, PropertyDefinition method, ApiDefinition api, InterfaceDefinition sourceIntf, String currentPackage, GenericContext generics)
   {
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
      if (method.optional)
         out.println("Unhandled method with optional " + method.name);

      iterateOverCallVariants(method.callSigType, api, currentPackage, generics, (numOptional, variants) -> {
         generateMethodWithOptionals(out, method.name, method.callSigType, api, sourceIntf, currentPackage, generics, method.problems, numOptional, variants, true);
      });
   }
   
   private void generateMethodWithOptionals(PrintWriter out, String methodName, CallSignatureDefinition callSigType, ApiDefinition api, InterfaceDefinition sourceIntf, String currentPackage, GenericContext genericsParent, ProblemTracker methodProblems, int numOptionals, int[] variants, boolean withJsMethodAnnotation)
   {
      GenericContext generics = new GenericContext(callSigType.genericTypeParameters, genericsParent);
      String returnType = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage, generics));
      if (returnType.equals("this") && sourceIntf != null)
         // TODO: Add generic type arguments
         returnType = sourceIntf.name;

      if (withJsMethodAnnotation)
         out.println(String.format("@JsMethod(name=\"%1$s\")", methodName));
      if (callSigType.genericTypeParameters != null)
      {
         generateGenericTypeParams(out, callSigType.genericTypeParameters, api, currentPackage, generics);
      }
      boolean isFirst = true;
      out.print(String.format("%1$s %2$s(", returnType, methodName(methodName)));
      generateMethodParameters(out, callSigType, api, currentPackage, generics, numOptionals, variants, isFirst, true, null); // generics.getGenericParamAsType(callSigType.returnType));
      out.println(");");
      if (methodProblems != null)
         methodProblems.dump(out);
      callSigType.problems.dump(out);
      for (CallParameter param: callSigType.params)
         param.problems.dump(out);
   }

   private void generateStaticMethod(PrintWriter out, String className, PropertyDefinition method, ApiDefinition api, String currentPackage, GenericContext generics)
   {
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
      if (method.optional)
         out.println("Unhandled static method with optional " + method.name);
      iterateOverCallVariants(method.callSigType, api, currentPackage, generics, (numOptional, variants) -> {
         generateStaticMethodWithOptionals(out, className, method.name, method.callSigType, api, currentPackage, generics, method.problems, numOptional, variants);
      });
   }
   

   private void generateStaticMethodWithOptionals(PrintWriter out, String className, String methodName, CallSignatureDefinition callSigType, ApiDefinition api, String currentPackage, GenericContext genericsParent, ProblemTracker methodProblems, int numOptionals, int[] variants)
   {
      GenericContext generics = new GenericContext(callSigType.genericTypeParameters, genericsParent);
      String returnType = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage, generics));
      String returnTypeDescription = typeString(callSigType.returnType, new TypeStringGenerationContext(api, currentPackage, generics).withTypeDescription(true));

      out.println("@JsOverlay");
      out.print("public static ");
      if (callSigType.genericTypeParameters != null)
      {
         generateGenericTypeParams(out, callSigType.genericTypeParameters, api, currentPackage, generics);
      }
      boolean isFirst = true;
      out.print(String.format("%1$s %2$s(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win", returnType, methodName(methodName)));
      isFirst = false;
      generateMethodParameters(out, callSigType, api, currentPackage, generics, numOptionals, variants, isFirst, true, generics.getGenericParamAsType(callSigType.returnType));
      out.println(") {");
      out.print("  ");
      if (!returnType.equals("void"))
         out.print("return ");
      if (generics.getGenericParamAsType(callSigType.returnType) != null)
         returnTypeDescription = "_type";
      out.print(String.format("com.user00.domjnate.util.Js.callStaticMethod(_win, \"%1$s\", \"%2$s\", %3$s", className, methodName, returnTypeDescription));
      generateMethodParameters(out, callSigType, api, currentPackage, generics, numOptionals, variants, false, false, generics.getGenericParamAsType(callSigType.returnType));
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
