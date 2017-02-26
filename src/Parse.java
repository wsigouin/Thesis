import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

import static java.lang.Character.isDigit;

/**
 * Created by wills on 2017-02-07.
 */
public class Parse {

    private static int NUM_INPUTS = 10;
    private static String[] inputTypes = {"Number of Characters", "Number of Links", "Max Number of Dots per Link", "Number of Linked Images","HTML", "Urgent", "Currency", "JavaScript", "New Link"};
    ;

    public static void main (String[] args) throws FileNotFoundException{
        createSet( 1500, "training.data");
    }

    private static void createSet(int size, String filename) throws FileNotFoundException{
        File[] spamFolder = new File("emails/phishing").listFiles();
        File[] legitFolder = new File("emails/legit").listFiles();
        File outFile = new File(filename);
        PrintWriter out = new PrintWriter(outFile);

        double[][] data = new double[size][];

        int spamCounter = 0;
        int legitCounter = 0;

        double minCharacters = Double.MAX_VALUE;
        double maxCharacters = 0;
        double minLinks = Double.MAX_VALUE;
        double maxLinks = 0;
        double minDots = Double.MAX_VALUE;
        double maxDots = 0;
        double minLinkedImages = Double.MAX_VALUE;
        double maxLinkedImages = 0;

        File currFile = null;
        double[] currEmailData = null;

        for(int i = 0; i < size; i++){
            double emailChoice = Math.random();
            if(i % 10 == 0){
                System.out.println(i*100/size + "% Complete");
            }
            if(emailChoice > 0.5 && spamCounter < spamFolder.length){
                currFile = spamFolder[spamCounter];
                currEmailData = parseEmail(currFile, 1);
                if(currEmailData[0] != -1) {
                    spamCounter++;
                }
                else{
                    i--;
                    continue;
                }

            }
            else{
                currFile = legitFolder[legitCounter];
                currEmailData = parseEmail(currFile, 0);
                if(currEmailData[0] != -1) {
                    legitCounter++;
                }
                else{
                    i--;
                    continue;
                }
            }


            if(currEmailData[0] > maxCharacters){
                maxCharacters = currEmailData[0];
            }
            else if(currEmailData[0] < minCharacters){
                minCharacters = currEmailData[0];
            }

            if(currEmailData[1] > maxLinks){
                maxLinks = currEmailData[1];
            }
            else if(currEmailData[1] < minLinks){
                minLinks = currEmailData[1];
            }

            if(currEmailData[2] > maxDots){
                maxDots = currEmailData[2];
            }
            else if(currEmailData[2] < minDots){
                minDots = currEmailData[2];
            }

            if(currEmailData[3] > maxLinkedImages){
                maxLinkedImages = currEmailData[3];
            }
            else if(currEmailData[3] < minLinkedImages){
                minLinkedImages = currEmailData[3];
            }


            data[i] = currEmailData;



        }
            out.write(size + " " + NUM_INPUTS + " 1\n\n");

            for (double[] datum : data) {
                out.write(
                        ((datum[0]-minCharacters)/(maxCharacters-minCharacters)) + " " +
                                ((datum[1]-minLinks)/(maxLinks-minLinks)) + " " +
                                ((datum[2]-minDots)/(maxDots-minDots)) + " " +
                                ((datum[3]-minLinkedImages)/(maxLinkedImages-minLinkedImages)) + " " + datum[4] + " " + datum[5] + " " + datum[6] + " " + datum[7] + " " + datum[8] +  " " + datum [9] + "\n\n"
                                + datum[10] + "\n\n"
                );
            }

        out.close();
    }


    private static double[] parseEmail(File file, int type) throws FileNotFoundException{
        Scanner in = new Scanner(file);

        Pattern lPattern = Pattern.compile ("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)");
        Matcher lMatcher = null;




        String[] currencyWords = {
                "money", "Money", "MONEY",
                "pounds", "Pounds", "POUNDS",
                "dollars", "Dollars", "DOLLARS",
                "rupees", "Rupees", "RUPEES"
        };

        String[] urgencyWords = {
                "now", "Now", "NOW",
                "today", "Today", "TODAY",
                "instantly", "Instantly", "INSTANTLY",
                "straightaway", "Straightaway", "STRAIGHTAWAY",
                "directly", "Directly", "DIRECTLY",
                "once", "Once", "ONCE",
                "urgently", "Urgently", "URGENTLY",
                "desperately", "Desperately", "DESPERATELY",
                "immediately", "Immediately", "IMMEDIATELY",
                "soon", "Soon", "SOON",
                "shortly", "Shortly", "SHORTLY",
                "quickly", "Quickly", "QUICKLY"
        };

        String[] jsWords = {
                "javascript", "Javascript", "JavaScript", "<script>",
                "onclick", "onmouseover", "onchange", "onmouseout", "onkeydown", "onload"
        };



        double linkCount = 0;
        double linkDots = 0;
        double overallLength = 0;
        double imageLink = 0;
        boolean isUrgent = false;
        boolean mentionsCurrency = false;
        boolean contentFound = false;
        boolean foundJS = false;
        boolean containsHTML = false;
        boolean lookingForImage = false;
        boolean dateFound = false;
        boolean hasNewSite = false;
        boolean misleadingLink = false;


        ArrayList<String> domains = new ArrayList<>();

        String line = "";
        String contentType = "N/A";
        String date = "N/A";
        Date currDate = null;

        DateFormat format1 = new SimpleDateFormat("EEE, d MMM yyyy");
        DateFormat format2 = new SimpleDateFormat("dd MMM yyyy");



        do{
            if(in.hasNext()) {
                line = in.nextLine();
                try {
                    if (!contentFound && line.contains("Content-Type:")) {
                        int semiPosition = line.indexOf(';');
                        if (semiPosition != -1) {
                            contentType = line.substring(14, semiPosition);
                        } else {
                            contentType = line.substring(14, line.length() - 1);
                        }
                        if (contentType.equals("multipart/alternative") || contentType.equals("text/html")) {
                            containsHTML = true;
                        }
                    }

                    if (!dateFound && line.contains("Date: ")) {
                        int semiPosition = line.indexOf(';');
                        if (semiPosition >= 0) {
                            date = line.substring(line.indexOf(';')+2, semiPosition);
                        } else {
                            date = line.substring(line.indexOf(':')+2, line.length() - 1);
                        }
                        try {
                            if (isDigit(date.charAt(0))) {
                                currDate = format2.parse(date);
                            } else {
                                currDate = format1.parse(date);
                            }
                        }
                        catch(ParseException e){
                            date = "";
                        }
                    }
                }
                catch(StringIndexOutOfBoundsException e){
                    double[] errorArray = {-1};
                    return errorArray;
                }

            }
        }while(!line.equals("") && in.hasNext());


        while(in.hasNextLine()){
            line = in.nextLine();
            overallLength += line.length();

            if(line.matches(".*(?<!((src=\")|(src =\")|(src= \")|(src = \")))( *)https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*).*")) {
                lMatcher = lPattern.matcher(line);

                while(lMatcher.find()) {
                    String link = lMatcher.group(0);
                    String[] lArray = link.split("\\.");

                    if(lArray.length > linkDots){
                        linkDots = lArray.length;
                    }
                    linkCount++;


                    if(!hasNewSite && currDate != null){
                        if (link.indexOf('/', link.indexOf('.')) <= 0) {
                            link = link.substring(link.indexOf('.') + 1, link.length() - 1);
                        } else {
                            link = link.substring(link.indexOf('.') + 1, link.indexOf('/', link.indexOf('.')));
                        }

                        if(!domains.contains(link)) {
                            long tempAge = getLinkAge(link, currDate);
                            if (tempAge < 365) {
                                hasNewSite = true;
                            }
                            domains.add(link);
                        }
                    }
                }

            }

            if(line.matches(".*<( *)img(.*)>.*")){
                if(line.matches(".*<( *)a(.*)href( *)=( *)\"(.+)\"(.*)>(.*)<(.*)img(.*)>(.*)</a>.*")){
                    imageLink ++;
                }
                else if(!lookingForImage && line.matches(".*<( *)a(.*)href( *)=( *)\".+\"(.*)>.*")){
                    lookingForImage = true;
                }
                else if(lookingForImage && line.matches(".*href( *)=( *)\".+\"(.*)<( *)img(.*)>.*")){
                    imageLink ++;
                    lookingForImage = false;
                }
                else{
                    lookingForImage = false;
                }
            }

            if(!misleadingLink && line.matches(".*<( *)a(.*)href( *)=( *)\"(.+)\"(.*)>(.+)\\.(.+)</a( *)>.*")){
                misleadingLink = true;
            }

            if(!mentionsCurrency && line.matches("(.*)[+-]?[0-9]{1,3}(?:[0-9]*(?:[.,][0-9]{2})?|(?:,[0-9]{3})*(?:\\.[0-9]{2})?|(?:\\.[0-9]{3})*(?:,[0-9]{2})?)(\\s?)(₹|INR|Rs|$|USD|US|GBP|£)(.*)")){
                mentionsCurrency = true;
            }

            if(!foundJS){
                foundJS = detectWords(line, jsWords);
            }
            if(!isUrgent){
                isUrgent = detectWords(line, urgencyWords);
            }
        }



        double[] resultsArray = {overallLength, linkCount, linkDots, imageLink, containsHTML?1:0, isUrgent?1:0, mentionsCurrency?1:0, foundJS?1:0, hasNewSite?1:0, misleadingLink?1:0, type};
        return resultsArray;
    }

    private static long getLinkAge(String link, Date today) {
        DateFormat format3 = new SimpleDateFormat("yyyy-dd-MM");

        try {

        }
        catch(Exception e){
            System.out.println("Malformed Link: " + link + " indexes of " + link.indexOf('.') + " or " + link.indexOf('/', link.indexOf('.')) + " are invalid");
            System.exit(1);
        }

        try {


            Socket socket = new Socket("whois.iana.org", 43);

            InputStreamReader inStream = new InputStreamReader(socket.getInputStream());
            BufferedReader in = new BufferedReader(inStream);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(link);

            String currLine = "";
            String linkDate = "";
            while((currLine = in.readLine()) != null){
                if(currLine.contains("created:")){
                    linkDate = currLine.substring(currLine.indexOf(':')+7, currLine.length()-1);
                    break;
                }
            }

            if(!linkDate.equals("")){
                Date createdDate = format3.parse(linkDate);
                return TimeUnit.MILLISECONDS.toDays(today.getTime() - createdDate.getTime());
            }
        }
        catch (Exception e){
            return -1;
        }
        return 0;

    }

    private static boolean detectWords(String line, String[] words){
        for (String word : words) {
            if (line.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static void printResults(double[] results){
        for(int i = 0; i < results.length-1; i++){
            System.out.println(inputTypes[i] + ": " + results[i]);
        }
    }


}
