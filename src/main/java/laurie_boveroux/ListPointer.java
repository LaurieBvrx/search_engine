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

        String currentPath = new java.io.File(".").getCanonicalPath();
        // File for the docId of the posting lists
        String IdPath = currentPath + "/InvertedIndexDocid.txt";
        RandomAccessFile fileDocIds = new RandomAccessFile(IdPath, "r");
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
        // pritn start and end index
        System.out.println("in pointer initialisation");
        System.out.println("start index: " + this.startIndex);
        System.out.println("end index: " + this.endIndex);
        this.length = (this.endIndex - this.startIndex)/4;
        //this.docIdsArray = new int[length];
        //this.freqsArray = new int[length];
        this.index = 0;

        // print start and end index
        System.out.println("start: " + startIndex + " end: " + endIndex);

        // read the docIds: length*4 bytes from startIndex in fileDocIds   
        byte[] bytesId = new byte[length*4];
        fileDocIds.seek(startIndex);
        fileDocIds.read(bytesId);
        fileDocIds.close();

        // read the frequencies: length*4 bytes from startIndex in fileFreqs
        byte[] bytesFreq = new byte[length*4];
        fileFreqs.seek(startIndex);
        fileFreqs.read(bytesFreq);
        fileFreqs.close();

        // convert the bytes to integers 4 by 4
        // NEW: have to be replace by uncompress funtction
        // for (int i = 0; i < length; i++){
        //     // docIds
        //     byte[] bytesId4 = new byte[4];
        //     for (int j = 0; j < 4; j++){
        //         bytesId4[j] = bytesId[i*4+j];
        //     }
        //     // convert the bytes to int
        //     int docId = ByteBuffer.wrap(bytesId4).getInt();
        //     // add the docId to the array
        //     docIdsArray[i] = docId;
        //     // freqs
        //     byte[] bytesFreq4 = new byte[4];
        //     for (int j = 0; j < 4; j++){
        //         bytesFreq4[j] = bytesFreq[i*4+j];
        //     }
        //     // convert the bytes to int
        //     int freq = ByteBuffer.wrap(bytesFreq4).getInt();
        //     // add the freq to the array
        //     freqsArray[i] = freq;
        // }

        // Uncompress
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