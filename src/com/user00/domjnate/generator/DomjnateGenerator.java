package com.user00.domjnate.generator;

import java.io.IOException;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.tsparser.TsDeclarationsReader;
import com.user00.domjnate.generator.tsparser.TsIdlParser;

public class DomjnateGenerator
{
   String outputDir = "apigen";
   public void go() throws IOException
   {
      // Read out type information for DOM
      ApiDefinition api = new ApiDefinition();
      TsIdlParser.DeclarationSourceFileContext libDomTs = TsDeclarationsReader.parseTs("idl/lib.dom.d.ts");
      libDomTs.accept(new TsDeclarationsReader.TopLevelReader(api));
      
      // Generate JsInterop API based on type data that we've read
      ApiGenerator generator = new ApiGenerator();
      generator.outputDir = outputDir;
      generator.api = api;
      generator.generate();
   }
   


   public static void main(String [] args) throws IOException
   {
      DomjnateGenerator generator = new DomjnateGenerator();
      if (args.length > 0)
         generator.outputDir = args[0];
      generator.go();
   }
}
