package com.user00.domjnate.generator.ast;

import java.util.ArrayList;
import java.util.List;

public class CallSignatureDefinition
{
   public BasicJsType returnType;
   public static class CallParameter
   {
      public String name;
      public BasicJsType type;
      public ProblemTracker problems = new ProblemTracker();
   }
   public List<CallParameter> params = new ArrayList<>(); 
   public List<CallParameter> optionalParams = new ArrayList<>(); 
   
   public ProblemTracker problems = new ProblemTracker();
}
