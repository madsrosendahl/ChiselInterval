import java.io.*;
import java.util.ArrayList;

public class InOut {
  private InOut(){} // class just for static methods
  // the file includes functions for input-output for files

  public static PrintStream outfile(String f){
    try{ return new PrintStream(new FileOutputStream(f));
    }catch(IOException e){
      System.out.println(e);
      return null;
    }
  }

  public static BufferedReader infile(String f){
    try { return new BufferedReader(new FileReader(f));
    }catch(IOException e){
      return null;
    }
  }

  public static String readln(BufferedReader in){
    try { return in.readLine();
    }catch(IOException e){
      return null;
    }
  }
  public static void close(BufferedReader in){
    try { in.close();
    }catch(IOException e){
    }
  }
  public static void close(PrintStream out){
    out.close();
  }
  public static boolean probef(String f){
    File ff=new File(f);
    return ff.exists();
  }
  public static long filesize(String f){
    File ff=new File(f);
    return ff.length();
  }
  //---------------------------------------------
  public static void writefile(ArrayList<String> list, String f){
    try{
      PrintWriter out=new PrintWriter(new FileWriter(f));
      for(String s:list){
        out.println(s);
      }
      out.close();
    }catch(IOException e){
      System.out.println("error "+e);
    }
  }

  public static ArrayList<String> readfile(String f){
    ArrayList<String> list=new ArrayList<>();
    try{
      BufferedReader in=new BufferedReader(new FileReader(f));
      while(true){
        String s=in.readLine();
        if(s==null)break;
        list.add(s);
      }
      in.close();
    }catch(IOException e){
      return null;
    }
    return list;
  }

  //---------------------------------------------
  public static String[] dirlist(String d){
    File f=new File(d);
    return f.list();
  }
  public static void delfile(String f){
    File f1=new File(f);
    f1.delete();
  }
}
