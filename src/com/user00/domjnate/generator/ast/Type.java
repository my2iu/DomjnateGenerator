package com.user00.domjnate.generator.ast;

public abstract class Type
{
   public ProblemTracker problems = new ProblemTracker();
   public abstract <U> U visit(TypeVisitor<U> visitor);
   
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
}
