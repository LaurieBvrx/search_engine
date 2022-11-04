package laurie_boveroux;

import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.compressors.gzip.*;

import java.io.*;
import java.util.*;
import org.apache.commons.io.*;




/**
 * Hello world!
 *
 */
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
        try{
            // Hello world !
            System.out.println( "Hello World!" );
            // get the path to the current directory
            String currentPath = new java.io.File(".").getCanonicalPath();
            String dirData = currentPath + "\\data\\";
            System.out.println("Current path : " + currentPath);
            System.out.println("Data path : " + dirData);
            String compressedFileName = "collection.tar.gz";

            // Extract tar.gz file
            long startTime = System.nanoTime();
            //String fileName = extractTarGzFile(dirData + compressedFileName, dirData);
            String fileName = "collection.tsv";
            long endTimeExtraction = System.nanoTime();
            System.out.println("Extraction time: " + (endTimeExtraction - startTime)/1000000000.0 + " seconds");

            // Parse tsv file
            Indexer indexer = new Indexer();
            //indexer.parseTsvFile(dirData + fileName, 1000000); 
            //indexer.mergeBlocks();
            //indexer.test();
            
            //Query Search
            //QuerySearch querySearch = new QuerySearch();
            //querySearch.loadLexiconIntoMemory();
            //querySearch.loadDocumentIndex();
        


        }
        catch(Exception e){
            System.err.println("Error: " + e.getMessage());
            System.out.println(e);
            e.printStackTrace();
        }
    }

}
