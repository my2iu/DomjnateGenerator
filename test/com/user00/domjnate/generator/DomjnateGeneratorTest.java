package com.user00.domjnate.generator;

import java.io.IOException;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.tsparser.TsDeclarationsReader;
import com.user00.domjnate.generator.tsparser.TsIdlParser;

public class DomjnateGeneratorTest
{
   void generateFilesFromTs(TestGeneratorFileOutputManagerHelper files, String tsResourceName) throws IOException
   {
      ApiDefinition api = new ApiDefinition();
      TsIdlParser.DeclarationSourceFileContext libDomTs = 
            TsDeclarationsReader.parseTs(CharStreams.fromStream(this.getClass().getResourceAsStream(tsResourceName)));
      libDomTs.accept(new TsDeclarationsReader.InterfaceFinder(api));
      
      // Generate JsInterop API based on type data that we've read
      generateFiles(api, files);
   }
   
   void generateFiles(ApiDefinition api, TestGeneratorFileOutputManagerHelper files) throws IOException
   {
      ApiGenerator generator = new ApiGenerator();
      generator.outputDir = "";
      generator.pkg = "test.pkg";
      generator.files = files;
      generator.generateFor(api);
   }
   
   @Test
   public void testInterfaceProperty() throws IOException
   {
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generateFilesFromTs(files, "interfaceProperty.d.ts.in");
      files.compareWithTestFiles(1, this.getClass(), "interfaceProperty", ".out");
   }
   
   @Test
   public void testInterfaceMethod() throws IOException
   {
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generateFilesFromTs(files, "interfaceMethod.d.ts.in");
      files.compareWithTestFiles(1, this.getClass(), "interfaceMethod", ".out");
   }
   
   @Test
   public void testInterfaceExtends() throws IOException
   {
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generateFilesFromTs(files, "interfaceExtends.d.ts.in");
      files.compareWithTestFiles(2, this.getClass(), "interfaceExtends", ".out");
   }
}
