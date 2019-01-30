package com.user00.domjnate.generator.ast;

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
}
