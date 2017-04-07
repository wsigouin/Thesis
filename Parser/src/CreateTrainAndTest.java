import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Creates Trainging and testing data for the NN to directly use
 * random seleciton of emails, created several times in order to do several trials
 * data is normalized based on the set
 */

public class CreateTrainAndTest {
    private static int NUM_INPUTS = 15;
    static double minCharacters = Double.MAX_VALUE;
    static double maxCharacters = 0;
    static double minLinks = Double.MAX_VALUE;
    static double maxLinks = 0;
    static double minDots = Double.MAX_VALUE;
    static double maxDots = 0;
    static double minDomains = Double.MAX_VALUE;
    static double maxDomains = 0;

    public static void main (String[] args) throws FileNotFoundException{
		//size and distribution of dataset, legacy, it is not fixed
        double[] testParams = {4000,50};

        double[][] tests = {
                testParams,
                testParams,
                testParams,
                testParams,
                testParams,
                testParams,
                testParams,
                testParams,
                testParams,
                testParams,
                testParams,
                testParams
    };


        File pone = new File("phishingData.txt");

        File lone = new File("hamDataEasy.txt");


        ArrayList<double[]> phish = new ArrayList<>();
        ArrayList<double[]> legit = new ArrayList<>();

		//create list for each email type
        String line;
        Scanner in = new Scanner(pone);
        int lineCount = 1;
        while(in.hasNextLine()){
            line = in.nextLine();
            phish.add(convertEmail(line));
            if(phish.get(phish.size() -1 ).length < 11){
                System.out.println("Broken A " + lineCount);
            }
            lineCount++;
        }

        lineCount = 1;
        in = new Scanner(lone);
        while(in.hasNextLine()){
            line = in.nextLine();
            legit.add(convertEmail(line));
            if(legit.get(legit.size() -1 ).length < 11){
                System.out.println("Broken B " + lineCount);
            }
            lineCount++;
        }


        in.close();
        int set = 1;
        for( double[] test : tests) {

			//training set
            double OverallSize = test[0];
            double size = OverallSize;
            double percentPhishing = test[1];
            PrintWriter out = new PrintWriter(
                            "emails/test/out/" + (int)OverallSize + "_" + (int)percentPhishing + "_" + set +"_train.data");
            ArrayList<double[]> pCurr = new ArrayList<>(phish);
            ArrayList<double[]> lCurr = new ArrayList<>(legit);
            ArrayList<double[]> outEmails = new ArrayList<>();

            double numPhishing = size * (percentPhishing/100);

            for(int i = 0; i < numPhishing; i++){
                int selection = (int)(Math.random()*(pCurr.size()));
                addItem(pCurr, outEmails, selection);

            }

            for(int i = 0; i < size-numPhishing; i++){
                int selection = (int)(Math.random()*(lCurr.size()));

                addItem(lCurr, outEmails, selection);
            }

            out.write((int)size + " " + NUM_INPUTS + " 1\n");


            for(int i = 0; i < size; i ++){
                int selection = (int)(Math.random()*(outEmails.size()));
                double[] datum = outEmails.get(selection);
                outEmails.remove(selection);
                out.write(infoArrayToString(datum, minCharacters, maxCharacters, minLinks, maxLinks, minDots, maxDots, minDomains, maxDomains));
            }
            out.close();

			//verification set
            out = new PrintWriter("emails/test/out/" + (int)OverallSize + "_" + (int)percentPhishing + "_" + set + "_validate.data");
            out.write("2000 " + NUM_INPUTS + " 1\n");
            outEmails = new ArrayList<>();
            minCharacters = Double.MAX_VALUE;
            maxCharacters = 0;
            minLinks = Double.MAX_VALUE;
            maxLinks = 0;
            minDots = Double.MAX_VALUE;
            maxDots = 0;
            minDomains = Double.MAX_VALUE;
            maxDomains = 0;

            for(int i = 0; i < 2000; i ++){
                if( i < 1000){
                    int selection = (int)(Math.random()*(pCurr.size()));
                    addItem(pCurr, outEmails, selection);
                }
                else{
                    int selection = (int)(Math.random()*(lCurr.size()));
                    addItem(lCurr, outEmails, selection);
                }
            }
            size = outEmails.size();
            for(int i = 0; i < size; i++){
                int selection = (int)(Math.random()*(outEmails.size()));
                double[] datum = outEmails.get(selection);
                outEmails.remove(selection);
                out.write(infoArrayToString(datum, minCharacters, maxCharacters, minLinks, maxLinks, minDots, maxDots, minDomains, maxDomains));
            }
            out.close();
			
			//testing set
            out = new PrintWriter("emails/test/out/" + (int)OverallSize + "_" + (int)percentPhishing + "_" + set++ +"_test.data");
            out.write("2000 " + NUM_INPUTS + " 1\n");
            outEmails = new ArrayList<>();
            minCharacters = Double.MAX_VALUE;
            maxCharacters = 0;
            minLinks = Double.MAX_VALUE;
            maxLinks = 0;
            minDots = Double.MAX_VALUE;
            maxDots = 0;
            minDomains = Double.MAX_VALUE;
            maxDomains = 0;

            for(int i = 0; i < 2000; i ++){
                if( i < 1000){
                    int selection = (int)(Math.random()*(pCurr.size()));
                    addItem(pCurr, outEmails, selection);
                }
                else{
                    int selection = (int)(Math.random()*(lCurr.size()));
                    addItem(lCurr, outEmails, selection);
                }
            }
            size = outEmails.size();
            for(int i = 0; i < size; i++){
                int selection = (int)(Math.random()*(outEmails.size()));
                double[] datum = outEmails.get(selection);
                outEmails.remove(selection);
                out.write(infoArrayToString(datum, minCharacters, maxCharacters, minLinks, maxLinks, minDots, maxDots, minDomains, maxDomains));
            }
            out.close();

        }



    }
	
	//convert email into a readable array
    public static double[] convertEmail(String line){
        StringTokenizer st = new StringTokenizer(line, " ");
        double[] out = new double[st.countTokens()];

        for(int i = 0; i < out.length; i++){
            out[i] = Double.parseDouble(st.nextToken());
        }
        return out;

    }
	
	//create output string from email data to place in data file
    public static String infoArrayToString(double[] datum, double minLines, double maxLines, double minLinks, double maxLinks, double minDots, double maxDots, double minDomains, double maxDomains){
        DecimalFormat format = new DecimalFormat("0.0000000000");
        return format.format((datum[0]-minLines)/(maxLines-minLines)) + " " +
                format.format((datum[1]-minLinks)/(maxLinks-minLinks)) + " " +
                format.format((datum[2]-minDots)/(maxDots-minDots)) + " " +
                format.format((datum[3]-minDomains)/(maxDomains-minDomains)) + " " +
                datum[4] + " " + datum[5] + " " + datum[6] + " " + datum[7] + " " + datum[8] + " " + datum[9] +  " " + datum[10] + " " + datum[11] + " " + datum[12] + " " + datum[13]+ " " + datum[14] +"\n"
                + datum[15] + "\n";
    }
	
	//add item to overall array
    public static void addItem(ArrayList<double[]> source, ArrayList<double[]> destination, int selection){
        destination.add(source.get(selection));
        double[] currEmailData = source.get(selection);
        if(currEmailData.length < NUM_INPUTS+1){
            System.out.println("PROBLEM");
        }

        source.remove(selection);

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

        if(currEmailData[3] > maxDomains){
            maxDomains = currEmailData[3];
        }
        else if(currEmailData[3] < minDomains){
            minDomains = currEmailData[3];
        }
    }
}
