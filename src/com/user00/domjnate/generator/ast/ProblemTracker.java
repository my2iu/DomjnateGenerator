package com.user00.domjnate.generator.ast;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ProblemTracker
{
   /** Problems encountered when generating the API */
   public List<String> problems = new ArrayList<>();
   
   public void dump(PrintWriter out)
   {
      for (String err: problems)
         out.println(err);
   }

   public void dump(PrintStream out)
   {
      for (String err: problems)
         out.println(err);
   }

   public void add(String err)
   {
      problems.add(err);
   }
   
   public void addAll(ProblemTracker other)
   {
      problems.addAll(other.problems);
   }

}
