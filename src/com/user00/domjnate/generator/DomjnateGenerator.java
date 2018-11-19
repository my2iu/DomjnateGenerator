package com.user00.domjnate.generator;

import java.io.IOException;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.tsparser.TsDeclarationsReader;
import com.user00.domjnate.generator.tsparser.TsIdlParser;

public class DomjnateGenerator
{
   public void go() throws IOException
   {
      // Read out type information for DOM
      ApiDefinition api = new ApiDefinition();
      TsIdlParser.DeclarationSourceFileContext libDomTs = TsDeclarationsReader.parseTs();
      libDomTs.accept(new TsDeclarationsReader.InterfaceFinder(api));
      
      // Generate JsInterop API based on type data that we've read
      ApiGenerator generator = new ApiGenerator();
      generator.generateFor(api);
   }
   


   public static void main(String [] args) throws IOException
   {
      new DomjnateGenerator().go();
   }
}
