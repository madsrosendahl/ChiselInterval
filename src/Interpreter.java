
import java.util.*;

//--------------------------------------------------------------------------
// data type for environments with pretty printer
// pretty printer for environment includes newline after each four elements
record EnvC(TreeMap<String,Integer> map){
  public String toString(){
    String r="{"; int i=0;
    for(String k:map.keySet()){
      if(i>0)r=r+", ";
      if(i%4==0&&i>0)r=r+"\n ";
      r=r+"("+k+")="+map.get(k);
      i++;
    }
    return r+"}"; }
    //return map().toString();}
}

public class Interpreter extends Aux1{
  public static void main(String[] args) {
    Parser parser = new Parser("src\\in\\prog7.txt");
    Design d = parser.parser();
    PrettyPrint pp = new PrettyPrint();
    pp.pp(d);
    Interpreter ii=new Interpreter();
    EnvC env0=ii.ii(d);
    System.out.println(env0);
    EnvC iif=ii.iterate(d,env0,20);
    //ii.iterate2(d,env0,30);
  }

/*--------------------------------------------------------------------------
 Construction of initial environment
  I[[mod+con∗mdcl+]] = c[[con∗]] ⊕ m[[mod∗]](D[[mdcl∗]])
  c[[con1 . . . conn]] = c[[con1]] 1 ⊕ · · · c[[con1]] n
  c[[m1.out1’<>’m2.in2]] i = [(m1.out1) → i] ⊕ [(m2.in2) → i]⊕
    [(i, ’ready’) → 0] ⊕ [(i, ’valid’) → 0] ⊕ [(i, ’data’) → 0]⊕
    [(m1, c1) → i] ⊕ [(m2, c2) → i]
  m[[mod1 · · · modn]] d = m[[mod1]] d ⊕ · · · ⊕ m[[modn]] d
  m[[’val’m’=’’Module’’(’M’)’]]d = d M m
*/

  EnvC ii(Design d){
    return addEnv(mm(d.decl(),d.mod()),cc(d.con()));
  }

  EnvC mm(ArrayList<ValDecl> decl, ArrayList<Module> mod) {
    EnvC env0=null;
    for(ValDecl dl:decl)env0=addEnv(env0,mm(dl,mod));
    return env0;
  }
  EnvC mm(ValDecl dl, ArrayList<Module> mod) {
    return dd(getMod(dl.rhs(),mod),dl.lhs());
  }
  EnvC cc(ArrayList<Conc> con){
    EnvC env0=null;
    int n=1;
    for(Conc c:con){
      env0=addEnv(env0,mkEnv(n+",ready",0));
      env0=addEnv(env0,mkEnv(n+",valid",0));
      env0=addEnv(env0,mkEnv(n+",data",0));
      env0=addEnv(env0,mkEnv(c.m1() +","+c.out(),n));
      env0=addEnv(env0,mkEnv(c.m2() +","+c.in(),n));
      n++;
    }
    return env0;
  }
  /*--------------------------------------------------------------------------
  Declaration of registers in modules
    D[[mdcl1 . . . mdcln]] M m = D[[mdcl1]] M m ⊕ · · · ⊕ D[[mdcln]] M m
    D[[’module’M1decl states]] M m =if M = M1 then D[[decl]]m else⊥Σ
    D[[d1 · · · dn]] m = D[[d1]] m ⊕ D[[dn]] m ⊕ [(m, ’state’) → 1]
    D[[’int’x = n]] m = [(m, x) → n]
    D[[’int’[n]a]] m = [(m, a′0) → n] ⊕ · · · ⊕ [(m, a′(n − 1))) → n]
  */
  EnvC dd(Module mod, String m) {
    EnvC env0=Aux1.mkEnv(m+",state",1);
    for(Decl d: mod.decl())env0=addEnv(env0,dd(d,m));
    return env0;
  }
  EnvC dd(Decl d, String m) {
    switch(d){
      case VarDecl v -> {return Aux1.mkEnv(m+","+v.nm(),ee(v.init(),null,null));}
      case ArrDecl v -> {EnvC env0=null;
        for(int i=0;i<v.idx();i++)env0=addEnv(env0,Aux1.mkEnv(m+","+v.nm()+"'"+i,0));
        return env0;
      }
      case InDecl v -> {return null;}  // set by declaration of connections
      case OutDecl v -> {return null;} // set by declaration of connections
      default -> {fail(); return null;}
    }
  }

/*--------------------------------------------------------------------------
  Channel reset
    R[[con1 · · · conn]] σ = R[[con1]] σ ⊕ · · · ⊕ R[[conn]] σ
    R[[m1.out1’<>’m2.in2]] σ =let c = σ(m1, out1)
      if σ(c, ’ready’) = 0 then [(c, ’ready’) → 1, (c, ’valid’) → 0] else ⊥Σ
*/
  EnvC rr(Design d,EnvC env){
    EnvC env0=null;
    for(Conc c:d.con()){
      int ch=getV(c.m1()+","+c.out(),env);
      //if(getV(ch+",ready",env)==0) System.out.println("Reset channel "+ch);
      if(getV(ch+",ready",env)==0)env0=addEnv(env0,mkEnv(ch+",ready",1,ch+",valid",0));
    }
    return env0;
  }
/*--------------------------------------------------------------------------
  Transition function for modules and states
    T[[mod+con∗mdcl+]]σ = (Tm[[mod∗]](Td[[mdcl∗]]) σ) ⊕ R[[con∗]]σ
    Tm[[mod1 · · · modn]] d σ = Tm[[mod1]] d σ ⊕ · · · ⊕ Tm[[modn]] d σ
    Tm[[’val’m’=’’Module’’(’M’)’]] d σ m = d σ M m
    Td[[mdcl1 · · · mdcln]] σ M m = Td[[mdcl1]] σ M m ⊕ · · · ⊕ Td[[mdcln]] σ M m
    Td[[’module’ M1 decl states]] σ M m =if M = M1 then Tt[[states]]σ m else ⊥Σ
    Tt[[state1 · · · staten]] σ m = Tt[[state1]]σ m ⊕ · · · ⊕ Tt[[staten]]σ m
    Tt[[’state’n’when’e s1 · · · sn’goto’eg]] σ m =
      if σ(m, ’state’)  ̸= n then ⊥Σ else
      if E[[e]] σ m ̸= 1 then ⊥Σ else
    Tt[[s1]] σ m ⊕ · · · ⊕ Tt[[sn]] σ m ⊕ [(m, ’state’) → E[[eg]] σ m]
*/

  EnvC tt(Design d,EnvC env){
    EnvC env0= null;
    for(ValDecl v:d.decl())env0=addEnv(env0,td(v.lhs(),v.rhs(),d.mod(),env));
    //env0=addEnv(env0,rr(d.con()));
    return addEnv(env0,rr(d,env));
  }
  EnvC td(String lhs, String rhs, ArrayList<Module> mod, EnvC env) {
    Module mm=getMod(rhs,mod);
    return td(mm,env,lhs);
  }
  EnvC td(Module mod, EnvC env,String m) {
    return tt(mod.states(),env,m);
  }
  EnvC tt(ArrayList<State> sts,EnvC env,String m){
    int sn=Aux1.getV(m+",state",env);
    State s=getState(sn,sts);
    return tt(s,env,m);
  }
  EnvC tt(State s,EnvC env,String m){
    EnvC env0=null;
    if(ee(s.cmd(),env,m)!=1)return env0;
    for(Stat s1:s.stm())env0=addEnv(env0,ts(s1,env,m));
    env0=addEnv(env0,eg(s.g(),env,m));
    return env0;
  }
  EnvC eg(Goto g, EnvC env, String m) {
    switch(g){
      case Next n -> {return mkEnv(m+",state",n.i());}
      case Cond gt -> {return eg((ee(gt.e(),env,m)==1)?gt.g1():gt.g2(),env,m);}
      default ->{fail("eg");return null;}
    }
  }

  /*--------------------------------------------------------------------------
  Transition function for statements

    Ts[[x’=’e]] σ m = [(m, x) → E[[e]]σm]
    Ts[[a[e1]’=’e2]] σ m = [(m, a::E[[e1]] σ m) → E[[e2]] σ m]
    Ts[[x’=’a[e1]]] σ m = [(m, x) → σ(m, a::E[[e1]] σ m)]
    Ts[[out’.write(’e’)’]] σ m =
      [(σ(m, out), ’data’) → E[[e]]σ m, (σ(m, out), ’valid’) → 1]
    Ts[[x’=’in’.read()’]] σ m =
      [(m, x) → σ(σ(m, in), ’data’), (σ(m, out), ’ready’) → 0]   ---- ups out should be in
*/

  private EnvC ts(Stat s, EnvC env, String m) {
    switch(s){
      case Asg a->{return mkEnv(m+","+a.lhs(),ee(a.rhs(),env,m));}
      case AsgMem am->{return mkEnv(m+","+am.lhs()+"'"+ee(am.idx(), env,m),ee(am.rhs(),env,m));}
      case ReadMem rm->{return mkEnv("m,"+rm.lhs(),getV(m+","+rm.rhs()+"'"+ee(rm.idx(),env,m),env));}
      case WriteCh wc->{int ch=getV(m+","+wc.ch(),env);
        return mkEnv(ch+",data",ee(wc.rhs(), env,m), ch+",valid",1);}
      case ReadCh rc->{int ch=getV(m+","+rc.ch(),env);
        return mkEnv(m+","+rc.lhs(),getV(ch+",data",env),ch+",ready",0);}
      default -> {fail("ts");return null;}
    }
  }

  /*--------------------------------------------------------------------------
  Evaluation function for expressions

  E[[n]] σ m = n
  E[[v]] σ m = σ(m, v)
  E[[’Mux(’e1, e2, e3’)’]] σ m =if E[[e1]] σ m = 1 then E[[e2]] σ m then E[[e3]] σ m
  E[[e1 op e2]] σ m = op(E[[e1]] σ m, E[[e2]] σ m)
  E[[in’.ready()’]] σ m = σ(σ(m, in), ’ready’) = 1 ∧ σ(σ(m, in), ’valid’) = 0
  E[[in’.valid()’]]σ m = σ(σ(m, in), ’ready’) = 1 ∧ σ(σ(m, in), ’valid’) = 1  ---   use  out
*/
  int ee(Exp e,EnvC env,String m){
    switch(e){
      case Num n -> {return n.i();}
      case Var v -> {return getV(m+","+v.nm(),env);}
      case Mux mx -> {return ee(mx.e0(),env,m)==1 ? ee(mx.e1(),env,m) : ee(mx.e2(),env,m);}
      case Bin b -> {return binop(b.cmd(),ee(b.e1(),env,m),ee(b.e2(),env,m));}
      case Ready r ->{int ch=getV(m+","+r.s(),env);
           return getV(ch+",ready",env)==1&&getV(ch+",valid",env)==0 ? 1 : 0;}
      case Valid r ->{int ch=getV(m+","+r.s(),env);
           return getV(ch+",ready",env)==1&&getV(ch+",valid",env)==1 ? 1 : 0;}
      default -> {fail("ee");return 0;}
    }
  }

  int binop(String c,int x,int y){
    switch(c){
      case "+"  -> {return x+y; }
      case "-"  -> {return x-y; }
      case "*"  -> {return x*y; }
      case "/"  -> {return x/y; }
      case "%"  -> {return x%y; }
      case ">"  -> {return (x>y?1:0); }
      case ">=" -> {return (x>=y?1:0); }
      case "<"  -> {return (x<y?1:0); }
      case "<=" -> {return (x<=y?1:0); }
      case "==" -> {return (x==y?1:0); }
      case "!=" -> {return (x!=y?x:y); }
      case "|"  -> {return (x==1||y==1?1:0); }
      case "&"  -> {return (x==1&&y==1?1:0); }
      default -> {fail("bin");return 0;}
    }
  }

  /*--------------------------------------------------------------------------
     Iterate transistion function from initial state
  */
  EnvC iterate(Design d,EnvC env0,int mx){
    EnvC env=env0;
    for(int i=0;i<mx;i++) {
      System.out.println("clock at "+i+" "+tos(getLab(env0,getMds(d.decl()))));
      env0=env;
      EnvC env1 = tt(d, env);
      System.out.println(env1);
      env=addEnv(env1,env);
      System.out.println(env);
      if(env.equals(env0)){
        System.out.println("done");break;
      }
    }
    return env;
  }

/*--------------------------------------------------------------------------
 Collecting interpretation
 create map of label vector to environments

    lab(σ) = ⟨σ(m1, ’state’), . . . , σ(mn, ’state’)⟩
    f0 : [⟨1, . . . , 1⟩ → {σinit}]
    F(f) = f0 ⊔ f ⊔
      {[ℓ′ → σ′] | ℓ ∈ dom(f) ∧ σ ∈ f(ℓ) ∧ σ′=(T[[prg]]σ ⊕ σ) ∧ ℓ′=lab(σ′)}
*/
  void iterate2(Design d, EnvC env0,int mx){
    String[] ms =getMds(d.decl());
    String lb=tos(getLab(env0,ms));
    HashMap<String,HashSet<EnvC>> map=new HashMap<>();
    map.put(lb,new HashSet<EnvC>());
    map.get(lb).add(env0);
    for(int i=0;i<mx;i++){
      System.out.println("Iterate "+i);
      for(String lb1:copy(map.keySet())){
        for(EnvC env1:map.get(lb1)){
          EnvC env2= addEnv(tt(d,env1),env1);
          String lb2=tos(getLab(env2,ms));
          if(!map.containsKey(lb2))map.put(lb2,new HashSet<EnvC>());
          map.get(lb2).add(env2);
        }
      }
      for(String lb1:copy(map.keySet())){
        System.out.println("Lab: "+lb1);
        for(EnvC env1:map.get(lb1))
          System.out.println(env1);
      }
    }
  }
}

/*--------------------------------------------------------------------------

*/

class Aux1{
  static String tos(int[] a){return Arrays.toString(a);}
  static int getV(String s,EnvC env){
    if(!env.map().containsKey(s))fail("no var "+s+" in"+env);
    return env.map().get(s);}
  static State getS(int n, ArrayList<State> st){
    for(State s:st)if(s.n()==n)return s;
    return null;
  }
  static Set<String> vars(EnvC env){return env.map().keySet();}
  static final int Maxint=1000000;
  static EnvC mkEnv(String s,int i){
    TreeMap<String,Integer> map=new TreeMap<>();
    map.put(s,i);
    return new EnvC(map);
  }
  static EnvC mkEnv(String s1,int i1,String s2,int i2){
    TreeMap<String,Integer> map=new TreeMap<>();
    map.put(s1,i1);
    map.put(s2,i2);
    return new EnvC(map);
  }
  static EnvC updE(String s,int v,EnvC env){
    TreeMap<String,Integer> map =new TreeMap<>();
    map.put(s,v);
    if(env==null)return new EnvC(map);
    for(String t:env.map().keySet()){
      if(!s.equals(t))map.put(t,env.map().get(t));
    }
    return new EnvC(map);
  }
  static void fail(){throw new RuntimeException("Fail");}
  static void fail(String s){
    System.out.println("Fail: "+s);
    throw new RuntimeException("Fail "+s);}

  static final boolean isnumber(String s){
    try{long l= Long.parseLong(s);return true;}
    catch(NumberFormatException e){return false;}
  }

  static Module getMod(String rhs, ArrayList<Module> mod) {
    for(Module m:mod)if(rhs.equals(m.nm()))return m;
    return null;
  }
  static State getState(int n, ArrayList<State> st){
    for(State s:st)if(s.n()==n)return s;
    return null;
  }

  static EnvC addEnv(EnvC env1,EnvC env2){
    if(env1==null)return env2;
    if(env2==null)return env1;
    for(String v:env1.map().keySet()){
      env2 =updEnv(v,env1.map().get(v),env2);
    }
    return env2;
  }
  static EnvC updEnv(String s,int v,EnvC env){
    TreeMap<String,Integer> map =new TreeMap<>();
    map.put(s,v);
    if(env==null)return new EnvC(map);
    for(String t:env.map().keySet()){
      if(!s.equals(t))map.put(t,env.map().get(t));
    }
    return new EnvC(map);
  }

  static ArrayList<String> copy(Set<String> set){
    ArrayList<String> r=new ArrayList<>();
    r.addAll(set);
    Collections.sort(r);
    return r;
  }
  static String[] getMds(ArrayList<ValDecl> decl) {
    String[] r=new String[decl.size()];
    for(int i=0;i<r.length;i++)r[i]=decl.get(i).lhs();
    return r;
  }

  static int[] getLab(EnvC env,String[] ms){
    int[] r=new int[ms.length];
    for(int i=0;i<r.length;i++) r[i]=getV(ms[i]+",state",env);
    return r;
  }

}

