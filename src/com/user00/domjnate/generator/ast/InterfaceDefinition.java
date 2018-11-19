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
   public List<PropertyDefinition> methods = new ArrayList<>();
   
   public ProblemTracker problems = new ProblemTracker();
}
