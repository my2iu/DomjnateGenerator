package com.user00.domjnate.generator.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds all the JsInterop definitions.
 */
public class ApiDefinition
{
   public Map<String, InterfaceDefinition> interfaces = new HashMap<>();
   
   /** Problems encountered when generating the API */
   public List<String> problems = new ArrayList<>();
   
   public void addProblem(String err)
   {
      problems.add(err);
   }
}
