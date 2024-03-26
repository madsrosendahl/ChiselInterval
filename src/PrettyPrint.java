// This class contains a prettyprinter for CoreChisel abstract syntax
// as described by the constructors in the AbsSyn file
// most of the

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class PrettyPrint {
  public static void main(String[] args) {
    Parser parser=new Parser("src\\in\\prog9.txt");
    Design d=parser.parser();
    //System.out.println(d);
    PrettyPrint pp=new PrettyPrint();
    PrintStream out1=pp.outfile("src\\in\\pprog9.txt");
    //pp.pp(System.out,d);
    pp.pp(out1,d);
    out1.close();
    Parser parser2=new Parser("src\\in\\prog9.txt");
    Design d2=parser2.parser();
    pp.pp(System.out,d2);
  }
  void pp( Design d){
    pp(System.out,d);
  }
  void pp(PrintStream out, Design d){
    for(ValDecl vd:d.decl())pp(out,vd);
    for(Conc cn:d.con())pp(out,cn);
    out.println();
    for(Module m:d.mod())pp(out,m);
  }
  void pp(PrintStream out,ValDecl vd){
    out.println(vd);
  }
  void pp(PrintStream out,Conc cn){
    out.println(cn);
  }
  void pp(PrintStream out,Module m){
    out.println("module "+m.nm());
    for(Decl d: m.decl())pp(out,d);
    for(State s: m.states())pp(out,s);
  }
  void pp(PrintStream out,Decl d){
    out.println(d);
  }

  void pp(PrintStream out,State s){
    if(s.cmd().equals(new Num(1)))
      out.println("state "+s.n());
    else
      out.println("state "+s.n()+" when "+s.cmd());
    for(Stat st:s.stm())pp(out,st);
    out.println("  goto "+s.g());
    out.println();
  }

  void pp(PrintStream out,Stat s){
    out.println("  "+s);
  }

  PrintStream outfile(String f){
    try{
      return new PrintStream(new FileOutputStream(f));
    }catch(IOException e){
      System.out.println(e);
      return null;
    }
  }
}
