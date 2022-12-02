package laurie_boveroux;

import java.io.*;
import java.util.*;

public class ListPointer{

    public String term;
    public int startIndex;
    public int endIndex;
    public int lengthBytes;
    public int[] docIdsArray;
    public int[] freqsArray;
    public int index;
    public int nbRelevantDocs;


    public ListPointer(String term,int startIndex,int endIndex) throws IOException {

        String currentPath = new java.io.File(".").getCanonicalPath();
        // File for the docId of the posting lists
        String IdPath = currentPath + "/InvertedIndexDocid.txt";
        RandomAccessFile fileDocIds = new RandomAccessFile(IdPath, "r");
        long lenDoc = fileDocIds.length();
        System.out.println("lenDoc inverted index docId : " + lenDoc);
        // File for the frequencies of the posting lists
        String freqs = currentPath + "/InvertedIndexFreq.txt";
        RandomAccessFile fileFreqs = new RandomAccessFile(freqs, "r");

        this.term = term;
        this.startIndex = startIndex;
        if (endIndex == -1){
            this.endIndex = (int) fileDocIds.length();
        }
        else{
            this.endIndex = endIndex;
        }
        this.lengthBytes = this.endIndex - this.startIndex;
        this.index = 0;

        // read the docIds: length*4 bytes from startIndex in fileDocIds   
        byte[] bytesId = new byte[lengthBytes];
        fileDocIds.seek(startIndex);
        fileDocIds.read(bytesId);
        fileDocIds.close();

        // read the frequencies: length*4 bytes from startIndex in fileFreqs
        byte[] bytesFreq = new byte[lengthBytes];
        fileFreqs.seek(startIndex);
        fileFreqs.read(bytesFreq);
        fileFreqs.close();        
  
        // docIds
        List<Integer> decodedDocIdsList = VBDecode(bytesId);
        this.docIdsArray = new int[decodedDocIdsList.size()];
        for (int i = 0; i < decodedDocIdsList.size(); i++){
            this.docIdsArray[i] = decodedDocIdsList.get(i);
        }

        // freqs
        List<Integer> decodedFreqsList = VBDecode(bytesFreq);
        this.freqsArray = new int[decodedFreqsList.size()];
        for (int i = 0; i < decodedFreqsList.size(); i++){
            this.freqsArray[i] = decodedFreqsList.get(i);
        }

        this.nbRelevantDocs = this.docIdsArray.length;
    }

    public int getLength() {
        return this.nbRelevantDocs;
    }

    public int getDocFreq() {
        return this.nbRelevantDocs;
    }

    public int getMaxDocId() {        
        return docIdsArray[nbRelevantDocs-1];
    }

    public int getFreq(int index){
        return freqsArray[index];
    }

    public static List<Integer> VBDecode(byte[] bytesTab){
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < bytesTab.length; i++){
            String intermediateNumber = "";
            while (bytesTab[i] < 0){ // 1xxxxxxx, read the next byte while the first bit is 1
                String binaryNum = Integer.toBinaryString(bytesTab[i] & 0x7F); // to binary by replacing the first bit by 0
                while (binaryNum.length() < 7){ // add 0 to the left if the binary is not 8 bits
                    binaryNum = "0" + binaryNum;
                }
                intermediateNumber = binaryNum + intermediateNumber;
                i++;
            }
            // last number
            String binaryNum = Integer.toBinaryString(bytesTab[i] & 0x7F);
            intermediateNumber = binaryNum + intermediateNumber;
            result.add(Integer.parseInt(intermediateNumber,2));
        }
        return result;        
    }
}