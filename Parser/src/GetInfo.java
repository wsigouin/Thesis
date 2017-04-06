import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Simple function used to derive the information about the dataset for the final results
 */
public class GetInfo {
    public static void main(String args[]) throws FileNotFoundException{
        File l = new File("phishingData.txt");

        Scanner in = new Scanner(l);

        int numEmails = 0;
        double size = 0;
        double numLinks = 0;
        double numDots = 0;
        double numDomains = 0;
        double imageLink = 0;
        double html = 0;
        double urgent = 0;
        double currency = 0;
        double js = 0;
        double newsite = 0;
        double misleading = 0;
        double subject = 0;
        double badMessage = 0;
        double ipBased = 0;
        double here = 0;

        while(in.hasNextLine()){
            numEmails ++;
            String line = in.nextLine();

            StringTokenizer st = new StringTokenizer(line, " ");

            size+= Double.parseDouble(st.nextToken());
            numLinks += Double.parseDouble(st.nextToken());
            numDots += Double.parseDouble(st.nextToken());
            numDomains += Double.parseDouble(st.nextToken());
            imageLink += Double.parseDouble(st.nextToken());
            html += Double.parseDouble(st.nextToken());
            urgent += Double.parseDouble(st.nextToken());
            currency += Double.parseDouble(st.nextToken());
            js += Double.parseDouble(st.nextToken());
            newsite += Double.parseDouble(st.nextToken());
            misleading += Double.parseDouble(st.nextToken());
            subject += Double.parseDouble(st.nextToken());
            badMessage += Double.parseDouble(st.nextToken());
            ipBased += Double.parseDouble(st.nextToken());
            here += Double.parseDouble(st.nextToken());


        }
        System.out.println("Phishing Stats: ");
        System.out.println("Size Avg: " + (size/numEmails));
        System.out.println("Links Avg: " + (numLinks/numEmails));
        System.out.println("Dots Avg: " + (numDots/numEmails));
        System.out.println("Domains Avs: " + (numDomains/numEmails));
        System.out.println("Image Link: " + (imageLink/numEmails*100));
        System.out.println("Exclusively HTML: " + (html/numEmails*100));
        System.out.println("Urgent : " + (urgent/numEmails*100));
        System.out.println("Currency : " + (currency/numEmails*100));
        System.out.println("JS: " + (js/numEmails*100));
        System.out.println("New Sites: " + (newsite/numEmails*100));
        System.out.println("Misleading : " + (misleading/numEmails*100));
        System.out.println("Subject : " + (subject/numEmails*100));
        System.out.println("Bad Message : " + (badMessage/numEmails*100));
        System.out.println("IP Based : " + (ipBased/numEmails*100));
        System.out.println("Here: " + (here/numEmails*100));







    }
}
