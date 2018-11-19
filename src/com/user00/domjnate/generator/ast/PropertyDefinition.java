package com.user00.domjnate.generator.ast;

import java.util.ArrayList;
import java.util.List;

public class PropertyDefinition
{
   public String name;
   public boolean readOnly = false;
   public boolean optional = false;
   
   
   /** Problems encountered when generating the API */
   public List<String> problems = new ArrayList<>();
   
   public void addProblem(String err)
   {
      problems.add(err);
   }

}
