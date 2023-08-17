import java.io.*;
import java.util.Arrays;
import java.util.List;

public class Token {

//    public static void main(String[] args){
//        Lexer lex = new Lexer();
//        lex.Init(args[0]);
//        Token token = lex.GetNextToken();
//        while (token.Type != TokenTypes.EOF){
//            token = lex.GetNextToken();
//            token = lex.PeekNextToken();
//            System.out.println(token.Lexeme + " " + token.LineNum + " " + token.Type);
//        }
//        lex.CloseFile();
//    }

    public enum TokenTypes {KeyW, Identifier, TypeNumber, TypeBool, EOF, Symbol, StringLiteral}

    public String Lexeme;
    public TokenTypes Type;
    public int LineNum;

    public Token(){
        Lexeme = "";
    }

}

class Lexer {
    private RandomAccessFile fr;
    private int lineNumber; //Keeps track of current line number

    //Used to check if the Lexeme holds a keyword.
    public List<String> Keywords = Arrays.asList("class", "method", "function", "constructor", "var", "char",
            "static", "field", "let", "do", "if", "else", "while", "return", "this", "void", "int", "boolean");

    public boolean Init(String file_name){
        File temp = new File(file_name);
        if(!temp.exists()){
            System.err.println("File " + file_name + " does not exist");
            return false;
        }

        try {
            fr = new RandomAccessFile(file_name, "r");
        } catch (FileNotFoundException e) {
            System.err.println("Could not open file " + file_name);
            return false;
        }
        lineNumber = 1;
        return true;
    }

    public void CloseFile() {
        try {
            fr.close();
        }catch(IOException e){
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private int Peek() {
        long pos = 0;
        try {
            pos = fr.getFilePointer();
            int current = fr.read();
            fr.seek(pos);
            return current;
        }catch(IOException e){
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return -1;
    }

    public Token GetNextToken() {

        Token token = new Token();
        int c;

        while(true) {
            try{
            c = Peek();

            if (c == -1) {
                c = fr.read();
                token.Type = Token.TokenTypes.EOF;
                token.LineNum = lineNumber;
                return token;
            }

            //If "/" is detected, this could be the start of a comment
            if (c == '/') {
                c = fr.read();
                int p = Peek();
                if (p == '/') {
                    c = fr.read();
                    while (c != '\n') {
                        c = fr.read();
                        c = Peek();
                    }
                } else if (p == '*') {
                    c = fr.read();
                    //Read the first / and THEN the *
                    c = fr.read();
                    p = Peek();
                    while (c != '*' || p != '/') {
                        if (c == '\n') {
                            lineNumber++;
                        }
                        if (c == -1) {
                            break;
                        }
                        c = fr.read();
                        p = Peek();
                    }
                    c = fr.read();
                    c = Peek();
                }
                else{
                    token.Lexeme += (char)c;
                    token.Type = Token.TokenTypes.Symbol;
                    token.LineNum = lineNumber;
                    return token;
                }

            }


            //If a String is detected, store the literal without double quotes
            if (c == '"') {
                token.Type = Token.TokenTypes.StringLiteral;
                c = fr.read();
                //Skips quote
                c = fr.read();
                while (c != '"' && c != -1) {
                    token.Lexeme += (char) c;
                    c = fr.read();
                }
                token.LineNum = lineNumber;
                return token;
            }


            //If token starts with a letter/underscore, then its an Identifier
            //or a KeyW
            if (Character.isLetter((char) c) || c == '_') {

                while ((c != -1) && (Character.isLetter((char) c)) || Character.isDigit((char) c)) {
                    c = fr.read();
                    token.Lexeme += (char) c;
                    c = Peek();

                }
                for (int i = 0; i < Keywords.size(); i++) {
                    if (Keywords.get(i).equals(token.Lexeme)) {
                        token.Type = Token.TokenTypes.KeyW;
                        token.LineNum = lineNumber;
                        return token;
                    }
                }
                if (token.Lexeme.equals("true") || token.Lexeme.equals("false")) {
                    token.Type = Token.TokenTypes.TypeBool;
                    token.LineNum = lineNumber;
                    return token;
                }
                token.Type = Token.TokenTypes.Identifier;
                token.LineNum = lineNumber;
                return token;
            }

            if (Character.isDigit((char) c)) {
                while ((c != -1) && Character.isDigit((char) c)) {
                    c = fr.read();
                    token.Lexeme += (char) c;
                    c = Peek();
                }

                token.Type = Token.TokenTypes.TypeNumber;
                token.LineNum = lineNumber;
                return token;
            }

            if (!Character.isWhitespace((char) c)) {
                c = fr.read();
                token.Lexeme += (char) c;
                token.Type = Token.TokenTypes.Symbol;
                token.LineNum = lineNumber;
                return token;
            }


            while (c != -1 && Character.isWhitespace((char) c)) {

                if ( c == '\n') {
                    lineNumber++;
                   // System.out.println();
                }
                c = fr.read();
                c = Peek();
            }
        }catch(IOException e){
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }


    }

    public Token PeekNextToken(){
        long pos = 0;
        int oldLineNum = lineNumber;
        Token peek;
        try {
            pos = fr.getFilePointer();
            peek = GetNextToken();
            lineNumber = oldLineNum;
            fr.seek(pos);
            return peek;
        }catch(IOException e){
            System.err.println(e.getMessage());
            System.exit(1);
        }
        //Shouldn't get here
        return new Token();
    }
}
