package com.user00.domjnate.generator.tsparser;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.tsparser.TsIdlParser.DeclarationElementContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.InterfaceDeclarationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.PropertySignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeMemberContext;

public class TsDeclarationsReader
{
   static class TypeBodyReader extends TsIdlBaseVisitor<Void>
   {
      InterfaceDefinition intf;
      public TypeBodyReader(InterfaceDefinition intf)
      {
         this.intf = intf;
      }
      
      @Override
      public Void visitTypeMember(TypeMemberContext ctx)
      {
         if (ctx.callSignature() != null)
            intf.addProblem("Unhandled call signature");
         if (ctx.constructSignature() != null)
            intf.addProblem("Unhandled construct signature");
         if (ctx.indexSignature() != null)
            intf.addProblem("Unhandled index signature");
         if (ctx.methodSignature() != null)
            intf.addProblem("Unhandled method signature");
         if (ctx.propertySignature() != null)
            ctx.propertySignature().accept(this);
         return null;
      }
      
      @Override
      public Void visitPropertySignature(PropertySignatureContext ctx)
      {
         if (ctx.propertyName().NumericLiteral() != null || ctx.propertyName().StringLiteral() != null)
         {
            intf.addProblem("Property with string literal or numeric literal as a name");
            return null;
         }
         String name = ctx.propertyName().identifierName().getText(); 
         
         PropertyDefinition prop = new PropertyDefinition();
         prop.name = name;
         
         if (ctx.propertySignatureReadOnly() != null)
            prop.readOnly = true;
         if (ctx.propertySignatureOptional() != null)
            prop.optional = true;
         
         intf.properties.add(prop);
         return null;
      }
   }
   
   public static class InterfaceFinder extends TsIdlBaseVisitor<Void>
   {
      public InterfaceFinder(ApiDefinition api)
      {
         this.api = api;
      }
      ApiDefinition api;
      @Override
      public Void visitDeclarationElement(DeclarationElementContext ctx)
      {
         if (ctx.interfaceDeclaration() != null)
            ctx.interfaceDeclaration().accept(this);
         else if (ctx.ambientDeclaration() != null)
            api.addProblem("Unhandled ambient declaration " + ctx.ambientDeclaration().getText());
         else if (ctx.typeAliasDeclaration() != null)
            api.addProblem("Unhandled type alias declaration " + ctx.typeAliasDeclaration().getText());
         return null;
      }
      
      @Override
      public Void visitInterfaceDeclaration(InterfaceDeclarationContext ctx)
      {
         String name = ctx.bindingIdentifier().getText();
         
         InterfaceDefinition intf = new InterfaceDefinition();
         intf.name = name;
         api.interfaces.put(intf.name, intf);
         
         if (ctx.interfaceExtendsClause() != null)
            intf.addProblem("Unhandled extends " + ctx.interfaceExtendsClause().getText());
         if (ctx.typeParameters() != null)
            intf.addProblem("Unhandled generics " + ctx.typeParameters().getText());
         
         if (ctx.objectType().typeBody() != null)
            ctx.objectType().typeBody().accept(new TypeBodyReader(intf));
         return null;
      }
   }
   
   public static TsIdlParser.DeclarationSourceFileContext parseTs() throws IOException
   {
      CharStream file = CharStreams.fromFileName("idl/lib.dom.d.ts");
      TsIdlLexer lexer = new TsIdlLexer(file);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      TsIdlParser parser = new TsIdlParser(tokens);
      TsIdlParser.DeclarationSourceFileContext tree = parser.declarationSourceFileEOF().declarationSourceFile();
      
      return tree;
   }
}
