package com.user00.domjnate.generator.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about a JsInterop interface
 */
public class InterfaceDefinition
{
   public String name;
   
   public List<PropertyDefinition> properties = new ArrayList<>();
   
   /** Problems encountered when generating the API */
   public List<String> problems = new ArrayList<>();
   
   public void addProblem(String err)
   {
      problems.add(err);
   }
}
