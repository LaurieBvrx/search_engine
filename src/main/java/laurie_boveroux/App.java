package laurie_boveroux;

import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.*;
import java.io.*;
import java.util.*;
import org.apache.commons.io.*;

public class App 
{
    // Function to extract the tar.gz file
    // Takes as input the path to the tar.gz file and the path to the output folder
    public static String extractTarGzFile(String tarGzFile, String destDir) throws IOException {

        TarArchiveInputStream tarIn = null;
        tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarGzFile)))); //
        TarArchiveEntry entry = (TarArchiveEntry) tarIn.getNextEntry(); // Get the first entry in the tar file
        String nameFile = entry.getName(); // Get the name of the file
        File file = new File(destDir, entry.getName()); // Create a new file with the name of the file in the tar.gz
        
        OutputStream out = null;
        out = new BufferedOutputStream(new FileOutputStream(file)); // Create a new output stream to write the file
        IOUtils.copy(tarIn, out); // Copy the file in the output stream
        IOUtils.closeQuietly(out); // Close the output stream
        IOUtils.closeQuietly(tarIn); // Close the tar input stream        

        return nameFile;
    }
   
    
    public static void main( String[] args ){
        // User interface
        try{
            String fileName = "";
            String currentPath = new java.io.File(".").getCanonicalPath();
            String dirData = currentPath + "/data/";
            boolean stemFlag = false;

            // Extract the collection if the user wants to
            String welcomeMsg = "\n\n\u001B[36m================================================\u001B[32m Welcome to Search Engine \u001B[36m================================================\u001B[0m" +
                                "\nPlease create a \u001B[33mdata\u001B[0m folder in the root directory, containing the collection of documents to process." +
                                "\nTo run this application smoothly, you may need the compressed \u001B[33mtar.gz\u001B[0m file in your `data` folder." +
                                "\n\u001B[36m==========================================================================================================================\u001B[0m" +
                                "\n\n=> Do you need to extract the collection? Please enter \u001B[33my\u001B[0m or \u001B[33mn\u001B[0m.";
            System.out.println(welcomeMsg);
            while(true){
                Scanner sc = new Scanner(System.in);
                String answer = sc.nextLine();
                if(answer.equals("y")){
                    String compressedFileName = "collection.tar.gz";
                    fileName = extractTarGzFile(dirData + compressedFileName, dirData);
                    System.out.println("\nThe file \u001B[35m" + fileName + "\u001B[0m has been extracted in the folder `data`.");
                    break;
                }
                else if(answer.equals("n")){
                    fileName = "collection.tsv";
                    System.out.println("Ok, let's go!");
                    break;
                }
                else{
                    System.out.println("Please enter \u001B[33my\u001B[0m or \u001B[33mn\u001B[0m");
                }
            }

            // Create the index and lexicon if needed
            String indexMsg = "\n\n=> Do you need to create the index and the lexicon? Please enter \u001B[33my\u001B[0m or \u001B[33mn\u001B[0m." +
                              "\n\t\033[3mIf \u001B[33mn\u001B[97m, we suppose that the indexes already exist." +
                              "\n\t(i.e. DocumentIndex.txt, InvertedIndexDocID.txt, InvertedIndexFreq.txt and Lexicon.txt)\033[0m";
            System.out.println(indexMsg);
            int nbDocProcessed = 0;
            while(true){
                Scanner sc = new Scanner(System.in);
                String answer = sc.nextLine();

                if(answer.equals("y")){

                    System.out.println("\n\n=> How many documents do you want to process? Please enter a number." +
                                       "\n\t\033[3mIf you want to process all the documents, please enter \u001B[33m-1\u001B[0m.");
                    int nbDocToProcess = 0;

                    // Verify that the user enters a number
                    while(true){
                        Scanner sc2 = new Scanner(System.in);
                        String answer2 = sc2.nextLine();                    
                        try{
                            nbDocToProcess = Integer.parseInt(answer2);
                            break;
                        }
                        catch(NumberFormatException e){
                            System.out.println("\u001B[31m!Invalid input!\u001B[31m");
                        }
                        // Verify that the user enters a positive number
                        if (nbDocToProcess == -1 || nbDocToProcess > 0){
                            break;
                        }
                        else{
                            System.out.println("Please enter a \033[4mstrictly positive\033[0m number or \033[4m-1\033[0m.");
                        }
                    }
                    // Create the index and the lexicon
                    Long startTime = (long) 0;
                    Indexer indexer = new Indexer("Lexicon.txt");
                    String stemmingMsg = "\n\n=> Would you like to use stemming? Please enter y or n.";
                    System.out.println(stemmingMsg);
                    while(true){
                        Scanner scStem = new Scanner(System.in);
                        String answerStem = scStem.nextLine();
                        if(answerStem.equals("y")){
                            stemFlag = true;
                            startTime = System.nanoTime();
                            nbDocProcessed = indexer.parseTsvFile(dirData + fileName, nbDocToProcess, stemFlag);
                            break;
                        }else if(answerStem.equals("n")){
                            stemFlag = false;
                            startTime = System.nanoTime();
                            nbDocProcessed = indexer.parseTsvFile(dirData + fileName, nbDocToProcess, stemFlag);
                            break;
                        }
                        else{
                            System.out.println("Please enter \u001B[33my\u001B[0m or \u001B[33mn\u001B[0m.");
                        }
                        indexer.mergeBlocks();
                        long endTime = System.nanoTime();
                        long duration = (endTime - startTime)/1000000000;
                        System.out.println("\n\t>> The index and the stemmed lexicon have been created in " + duration + " seconds.");
                    }
                }
                else if(answer.equals("n")){
                    nbDocProcessed = 8841822;
                    System.out.println("Ok, let's go!");
                    break;
                }
                else{
                    System.out.println("Please enter \u001B[33my\u001B[0m or \u001B[33mn\u001B[0m.");
                }
            }
            
            // Query search engine
            QuerySearch querySearch = new QuerySearch(nbDocProcessed);
            while(true){
                String queryMsg = "\n\n=> Please enter your query. " +
                                  "\n\t\033[3mIf you want to exit, please enter \u001B[33m-1\u001B[0m.";
                System.out.println(queryMsg);
                Scanner sc = new Scanner(System.in);
                String query = sc.nextLine();
                if(query.equals("-1")){
                    break;
                }
                else{
                    // Get the type of the query
                    String typeQuery = "";
                    String queryMsg2 = "\n\n=> Please enter the type of the query. " +
                                       "\n\t\033[3mPlease enter \u001B[33m1\u001B[0m \033[3mfor a conjunctive query, \u001B[33m2\u001B[0m \033[3mfor a disjunctive query\033[0m.";
                    System.out.println(queryMsg2);

                    // Verify that the user enters a number
                    while(true){
                        Scanner sc5 = new Scanner(System.in);
                        String answer2 = sc5.nextLine();
                        // Verify that the user enters a number
                        try{
                            int typeQueryInt = Integer.parseInt(answer2);
                            //int typeQueryInt = Integer.parseInt(answer2);
                            if(typeQueryInt == 1){
                                typeQuery = "conjunctive";
                                break;
                            }
                            else if(typeQueryInt == 2){
                                typeQuery = "disjunctive";
                                break;
                            }
                            else{
                                System.out.println("Please enter \u001B[33m1\u001B[0m or \u001B[33m2\u001B[0m.");
                            }
                        }catch (NumberFormatException ex) {
                            System.out.println("\n\u001B[31m!Invalid input!\u001B[0m");
                            System.out.println("Please enter \u001B[33m1\u001B[0m or \u001B[33m2\u001B[0m.");
                        }
                    }
                    // Beginning of the search
                    Long startTime = System.nanoTime();
                    String typeScore = "okapibm25"; //"tfidf", "okapibm25".
                    querySearch.executeQuery(typeQuery, query, stemFlag, typeScore);
                    Long endTime = System.nanoTime();
                    // in milliseconds
                    System.out.println("\n\t>> Search done in \u001B[33m" + (endTime - startTime)/1000000 + "\u001B[0m milliseconds");
                    //System.out.println("Query processed in " + (endTime - startTime)/1000000000 + " seconds");

                    String msg = "\n\n=> Do you want to print the relevant documents? " +
                                 "\n\t\033[3mPlease enter \u001B[33my\u001B[0m \033[3mor \u001B[33mn\u001B[0m.";
                    System.out.println(msg);
                    while(true){
                        Scanner sc3 = new Scanner(System.in);
                        String answer3 = sc3.nextLine();
                        if(answer3.equals("y")){
                            querySearch.printRelevantDocs();
                            break;
                        }
                        else if(answer3.equals("n")){
                            break;
                        }
                        else{
                            System.out.println("Please enter \u001B[33my\u001B[0m or \u001B[33mn\u001B[0m.");
                        }
                    }
                    querySearch.closeList();
                }
            }

        }
        catch(Exception e){
            System.err.println("\u001B[31mError: " + e.getMessage() + "\u001B[0m");
            System.out.println(e);
            e.printStackTrace();
        }
    }
}
