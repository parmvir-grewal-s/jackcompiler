import java.util.Arrays;
import java.util.List;

public class Symbol {
    public enum SymbolKind{STATIC, VAR, FIELD, ARG, CLASS, SUBROUTINE};

    public String type;
    public SymbolKind kind;
    public String name;

    public int offset;

}