package laurie_boveroux;
import java.io.*;
import java.util.*;

import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.ByteBuffer;

public class Test{

    public static BufferedOutputStream invIndexDocidBuffer;
    public static BufferedOutputStream invIndexFreqBuffer;

    public Test() throws IOException{
        this.invIndexDocidBuffer = new BufferedOutputStream(new FileOutputStream("InvertedIndexDocid.txt"),4096 * 10000);
        this.invIndexFreqBuffer  = new BufferedOutputStream(new FileOutputStream("InvertedIndexFreq.txt"),4096 * 10000);
    }

    public static List<Integer> VBEncodeNumber(Integer n){
        List<Integer> result = new ArrayList<Integer>();
        while(true){
            result.add(n % 128);
            if (n < 128){
                break;
            }
            else{
                n = n / 128;
            }
        }
        return result;        
    }

    public static void VBEncode(List<Integer> list, Boolean flag) throws IOException{
        for (int i = 0; i < list.size(); i++){
            int n = list.get(i);
            String binary = Integer.toBinaryString(n);
            while (binary.length() < 7){ // add 0 to the left if the binary is not 8 bits
                binary = "0" + binary;
            }
            if (i == list.size() - 1){ // if it is the last number, add 0 to the left
                binary = "0" + binary;
            }else{ // else add 1 to the left
                binary = "1" + binary;
            }
            if (flag){
                invIndexDocidBuffer.write(Integer.parseInt(binary,2));
            }
            else{
                invIndexFreqBuffer.write(Integer.parseInt(binary,2));
            }
        }
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

    public static void main(String[] args) throws IOException{
        // Variable-bytes code, an algorithm to compress integer to bytes
        // https://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html
        Test test = new Test();
        List<Integer> listDocId = new ArrayList<Integer>();
        //add random numbers between 0 and 1000000
        for (int i = 0; i < 1000000; i++){
            listDocId.add((int)(Math.random() * 1000000));
        }
        // listDocId.add(6964361);

        List<Integer> listFreq = new ArrayList<Integer>();
        //add random numbers between 0 and 1000000
        for (int i = 0; i < 1000000; i++){
            listFreq.add((int)(Math.random() * 200));
        }
        //listFreq.add(7);

        for (int i = 0; i < listDocId.size(); i++){
            List<Integer> encodedDocId = new ArrayList<Integer>();
            List<Integer> encodedFreq = new ArrayList<Integer>();

            if (listDocId.get(i) > listFreq.get(i)){ // if the docid is greater than the frequency, adapt the frequency
                encodedDocId = VBEncodeNumber(listDocId.get(i));
                VBEncode(encodedDocId, true);
                int lenDocID = encodedDocId.size();
                encodedFreq = VBEncodeNumber(listFreq.get(i));
                while (encodedFreq.size() < lenDocID){
                    encodedFreq.add(0);
                }
                VBEncode(encodedFreq, false);                
            }
            else{ // if the frequency is greater than the docid, adapt the docid
                encodedFreq = VBEncodeNumber(listFreq.get(i));
                VBEncode(encodedFreq, false);
                int lenFreq = encodedFreq.size();                
                encodedDocId = VBEncodeNumber(listDocId.get(i));
                while (encodedDocId.size() < lenFreq){
                    encodedDocId.add(0);
                }
                VBEncode(encodedDocId, true);                
            }           
        }
        
        invIndexDocidBuffer.close();
        invIndexFreqBuffer.close();

        // read the file
        byte[] bytesTab = Files.readAllBytes(Paths.get("InvertedIndexDocid.txt"));
        List<Integer> decodedList = VBDecode(bytesTab);
        //System.out.println(decodedList);

        // read the file
        byte[] bytesTab2 = Files.readAllBytes(Paths.get("InvertedIndexFreq.txt"));
        List<Integer> decodedList2 = VBDecode(bytesTab2);
        //System.out.println(decodedList2);


    }
}