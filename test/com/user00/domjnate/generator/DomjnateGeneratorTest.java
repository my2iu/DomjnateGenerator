package com.user00.domjnate.generator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import com.user00.domjnate.generator.ApiGenerator.FileOutputManager;
import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.tsparser.TsDeclarationsReader;
import com.user00.domjnate.generator.tsparser.TsIdlParser;

public class DomjnateGeneratorTest
{
   @Test
   public void testInterfaceProperty() throws IOException
   {
      ApiDefinition api = new ApiDefinition();
      TsIdlParser.DeclarationSourceFileContext libDomTs = 
            TsDeclarationsReader.parseTs(CharStreams.fromStream(this.getClass().getResourceAsStream("interfaceProperty.d.ts.in")));
      libDomTs.accept(new TsDeclarationsReader.InterfaceFinder(api));
      
      // Generate JsInterop API based on type data that we've read
      ApiGenerator generator = new ApiGenerator();
      generator.outputDir = "";
      generator.pkg = "test.pkg";
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generator.files = files;
      generator.generateFor(api);
      files.compareWithTestFiles(1, this.getClass(), "interfaceProperty", ".out");
      
      

   }
}
