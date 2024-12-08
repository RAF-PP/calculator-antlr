package rs.raf;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import rs.raf.calculator.Calculator;
import rs.raf.calculator.Parser;
import rs.raf.calculator.Scanner;
import rs.raf.calculator.Typecheck;
import rs.raf.calculator.ast.ASTPrettyPrinter;
import rs.raf.calculator.ast.CSTtoASTConverter;
import rs.raf.calculator.ast.StatementList;
import rs.raf.utils.PrettyPrint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    private static final Calculator calculator = new Calculator();
    /* Holds the global scope, so keep it open all the time.  */
    private static final CSTtoASTConverter treeProcessor
        = new CSTtoASTConverter(calculator);

    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        run(CharStreams.fromFileName(path));
        if (calculator.hadError()) System.exit(65);
        if (calculator.hadRuntimeError()) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            System.out.print("> ");
            String line = reader.readLine();

            if (line == null || line.equalsIgnoreCase("exit")) {
                /* Terminate the possibly EOF line.  */
                System.out.println();
                break;
            }

            calculator.setHadError(false);
            calculator.setHadRuntimeError(false);
            run(CharStreams.fromString(line));
        }
    }

    private static void run(CharStream source) {
        Scanner scanner = new Scanner(calculator);
        var tokens = scanner.getTokens(source);

        if (calculator.hadError()) return;

        Parser parser = new Parser(calculator);
        var tree = parser.getSyntaxTree(tokens);

        /* ANTLR error recovers, so lets print it in its error recovered
           form.  */
        System.out.println("Syntax Tree: " + PrettyPrint.prettyPrintTree(tree, parser.getCalculatorParser().getRuleNames()));

        if (calculator.hadError()) return;

        System.out.println("AST:");
        var pp = new ASTPrettyPrinter(System.out);
        var program = (StatementList) tree.accept(treeProcessor);
        program.prettyPrint(pp);
        if (calculator.hadError()) return;

//        new Typecheck(calculator).typecheck(program);
//        System.out.println("tAST:");
//        program.prettyPrint(pp);
//        if (calculator.hadError()) return;
    }
}
