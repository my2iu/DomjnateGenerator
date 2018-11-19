package com.user00.domjnate.generator.tsparser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.BasicJsType;
import com.user00.domjnate.generator.ast.CallSignatureDefinition;
import com.user00.domjnate.generator.ast.CallSignatureDefinition.CallParameter;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.TypeReference;
import com.user00.domjnate.generator.tsparser.TsIdlParser.CallSignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.ClassOrInterfaceTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.DeclarationElementContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.InterfaceDeclarationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.InterfaceExtendsClauseContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.MethodSignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.NamespaceNameContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.ParameterListContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.PrimaryTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.PropertySignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.RequiredParameterContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeMemberContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeReferenceContext;

public class TsDeclarationsReader
{
   static TsIdlBaseVisitor<TypeReference> TYPE_REFERENCE_READER = new TsIdlBaseVisitor<TypeReference>() {
      @Override
      public TypeReference visitTypeReference(TypeReferenceContext ctx) {
         TypeReference ref = new TypeReference();
         String typeName = ctx.typeName().identifierReference().getText();
         NamespaceNameContext namespace = ctx.typeName().namespaceName();
         while (namespace != null)
         {
            typeName = namespace.identifierReference().getText() + "." + typeName;
            namespace = namespace.namespaceName();
         }
         ref.typeName = typeName;
         if (ctx.typeArguments() != null)
            ref.problems.add("Unhandled type arguments on " + ref.typeName);
         return ref;
      }
   }; 
   
   static class InterfaceExtendsReader extends TsIdlBaseVisitor<Void>
   {
      List<TypeReference> types = new ArrayList<>();
      @Override
      public Void visitInterfaceExtendsClause(InterfaceExtendsClauseContext ctx)
      {
         ctx.classOrInterfaceTypeList().accept(this);
         return null;
      }
      @Override
      public Void visitClassOrInterfaceType(ClassOrInterfaceTypeContext ctx)
      {
         types.add(ctx.typeReference().accept(TYPE_REFERENCE_READER));
         return null;
      }
   }
   static class CallSignatureReader extends TsIdlBaseVisitor<Void>
   {
      CallSignatureDefinition sig = new CallSignatureDefinition();
      
      @Override
      public Void visitCallSignature(CallSignatureContext ctx)
      {
         if (ctx.typeParameters() != null)
            sig.problems.add("Call signature has unhandled type parameters");
         
         if (ctx.typeAnnotation() != null)
         {
            // Return type
         }
         
         if (ctx.parameterList() != null)
         {
            ctx.parameterList().accept(this);
         }
         
         return null;
      }
      
      @Override
      public Void visitParameterList(ParameterListContext ctx)
      {
         if (ctx.restParameter() != null)
            sig.problems.add("Unhandled rest parameter");
         if (ctx.optionalParameterList() != null)
            sig.problems.add("Unhandled optional parameter");
         if (ctx.requiredParameterList() != null)
            ctx.requiredParameterList().accept(this);
         
         return null;
      }
      
      @Override
      public Void visitRequiredParameter(RequiredParameterContext ctx)
      {
         String name = null;
         if (ctx.bindingIdentifier() != null)
            name = ctx.bindingIdentifier().getText();
         if (ctx.bindingIdentifierOrPattern() != null)
         {
            if (ctx.bindingIdentifierOrPattern().bindingPattern() != null)
               sig.problems.add("binding pattern used for call signature parameter");
            else
               name = ctx.bindingIdentifierOrPattern().bindingIdentifier().getText();
         }
         if (name == null)
         {
            sig.problems.add("No call signature parameter name");
            return null;
         }
         if (ctx.accessibilityModifier() != null)
            sig.problems.add("Unhandled accessibility modifier on parameter " + name);
         if (ctx.StringLiteral() != null)
            sig.problems.add("Unhandled binding to string literal" + name);
         if (ctx.typeAnnotation() != null)
         {
            
         }
         CallParameter param = new CallParameter();
         param.name = name;
         sig.params.add(param);
         
         return null;
      }
      
   }
   
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
            intf.problems.add("Unhandled call signature");
         if (ctx.constructSignature() != null)
            intf.problems.add("Unhandled construct signature");
         if (ctx.indexSignature() != null)
            intf.problems.add("Unhandled index signature");
         if (ctx.methodSignature() != null)
            ctx.methodSignature().accept(this);
         if (ctx.propertySignature() != null)
            ctx.propertySignature().accept(this);
         return null;
      }
      
      @Override
      public Void visitMethodSignature(MethodSignatureContext ctx)
      {
         if (ctx.propertyName().NumericLiteral() != null || ctx.propertyName().StringLiteral() != null)
         {
            intf.problems.add("Method with string literal or numeric literal as a name");
            return null;
         }
         String name = ctx.propertyName().identifierName().getText();
         
         if (ctx.optional() != null)
            intf.problems.add("Unhandled method with optional " + name);
         
         CallSignatureReader call = new CallSignatureReader();
         ctx.callSignature().accept(call);
         
         PropertyDefinition prop = new PropertyDefinition();
         prop.name = name;
         prop.optional = (ctx.optional() != null);
         prop.callSigType = call.sig;
         
         intf.methods.add(prop);
         
         return null;
      }
      
      @Override
      public Void visitPropertySignature(PropertySignatureContext ctx)
      {
         if (ctx.propertyName().NumericLiteral() != null || ctx.propertyName().StringLiteral() != null)
         {
            intf.problems.add("Property with string literal or numeric literal as a name");
            return null;
         }
         String name = ctx.propertyName().identifierName().getText(); 
         
         PropertyDefinition prop = new PropertyDefinition();
         prop.name = name;
         
         if (ctx.propertySignatureReadOnly() != null)
            prop.readOnly = true;
         if (ctx.optional() != null)
            prop.optional = true;
         
         if (ctx.typeAnnotation() != null && ctx.typeAnnotation().type() != null 
               && ctx.typeAnnotation().type().unionOrIntersectionOrPrimaryType() != null
               && ctx.typeAnnotation().type().unionOrIntersectionOrPrimaryType().unionOrIntersectionOrPrimaryType() == null
               && ctx.typeAnnotation().type().unionOrIntersectionOrPrimaryType().intersectionOrPrimaryType().intersectionOrPrimaryType() == null)
         {
            // Primary type, no intersection or union
            PrimaryTypeContext primary = ctx.typeAnnotation().type().unionOrIntersectionOrPrimaryType().intersectionOrPrimaryType().primaryType();
            if (primary.predefinedType() != null)
            {
               BasicJsType basicType = new BasicJsType();
               basicType.type = primary.predefinedType().getText();
               prop.basicType = basicType;
            }
         }
         
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
            api.problems.add("Unhandled ambient declaration " + ctx.ambientDeclaration().getText());
         else if (ctx.typeAliasDeclaration() != null)
            api.problems.add("Unhandled type alias declaration " + ctx.typeAliasDeclaration().getText());
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
         {
            InterfaceExtendsReader extendsReader = new InterfaceExtendsReader();
            ctx.interfaceExtendsClause().accept(extendsReader);
            intf.extendsTypes = extendsReader.types;
         }
         if (ctx.typeParameters() != null)
            intf.problems.add("Unhandled generics " + ctx.typeParameters().getText());
         
         if (ctx.objectType().typeBody() != null)
            ctx.objectType().typeBody().accept(new TypeBodyReader(intf));
         return null;
      }
   }
   
   public static TsIdlParser.DeclarationSourceFileContext parseTs(String fileName) throws IOException
   {
      CharStream file = CharStreams.fromFileName(fileName);
      return parseTs(file);
   }

   public static TsIdlParser.DeclarationSourceFileContext parseTs(CharStream file)
   {
      TsIdlLexer lexer = new TsIdlLexer(file);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      TsIdlParser parser = new TsIdlParser(tokens);
      TsIdlParser.DeclarationSourceFileContext tree = parser.declarationSourceFileEOF().declarationSourceFile();
      
      return tree;
   }
}
