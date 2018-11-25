package com.user00.domjnate.generator.tsparser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;

import com.user00.domjnate.generator.ast.ApiDefinition;
import com.user00.domjnate.generator.ast.PredefinedType;
import com.user00.domjnate.generator.ast.CallSignatureDefinition;
import com.user00.domjnate.generator.ast.CallSignatureDefinition.CallParameter;
import com.user00.domjnate.generator.ast.ErrorType;
import com.user00.domjnate.generator.ast.GenericParameter;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.NullableType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.TypeReference;
import com.user00.domjnate.generator.tsparser.TsIdlParser.CallSignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.ClassOrInterfaceTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.DeclarationElementContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.InterfaceDeclarationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.InterfaceExtendsClauseContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.IntersectionOrPrimaryTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.MethodSignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.NamespaceNameContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.OptionalParameterContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.ParameterListContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.PredefinedTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.PrimaryTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.PropertySignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.RequiredParameterContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeMemberContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeParameterContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeParameterListContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeParametersContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeReferenceContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.UnionOrIntersectionOrPrimaryTypeContext;

public class TsDeclarationsReader
{
   static TsIdlBaseVisitor<Type> TYPE_READER = new TsIdlBaseVisitor<Type>() {
      @Override
      public Type visitPredefinedType(PredefinedTypeContext ctx) {
         PredefinedType basicType = new PredefinedType();
         basicType.type = ctx.getText();
         return basicType;
      }

      @Override
      public Type visitUnionOrIntersectionOrPrimaryType(UnionOrIntersectionOrPrimaryTypeContext ctx) {
         if (ctx.unionOrIntersectionOrPrimaryType() != null)
         {
            // Gather all the types being unioned together
            List<Type> subtypes = new ArrayList<>();
            UnionOrIntersectionOrPrimaryTypeContext union = ctx;
            subtypes.add(parseType(union.intersectionOrPrimaryType()));
            while (union.unionOrIntersectionOrPrimaryType() != null)
            {
               union = union.unionOrIntersectionOrPrimaryType();
               subtypes.add(parseType(union.intersectionOrPrimaryType()));
            }
            for (int n = 0; n < subtypes.size(); n++)
            {
               Type t = subtypes.get(n);
               if (t instanceof TypeReference && ((TypeReference)t).typeName.equals("null"))
               {
                  subtypes.remove(n);
                  NullableType nullable = new NullableType();
                  if (subtypes.size() == 1)
                  {
                     nullable.subtype = subtypes.get(0);
                     return nullable;
                  }
                  return new ErrorType("Unhandled union type");
               }
            }
            return new ErrorType("Unhandled union type");
         }
         return ctx.intersectionOrPrimaryType().accept(this);
         
      }
      
      @Override
      public Type visitIntersectionOrPrimaryType(IntersectionOrPrimaryTypeContext ctx) {
         if (ctx.intersectionOrPrimaryType() != null)
         {
            // Gather all the types being intersectioned together
            List<Type> subtypes = new ArrayList<>();
            IntersectionOrPrimaryTypeContext intersection = ctx;
            subtypes.add(parseType(intersection.primaryType()));
            while (intersection.intersectionOrPrimaryType() != null)
            {
               intersection = intersection.intersectionOrPrimaryType();
               subtypes.add(parseType(intersection.primaryType()));
            }
            return new ErrorType("Unhandled intersection type");
         }
         return ctx.primaryType().accept(this);
      }
      
      @Override public TypeReference visitTypeReference(TypeReferenceContext ctx) {
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
      };
   };
   
   static Type parseType(RuleContext ctx)
   {
      Type type = ctx.accept(TYPE_READER);
      if (type != null)
         return type;
      return new ErrorType("Could not parse type " + ctx.getText());
   }

   static TsIdlBaseVisitor<List<GenericParameter>> TYPE_PARAMETERS_READER = new TsIdlBaseVisitor<List<GenericParameter>>() {
      public List<GenericParameter> visitTypeParameters(TypeParametersContext ctx) {
         return ctx.typeParameterList().accept(this);
      }
      @Override
      public List<GenericParameter> visitTypeParameterList(TypeParameterListContext ctx) 
      {
         List<GenericParameter> genericParams = new ArrayList<>();
         if (ctx.typeParameterList() != null)
         {
            genericParams.addAll(ctx.typeParameterList().accept(this));
         }
         genericParams.addAll(ctx.typeParameter().accept(this));
         return genericParams;
      }
      @Override
      public List<GenericParameter> visitTypeParameter(TypeParameterContext ctx) {
         GenericParameter genericParam = new GenericParameter();
         genericParam.name = ctx.bindingIdentifier().getText();
         if (ctx.constraint() != null)
         {
            if (ctx.constraint().KeyOf() != null)
            {
               if (ctx.constraint().type().unionOrIntersectionOrPrimaryType() == null)
                  genericParam.problems.add("Unhandled type parameter type");
               else if (ctx.constraint().type().unionOrIntersectionOrPrimaryType().unionOrIntersectionOrPrimaryType() != null)
                  genericParam.problems.add("Unhandled type parameter type");
               else if (ctx.constraint().type().unionOrIntersectionOrPrimaryType().intersectionOrPrimaryType().intersectionOrPrimaryType() != null)
                  genericParam.problems.add("Unhandled type parameter type");
               else if (ctx.constraint().type().unionOrIntersectionOrPrimaryType().intersectionOrPrimaryType().primaryType().typeReference() == null)
                  genericParam.problems.add("Unhandled type parameter type");
               else
               {
                  TypeReference ref = (TypeReference)ctx.constraint().type().unionOrIntersectionOrPrimaryType().intersectionOrPrimaryType().primaryType().typeReference().accept(TYPE_READER);
                  genericParam.simpleExtendsKeyOf = ref.typeName;
                  genericParam.problems.addAll(ref.problems);
               }
            }
            else
            {
               if (ctx.constraint().type().unionOrIntersectionOrPrimaryType() == null)
                  genericParam.problems.add("Unhandled type parameter type");
               else if (ctx.constraint().type().unionOrIntersectionOrPrimaryType().unionOrIntersectionOrPrimaryType() != null)
                  genericParam.problems.add("Unhandled type parameter type");
               else if (ctx.constraint().type().unionOrIntersectionOrPrimaryType().intersectionOrPrimaryType().intersectionOrPrimaryType() != null)
                  genericParam.problems.add("Unhandled type parameter type");
               else if (ctx.constraint().type().unionOrIntersectionOrPrimaryType().intersectionOrPrimaryType().primaryType().typeReference() == null)
                  genericParam.problems.add("Unhandled type parameter type");
               else
               {
                  TypeReference ref = (TypeReference)ctx.constraint().type().unionOrIntersectionOrPrimaryType().intersectionOrPrimaryType().primaryType().typeReference().accept(TYPE_READER);
                  genericParam.simpleExtends = ref.typeName;
                  genericParam.problems.addAll(ref.problems);
               }
            }
         }
         if (ctx.typeParameterDefault() != null)
            genericParam.problems.add("Unhandled type parameter default");
         return Collections.singletonList(genericParam);
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
         types.add((TypeReference)ctx.typeReference().accept(TYPE_READER));
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
         {
            sig.genericTypeParameters = ctx.typeParameters().accept(TYPE_PARAMETERS_READER);
         }
         
         // Return type
         if (ctx.typeAnnotation() != null)
         {
            sig.returnType = parseType(ctx.typeAnnotation().type());
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
            ctx.optionalParameterList().accept(this);
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
         Type paramType = null;
         if (ctx.typeAnnotation() != null)
         {
            paramType = parseType(ctx.typeAnnotation().type());
         }
         CallParameter param = new CallParameter();
         param.name = name;
         param.type = paramType;
         sig.params.add(param);
         
         return null;
      }
      
      @Override
      public Void visitOptionalParameter(OptionalParameterContext ctx)
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
         if (ctx.initializer() != null)
            sig.problems.add("Unhandled initializer on optional parameter " + name);
         if (ctx.accessibilityModifier() != null)
            sig.problems.add("Unhandled accessibility modifier on parameter " + name);
         if (ctx.StringLiteral() != null)
            sig.problems.add("Unhandled binding to string literal" + name);
         Type paramType = null;
         if (ctx.typeAnnotation() != null)
         {
            paramType = parseType(ctx.typeAnnotation().type());
         }
         CallParameter param = new CallParameter();
         param.name = name;
         param.type = paramType;
         sig.optionalParams.add(param);
         
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
            ctx.callSignature().accept(this);
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
         if (ctx.propertyName().NumericLiteral() != null)
         {
            intf.problems.add("Property with string literal or numeric literal as a name");
            return null;
         }
         String name = null;
         if (ctx.propertyName().identifierName() != null)
            name = ctx.propertyName().identifierName().getText();
         else if (ctx.propertyName().StringLiteral() != null)
         {
            name = ctx.propertyName().StringLiteral().getText();
            if (name.startsWith("\"") && name.endsWith("\""))
               name = name.substring(1, name.length() - 1);
         }
         
         PropertyDefinition prop = new PropertyDefinition();
         prop.name = name;
         
         if (ctx.propertySignatureReadOnly() != null)
            prop.readOnly = true;
         if (ctx.optional() != null)
            prop.optional = true;
         
         if (ctx.typeAnnotation() != null && ctx.typeAnnotation().type() != null) 
         {
            prop.type = parseType(ctx.typeAnnotation().type());
         }
         
         intf.properties.add(prop);
         return null;
      }
      
      @Override
      public Void visitCallSignature(CallSignatureContext ctx)
      {
         CallSignatureReader call = new CallSignatureReader();
         ctx.accept(call);
         intf.callSignatures.add(call.sig);
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
