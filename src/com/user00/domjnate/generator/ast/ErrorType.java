package com.user00.domjnate.generator.ast;

public class ErrorType extends Type
{
   public ErrorType(String err)
   {
      problems.add(err);
   }
   
   @Override
   public <U> U visit(TypeVisitor<U> visitor)
   {
      return visitor.visitErrorType(this);
   }

   @Override
   public <I, U> U visit(TypeVisitorWithInput<I, U> visitor, I in)
   {
      return visitor.visitErrorType(this, in);
   }

}
