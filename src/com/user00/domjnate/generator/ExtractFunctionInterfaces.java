package com.user00.domjnate.generator;

import java.util.Map;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.CallSignatureDefinition;
import com.user00.domjnate.generator.ast.FunctionType;
import com.user00.domjnate.generator.ast.IndexSignatureDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.LocalFunctionType;
import com.user00.domjnate.generator.ast.NullableType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.Type;

/**
 * Finds all the callback functions used and converts them to 
 * interfaces that can be easily represented in Java. 
 */
public class ExtractFunctionInterfaces
{
   static boolean checkInterfaceExists(String name, ApiDefinition api, Map<String, CallSignatureDefinition> localFunctionTypes)
   {
      // Not quite correct because we're using JS namespaces instead of Java packages
      if (api.interfaces.containsKey(name)) return true;
      if (localFunctionTypes.containsKey(name)) return true;
      return false;
   }
   
   class FunctionTypeExtractor extends Type.TypeVisitorWithInput<String, Type>
   {
      Map<String, CallSignatureDefinition> localFunctionTypes;
      ApiDefinition api;
      FunctionTypeExtractor(ApiDefinition api, InterfaceDefinition intf)
      {
         localFunctionTypes = intf.functionTypes;
         this.api = api;
      }

      Type substituteFunctionType(FunctionType type, String name)
      {
         LocalFunctionType nestedType = new LocalFunctionType();
         nestedType.callSigType = type.callSigType;
         nestedType.nestedName = name;
         // Sometimes the callback method signatures have a "this" argument set, but that
         // doesn't work in Java anyway, so I'll remove it from the argument list.
         if (nestedType.callSigType.params != null && nestedType.callSigType.params.size() > 0
               && nestedType.callSigType.params.get(0).name.equals("this"))
         {
            nestedType.callSigType.params.remove(0);
         }
         localFunctionTypes.put(nestedType.nestedName, nestedType.callSigType);
         return nestedType;
      }
      
      @Override
      public Type visitFunctionType(FunctionType type, String functionTypeBaseName) 
      {
         functionTypeBaseName = functionTypeBaseName.substring(0, 1).toUpperCase() + functionTypeBaseName.substring(1);
         String fnTypeName = functionTypeBaseName + "Callback";
         if (!checkInterfaceExists(fnTypeName, api, localFunctionTypes))
         {
            return substituteFunctionType(type, fnTypeName);
         }
         for (int n = 0; ; n++)
         {
            fnTypeName = functionTypeBaseName + "Callback" + n;
            if (!checkInterfaceExists(fnTypeName, api, localFunctionTypes))
            {
               return substituteFunctionType(type, fnTypeName);
            }
         }
      }
      
      @Override
      public Type visitNullableType(NullableType type, String in)
      {
         type.subtype = type.subtype.visit(this, in);
         return type;
      }
      
      @Override
      public Type visitType(Type type, String functionTypeBaseName)
      {
         return type;
      }

   }

   void handleFunctionInterface(InterfaceDefinition intf, ApiDefinition api) 
   {
      
   }

   void handleInterface(ApiDefinition api, InterfaceDefinition intf) 
   {
      if (intf.doNotGenerateJava) return;
      if (intf.isFunction())
      {
         handleFunctionInterface(intf, api);
         return;
      }
      String name = intf.name;
      InterfaceDefinition staticIntf = intf.staticIntf;
      FunctionTypeExtractor functionReplacer = new FunctionTypeExtractor(api, intf);
      
      if (!intf.isStaticOnly)
      {
         for (PropertyDefinition prop: intf.properties)
         {
            prop.type = prop.type.visit(functionReplacer, prop.name);
         }
         for (PropertyDefinition method: intf.methods)
         {

//            imports.add("jsinterop.annotations.JsMethod");
//            generateMethod(out, method, api, intf, fullPkg, generics);
         }
         for (IndexSignatureDefinition idxSig: intf.indexSignatures)
         {
//            // TODO: Just a temporary handling of wrapping generics to get things to compile
//            // (this should eventually be handled better)
//            boolean isReturnGeneric = generics.isGeneric(idxSig.returnType);
//            
//            String returnType = typeString(idxSig.returnType, new TypeStringGenerationContext(api, fullPkg, generics));
//            String returnTypeDescription = typeString(idxSig.returnType, new TypeStringGenerationContext(api, fullPkg, generics).withTypeDescription(true));
//            imports.add("jsinterop.annotations.JsOverlay");
//            if (idxSig.indexType instanceof PredefinedType && ((PredefinedType)idxSig.indexType).type.equals("number"))
//            {
//               out.println("@JsOverlay");
//               if (isReturnGeneric)
//               {
//                  out.println(String.format("public default %1$s get(double %2$s, Class<%1$s> _type) {", returnType, idxSig.indexName));
//                  out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getIndex(this, %2$s, _type);", returnType, idxSig.indexName));
//               }
//               else
//               {
//                  out.println(String.format("public default %1$s get(double %2$s) {", returnType, idxSig.indexName));
//                  out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getIndex(this, %2$s, %3$s);", returnType, idxSig.indexName, returnTypeDescription));
//               }
//               out.println("}");
//               if (!idxSig.readOnly) 
//               {
//                  out.println("@JsOverlay");
//                  out.println(String.format("public default void set(double %2$s, %1$s val) {", returnType, idxSig.indexName));
//                  out.println(String.format("  com.user00.domjnate.util.Js.setIndex(this, %2$s, val);", returnType, idxSig.indexName));
//                  out.println("}");
//               }
//            }
//            else if (idxSig.indexType instanceof PredefinedType && ((PredefinedType)idxSig.indexType).type.equals("string"))
//            {
//               out.println("@JsOverlay");
//               if (isReturnGeneric)
//               {
//                  out.println(String.format("public default %1$s get(String %2$s, Class<%1$s> _type) {", returnType, idxSig.indexName));
//                  out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getMember(this, %2$s, _type);", returnType, idxSig.indexName));
//               }
//               else
//               {
//                  out.println(String.format("public default %1$s get(String %2$s) {", returnType, idxSig.indexName));
//                  out.println(String.format("  return (%1$s)com.user00.domjnate.util.Js.getMember(this, %2$s, %3$s);", returnType, idxSig.indexName, returnTypeDescription));
//               }
//               out.println("}");
//               if (!idxSig.readOnly) 
//               {
//                  out.println("@JsOverlay");
//                  out.println(String.format("public default void set(String %2$s, %1$s val) {", returnType, idxSig.indexName));
//                  out.println(String.format("  com.user00.domjnate.util.Js.setMember(this, %2$s, val);", returnType, idxSig.indexName));
//                  out.println("}");
//               }
//            }
//            else
//               out.println("Unhandled index signature on interface");
         }
         for (CallSignatureDefinition construct: intf.constructSignatures)
         {
//            makeConstructor(out, construct, api, fullPkg, generics, false, imports);
         }
         for (CallSignatureDefinition call: intf.callSignatures)
         {
//            out.println("Unhandled call signature on interface");
         }
      }
      
      if (staticIntf != null)
      {
         for (PropertyDefinition prop: staticIntf.properties)
         {
            prop.type = prop.type.visit(functionReplacer, prop.name);
//            makeProperty(out, name, prop, api, fullPkg, generics, true, imports);
         }
         for (PropertyDefinition method: staticIntf.methods)
         {
//            imports.add("jsinterop.annotations.JsOverlay");
//            generateStaticMethod(out, name, method, api, fullPkg, generics);
         }
         for (CallSignatureDefinition call: staticIntf.callSignatures)
         {
//            makeStaticCallOnInterface(out, intf.name, call, api, fullPkg, generics, imports);
         }
         for (CallSignatureDefinition construct: staticIntf.constructSignatures)
         {
//            makeConstructor(out, construct, api, fullPkg, generics, true, imports);
         }
         for (IndexSignatureDefinition idxSig: staticIntf.indexSignatures)
         {
//            out.println("Unhandled static index");
         }
      }

   }
   
   public void convertFunctions(ApiDefinition api)
   {
      for (InterfaceDefinition intf: api.interfaces.values())
      {
         handleInterface(api, intf);
      }
      for (Map.Entry<String, ApiDefinition> namespace: api.namespaces.entrySet())
      {
         convertFunctions(namespace.getValue());
      }
   }
}
