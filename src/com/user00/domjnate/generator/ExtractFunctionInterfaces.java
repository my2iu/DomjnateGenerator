package com.user00.domjnate.generator;

import java.util.Map;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.CallSignatureDefinition;
import com.user00.domjnate.generator.ast.FunctionType;
import com.user00.domjnate.generator.ast.IndexSignatureDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.PredefinedType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.Type;

/**
 * Finds all the callback functions used and converts them to 
 * interfaces that can be easily represented in Java. 
 */
public class ExtractFunctionInterfaces
{
   void handleFunctionInterface(InterfaceDefinition intf, ApiDefinition api) 
   {
      
   }

   class FunctionTypeExtractor extends Type.TypeVisitor<Type>
   {
      Map<String, CallSignatureDefinition> localFunctionTypes;
      FunctionTypeExtractor(InterfaceDefinition intf)
      {
         localFunctionTypes = intf.functionTypes;
      }

      @Override
      public Type visitFunctionType(FunctionType type) 
      {
         return type;
      }
      
      public Type visitType(Type type)
      {
         return type;
      }

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
      FunctionTypeExtractor functionReplacer = new FunctionTypeExtractor(intf);
      
      if (!intf.isStaticOnly)
      {
         for (PropertyDefinition prop: intf.properties)
         {
            prop.type = prop.type.visit(functionReplacer);
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
            prop.type = prop.type.visit(functionReplacer);
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
