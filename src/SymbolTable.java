import java.util.ArrayList;

public class SymbolTable {
    ArrayList<Symbol> table;
    SymbolTable parent;     //  The parent associated with current symbol table
    String identifier;          //  The identifier associated with current symbol table

    int argCount = 0;
    int varCount = 0;
    int fieldCount = 0;
    int staticCount = 0;
    int classCount = 0;
    int subroutineCount = 0;

    //  Initialise a symbol table with no parent (global)
    public SymbolTable(){
        table = new ArrayList<Symbol>();
    }

    //  Initialise a symbol table with a parent (child)
    public SymbolTable(SymbolTable p, String i){
        table = new ArrayList<Symbol>();
        parent = p;
        identifier = i;
    }

    public void AddSymbol(String name, Symbol.SymbolKind kind, String type){
        Symbol symbol = new Symbol();
        symbol.name = name;
        symbol.kind = kind;
        symbol.type = type;
        if(kind == Symbol.SymbolKind.ARG){
            symbol.offset = argCount;
            argCount++;
        }
        else if(kind == Symbol.SymbolKind.VAR){
            symbol.offset = varCount;
            varCount++;
        }
        else if(kind == Symbol.SymbolKind.FIELD){
            symbol.offset = fieldCount;
            fieldCount++;
        }
        else if(kind == Symbol.SymbolKind.STATIC){
            symbol.offset = staticCount;
            staticCount++;
        }
        else if(kind == Symbol.SymbolKind.CLASS){
            symbol.offset = classCount;
            classCount++;
        }
        else if(kind == Symbol.SymbolKind.SUBROUTINE){
            symbol.offset = subroutineCount;
            subroutineCount++;
        }

        table.add(symbol);
    }

    public boolean LookUp(String name){
        for(Symbol s: table){
            if(s.name.equals(name))
                return true;
        }
        return false;
    }

    public void Print(){
        for(Symbol s: table){
            System.out.println(s.kind + ", " + s.type + ", " + s.name + ", " + s.offset);
        }
    }
}
