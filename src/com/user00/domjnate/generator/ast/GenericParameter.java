package com.user00.domjnate.generator.ast;

public class GenericParameter
{
   public String name;
   public TypeReference simpleExtends;
   public String simpleExtendsKeyOf;
   
   public ProblemTracker problems = new ProblemTracker();
}
