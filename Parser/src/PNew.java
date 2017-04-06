import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

import static java.lang.Character.isDigit;

/**
    Main Parser class that reads each emails and parses them line by line, collecting information and output it to the files
    Outputs files to be used with the "CreateTrainAndTest" class located in another file
    Created by William Sigouin; last updated March 11th, 2017
 */
public class PNew {

    //Regular expressions here are defined for multiple uses, though they are in a Pattern/Matcher format, which cannot be easily used in all scenarioes
    private static int NUM_INPUTS = 15;
    private static Pattern lPattern = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z0-9]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)");
    private static Matcher lMatcher = null;

    private static Pattern aPattern = Pattern.compile("<( *)((a|A)|(h|H)(r|R)(=?)(e|E)(f|F)|(target=(3(d|D))?\"_blank\"))");
    private static Matcher aMatcher = null;

    private static Pattern endAPattern = Pattern.compile("<( *)/( *)(a|A)( *)");
    private static Matcher endAMatcher = null;

    private static Pattern hPattern = Pattern.compile("((h|H)(r|R)(=?)(e|E)(f|F))( *)=(3D|)(\"|\'|)");
    private static Matcher hMatcher = null;

    ;

    public static void main(String[] args) throws FileNotFoundException {
        parseAll("hamDataEasy.txt",  "emails/legit", 0);
    }

    /**
     * parseAll takes in the output file, the path to the emails, and whether or not they are phishing emails
     * By doing this, it creates files associated with each type of email for further processing
     */
    public static void parseAll(String outfile, String pathname, int type) throws FileNotFoundException {
        File[] folder = new File(pathname).listFiles();
        File outFile = new File(outfile);
        PrintWriter out = new PrintWriter(outFile);
        //format decimals
        DecimalFormat perFormat = new DecimalFormat("#0.##");


        double successCount = 0;
        double failCount = 0;
        for (File file : folder) {
            try {
                double[] data = parseEmail(file, type);
                if (data[0] != -1 && data[0] != 0) {
                    out.write(printResults(data));
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                //dataset contains malformed files at times, aborting these
                System.out.println(file.getName() + " ABORTED " + e.getMessage());
                e.printStackTrace();
                failCount++;
            }
            //Progress tracking
            if ((successCount + failCount) % 10 == 0) {
                System.out.println((successCount + failCount) + " of " + folder.length + " " + perFormat.format(successCount / folder.length * 100) + "% complete.");
            }
        }
        out.close();
    }

    //Single email parsing function, takes file and type to create an array of the data needed
    private static double[] parseEmail(File file, int type) throws FileNotFoundException {
        Scanner in = new Scanner(file);

        //various wordbanks
        String[] currencyWords = {
                "money", "Money", "MONEY",
                "pounds", "Pounds", "POUNDS",
                "dollars", "Dollars", "DOLLARS",
                "rupees", "Rupees", "RUPEES",
                "deal", "Deal", "DEAL",
                "free", "Free", "FREE"
        };

        String[] urgencyWords = {
                "instantly", "Instantly", "INSTANTLY",
                "straightaway", "Straightaway", "STRAIGHTAWAY",
                "directly", "Directly", "DIRECTLY",
                "urgently", "Urgently", "URGENTLY",
                "desperately", "Desperately", "DESPERATELY",
                "immediately", "Immediately", "IMMEDIATELY",
                "quickly", "Quickly", "QUICKLY"
        };

        String[] jsWords = {
                "javascript", "Javascript", "JavaScript", "<script>",
                "onclick", "onmouseover", "onchange", "onmouseout", "onkeydown", "onload", "onkeypressed"
        };


        //variables to track the outputs
        double linkCount = 0;
        double linkDots = 0;
        double overallLength = 0;
        boolean imageLink = false;
        boolean isUrgent = false;
        boolean mentionsCurrency = false;
        boolean contentFound = false;
        boolean foundJS = false;
        boolean exclusivelyHTML = false;
        boolean lookingForA = false;
        boolean dateFound = false;
        boolean hasNewSite = false;
        boolean misleadingLink = false;
        boolean badMessageID = false;
        boolean subjectFound = false;
        boolean urgentSubject = false;
        boolean foundMessageID = false;
        boolean IPBasedLink = false;
        boolean hereLink = false;


        ArrayList<String> domains = new ArrayList<>();

        String line = "";
        String contentType = "N/A";
        String date = "N/A";
        String subject = "";
        String messageID = "";
        Date currDate = null;
        String aString = "";

        //Date formats to check the age of links and emails
        DateFormat format1 = new SimpleDateFormat("EEE, d MMM yyyy");
        DateFormat format2 = new SimpleDateFormat("dd MMM yyyy");
        DateFormat format3 = new SimpleDateFormat("EEE, d MMM yy");


        do {
            if (in.hasNext()) {
                line = in.nextLine();
                if ((line.length() > 0) && line.charAt(line.length() - 1) == '=') {
                    line = line.substring(0, line.length() - 2);
                }
                try {
                    //Exclusiveley HTML check
                    if (!contentFound && line.contains("Content-Type:")) {
                        contentFound = true;
                        int semiPosition = line.indexOf(';');
                        int colPosition = line.indexOf(":", line.indexOf("Content-Type")) + 2;
                        if (semiPosition != -1) {
                            contentType = line.substring(colPosition, semiPosition);
                        } else {
                            contentType = line.substring(colPosition, line.length());
                        }
                        contentType = contentType.toLowerCase();
                        if (contentType.equals("text/html")) {
                            exclusivelyHTML = true;
                        }
                    }
                    //Subject capitalization check
                    if (!subjectFound && line.contains("Subject:")) {
                        subjectFound = true;
                        int semiPosition = line.indexOf(';');
                        int colPosition = line.indexOf(":", line.indexOf("Subject"));
                        if (semiPosition != -1) {
                            subject = line.substring(colPosition, semiPosition);
                        } else {
                            subject = line.substring(colPosition, line.length());
                        }
                        if (subject.equals(subject.toUpperCase()) || subject.contains("!")) {
                            urgentSubject = true;
                        }

                    }
                    //Message ID capitalization check
                    if (!foundMessageID && line.contains("Message-ID:")) {
                        subjectFound = true;
                        int addressPosition = line.indexOf('@');
                        int bracePosition = line.indexOf("<", line.indexOf("MessageID"));

                        messageID = line.substring(bracePosition + 1, addressPosition);

                        if (messageID.equals(messageID.toUpperCase())) {
                            badMessageID = true;
                        }

                    }
                    //Newly created domain check
                    if (!dateFound && line.contains("Date:")) {
                        int semiPosition = line.indexOf(';');
                        if (semiPosition >= 0) {
                            date = line.substring(line.indexOf(';') + 2, semiPosition);
                        } else {
                            date = line.substring(line.indexOf(':') + 2, line.length() - 1);
                        }
                        try {
                            if (isDigit(date.charAt(0))) {
                                currDate = format2.parse(date);
                            } else {
                                StringTokenizer st = new StringTokenizer(date, " ");
                                ArrayList<String> dateInfo = new ArrayList<>();
                                while (st.hasMoreTokens()) {
                                    dateInfo.add(st.nextToken());
                                }
                                if (dateInfo.get(3).length() > 2) {
                                    currDate = format1.parse(date);
                                } else {
                                    currDate = format3.parse(date);
                                }
                            }
                        } catch (ParseException e) {
                            date = "";
                        }
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    double[] errorArray = {-1};
                    return errorArray;
                }

            }
        } while (!line.equals("") && in.hasNext());


        while (in.hasNextLine()) {
            line = in.nextLine();
            overallLength++;


            //Garbage line that is thrown out, part of MIME format, unimportant and adds chance for false positives
            if (line.matches("(-+)([0-9]|[A-Z])+$")) continue;

            if (line.matches(".*(?<!((src=\")|(src =\")|(src= \")|(src = \")))( *)https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z0-9]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*).*")) {
                lMatcher = lPattern.matcher(line);
                while (lMatcher.find()) {
                    String link = lMatcher.group(0);
                    String[] lArray = link.split("\\.");

                    if (lArray.length > linkDots) {
                        linkDots = lArray.length;
                    }
                    linkCount++;


                    if (link.indexOf('/', link.indexOf("//") + 2) <= 0) {
                        link = link.substring(link.indexOf("//") + 2, link.length() - 1);
                    } else {
                        try {
                            link = link.substring(link.indexOf("//") + 2, link.indexOf('/', link.indexOf('.')));
                        } catch (Exception e) {
                            System.out.println(link);
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                    if (!domains.contains(link)) {
                        if (!hasNewSite && currDate != null) {
                            long tempAge = getLinkAge(link, currDate);
                            if (tempAge < 365) {
                                hasNewSite = true;
                            }
                        }
                        if(!IPBasedLink && link.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}")){
                            IPBasedLink = true;
                        }
                        domains.add(link);
                    }

                }

            }

            //most complex part of parser, to avoid malformed HTML multiple lines are stored to look for hidden links and images with links
            if ((!misleadingLink || !imageLink) && (line.matches(".*<( *)(a|A|(target=3d\"_blank\"))(.*)") || lookingForA)) {
                if (!lookingForA && line.matches(".*<( *)(a|A|(target=3d\"_blank\"))(.*).*") && aString.equals("")) {
                    aMatcher = aPattern.matcher(line);
                    aMatcher.find();
                    if (line.matches(".*<( *)\\/( *)(a|A)( *)>.*")) {
                        endAMatcher = endAPattern.matcher(line);
                        aString = line.substring(aMatcher.start(), line.length() - 1);
                        endAMatcher.find();
                        if (endAMatcher.end() < aMatcher.start()) {
                            lookingForA = true;
                        }
                    } else {
                        aString = line.substring(aMatcher.start(), line.length() - 1);

                        lookingForA = true;
                    }
                } else if (lookingForA) {
                    if (line.matches(".*<( *)\\/( *)(a|A)( *)>.*")) {
                        aString += line;
                        lookingForA = false;
                    } else {
                        aString += line;
                    }
                }

                if (!aString.equals("") && !lookingForA) {
                    try {
                        aMatcher = aPattern.matcher(aString);
                        endAMatcher = endAPattern.matcher(aString);
                        hMatcher = hPattern.matcher(aString);
                        String outLink = "";
                        String inLink = "";
                        boolean h = hMatcher.find();
                        boolean a = aMatcher.find();
                        boolean e = endAMatcher.find();
                        if (h && a && e) {
                            while (endAMatcher.start() < aMatcher.start()) {
                                endAMatcher.find();
                            }
                            outLink = aString.substring(hMatcher.end(), aString.indexOf("\"", hMatcher.end()) != -1 ? aString.indexOf("\"", hMatcher.end()) : aString.indexOf("\'", hMatcher.end()));
                            if (aString.indexOf(">", aMatcher.end() + 1) > endAMatcher.start()) {
                                inLink = aString.substring(aString.indexOf("\"", hMatcher.end()) - 1, endAMatcher.start());
                            } else {
                                inLink = aString.substring(aString.indexOf(">", aMatcher.end()) + 1, endAMatcher.start());
                            }
                            inLink.trim();

                            if (inLink.matches(".*(?<!((src=\")|(src =\")|(src= \")|(src = \")))( *)https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z0-9]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*).*")) {
                                lMatcher = lPattern.matcher(inLink);
                                lMatcher.find();
                                String actualLink = lMatcher.group(0);
                                if (!actualLink.equals(outLink)) {
                                    misleadingLink = true;
                                }
                            }

                            if (inLink.matches("<( *)((i|I)(m|M))(.*)>")) {
                                imageLink = true;
                            }

                            aString = "";
                            lookingForA = false;
                        }
                        //debugging
                        else {
                            System.out.println(file.getName() + " ERROR READING ANCHOR " + (a ? 1 : 0) + " " + (h ? 1 : 0) + " " + (e ? 1 : 0) + "\n" + aString);
                        }
                    }
                    //debugging
                    catch (Exception e) {
                        System.out.println("Critical Error Reading Anchor: " + aString);
                    }


                }
            }

            //final checks for simpler aspects
            if (!mentionsCurrency && !line.contains("<") && !line.contains(">")
                    //currency check
                    && (line.matches("(.*)[+-]?[0-9]{1,3}(?:[0-9]*(?:[.,][0-9]{2})?|(?:,[0-9]{3})*(?:\\.[0-9]{2})?|(?:\\.[0-9]{3})*(?:,[0-9]{2})?)(\\s?)(₹|INR|Rs|\\$|USD|US|GBP|£)(.*)")
                    || detectWords(line, currencyWords))) {
                mentionsCurrency = true;
            }


            if (!foundJS) {
                foundJS = detectWords(line, jsWords);
            }
            if (!isUrgent) {
                isUrgent = detectWords(line, urgencyWords);
            }
            if(!hereLink && line.matches("(?i:.*click( )?here(\\.)?.*)")){
                hereLink = true;
            }


        }


        double[] resultsArray = {overallLength, linkCount, linkDots, domains.size(),
                imageLink ? 1 : 0, exclusivelyHTML ? 1 : 0, isUrgent ? 1 : 0,
                mentionsCurrency ? 1 : 0, foundJS ? 1 : 0, hasNewSite ? 1 : 0, misleadingLink ? 1 : 0,
                urgentSubject ? 1 : 0, badMessageID ? 1 : 0, IPBasedLink ? 1 : 0, hereLink ? 1 : 0,
                type};
        return resultsArray;
    }

    //compare link age to email send date
    private static long getLinkAge(String link, Date today) {
        DateFormat format3 = new SimpleDateFormat("yyyy-dd-MM");

        try {


            Socket socket = new Socket("whois.iana.org", 43);

            InputStreamReader inStream = new InputStreamReader(socket.getInputStream());
            BufferedReader in = new BufferedReader(inStream);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(link);

            String currLine = "";
            String linkDate = "";
            while ((currLine = in.readLine()) != null) {
                if (currLine.contains("created:")) {
                    linkDate = currLine.substring(currLine.indexOf(':') + 7, currLine.length() - 1);
                    break;
                }
            }

            if (!linkDate.equals("")) {
                Date createdDate = format3.parse(linkDate);
                return TimeUnit.MILLISECONDS.toDays(today.getTime() - createdDate.getTime());
            }
        } catch (Exception e) {
            return -1;
        }
        return 0;

    }

    //word bank comparison
    private static boolean detectWords(String line, String[] words) {
        for (String word : words) {
            if (line.contains(word)) {
                return true;
            }
        }
        return false;
    }

    //debugging
    private static String printResults(double[] results) {
        String out = "";
        for (int i = 0; i < results.length; i++) {
            out += (results[i] + " ");
        }
        return out + "\n";
    }
}
