package com.user00.domjnate.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.PropertyDefinition;

public class ApiGenerator
{
   String outputDir = "apigen";
   String pkg = "com.user00.domjnate.api";
   
   String getterName(String propName)
   {
      return "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
   }

   String setterName(String propName)
   {
      return "set" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
   }

   void generateInterface(InterfaceDefinition intf) throws IOException
   {
      String pkgDir = outputDir + File.separatorChar + pkg.replace('.', File.separatorChar);
      String name = intf.name;
      new File(pkgDir).mkdirs();
      try (FileOutputStream outStream = new FileOutputStream(pkgDir + File.separatorChar + name + ".java");
            Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
            PrintWriter out = new PrintWriter(writer))
      {
         out.println(String.format("package %0$s;", pkg));
         out.println();
         out.println("import jsinterop.annotations.JsType;");
         out.println("import jsinterop.annotations.JsProperty;");
         out.println();

         for (String err: intf.problems)
         {
            out.println(err);
         }
         
         out.println(String.format("@JsType(isNative=true,name=\"%0$s\")", name));
         out.println(String.format("interface %0$s", name));
         out.println("{");
         
         for (PropertyDefinition prop: intf.properties)
         {
            out.println(String.format("@JsProperty(name=\"%0$s\")", prop.name));
            out.println(String.format("Object %0$s();", getterName(prop.name)));
            
            if (!prop.readOnly)
            {
               out.println(String.format("@JsProperty(name=\"%0$s\")", prop.name));
               out.println(String.format("void %0$s(Object val);", setterName(prop.name)));
            }

            if (prop.optional)
               out.println("Unhandled optional property");
         }
         
         out.println("}");
      }
   }
   
   public void generateFor(ApiDefinition api) throws IOException
   {
      for (InterfaceDefinition intf: api.interfaces.values())
      {
         generateInterface(intf);
      }
      
      for (String err: api.problems)
      {
         System.err.println(err);
      }

   }
}
