package laurie_boveroux;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

public class ListPointer{

    public String term;
    public int startIndex;
    public int endIndex;
    public int length;
    public int[] docIdsArray;
    public int[] freqsArray;
    public int index;


    public ListPointer(String term,int startIndex,int endIndex) throws IOException {
        this.term = term;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.length = (endIndex - startIndex)/4;
        this.docIdsArray = new int[length];
        this.freqsArray = new int[length];
        this.index = 0;
        // print start and end index
        System.out.println("start: " + startIndex + " end: " + endIndex);

        // read the posting list (docIds and freqs) from the file
        String currentPath = new java.io.File(".").getCanonicalPath();
        //String docIds = currentPath + "/data/InvertedIndexDocid.txt";
        String IdPath = currentPath + "/InvertedIndexDocid.txt";
        RandomAccessFile fileDocIds = new RandomAccessFile(IdPath, "r");
        byte[] bytesId = new byte[length*4];
        // read legnth*4 bytes from startIndex in fileDocIds        
        fileDocIds.seek(startIndex);
        fileDocIds.read(bytesId);
        fileDocIds.close();

        //String freqs = currentPath + "/data/InvertedIndexFreq.txt";
        String freqs = currentPath + "/InvertedIndexFreq.txt";
        RandomAccessFile fileFreqs = new RandomAccessFile(freqs, "r");
        byte[] bytesFreq = new byte[length*4];
        // read legnth*4 bytes from startIndex in fileFreqs
        fileFreqs.seek(startIndex);
        fileFreqs.read(bytesFreq);
        fileFreqs.close();

        // convert the bytes to integers 4 by 4
        for (int i = 0; i < length; i++){
            // docIds
            byte[] bytesId4 = new byte[4];
            for (int j = 0; j < 4; j++){
                bytesId4[j] = bytesId[i*4+j];
            }
            // convert the bytes to int
            int docId = ByteBuffer.wrap(bytesId4).getInt();
            // add the docId to the array
            docIdsArray[i] = docId;
            // freqs
            byte[] bytesFreq4 = new byte[4];
            for (int j = 0; j < 4; j++){
                bytesFreq4[j] = bytesFreq[i*4+j];
            }
            // convert the bytes to int
            int freq = ByteBuffer.wrap(bytesFreq4).getInt();
            // add the freq to the array
            freqsArray[i] = freq;
        }


    }

    public int getLength() {
        return this.length;
    }

    public int getDocFreq() {
        return this.length;
    }

    public int getMaxDocId() {
        return docIdsArray[length-1];
    }

    public int getTermFreq(int index){
        return freqsArray[index];
    }

}