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
import com.user00.domjnate.generator.ast.CallSignatureDefinition;
import com.user00.domjnate.generator.ast.CallSignatureDefinition.CallParameter;
import com.user00.domjnate.generator.ast.ErrorType;
import com.user00.domjnate.generator.ast.GenericParameter;
import com.user00.domjnate.generator.ast.IndexSignatureDefinition;
import com.user00.domjnate.generator.ast.InterfaceDefinition;
import com.user00.domjnate.generator.ast.NullableType;
import com.user00.domjnate.generator.ast.ObjectType;
import com.user00.domjnate.generator.ast.PredefinedType;
import com.user00.domjnate.generator.ast.PropertyDefinition;
import com.user00.domjnate.generator.ast.Type;
import com.user00.domjnate.generator.ast.TypeQueryType;
import com.user00.domjnate.generator.ast.TypeReference;
import com.user00.domjnate.generator.ast.UnionType;
import com.user00.domjnate.generator.tsparser.TsIdlParser.AmbientBindingContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.AmbientBindingListContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.AmbientConstDeclarationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.AmbientDeclarationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.AmbientLetDeclarationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.AmbientVarDeclarationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.CallSignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.ClassOrInterfaceTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.ConstructSignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.DeclarationElementContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.IndexSignatureMappedContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.IndexSignatureNumberContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.IndexSignatureStringContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.InterfaceDeclarationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.InterfaceExtendsClauseContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.IntersectionOrPrimaryTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.MethodSignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.NamespaceNameContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.ObjectTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.OptionalParameterContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.ParameterListContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.PredefinedTypeContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.PropertySignatureContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.RequiredParameterContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.RestParameterContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeAliasDeclarationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeAnnotationContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeArgumentContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeArgumentListContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeArgumentsContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeMemberContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeParameterContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeParameterListContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeParametersContext;
import com.user00.domjnate.generator.tsparser.TsIdlParser.TypeQueryContext;
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
            int orNullPos = -1;
            for (int n = 0; n < subtypes.size(); n++)
            {
               Type t = subtypes.get(n);
               if (t instanceof TypeReference && ((TypeReference)t).typeName.equals("null"))
               {
                  orNullPos = n;
               }
            }
            if (orNullPos >= 0)
               subtypes.remove(orNullPos);
            Type toReturn;
            if (subtypes.size() == 1)
            {
               toReturn = subtypes.get(0);
            }
            else
            {
               UnionType unionToReturn = new UnionType();
               unionToReturn.subtypes = subtypes;
               toReturn = unionToReturn;
            }
            if (orNullPos >= 0)
            {
               NullableType nullable = new NullableType();
               nullable.subtype = toReturn;
               toReturn = nullable;
            }
            return toReturn;
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
      
      @Override public TypeQueryType visitTypeQuery(TypeQueryContext ctx) {
         if (ctx.typeQueryExpression().identifierReference() == null)
            return null;
         String typeName = ctx.typeQueryExpression().identifierReference().getText();
         TypeQueryType typeof = new TypeQueryType();
         typeof.simpleType = typeName;
         return typeof;
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
         {
            List<Type> typeArgs = ctx.typeArguments().accept(TYPE_ARGUMENTS_READER);
            ref.typeArgs = typeArgs;
         }
         return ref;
      };
      
      @Override public Type visitObjectType(ObjectTypeContext ctx) {
         InterfaceDefinition intf = new InterfaceDefinition();
         
         if (ctx.typeBody() != null)
            ctx.typeBody().accept(new TypeBodyReader(intf));
         
         ObjectType objType = new ObjectType();
         objType.intf = intf;
         return objType;
      };
   };
   
   static Type parseType(RuleContext ctx)
   {
      Type type = ctx.accept(TYPE_READER);
      if (type != null)
         return type;
      return new ErrorType("Could not parse type " + ctx.getText());
   }

   static TsIdlBaseVisitor<List<Type>> TYPE_ARGUMENTS_READER = new TsIdlBaseVisitor<List<Type>>() {
      @Override
      public List<Type> visitTypeArguments(TypeArgumentsContext ctx) {
         return ctx.typeArgumentList().accept(this);
      }
      @Override
      public List<Type> visitTypeArgumentList(TypeArgumentListContext ctx) {
         List<Type> genericParams = new ArrayList<>();
         if (ctx.typeArgumentList() != null)
         {
            genericParams.addAll(ctx.typeArgumentList().accept(this));
         }
         genericParams.addAll(ctx.typeArgument().accept(this));
         return genericParams;
      }
      @Override
      public List<Type> visitTypeArgument(TypeArgumentContext ctx) {
         return Collections.singletonList(parseType(ctx.type()));
      }
      
   };

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
            readReturnTypeAnnotation(ctx.typeAnnotation());
         }
         
         if (ctx.parameterList() != null)
         {
            ctx.parameterList().accept(this);
         }
         
         return null;
      }

      @Override
      public Void visitConstructSignature(ConstructSignatureContext ctx)
      {
         if (ctx.typeParameters() != null)
         {
            sig.genericTypeParameters = ctx.typeParameters().accept(TYPE_PARAMETERS_READER);
         }
         
         // Return type
         if (ctx.typeAnnotation() != null)
         {
            readReturnTypeAnnotation(ctx.typeAnnotation());
         }
         
         if (ctx.parameterList() != null)
         {
            ctx.parameterList().accept(this);
         }
         
         return null;
      }

      void readReturnTypeAnnotation(TypeAnnotationContext ctx)
      {
         if (ctx.type() != null)
            sig.returnType = parseType(ctx.type());
         else if (ctx.booleanTypeGuard() != null)
         {
            // Ignore the type guard properties and just treat the type like a boolean
            PredefinedType type = new PredefinedType();
            type.type = "boolean";
            sig.returnType = type;
         }
      }

      @Override
      public Void visitParameterList(ParameterListContext ctx)
      {
         if (ctx.restParameter() != null)
            ctx.restParameter().accept(this);
         if (ctx.optionalParameterList() != null)
            ctx.optionalParameterList().accept(this);
         if (ctx.requiredParameterList() != null)
            ctx.requiredParameterList().accept(this);
         
         return null;
      }
      
      @Override
      public Void visitRestParameter(RestParameterContext ctx)
      {
         String name = ctx.bindingIdentifier().getText();
         Type paramType = null;
         if (ctx.typeAnnotation() != null)
         {
            paramType = parseType(ctx.typeAnnotation().type());
         }
         CallParameter param = new CallParameter();
         param.name = name;
         param.type = paramType;
         sig.restParameter = param;
         
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

   static class AmbientVariableReader extends TsIdlBaseVisitor<Void>
   {
      boolean isConst;
      ApiDefinition api;
//      List<TypeReference> types = new ArrayList<>();
      
      @Override
      public Void visitAmbientBindingList(AmbientBindingListContext ctx)
      {
         return super.visitAmbientBindingList(ctx);
      }
      
      @Override
      public Void visitAmbientBinding(AmbientBindingContext ctx)
      {
         String name = ctx.bindingIdentifier().getText();
         Type type = null;
         if (ctx.typeAnnotation() != null)
            type = parseType(ctx.typeAnnotation().type());
         if (isConst)
         {
            if (api.ambientConsts.containsKey(name))
            {
               Type oldType = api.ambientConsts.get(name);
               // Ignore multiple definitions
               if (type instanceof TypeQueryType && ((TypeQueryType)type).simpleType.equals(name)) {}
               else if (type instanceof TypeReference && oldType instanceof TypeReference && type.equals(oldType)) {}
               else
               {
                  api.ambientConsts.put(name, new ErrorType("Multiple declare const definitions for " + name));
               }
            }
            else
               api.ambientConsts.put(name, type);
         }
         else
         {
            if (api.ambientVars.containsKey(name))
            {
               Type oldType = api.ambientVars.get(name);
               // Ignore multiple definitions
               if (type instanceof TypeQueryType && ((TypeQueryType)type).simpleType.equals(name)) {}
               else if (type instanceof TypeReference && oldType instanceof TypeReference && type.equals(oldType)) {}
               else
               {
                  api.ambientConsts.put(name, new ErrorType("Multiple declare var definitions for " + name));
               }
            }
            else
               api.ambientVars.put(name, type);
         }
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
            ctx.constructSignature().accept(this);
         if (ctx.indexSignature() != null)
            ctx.indexSignature().accept(this);
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
      public Void visitConstructSignature(ConstructSignatureContext ctx)
      {
         CallSignatureReader call = new CallSignatureReader();
         ctx.accept(call);
         
         intf.constructSignatures.add(call.sig);
         
         return null;
      }

      @Override
      public Void visitIndexSignatureMapped(IndexSignatureMappedContext ctx)
      {
         intf.problems.add("Unhandled index signature mapped");
         return null;
      }

      @Override
      public Void visitIndexSignatureString(IndexSignatureStringContext ctx)
      {
         intf.problems.add("Unhandled index signature string");
         return null;
      }

      @Override
      public Void visitIndexSignatureNumber(IndexSignatureNumberContext ctx)
      {
         IndexSignatureDefinition idxSig = new IndexSignatureDefinition();
         idxSig.indexName = ctx.bindingIdentifier().getText();
         idxSig.readOnly = ctx.propertySignatureReadOnly() != null;
         PredefinedType numberType = new PredefinedType();
         numberType.type = "number";
         idxSig.indexType = numberType;
         if (ctx.typeAnnotation() != null)
            idxSig.returnType = parseType(ctx.typeAnnotation().type());
         intf.indexSignatures.add(idxSig);
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
   
   public static class TopLevelReader extends TsIdlBaseVisitor<Void>
   {
      public TopLevelReader(ApiDefinition api)
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
            ctx.ambientDeclaration().accept(this);
         else if (ctx.typeAliasDeclaration() != null)
            ctx.typeAliasDeclaration().accept(this);
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
         {
            List<GenericParameter> typeParams = ctx.typeParameters().accept(TYPE_PARAMETERS_READER);
            intf.genericTypeParams = typeParams;
         }
         
         if (ctx.objectType().typeBody() != null)
            ctx.objectType().typeBody().accept(new TypeBodyReader(intf));
         return null;
      }
      
      @Override
      public Void visitAmbientDeclaration(AmbientDeclarationContext ctx)
      {
         if (ctx.ambientVariableDeclaration() != null)
         {
            // Check that ambient declaration refers to an interface meaning it's a class, 
            // otherwise, we've got a global variable
            ctx.ambientVariableDeclaration().accept(this);
         }
         else
            api.problems.add("Unhandled ambient declaration " + ctx.getText());

         return null;
      }
      
      @Override
      public Void visitAmbientVarDeclaration(AmbientVarDeclarationContext ctx)
      {
         AmbientVariableReader reader = new AmbientVariableReader();
         reader.isConst = false;
         reader.api = api;
         ctx.ambientBindingList().accept(reader);
         return null;
      }

      @Override
      public Void visitAmbientLetDeclaration(AmbientLetDeclarationContext ctx)
      {
         AmbientVariableReader reader = new AmbientVariableReader();
         reader.isConst = false;
         reader.api = api;
         ctx.ambientBindingList().accept(reader);
         return null;
      }

      @Override
      public Void visitAmbientConstDeclaration(AmbientConstDeclarationContext ctx)
      {
         AmbientVariableReader reader = new AmbientVariableReader();
         reader.isConst = true;
         reader.api = api;
         ctx.ambientBindingList().accept(reader);
         return null;
      }
      
      @Override
      public Void visitTypeAliasDeclaration(TypeAliasDeclarationContext ctx)
      {
         String name = ctx.bindingIdentifier().getText();
         if (ctx.typeParameters() != null)
         {
            api.problems.add("Unhandled type alias declaration with generic type parameters " + name);
         }
         Type type = parseType(ctx.type());
         api.typeAliases.put(name, type);
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
