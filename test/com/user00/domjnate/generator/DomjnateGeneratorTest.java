package com.user00.domjnate.generator;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

public class DomjnateGeneratorTest
{
   void generateFilesFromTs(TestGeneratorFileOutputManagerHelper files, String tsResourceName) throws IOException
   {
      ApiGenerator generator = new ApiGenerator();
      generator.outputDir = "";
      generator.pkg = "test.pkg";
      generator.files = files;
      DomjnateGenerator domjnateGenerator = new DomjnateGenerator();
      domjnateGenerator.go(new CharStream[] {CharStreams.fromStream(this.getClass().getResourceAsStream(tsResourceName))}, generator);
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
   public void testInterfaceMethodRestParameter() throws IOException
   {
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generateFilesFromTs(files, "interfaceMethodRestParameter.d.ts.in");
      files.compareWithTestFiles(1, this.getClass(), "interfaceMethodRestParameter", ".out");
   }

   @Test
   public void testInterfaceExtends() throws IOException
   {
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generateFilesFromTs(files, "interfaceExtends.d.ts.in");
      files.compareWithTestFiles(2, this.getClass(), "interfaceExtends", ".out");
   }
   
   @Test
   public void testFunctionInterface() throws IOException
   {
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generateFilesFromTs(files, "functionInterface.d.ts.in");
      files.compareWithTestFiles(1, this.getClass(), "functionInterface", ".out");
   }
   
   @Test
   public void testOrNull() throws IOException
   {
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generateFilesFromTs(files, "orNull.d.ts.in");
      files.compareWithTestFiles(1, this.getClass(), "orNull", ".out");
   }

   @Test
   public void testAlias() throws IOException
   {
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generateFilesFromTs(files, "typeAlias.d.ts.in");
      files.compareWithTestFiles(3, this.getClass(), "typeAlias", ".out");
   }

   @Test
   public void testEventHandler() throws IOException
   {
      // Special handling of event handlers
      TestGeneratorFileOutputManagerHelper files = new TestGeneratorFileOutputManagerHelper();
      generateFilesFromTs(files, "eventHandler.d.ts.in");
      files.compareWithTestFiles(4, this.getClass(), "eventHandler", ".out");
   }

}
