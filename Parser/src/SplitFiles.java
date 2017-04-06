import java.io.File;
import java.io.FileNotFoundException;
import java.io.*;
import java.util.*;
/**
 * Class used to split MBOX files for processing individual emails
  */
public class SplitFiles {
    public static void main (String[] args) throws FileNotFoundException{
        File mbox = new File("emails/mbox/phishing3.mbox");
        Scanner in = new Scanner(new FileInputStream(mbox), "UTF-8");

        int i = 0;
        String line = in.nextLine();
        String outString = "";

        while(in.hasNext()){
            System.out.println(i + " emails processed");
            File outFile = new File("emails/phishing2/phishing" + i + ".txt");
            PrintWriter out = new PrintWriter(outFile);
            while(in.hasNext()) {

                outString += line +"\n";
                line = in.nextLine();
                if(line.matches("^From .*")){
                    break;
                }
            }
            out.print(outString);
            i++;
            out.close();
        }

    }

}
