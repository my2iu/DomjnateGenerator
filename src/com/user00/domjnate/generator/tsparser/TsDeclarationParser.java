package com.user00.domjnate.generator.tsparser;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class TsDeclarationParser
{

   public static void lexFile(CharStream file)
   {
      TsIdlLexer lexer = new TsIdlLexer(file);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      tokens.fill();
      for (Token t: tokens.getTokens())
      {
         System.out.println(t.getText() + ":" + t.getType());
      }
      
   }
   
   public static void parseFile(CharStream file)
   {
      try {
         TsIdlLexer lexer = new TsIdlLexer(file);
         CommonTokenStream tokens = new CommonTokenStream(lexer);
         TsIdlParser parser = new TsIdlParser(tokens);
//         parser.setErrorHandler(new BailErrorStrategy());
         parser.addErrorListener(new ConsoleErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                  Object offendingSymbol, int line, int charPositionInLine,
                  String msg, RecognitionException e)
            {
               super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg,
                     e);
            }
         });
         TsIdlParser.DeclarationSourceFileContext tree = parser.declarationSourceFile();
         System.out.println("End at line " + lexer.getLine());
         
         
      } 
      catch (ParseCancellationException e)
      {
         e.printStackTrace();
      }

   }
   
   public static void main(String[] args) throws IOException
   {
      // Just do a test parse of the DOM description
//      CharStream file = CharStreams.fromFileName("idl/mini.d.ts");
      CharStream file = CharStreams.fromFileName("idl/lib.dom.d.ts");
//      lexFile(file);
      parseFile(file);
   }

}
