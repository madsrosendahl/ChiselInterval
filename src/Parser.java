import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
  public static void main(String[] args) {
    Parser parser=new Parser("src\\in\\prog8.txt");
    Design d=parser.parser();
    System.out.println(d);
  }

/*
    The parser is written to be close to the abstract syntax and uses
     a line based regular expressions parser.
     Each rule, except expressions, are expected to be on a separate line
     The parser defines identifiers, numbers, and expressions as separate
     syntactic categories. The parser creates an array of input parts from
     syntactic categories and send it to a semantic action to create an
     abstract syntax representation
     As an example
       ⟨ident⟩ ‘=’ ⟨ident⟩ ‘[’ ⟨expr⟩ ‘]’
     a line that matches this will be made into a String array of three elements
     the semantic action create a ReadMem object from the three parts in this form
        a -> new ReadMem(a[0],a[1],parseExp(a[2]))
     The parseExp also uses regular expressions  to match expressions with
     regular expressions and for an abstract syntax representation of the string
     The abstract syntax representation in the program can be found in AbsSyn
     and is very close to a ML style datatype representation.


    ⟨program⟩ ::= ⟨module⟩+⟨connnection⟩* ⟨module-decl ⟩+
    ⟨module⟩ ::= ‘val’ ⟨ident⟩ ‘=’ ‘Module’ ‘(’ ⟨ident⟩ ‘)’
    ⟨connection⟩ ::= ⟨ident⟩ ‘.’ ⟨ident⟩ ‘<>’ ⟨ident⟩ ‘.’ ⟨ident⟩
    ⟨module-decl ⟩ ::= ‘module’ ⟨ident⟩ ⟨declaration⟩* ⟨state⟩+
    ⟨declaration⟩ ::= ‘int’ ⟨ident⟩ [ ‘=’ ⟨number⟩ ]
      | ‘int’ ‘[’ ⟨number⟩ ‘]’ ⟨ident⟩
      | ‘instream’ ⟨ident⟩\
      | ‘outstream’ ⟨ident⟩
    ⟨state⟩ ::= ‘state’ ⟨number⟩ [ ‘when’ ⟨expr⟩ ] ⟨statement⟩* ‘goto’ ⟨gotoexp⟩
    ⟨statement⟩ ::= ⟨ident⟩ ‘=’ ⟨expr⟩
      | ⟨ident⟩ ‘[’ ⟨expr⟩ ‘]’ ‘=’ ⟨expr⟩\
      | ⟨ident⟩ ‘=’ ⟨ident⟩ ‘[’ ⟨expr⟩ ‘]’
      | ⟨ident⟩ ‘.’ ‘write’ ‘(’ ⟨expr⟩ ‘)’
      | ⟨ident⟩ ‘=’ ⟨ident⟩ ‘.’ ‘read’ ‘(’ ‘)’
    ⟨expr⟩ ::= ⟨ident⟩
      | ⟨number⟩
      | ⟨expr⟩ ⟨operation⟩ ⟨expr⟩
      | ‘Mux’ ‘(’ ⟨expr⟩ ‘,’ ⟨expr⟩ ‘,’ ⟨expr⟩ ‘)’\
      | ⟨ident⟩ ‘.’ ‘ready’
      | ⟨ident⟩ ‘.’ ‘valid’
    ⟨operation⟩ ::= ‘+’ | ‘-’ | ‘*’ | ‘/’ | ‘%’
      | ‘&’ | ‘|’ | ‘>’ | ‘<’ | ‘>=’ | ‘<=’| ‘==’ | ‘!=’
    ⟨gotoexp⟩ ::= ⟨number⟩
      | ‘Mux’ ‘(’ ⟨expr⟩ ‘,’ ⟨gotoexp⟩ ‘,’ ⟨gotoexp⟩ ‘)’
*/

  RegexParser p = new RegexParser();
  Infile in;

  Parser(String f) {
    in=new Infile(f);
    p.addAbrv("ident", "([a-zA-Z0-9]+)");
    p.addAbrv( "num", "([0-9]+)");
    p.addAbrv("exp", "([a-zA-Z0-9 \\(\\)\\<\\>\\=\\.\\,\\+\\-\\*\\%\\&\\|]+)");
  }

  String inp="";
  void next(){ inp=in.readLine();while(inp!=null&&inp.trim().equals(""))inp=in.readLine(); }
  Design parser() {
    ArrayList<ValDecl> valDecls=new ArrayList<>();
    ArrayList<Conc> concs=new ArrayList<>();
    ArrayList<Module> mods=new ArrayList<>();
    next();
    for(Object o : parseRep("val ident = Module(ident)", a -> new ValDecl(a[0], a[1])))
      valDecls.add((ValDecl) o);
    for(Object o : parseRep("ident.ident <> ident.ident", a -> new Conc(a[0],a[1],a[2],a[3])))
      concs.add((Conc) o);
    for(;;){
      if(inp==null)break;
      Object o= p.parse(inp, "module ident", a -> new Module(a[0],null,null));
      if(o==null)break; else next();
      Module mod=(Module)o;
      //System.out.println("Mod: "+mod);
      ArrayList<Decl> decls=parseDecls();
      ArrayList<State> states=parseStates();
      mod=new Module(mod.nm(),decls,states);
      mods.add(mod);
    }
    //System.out.println("End at "+inp);
    return new Design(valDecls,concs,mods);
  }

  ArrayList<Decl> parseDecls(){
    //System.out.println("parseDecls "+inp);
    ArrayList<Decl> decls=new ArrayList<>();
    for(;;) {
      Object o = null;
      if (o == null) o = p.parse(inp, "int ident = exp", a -> new VarDecl(a[0], parseExp(a[1])));
      if (o == null) o = p.parse(inp, "int ident", a -> new VarDecl(a[0], new Num(0)));
      if (o == null) o = p.parse(inp, "instream ident", a -> new InDecl(a[0]));
      if (o == null) o = p.parse(inp, "outstream ident", a -> new OutDecl(a[0]));
      if (o == null) o = p.parse(inp, "int[num] ident", a -> new ArrDecl(a[1],atoi(a[0])));
      if (o == null) o = p.parse(inp, "int ident, ident, ident, ident", a -> a);
      if (o == null) o = p.parse(inp, "int ident, ident, ident", a -> a);
      if (o == null) o = p.parse(inp, "int ident, ident", a -> a);
      if(o==null)break; else next();
      if(o instanceof Decl)decls.add((Decl) o);
      if(o instanceof String[])for(String s:((String[])o))decls.add(new VarDecl(s,new Num(0)));
    }
    return decls;
  }
  ArrayList<State> parseStates(){
    //System.out.println("parseStates "+inp);
    ArrayList<State> states=new ArrayList<>();
    for(;;) {
      if(inp==null)break;
      Object o = null;
      if (o == null) o = p.parse(inp, "state num when exp", a -> new State(atoi(a[0]), parseExp(a[1]), null, null));
      if (o == null) o = p.parse(inp, "state num", a -> new State(atoi(a[0]), new Num(1), null, null));
      if (o == null) break;else next();
      State st=(State) o;
      //System.out.println("State: "+st);
      ArrayList<Stat> stmt=parseStatements();
      Goto gt=parseGoto();
      st=new State(st.n(),st.cmd(),stmt,gt);
      states.add(st);
    }
    return states;
  }

  ArrayList<Stat> parseStatements() {
    //System.out.println("parseStatements " + inp);
    ArrayList<Stat> stmts = new ArrayList<>();
    for(;;) {
      Object o = null;
      if (o == null) o = p.parse(inp, "ident = ident.read()", a -> new ReadCh(a[0],a[1]));
      if (o == null) o = p.parse(inp, "ident.write(exp)", a -> new WriteCh(a[0],parseExp(a[1])));
      if (o == null) o = p.parse(inp, "ident = exp", a -> new Asg(a[0],parseExp(a[1])));
      if (o == null) o = p.parse(inp, "ident[exp] = exp", a -> new AsgMem(a[0],parseExp(a[1]),parseExp(a[2])));
      if (o == null) o = p.parse(inp, "ident = ident[exp]", a -> new ReadMem(a[0],a[1],parseExp(a[2])));
      if (o == null) break; else next();
      Stat st=(Stat) o;
      //System.out.println("Stat: "+st);
      stmts.add(st);
    }
    return stmts;
  }
  Goto parseGoto(){
    //System.out.println("Goto: "+inp);
    Object gt = p.parse(inp, "goto exp", a -> parseExp(a[0]));
    if(gt==null) return null; else next();
    return exp2goto((Exp) gt);
  }
  Goto exp2goto(Exp e){
    //System.out.println("Exp2Goto: "+e);
    switch (e){
      case Num n -> {return new Next(n.i());}
      case Mux mux -> {return new Cond(mux.e0(),exp2goto(mux.e1()),exp2goto(mux.e2()));}
      default -> {
        System.out.println("what "+e);return null;}
    }
  }

  Exp parseExp(String s) {
    //System.out.println("E: "+s);
    Object o = null;
    if (o == null) o = p.parse(s, "(exp)", a -> parseExp(a[0]));
    if (o == null) o = p.parse(s, "num", a -> new Num(atoi(a[0])));
    if (o == null) o = p.parse(s, "ident", a -> new Var(a[0]));
    if (o == null) o = p.parse(s, "Mux(exp,exp,exp)", a -> new Mux(parseExp(a[0]), parseExp(a[1]), parseExp(a[2])));
    if (o == null) o = p.parse(s, "exp + exp", a -> new Bin("+", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "exp - exp", a -> new Bin("-", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "exp * exp", a -> new Bin("*", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "exp / exp", a -> new Bin("/", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "exp % exp", a -> new Bin("%", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "exp >= exp", a -> new Bin(">=", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "exp <= exp", a -> new Bin("<=>", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "exp > exp", a -> new Bin(">", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "exp < exp", a -> new Bin("<", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "exp == exp", a -> new Bin("==", parseExp(a[0]), parseExp(a[1])));
    if (o == null) o = p.parse(s, "ident.valid()", a -> new Valid(a[0]));
    if (o == null) o = p.parse(s, "ident.ready()", a -> new Ready(a[0]));
    if (o != null && o instanceof Exp) return (Exp) o;
    return null;
  }
  static int atoi(String s){return Integer.parseInt(s);}

  ArrayList<Object> parseRep(String pat, Function<String[],Object> f){
    ArrayList<Object> ret=new ArrayList<>();
    for(;;){
      //System.out.println("parse: "+inp);
      Object o=p.parse(inp,pat,f);
      if(o==null) break;
      ret.add(o); next();
    }
    return ret;
  }
}

class RegexParser {
  HashMap<String, String> abrv = new HashMap<>();
  void addAbrv(String n,String p){abrv.put(n,p);}
  Object parse(String s, String p, Function<String[], Object> f) {
    s = s.trim();
    String meta = "()<>[]+*-/%.,|&";
    for (int i = 0; i < meta.length(); i++)
      p = replaceAll(p, "" + meta.charAt(i), "\\" + meta.charAt(i));
    for(String n:abrv.keySet())
      p=replaceAll(p,n,abrv.get(n));
    Pattern pt = Pattern.compile(p);
    Matcher ma = pt.matcher(s);
    boolean bl = ma.matches();
    if (!bl) {
      return null;
    }
    if (ma.start() != 0 || ma.end() != s.length()) return null;
    String[] r = new String[ma.groupCount()];
    for (int i = 0; i < r.length; i++) r[i] = ma.group(i + 1);
    Object rr = f.apply(r);
    return rr;
  }

  static String replaceAll(String s1, String s2, String s3) {
    int i, j = 0;
    while ((i = s1.indexOf(s2, j)) >= 0) {
      s1 = s1.substring(0, i) + s3 + s1.substring(i + s2.length());
      j = i + s3.length();
    }
    return s1;
  }
}

class Infile{
  BufferedReader in=null;
  Infile(String s){
    try {
      in= new BufferedReader(new FileReader(s));
    }catch(Exception e){
      System.out.println(e);
    }
  }
  String readLine(){
    try {
      return  in.readLine();
    }catch(Exception e){
      System.out.println(e);
    }
    return null;
  }
}

