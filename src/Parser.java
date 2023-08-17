import java.awt.*;
import java.io.IOException;

public class Parser {

    public static void main(String[] args) {
        Parser parser = new Parser(args[0]);
        Token t = parser.lexer.PeekNextToken();
        parser.classDeclare();
        parser.subSt.Print();
        System.out.println("Successfully parsed input file!");
    }

    private Lexer lexer;

    private SymbolTable rootSt;         // Symbol table for class (the root)
    private SymbolTable subSt;          // Symbol table for current subroutine
    private SymbolTable currentSt;      // Symbol table for current scope

    private String identMessage = "Expected an identifier!";

    public Parser(String file_name){
        lexer = new Lexer();
        if(!lexer.Init(file_name)){
            System.out.println("Unable to initialize lexer");
        }
    }

    private void Error(Token t, String message){
        System.err.println("Error in line: " + t.LineNum + " at or near " + t.Lexeme
         + ", " + message);
        System.exit(1);
    }

    private void OK(Token t){
        System.out.println(t.Lexeme + ": OK");
    }

    //Parses tokens that are ONLY Identifiers
    private void parseIdentifier(Token t){
        if(t.Type == Token.TokenTypes.Identifier){
            OK(t);
            t = lexer.GetNextToken();
        } else{
            Error(t, identMessage);
        }
    }

    //Parses tokens that ARE NOT IDENTIFIERS
    private void parse(Token t, String symbol){
        if(t.Lexeme.equals(symbol)){
            OK(t);
            t = lexer.GetNextToken();
        } else{
            Error(t, "Expected '" + symbol + "'!");
        }
    }

    private void classDeclare() {
        Token t = lexer.PeekNextToken();
        while (t.Type != Token.TokenTypes.EOF) {
            parse(t, "class");
            t = lexer.PeekNextToken();
            parseIdentifier(t);

            // Setting up root symbol table
            rootSt = new SymbolTable();
            rootSt.AddSymbol(t.Lexeme, Symbol.SymbolKind.CLASS, t.Lexeme);
            currentSt = rootSt;

            t = lexer.PeekNextToken();
            parse(t, "{");
            t = lexer.PeekNextToken();
            while (t.Lexeme.equals("static") || t.Lexeme.equals("field") || t.Lexeme.equals("constructor")
                    || t.Lexeme.equals("function") || t.Lexeme.equals("method")) {
                memberDeclare();
                t = lexer.PeekNextToken();
            }
            parse(t, "}");
            t = lexer.PeekNextToken();
        }
    }

    private void memberDeclare(){
        Token t = lexer.PeekNextToken();
        while(t.Lexeme.equals("static") || t.Lexeme.equals("field")){
            classVarDeclare();
            t = lexer.PeekNextToken();
        }

        while(t.Lexeme.equals("constructor") || t.Lexeme.equals("function") || t.Lexeme.equals("method")){
            subroutineDeclare();
            t = lexer.PeekNextToken();
        }

    }

    private void classVarDeclare(){
        String type = "";
        Symbol.SymbolKind kind = Symbol.SymbolKind.ARG;     // This will change
        Token t = lexer.PeekNextToken();
        if(t.Lexeme.equals("static") || t.Lexeme.equals("field")){
            OK(t);
            t = lexer.GetNextToken();
            if(t.Lexeme.equals("static"))
                kind = Symbol.SymbolKind.STATIC;
            else
                kind = Symbol.SymbolKind.FIELD;
        }
        else{
            Error(t, "Expected \"static\" or \"field\" keyword");
        }

        t = lexer.PeekNextToken();
        if(t.Lexeme.equals("int") || t.Lexeme.equals("char") || t.Lexeme.equals("boolean")
        || t.Type == Token.TokenTypes.Identifier){
            type = type();
        } else{
            Error(t, "Expected data type");
        }

        t = lexer.PeekNextToken();
        if(t.Type == Token.TokenTypes.Identifier){
            OK(t);
            t = lexer.GetNextToken();
            currentSt.AddSymbol(t.Lexeme, kind, type);
        }
        else{
            Error(t, identMessage);
        }

        t = lexer.PeekNextToken();
        while(!t.Lexeme.equals(";")) {
            if (t.Lexeme.equals(",")) {
                OK(t);
                t = lexer.GetNextToken();
                t = lexer.PeekNextToken();
                parseIdentifier(t);
                currentSt.AddSymbol(t.Lexeme, kind, type);
                t = lexer.PeekNextToken();
            }
        }
        t = lexer.PeekNextToken();
        parse(t, ";");
    }

    // Returns String as this helps for adding a Symbol to Symbol Table
    private String type(){
        Token t = lexer.PeekNextToken();
        if(t.Lexeme.equals("int") || t.Lexeme.equals("char") || t.Lexeme.equals("boolean") || t.Type == Token.TokenTypes.Identifier){
            OK(t);
            t = lexer.GetNextToken();
        }
        else{
            Error(t, "Expected data type");
        }
        return t.Lexeme;
    }

    private void subroutineDeclare(){
        String type = "";

        Token t = lexer.PeekNextToken();
        if(t.Lexeme.equals("constructor") || t.Lexeme.equals("function") || t.Lexeme.equals("method")){
            OK(t);
            t = lexer.GetNextToken();
        } else{
            Error(t, "Expected subroutine declaration");
        }

        t = lexer.PeekNextToken();
        if(t.Lexeme.equals("int") || t.Lexeme.equals("char") || t.Lexeme.equals("boolean") || t.Type == Token.TokenTypes.Identifier){
            type = type();
        }
        else if(t.Lexeme.equals("void")){
            OK(t);
            t = lexer.GetNextToken();
        } else{
            Error(t, "Expected data type / void");
        }

        t = lexer.PeekNextToken();
        parseIdentifier(t);
        currentSt.AddSymbol(t.Lexeme, Symbol.SymbolKind.SUBROUTINE, type);
        subSt = new SymbolTable(currentSt, t.Lexeme);
        currentSt = subSt;
        t = lexer.PeekNextToken();
        parse(t, "(");

        t = lexer.PeekNextToken();
        if(t.Lexeme.equals("int") || t.Lexeme.equals("char") || t.Lexeme.equals("boolean") || t.Type == Token.TokenTypes.Identifier){
            paramList();
            t = lexer.PeekNextToken();
            parse(t, ")");
        }
        else if(t.Lexeme.equals(")")){
            OK(t);
            t = lexer.GetNextToken();
        } else{
            Error(t, "Expected closing brace ')' or data type");
        }
        t = lexer.PeekNextToken();
        subroutineBody();

    }

    private void paramList(){
        Token t = lexer.PeekNextToken();
        String type = "";
        if (t.Lexeme.equals("int") || t.Lexeme.equals("char") || t.Lexeme.equals("boolean") || t.Type == Token.TokenTypes.Identifier) {
            type = type();
        }
        t = lexer.PeekNextToken();
        parseIdentifier(t);
        currentSt.AddSymbol(t.Lexeme, Symbol.SymbolKind.ARG, type);
        t = lexer.PeekNextToken();
        while(!t.Lexeme.equals(")")) {
            if(t.Lexeme.equals(",")) {
                OK(t);
                t = lexer.GetNextToken();
                t = lexer.PeekNextToken();
                type = type();
                t = lexer.PeekNextToken();
                parseIdentifier(t);
                currentSt.AddSymbol(t.Lexeme, Symbol.SymbolKind.ARG, type);
                t = lexer.PeekNextToken();
            } else{
                Error(t, "Expected closing brace ')'");
            }
        }
    }

    private void subroutineBody(){
        Token t = lexer.PeekNextToken();
        parse(t, "{");
        t = lexer.PeekNextToken();
        while(!t.Lexeme.equals("}")){
            stmt();
            t = lexer.PeekNextToken();
        }
        t = lexer.PeekNextToken();
        parse(t, "}");
    }

    private void stmt(){
        Token t = lexer.PeekNextToken();
        if(t.Lexeme.equals("var")){
            varDeclareStmt();
        }
        else if(t.Lexeme.equals("let")){
            letStmt();
        }
        else if(t.Lexeme.equals("if")){
            ifStmt();
        }
        else if(t.Lexeme.equals("while")){
            whileStmt();
        }
        else if(t.Lexeme.equals("do")){
            doStmt();
        }
        else if(t.Lexeme.equals("return")){
            returnStmt();
        }
        else{
            Error(t, "Statement expected");
        }
    }


    private void varDeclareStmt(){
        Token t = lexer.PeekNextToken();
        parse(t, "var");
        t = lexer.PeekNextToken();
        String type = type();
        t = lexer.PeekNextToken();
        parseIdentifier(t);
        currentSt.AddSymbol(t.Lexeme, Symbol.SymbolKind.VAR, type);
        t = lexer.PeekNextToken();
        while(!t.Lexeme.equals(";")) {
            if (t.Lexeme.equals(",")) {
                OK(t);
                t = lexer.GetNextToken();
                t = lexer.PeekNextToken();
                parseIdentifier(t);
                currentSt.AddSymbol(t.Lexeme, Symbol.SymbolKind.VAR, type);
                t = lexer.PeekNextToken();
            }
            else{
                Error(t, "Expected semicolon ';'");
            }
        }
        parse(t, ";");
    }

    private void letStmt(){
        Token t = lexer.PeekNextToken();
        if(t.Lexeme.equals("let")) {
            OK(t);
            t = lexer.GetNextToken();
        }
        else {
            Error(t, "'let' expected");
        }

        t = lexer.PeekNextToken();
        if(t.Type == Token.TokenTypes.Identifier){
            OK(t);
            t = lexer.GetNextToken();
        } else{
            Error(t, identMessage);
        }

        t = lexer.PeekNextToken();
        if(t.Lexeme.equals("[")){
            OK(t);
            t = lexer.GetNextToken();
            expression();
            t = lexer.PeekNextToken();
            parse(t, "]");
        }
        t = lexer.PeekNextToken();
        if(t.Lexeme.equals("=")){
            OK(t);
            t = lexer.GetNextToken();
        } else{
            Error(t, "Expected assignment operator '='");
        }

        t = lexer.PeekNextToken();
        expression();
        t = lexer.PeekNextToken();
        parse(t, ";");
    }

    private void ifStmt(){
        Token t = lexer.PeekNextToken();

        parse(t, "if");

        t = lexer.PeekNextToken();
        parse(t, "(");
        t = lexer.PeekNextToken();

        expression();
        t = lexer.PeekNextToken();


        parse(t, ")");
        t = lexer.PeekNextToken();

        parse(t, "{");

        t = lexer.PeekNextToken();
        while(t.Lexeme.equals("var") || t.Lexeme.equals("let") || t.Lexeme.equals("if") ||
                t.Lexeme.equals("while") || t.Lexeme.equals("do") || t.Lexeme.equals("return")){
            stmt();
            t = lexer.PeekNextToken();
        }

        t = lexer.PeekNextToken();
        parse(t,"}");

        t = lexer.PeekNextToken();
        if(t.Lexeme.equals("else")){
            OK(t);
            t = lexer.GetNextToken();
            t = lexer.PeekNextToken();
            if(t.Lexeme.equals("{")){
                OK(t);
                t = lexer.GetNextToken();
            }
            else{
                Error(t, "Expected '{'");
            }

            t = lexer.PeekNextToken();
            while(t.Lexeme.equals("var") || t.Lexeme.equals("let") || t.Lexeme.equals("if") ||
                    t.Lexeme.equals("while") || t.Lexeme.equals("do") || t.Lexeme.equals("return")){
                stmt();
                t = lexer.PeekNextToken();
            }

            t = lexer.PeekNextToken();
            if(t.Lexeme.equals("}")){
                OK(t);
                t = lexer.GetNextToken();
            } else {
                Error(t, "Expected '}'");
            }

        }
    }

    private void whileStmt(){
        Token t = lexer.PeekNextToken();
        parse(t, "while");
        t = lexer.PeekNextToken();
        parse(t, "(");
        t = lexer.PeekNextToken();
        expression();
        t = lexer.PeekNextToken();
        parse(t, ")");
        t = lexer.PeekNextToken();
        parse(t, "{");
        t = lexer.PeekNextToken();
        while(t.Lexeme.equals("var") || t.Lexeme.equals("let") || t.Lexeme.equals("if") ||
                t.Lexeme.equals("while") || t.Lexeme.equals("do") || t.Lexeme.equals("return")){
            stmt();
            t = lexer.PeekNextToken();
        }
        t = lexer.PeekNextToken();
        parse(t, "}");
    }

    private void doStmt(){
        Token t = lexer.PeekNextToken();
        parse(t, "do");
        t = lexer.PeekNextToken();
        subroutineCall();
        t = lexer.PeekNextToken();
        parse(t, ";");
    }

    private void subroutineCall(){
        Token t = lexer.PeekNextToken();
        parseIdentifier(t);
        t = lexer.PeekNextToken();
        if(t.Lexeme.equals(".")){
            OK(t);
            t = lexer.GetNextToken();
            t = lexer.PeekNextToken();
            parseIdentifier(t);
        }
        t = lexer.PeekNextToken();
        parse(t, "(");
        t = lexer.PeekNextToken();
        expressionList();
        t = lexer.PeekNextToken();
        parse(t, ")");
    }

    private void expressionList(){
        Token t = lexer.PeekNextToken();
        if(!t.Lexeme.equals(")")) {
            expression();
        }
        t = lexer.PeekNextToken();
        while(!t.Lexeme.equals(")")){
            parse(t, ",");
            t = lexer.PeekNextToken();
            expression();
            t = lexer.PeekNextToken();
        }
    }

    private void returnStmt(){
        Token t = lexer.PeekNextToken();
        parse(t, "return");
        t = lexer.PeekNextToken();
        if(!t.Lexeme.equals(";")){
            expression();
            t = lexer.PeekNextToken();
        }
        parse(t, ";");
    }

    private void expression(){
        relationalExpression();
        Token t = lexer.PeekNextToken();

        while(t.Lexeme.equals("&") || t.Lexeme.equals("|")){
            OK(t);
            t = lexer.GetNextToken();
            t = lexer.PeekNextToken();
            relationalExpression();
        }
    }

    private void relationalExpression(){
        arithmeticExpression();
        Token t = lexer.PeekNextToken();

        while(t.Lexeme.equals("=") || (t.Lexeme.equals(">") || t.Lexeme.equals("<"))){
            OK(t);
            t = lexer.GetNextToken();
            t = lexer.PeekNextToken();
            arithmeticExpression();
        }
    }

    private void arithmeticExpression(){
        term();
        Token t = lexer.PeekNextToken();
        while(t.Lexeme.equals("+") || t.Lexeme.equals("-")){
            OK(t);
            t = lexer.GetNextToken();
            t = lexer.PeekNextToken();
            term();
        }
    }

    private void term(){
        factor();
        Token t = lexer.PeekNextToken();
        while(t.Lexeme.equals("*") || t.Lexeme.equals("/") ){
            OK(t);
            t = lexer.GetNextToken();
            t = lexer.PeekNextToken();
            factor();
        }
    }

    private void factor(){
        Token t = lexer.PeekNextToken();
        if(t.Lexeme.equals("-") || t.Lexeme.equals("~")){
            OK(t);
            t = lexer.GetNextToken();
            t = lexer.PeekNextToken();
            if(t.Lexeme.equals("(")){
                parse(t, "(");
            }
        }
        t = lexer.PeekNextToken();
        operand();
    }

    private void operand(){
        Token t = lexer.PeekNextToken();
        if(t.Type == Token.TokenTypes.TypeNumber || t.Type == Token.TokenTypes.StringLiteral
                || t.Type == Token.TokenTypes.TypeBool || t.Lexeme.equals("null") || t.Lexeme.equals("this")){
            OK(t);
            t = lexer.GetNextToken();
        }
        else if(t.Type == Token.TokenTypes.Identifier){
            OK(t);
            t = lexer.GetNextToken();
            t = lexer.PeekNextToken();
            if(t.Lexeme.equals(".")){
                OK(t);
                t = lexer.GetNextToken();
                t = lexer.PeekNextToken();
                if(t.Type == Token.TokenTypes.Identifier){
                    OK(t);
                    t = lexer.GetNextToken();
                    t = lexer.PeekNextToken();
                    if(t.Lexeme.equals("[")){
                        OK(t);
                        t = lexer.GetNextToken();
                        expression();
                        t = lexer.PeekNextToken();
                        if(t.Lexeme.equals("]")){
                            OK(t);
                            t = lexer.GetNextToken();
                        }
                        else{
                            Error(t, "Expected closing square brace ']'");
                        }
                    }
                    else if(t.Lexeme.equals("(")){
                        parse(t, "(");
                        t = lexer.PeekNextToken();
                        expressionList();
                        t = lexer.PeekNextToken();
                        parse(t, ")");
                    }
                }
                else{
                    Error(t, identMessage);
                }
            }
            t = lexer.PeekNextToken();
            if(t.Lexeme.equals("[")) {
                OK(t);
                t = lexer.GetNextToken();
                t = lexer.PeekNextToken();
                expression();
                t = lexer.PeekNextToken();
                if (t.Lexeme.equals("]")) {
                    OK(t);
                    t = lexer.GetNextToken();
                } else {
                    Error(t, "Expected closing square brace ']'");
                }
            }
        }
        else if(t.Lexeme.equals("(")){
            expression();
            t = lexer.PeekNextToken();
            if(t.Lexeme.equals(")")){
                OK(t);
                t = lexer.GetNextToken();
            }
            Error(t, "Expected closing brace ')'");
        } else{
            Error(t, "Expected operand!");
        }
    }

    private void addVarSt(){

    }
}


