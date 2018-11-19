package com.user00.domjnate.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;

import com.user00.domjnate.generator.ApiGenerator.FileOutputManager;

public class TestGeneratorFileOutputManagerHelper extends FileOutputManager {
   Map<String, String> files = new HashMap<>();
   @Override
   void makeFile(String outputDir, String pkg, String name,
         Consumer<PrintWriter> worker) throws IOException
   {
      String fileName = outputDir + "-" + pkg + "-" + name + ".java";
      try (StringWriter writer = new StringWriter();
            PrintWriter out = new PrintWriter(writer))
      {
         worker.accept(out);
         out.close();
         files.put(fileName, writer.toString());
      }
   }
   
   void compareWithTestFiles(int expectedFileCount, Class<?> resourceClass, String prefix, String postfix) throws IOException
   {
      Assertions.assertEquals(expectedFileCount, files.size());
      for (Map.Entry<String, String> outputEntry: files.entrySet())
      {
         String goldenFileName = prefix + outputEntry.getKey() + postfix;
         InputStream expectedGoldenFile = resourceClass.getResourceAsStream(goldenFileName);
         Assertions.assertNotNull(expectedGoldenFile, "Could not find golden file " + goldenFileName);
         String goldenFile = new String(expectedGoldenFile.readAllBytes(), StandardCharsets.UTF_8);
         Assertions.assertEquals(goldenFile, outputEntry.getValue());
      }
   }
}
