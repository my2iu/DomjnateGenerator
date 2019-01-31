package com.user00.domjnate.generator.ast;

import com.user00.domjnate.generator.ast.CallSignatureDefinition.CallParameter;

public abstract class Type
{
   public ProblemTracker problems = new ProblemTracker();
   public abstract <U> U visit(TypeVisitor<U> visitor);
   public abstract <I, U> U visit(TypeVisitorWithInput<I, U> visitor, I in);
   
   public static class TypeVisitor<U>
   {
      public U visitPredefinedType(PredefinedType type)
      {
         return visitType(type);
      }
      public U visitNullableType(NullableType type)
      {
         return visitType(type);
      }
      public U visitTypeReferenceType(TypeReference type)
      {
         return visitType(type);
      }
      public U visitUnionType(UnionType type)
      {
         return visitType(type);
      }
      public U visitTypeQueryType(TypeQueryType type)
      {
         return visitType(type);
      }
      public U visitObjectType(ObjectType type)
      {
         return visitType(type);
      }
      public U visitFunctionType(FunctionType type)
      {
         return visitType(type);
      }
      public U visitLocalFunctionType(LocalFunctionType type)
      {
         return visitType(type);
      }
      public U visitArrayType(ArrayType type)
      {
         return visitType(type);
      }
      public U visitStringLiteralType(StringLiteralType type)
      {
         return visitType(type);
      }
      public U visitErrorType(ErrorType type)
      {
         return visitType(type);
      }
      public U visitType(Type type)
      {
         throw new IllegalArgumentException("Unhandled type");
      }
   }
   
   public static class TypeVisitorWithInput<I, U>
   {
      public U visitPredefinedType(PredefinedType type, I in)
      {
         return visitType(type, in);
      }
      public U visitNullableType(NullableType type, I in)
      {
         return visitType(type, in);
      }
      public U visitTypeReferenceType(TypeReference type, I in)
      {
         return visitType(type, in);
      }
      public U visitUnionType(UnionType type, I in)
      {
         return visitType(type, in);
      }
      public U visitTypeQueryType(TypeQueryType type, I in)
      {
         return visitType(type, in);
      }
      public U visitObjectType(ObjectType type, I in)
      {
         return visitType(type, in);
      }
      public U visitFunctionType(FunctionType type, I in)
      {
         return visitType(type, in);
      }
      public U visitLocalFunctionType(LocalFunctionType type, I in)
      {
         return visitType(type, in);
      }
      public U visitArrayType(ArrayType type, I in)
      {
         return visitType(type, in);
      }
      public U visitStringLiteralType(StringLiteralType type, I in)
      {
         return visitType(type, in);
      }
      public U visitErrorType(ErrorType type, I in)
      {
         return visitType(type, in);
      }
      public U visitType(Type type, I in)
      {
         throw new IllegalArgumentException("Unhandled type");
      }
   }
   
   public static class RecursiveTypeVisitorWithInput<I> extends TypeVisitorWithInput<I, Void>
   {
      public Void visitPredefinedType(PredefinedType type, I in)
      {
         return null;
      }
      public Void visitNullableType(NullableType type, I in)
      {
         type.subtype.visit(this, in);
         return null;
      }
      public Void visitTypeReferenceType(TypeReference type, I in)
      {
         return null;
      }
      public Void visitUnionType(UnionType type, I in)
      {
         for (Type subtype: type.subtypes)
            subtype.visit(this, in);
         return null;
      }
      public Void visitTypeQueryType(TypeQueryType type, I in)
      {
         return null;
      }
      public Void visitObjectType(ObjectType type, I in)
      {
         throw new IllegalArgumentException("Recursive traversal of object types not currently supported");
      }
      public Void visitFunctionType(FunctionType type, I in)
      {
         CallSignatureDefinition callSigType = type.callSigType;
         
         if (callSigType.returnType != null)
            callSigType.returnType.visit(this, in);
         if (callSigType.restParameter != null)
            callSigType.restParameter.type.visit(this, in);
         if (callSigType.optionalParams != null)
         {
            for (CallParameter param: callSigType.optionalParams)
            {
               if (param.type != null)
                  param.type.visit(this, in);
            }
         }
         if (callSigType.params != null)
         {
            for (CallParameter param: callSigType.params)
            {
               if (param.type != null)
                  param.type.visit(this, in);
            }
         }
         return null;
      }
      public Void visitLocalFunctionType(LocalFunctionType type, I in)
      {
         return null;
      }
      public Void visitArrayType(ArrayType type, I in)
      {
         type.type.visit(this, in);
         return null;
      }
      public Void visitStringLiteralType(StringLiteralType type, I in)
      {
         return null;
      }
      public Void visitErrorType(ErrorType type, I in)
      {
         return null;
      }
      public Void visitType(Type type, I in)
      {
         throw new IllegalArgumentException("Unhandled type");
      }
   }

}
