package com.user00.domjnate.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.function.Consumer;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.PredefinedType;
import com.user00.domjnate.generator.ast.CallSignatureDefinition.CallParameter;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.Type.TypeVisitor;
import com.user00.domjnate.generator.ast.TypeReference;

public class ApiGenerator
{
   String outputDir = "apigen";
   String pkg = "com.user00.domjnate.api";
   
   FileOutputManager files = new FileOutputManager();
   
   static class FileOutputManager
   {
      void makeFile(String outputDir, String pkg, String name, Consumer<PrintWriter> worker) throws IOException
      {
         String pkgDir = Paths.get(outputDir, pkg.replace('.', File.separatorChar)).toString();
         new File(pkgDir).mkdirs();
         try (FileOutputStream outStream = new FileOutputStream(pkgDir + File.separatorChar + name + ".java");
               Writer writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8);
               PrintWriter out = new PrintWriter(writer))
         {
            worker.accept(out);
         }         
      }
   }
   
   String getterName(String propName)
   {
      return "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
   }

   String setterName(String propName)
   {
      return "set" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
   }
   
   String methodName(String name)
   {
      return name;
   }
   
   String typeString(Type basicType, boolean nullable)
   {
      return basicType.visit(new TypeVisitor<String>() {
         @Override
         public String visitPredefinedType(PredefinedType basicType)
         {
            String type = "Object";
            switch(basicType.type)
            {
            case "any": type = "Object"; break;
            case "number": type = nullable ? "Double" : "double"; break;
            case "string": type = "String"; break;
            case "boolean": type = nullable ? "Boolean" : "boolean"; break;
            default: type = "unknown"; break;
            }
            return type;
         }
         @Override
         public String visitTypeReferenceType(TypeReference type)
         {
            return type.typeName;
         }
         
         @Override
         public String visitType(Type type)
         {
            return "Object";
         }
      });
   }

   void generateInterface(InterfaceDefinition intf) throws IOException
   {
      String name = intf.name;
      files.makeFile(outputDir, pkg, name, (out) -> {
         out.println(String.format("package %1$s;", pkg));
         out.println();
         out.println("import jsinterop.annotations.JsType;");
         out.println("import jsinterop.annotations.JsMethod;");
         out.println("import jsinterop.annotations.JsProperty;");
         out.println();

         intf.problems.dump(out);
         
         out.println(String.format("@JsType(isNative=true,name=\"%1$s\")", name));
         out.print(String.format("interface %1$s", name));
         if (intf.extendsTypes != null)
         {
            out.print(" extends ");
            boolean isFirst = true;
            for (TypeReference typeRef: intf.extendsTypes)
            {
               if (!isFirst)
                  out.print(", ");
               isFirst = false;
               out.print(typeRef.typeName);
               typeRef.problems.dump(out);
            }
         }
         out.println();
         out.println("{");
         
         for (PropertyDefinition prop: intf.properties)
         {
            String type = "Object";
            if (prop.basicType != null)
            {
               type = typeString(prop.basicType, prop.optional);
               prop.basicType.problems.dump(out);
            }
            out.println(String.format("@JsProperty(name=\"%1$s\")", prop.name));
            out.println(String.format("%2$s %1$s();", getterName(prop.name), type));
            
            if (!prop.readOnly)
            {
               out.println(String.format("@JsProperty(name=\"%1$s\")", prop.name));
               out.println(String.format("void %1$s(%2$s val);", setterName(prop.name), type));
            }
         }
         for (PropertyDefinition method: intf.methods)
         {
            generateMethod(out, method);
         }
         
         out.println("}");
      });
   }

   private void generateMethod(PrintWriter out, PropertyDefinition method)
   {
      if ("addEventListener".equals(method.name) && method.callSigType.genericTypeParameters != null)
      {
         out.println("// TODO: Suppressing addEventListener with typed events");
         return;
      }
      if ("removeEventListener".equals(method.name) && method.callSigType.genericTypeParameters != null)
      {
         out.println("// TODO: Suppressing removeEventListener with typed events");
         return;
      }
      if (method.callSigType.genericTypeParameters != null)
         out.println("Unhandled type parameters on method");
      for (int n = 0; n <= method.callSigType.optionalParams.size(); n++)
      {
         generateMethodWithOptionals(out, method, n);
      }
   }
   
   private void generateMethodWithOptionals(PrintWriter out, PropertyDefinition method, int numOptionals)
   {
      String returnType = "Object";
      
      out.println(String.format("@JsMethod(name=\"%1$s\")", method.name));
      out.print(returnType + " ");
      out.print(methodName(method.name));
      out.print("(");
      boolean isFirst = true;
      for (CallParameter param: method.callSigType.params)
      {
         if (!isFirst) out.print(", ");
         isFirst = false;
         String paramType = "Object";
         out.print(paramType + " ");
         out.print(param.name);
      }
      for (int n = 0; n < numOptionals; n++)
      {
         CallParameter param = method.callSigType.optionalParams.get(n);
         if (!isFirst) out.print(", ");
         isFirst = false;
         String paramType = "Object";
         out.print(paramType + " ");
         out.print(param.name);
      }
      out.print(");");
      out.println();
      method.problems.dump(out);
      method.callSigType.problems.dump(out);
      for (CallParameter param: method.callSigType.params)
         param.problems.dump(out);
   }

   public void generateFor(ApiDefinition api) throws IOException
   {
      for (InterfaceDefinition intf: api.interfaces.values())
      {
         generateInterface(intf);
      }

      api.problems.dump(System.err);
   }
}
