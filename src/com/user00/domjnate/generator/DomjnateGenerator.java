package com.user00.domjnate.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.ErrorType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.TypeReference;
import com.user00.domjnate.generator.tsparser.TsDeclarationsReader;
import com.user00.domjnate.generator.tsparser.TsIdlParser;

public class DomjnateGenerator
{
   String outputDir = "apigen";
   public void go() throws IOException
   {
      ApiGenerator generator = new ApiGenerator();
      generator.outputDir = outputDir;
      go(new CharStream[] {
            CharStreams.fromFileName("idl/lib.dom.d.ts"),     
            CharStreams.fromFileName("idl/lib.es5.d.ts"),     
            CharStreams.fromFileName("idl/lib.es2015.promise.d.ts"),     
            CharStreams.fromFileName("idl/lib.es2015.symbol.d.ts"),     
      }, generator);
   }

   public void go(CharStream[] inputStreams, ApiGenerator generator) throws IOException
   {
      // Read out type information for DOM
      ApiDefinition api = new ApiDefinition();
      for (CharStream input: inputStreams)
      {
         TsIdlParser.DeclarationSourceFileContext libDomTs = TsDeclarationsReader.parseTs(input);
         libDomTs.accept(new TsDeclarationsReader.TopLevelReader(api));
      }
      
      // Apply changes
      fixupApi(api);
      
      // Generate JsInterop API based on type data that we've read
      generator.api = api;
      generator.generate();
   }
   
   void fixupApi(ApiDefinition api)
   {
      // Ignore the fact that objects can be used for event listeners and allow for lambdas only
      TypeReference eventListenerOnly = new TypeReference();
      eventListenerOnly.typeName = "EventListener";
      api.typeAliases.put("EventListenerOrEventListenerObject", eventListenerOnly);

      // Remove some extra getElementsByTagNameNS() methods on Element
      if (api.interfaces.containsKey("Element"))
      {
         api.interfaces.get("Element").methods.removeIf(
               method -> "getElementsByTagNameNS".equals(method.name) && method.callSigType.params.get(0).type instanceof ErrorType);
      }

      // Remove some extra createEvent() and createElementNS() methods on Document
      if (api.interfaces.containsKey("Document"))
      {
         api.interfaces.get("Document").methods.removeIf(
               method -> "createElementNS".equals(method.name) && method.callSigType.params.get(0).type instanceof ErrorType);
         api.interfaces.get("Document").methods.removeIf(
               method -> "createEvent".equals(method.name) && method.callSigType.params.get(0).type instanceof ErrorType);
      }
      
      // SVGElement.className is deprecated, and it conflicts with Element.className
      if (api.interfaces.containsKey("SVGElement"))
      {
         api.interfaces.get("SVGElement").properties.removeIf( p -> "className".equals(p.name));
      }
      
      // Remove the String type
      api.interfaces.remove("String");
      
      // Remove artificial interfaces used to store constructors and static methods
      for (String intfName: Arrays.asList(
            "URIErrorConstructor",
            "PromiseConstructor",
            "Int8ArrayConstructor",
            "Float32ArrayConstructor",
            "Uint8ClampedArrayConstructor",
            "FunctionConstructor",
            "ArrayConstructor",
            "TypeErrorConstructor",
            "ArrayBufferConstructor",
            "Uint32ArrayConstructor",
            "Uint16ArrayConstructor",
            "BooleanConstructor",
            "ObjectConstructor",
            "SyntaxErrorConstructor",
            "Int32ArrayConstructor",
            "RangeErrorConstructor",
            "DateConstructor",
            "DataViewConstructor",
            "ReferenceErrorConstructor",
            "RegExpConstructor",
            "NumberConstructor",
            "Int16ArrayConstructor",
            "EvalErrorConstructor",
            "SymbolConstructor",
            "ErrorConstructor",
            "Uint8ArrayConstructor",
            "Float64ArrayConstructor"))
      {
         if (api.interfaces.containsKey(intfName))
         {
            api.interfaces.get(intfName).doNotGenerateJava = true;
         }
// static ambient CSS with CSS
// static ambient Math with Math
// static ambient JSON with JSON
// static ambient ServiceUIFrameContext with ServiceUIFrameContext

      }
   }


   public static void main(String [] args) throws IOException
   {
      DomjnateGenerator generator = new DomjnateGenerator();
      if (args.length > 0)
         generator.outputDir = args[0];
      generator.go();
   }
}
