package com.user00.domjnate.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.CallSignatureDefinition;
import com.user00.domjnate.generator.ast.CallSignatureDefinition.CallParameter;
import com.user00.domjnate.generator.ast.FunctionType;
import com.user00.domjnate.generator.ast.GenericParameter;
import com.user00.domjnate.generator.ast.IndexSignatureDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.LocalFunctionDefinition;
import com.user00.domjnate.generator.ast.LocalFunctionType;
import com.user00.domjnate.generator.ast.NullableType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.TypeReference;

/**
 * Finds all the callback functions used and converts them to 
 * interfaces that can be easily represented in Java. 
 */
public class ExtractFunctionInterfaces
{
   static boolean checkInterfaceExists(String name, ApiDefinition api, Map<String, LocalFunctionDefinition> localFunctionTypes)
   {
      // Not quite correct because we're using JS namespaces instead of Java packages
      if (api.interfaces.containsKey(name)) return true;
      if (localFunctionTypes.containsKey(name)) return true;
      return false;
   }
   
   class GenericParameterExtractor extends Type.RecursiveTypeVisitorWithInput<GenericContext>
   {
      List<GenericParameter> genericTypeParams = new ArrayList<>();
      
      @Override public Void visitTypeReferenceType(TypeReference type, GenericContext generics)
      {
         if (generics.isGeneric(type))
         {
            GenericParameter param = generics.getGenericParam(type);
            if (!genericTypeParams.contains(param))
               genericTypeParams.add(param);
         }
         return null;
      }
   }
   
   class FunctionTypeExtractorContext 
   {
      FunctionTypeExtractorContext(String name, GenericContext generics)
      {
         this.functionTypeBaseName = name;
         this.generics = generics;
      }
      String functionTypeBaseName;
      GenericContext generics;
   }
   
   class FunctionTypeExtractor extends Type.TypeVisitorWithInput<FunctionTypeExtractorContext, Type>
   {
      Map<String, LocalFunctionDefinition> localFunctionTypes;
      ApiDefinition api;
      FunctionTypeExtractor(ApiDefinition api, InterfaceDefinition intf)
      {
         localFunctionTypes = intf.functionTypes;
         this.api = api;
      }

      Type substituteFunctionType(FunctionType type, String name, GenericContext generics)
      {
         // Figure out what generic parameters are used
         GenericParameterExtractor genericParamExtractor = new GenericParameterExtractor();
         type.visit(genericParamExtractor, generics);

         // Replace the function type with a reference to a nested interface
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
         
         // Create the nested interface too
         LocalFunctionDefinition fn = new LocalFunctionDefinition();
         fn.callSigType = nestedType.callSigType;
         if (!genericParamExtractor.genericTypeParams.isEmpty())
            fn.genericTypeParams = genericParamExtractor.genericTypeParams;
         localFunctionTypes.put(nestedType.nestedName, fn);
         return nestedType;
      }
      
      @Override
      public Type visitFunctionType(FunctionType type, FunctionTypeExtractorContext ctx) 
      {
         String functionTypeBaseName = ctx.functionTypeBaseName.substring(0, 1).toUpperCase() + ctx.functionTypeBaseName.substring(1);
         String fnTypeName = functionTypeBaseName + "Callback";
         if (!checkInterfaceExists(fnTypeName, api, localFunctionTypes))
         {
            return substituteFunctionType(type, fnTypeName, ctx.generics);
         }
         for (int n = 0; ; n++)
         {
            fnTypeName = functionTypeBaseName + "Callback" + n;
            if (!checkInterfaceExists(fnTypeName, api, localFunctionTypes))
            {
               return substituteFunctionType(type, fnTypeName, ctx.generics);
            }
         }
      }
      
      @Override
      public Type visitNullableType(NullableType type, FunctionTypeExtractorContext in)
      {
         type.subtype = type.subtype.visit(this, in);
         return type;
      }
      
      @Override
      public Type visitType(Type type, FunctionTypeExtractorContext in)
      {
         return type;
      }

      public void visitCallSignature(CallSignatureDefinition callSigType, FunctionTypeExtractorContext superCtx)
      {
         FunctionTypeExtractorContext ctx = new FunctionTypeExtractorContext(superCtx.functionTypeBaseName, new GenericContext(callSigType.genericTypeParameters, superCtx.generics));
         if (callSigType.returnType != null)
            callSigType.returnType = callSigType.returnType.visit(this, ctx);
         if (callSigType.restParameter != null)
            callSigType.restParameter.type = callSigType.restParameter.type.visit(this, ctx);
         if (callSigType.optionalParams != null)
         {
            for (CallParameter param: callSigType.optionalParams)
            {
               if (param.type != null)
                  param.type = param.type.visit(this, ctx);
            }
         }
         if (callSigType.params != null)
         {
            for (CallParameter param: callSigType.params)
            {
               if (param.type != null)
                  param.type = param.type.visit(this, ctx);
            }
         }
      }
   }

   void handleFunctionInterface(InterfaceDefinition intf, ApiDefinition api) 
   {
      String name = intf.name;
      GenericContext generics = new GenericContext(intf.genericTypeParams);
      FunctionTypeExtractor functionReplacer = new FunctionTypeExtractor(api, intf);
      for (CallSignatureDefinition call: intf.callSignatures)
      {
         functionReplacer.visitCallSignature(call, new FunctionTypeExtractorContext(name, generics));
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
      GenericContext generics = new GenericContext(intf.genericTypeParams);
      FunctionTypeExtractor functionReplacer = new FunctionTypeExtractor(api, intf);
      
      if (!intf.isStaticOnly)
      {
         for (PropertyDefinition prop: intf.properties)
         {
            prop.type = prop.type.visit(functionReplacer, new FunctionTypeExtractorContext(prop.name, generics));
         }
         for (PropertyDefinition method: intf.methods)
         {
            if (hasGenericKeyOfParameters(method.callSigType)) continue;
            functionReplacer.visitCallSignature(method.callSigType, new FunctionTypeExtractorContext(method.name, generics));
         }
         for (IndexSignatureDefinition idxSig: intf.indexSignatures)
         {
            idxSig.indexType = idxSig.indexType.visit(functionReplacer, new FunctionTypeExtractorContext(name, generics)); 
         }
         for (CallSignatureDefinition construct: intf.constructSignatures)
         {
            functionReplacer.visitCallSignature(construct, new FunctionTypeExtractorContext(name, generics));
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
            prop.type = prop.type.visit(functionReplacer, new FunctionTypeExtractorContext(prop.name, generics));
         }
         for (PropertyDefinition method: staticIntf.methods)
         {
            if (hasGenericKeyOfParameters(method.callSigType)) continue;
            functionReplacer.visitCallSignature(method.callSigType, new FunctionTypeExtractorContext(method.name, generics));
         }
         for (CallSignatureDefinition call: staticIntf.callSignatures)
         {
            functionReplacer.visitCallSignature(call, new FunctionTypeExtractorContext(name, generics));
         }
         for (CallSignatureDefinition construct: staticIntf.constructSignatures)
         {
            functionReplacer.visitCallSignature(construct, new FunctionTypeExtractorContext(name, generics));
         }
         for (IndexSignatureDefinition idxSig: staticIntf.indexSignatures)
         {
//            out.println("Unhandled static index");
         }
      }

   }
   
   static boolean hasGenericKeyOfParameters(CallSignatureDefinition callSigType)
   {
      if (callSigType.genericTypeParameters != null)
      {
         // Ignore keyof generic type parameters for now
         for (GenericParameter generic: callSigType.genericTypeParameters)
         {
            if (generic.simpleExtendsKeyOf != null)
            {
               return true;
            }
         }
      }
      return false;
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
