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
import com.user00.domjnate.generator.ast.NullableType;
import com.user00.domjnate.generator.ast.ObjectType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.Type.TypeVisitor;

import netscape.javascript.JSObject;

import com.user00.domjnate.generator.ast.TypeQueryType;
import com.user00.domjnate.generator.ast.TypeReference;
import com.user00.domjnate.generator.ast.UnionType;

public class ApiGenerator
{
   String outputDir = "apigen";
   String pkg = "com.user00.domjnate.api";
   ApiDefinition api;
   
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
   
   String typeString(Type basicType, boolean nullable)
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
            case "number": type = nullable ? "Double" : "double"; break;
            case "string": type = "String"; break;
            case "boolean": type = nullable ? "Boolean" : "boolean"; break;
            case "void": type = nullable ? "Void" : "void"; break;
            default: type = "unknown"; break;
            }
            return type;
         }
         @Override
         public String visitTypeReferenceType(TypeReference type)
         {
            if (api.typeAliases.containsKey(type.typeName))
            {
               return typeString(api.typeAliases.get(type.typeName), nullable);
            }
            if (type.typeArgs != null)
            {
               String ref = type.typeName;
               ref += "<";
               boolean isFirst = true;
               for (Type typeArg: type.typeArgs)
               {
                  if (!isFirst) ref += ", ";
                  isFirst = false;
                  ref += typeString(typeArg, true);
               }
               ref += ">";
               return ref;
            }
            return type.typeName;
         }
         
         @Override
         public String visitNullableType(NullableType type)
         {
            return typeString(type.subtype, true);
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

   void generateFunctionInterface(InterfaceDefinition intf) throws IOException
   {
      Set<String> imports = new HashSet<>();

      String name = intf.name;
      files.makeFile(outputDir, pkg, name, (outmain) -> {
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
                  generateMethodWithOptionals(out, "accept", call, null, n, false);
               }
   
            }
            
            out.println("}");
            body = stringWriter.toString();
         }
         catch (IOException e)
         {
            throw new IllegalArgumentException(e);
         }
         
         outmain.println(String.format("package %1$s;", pkg));
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
      return null;
   }
   
   void generateInterface(InterfaceDefinition intf) throws IOException
   {
      if (intf.isFunction())
      {
         generateFunctionInterface(intf);
         return;
      }
      String name = intf.name;
      InterfaceDefinition intfAmbient = null;
      if (api.ambientVars.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, api.ambientVars);
      else if (api.ambientConsts.containsKey(name))
         intfAmbient = lookupInterfaceAmbient(name, api.ambientConsts);
      InterfaceDefinition staticIntf = intfAmbient;
      
      files.makeFile(outputDir, pkg, name, (outmain) -> {
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
                  out.print(typeString(typeRef, false));
                  typeRef.problems.dump(out);
               }
            }
            out.println();
            out.println("{");
            
            for (PropertyDefinition prop: intf.properties)
            {
               makeProperty(out, name, prop, false, imports);
            }
            for (PropertyDefinition method: intf.methods)
            {
               imports.add("jsinterop.annotations.JsMethod");
               generateMethod(out, method);
            }
            for (IndexSignatureDefinition idxSig: intf.indexSignatures)
            {
               String returnType = typeString(idxSig.returnType, false);
               imports.add("jsinterop.annotations.JsOverlay");
               if (idxSig.indexType instanceof PredefinedType && ((PredefinedType)idxSig.indexType).type.equals("number"))
               {
                  out.println("@JsOverlay");
                  out.println(String.format("public default %1$s get(double %2$s) {", returnType, idxSig.indexName));
                  out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getIndex(this, %2$s, %1$s.class);", returnType, idxSig.indexName));
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
               makeConstructor(out, construct, false, imports);
            }
            for (CallSignatureDefinition call: intf.callSignatures)
            {
               out.println("Unhandled call signature on interface");
            }
       
            
            if (staticIntf != null)
            {
               staticIntf.problems.dump(out);
               for (PropertyDefinition prop: staticIntf.properties)
               {
                  makeProperty(out, name, prop, true, imports);
               }
               for (PropertyDefinition method: staticIntf.methods)
               {
                  out.println("Unhandled static method " + method.name);
               }
               for (CallSignatureDefinition call: staticIntf.callSignatures)
               {
                  out.println("Unhandled static call");
               }
               for (CallSignatureDefinition construct: staticIntf.constructSignatures)
               {
                  makeConstructor(out, construct, true, imports);
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
         
         outmain.println(String.format("package %1$s;", pkg));
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

   private void makeConstructor(PrintWriter out, CallSignatureDefinition construct, boolean isStatic, Set<String> imports)
   {
      construct.problems.dump(out);
      imports.add("jsinterop.annotations.JsOverlay");
      if (construct.params.size() > 0)
      {
         out.println("Unhandled constructor with multiple parameters");
         return;
      }
      if (construct.optionalParams.size() > 0)
      {
         out.println("Unhandled constructor with optional parameters");
         return;
      }
      if (construct.restParameter != null)
      {
         out.println("Unhandled constructor with rest parameter");
         return;
      }
      if (construct.genericTypeParameters != null && construct.genericTypeParameters.size() > 0)
      {
         out.println("Unhandled constructor with generic type parameters");
         return;
      }
      String returnType = typeString(construct.returnType, false);
      out.println("@JsOverlay");
      out.println(String.format("public %2$s %1$s _new(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win) {", returnType, isStatic ? "static" : "default"));
      out.println(String.format("  java.lang.Object constructor = com.user00.domjnate.util.Js.getConstructor(_win, \"%1$s\");", returnType));
      out.println(String.format("  return com.user00.domjnate.util.Js.construct(_win, constructor, %1$s.class);", returnType));
      out.println("}");
   }

   private void makeProperty(PrintWriter out, String className, PropertyDefinition prop, boolean isStatic, Set<String> imports)
   {
      if (prop.type != null && prop.type instanceof TypeQueryType)
      {
         out.println("// TODO: Suppressing property with type query type " + prop.name);
         return;
      }
      String type = "java.lang.Object";
      if (prop.type != null)
      {
         type = typeString(prop.type, prop.optional);
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
         out.println(String.format("  com.user00.domjnate.api.Object obj = com.user00.domjnate.util.Js.getMember(_win, \"%1$s\", com.user00.domjnate.api.Object.class);", className));
         out.println(String.format("  return com.user00.domjnate.util.Js.getMember(obj, \"%1$s\", %2$s.class);", prop.name, type));
         out.println("}");
         
         if (!prop.readOnly)
         {
            if (!prop.name.equals("prototype")) System.out.println("MINGMING" +className);
            out.println("@JsOverlay");
            out.println(String.format("public static void %1$s(com.user00.domjnate.api.WindowOrWorkerGlobalScope _win, %2$s val) {", setterName(prop.name), type));
            out.println(String.format("  com.user00.domjnate.api.Object obj = com.user00.domjnate.util.Js.getMember(_win, \"%1$s\", com.user00.domjnate.api.Object.class);", className));
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
   
   private void generateMethod(PrintWriter out, PropertyDefinition method)
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
         generateMethodWithOptionals(out, method.name, method.callSigType, method.problems, n, true);
      }
   }
   
   private void generateMethodWithOptionals(PrintWriter out, String methodName, CallSignatureDefinition callSigType, ProblemTracker methodProblems, int numOptionals, boolean withJsMethodAnnotation)
   {
      String returnType = typeString(callSigType.returnType, false);

      if (withJsMethodAnnotation)
         out.println(String.format("@JsMethod(name=\"%1$s\")", methodName));
      if (callSigType.genericTypeParameters != null)
      {
         generateGenericTypeParams(out, callSigType.genericTypeParameters);
      }
      out.print(returnType + " ");
      out.print(methodName(methodName));
      out.print("(");
      boolean isFirst = true;
      for (CallParameter param: callSigType.params)
      {
         if (!isFirst) out.print(", ");
         isFirst = false;
         String paramType = typeString(param.type, false);
         out.print(paramType + " ");
         out.print(param.name);
      }
      for (int n = 0; n < numOptionals; n++)
      {
         CallParameter param = callSigType.optionalParams.get(n);
         if (!isFirst) out.print(", ");
         isFirst = false;
         String paramType = typeString(param.type, false);
         out.print(paramType + " ");
         out.print(param.name);
      }
      if (callSigType.restParameter != null)
      {
         CallParameter param = callSigType.restParameter;
         if (!isFirst) out.print(", ");
         isFirst = false;
         String paramType = typeString(param.type, false);
         out.print(paramType + "... ");
         out.print(param.name);
      }
      out.print(");");
      out.println();
      if (methodProblems != null)
         methodProblems.dump(out);
      callSigType.problems.dump(out);
      for (CallParameter param: callSigType.params)
         param.problems.dump(out);
   }

   public void generate() throws IOException
   {
      for (InterfaceDefinition intf: api.interfaces.values())
      {
         generateInterface(intf);
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

      api.problems.dump(System.err);
   }
}
