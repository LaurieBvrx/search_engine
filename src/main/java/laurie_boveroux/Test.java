package laurie_boveroux;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
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

    public static Integer gammaEncode(int n, int length, Boolean last) throws IOException {
        int ceiling = 0;
        String filler = "";

        System.out.println("\nOg DocId: "+ n);
        // Convert the number to binary
        String binary = Integer.toBinaryString(n);
        // Compute the number of bits needed to represent the binary number
        int numBits = binary.length();
        length = length + (numBits*2);
        System.out.println("Length: "+ length);

        //print length
        System.out.println("Num bits: " +numBits);

        // Compute the unary representation of the number of bits
        String unary = "1".repeat(numBits);

        if (last && !(length % 8 == 0)){
            ceiling = (int) (8*(Math.ceil(Math.abs(length/8)))) +8 ;
            System.out.println("Ceiling: " + ceiling + " \tFiller: " + (ceiling-length));
            filler = "0".repeat(ceiling-length);
        }

        // Write the concatenation of the unary representation and the binary number
        binary = unary+"0"+binary+filler;

        System.out.println("Gamma Encoding: " + binary);
//        System.out.println("Parsed long: " + Long.parseLong(binary, 2));
        //invIndexDocidBuffer.write((int) Long.parseLong(binary, 2));

        byte[] bytes = binary.getBytes();
        invIndexDocidBuffer.write(bytes);
        return length;

    }

    public static List<Integer> gammaDecodeList(byte[] bytesTab) throws IOException {
        // Initialize an empty list to store the decoded numbers
        List<Integer> numbers = new ArrayList<>();
        String docIdBinary = "";

        // Keep track of the current position in the encoded string
                int pos = 0;
                // Loop until we reach the end of the bytesTab
                while (pos < bytesTab.length) {

                    int unary = 0;
                    // Count the number of 1s in the unary representation
                    while (bytesTab[pos] == '1') {
                        //print bytestap in pos
                        unary++;
                        pos++;
                    }
                    // Skip the 0 in the unary representation
                    pos++;
                    for (int i =0; i < unary; i++) {
                        //convert ascii to binary
                        int numericValue = Character.getNumericValue(bytesTab[pos]);
                        //System.out.println("Pos: " + pos + "\tNumeric Value: " + numericValue);
                        //convert to string
                        docIdBinary += Integer.toString(numericValue);
                        //print binary
                        //System.out.println("Binary: " + docIdBinary);
                        pos++;

                    }

                    // Convert the binary representation to an integer
                    if (docIdBinary.length() > 0) {
                        numbers.add(Integer.parseInt(docIdBinary, 2));
                        docIdBinary = "";
                    }
                }

        // Return the list of decoded numbers
        return numbers;
    }

    public static Integer unaryEncode(int n, String separator, int length, Boolean last) throws IOException {
        int ceiling = 0;
        String filler = "";

        System.out.println("\nOg Freq: "+ n);

        // Compute the unary representation of the number

        String unary = ("1").repeat(n);

        length = length + unary.length()+1;
        System.out.println("Length: "+ length);

        if (last && !(length % 8 == 0)){
            ceiling = (int) (8*(Math.ceil(Math.abs(length/8)))) +8 ;
            System.out.println("Ceiling: " + ceiling + " \tFiller: " + (ceiling-length));
            filler = "0".repeat(ceiling-length);
        }

        String result = unary+separator+filler;
        System.out.println("Unary Encoding: " + result);
        //System.out.println("Parsed long: " + Long.parseLong(result, 2));

        //convert and write bytes to invIndexFreqBuffer
        byte[] bytes = result.getBytes();
        invIndexFreqBuffer.write(bytes);

        return length;
    }

    public static List<Integer> unaryDecodeList(byte[] bytesTab2) {
        // Initialize an empty list to store the decoded numbers
        List<Integer> numbers = new ArrayList<>();

        // Keep track of the current position in the encoded string
        int pos = 0;

        // Loop until we reach the end of the encoded string
        while (pos < bytesTab2.length) {

            int unary = 0;
            // Count the number of 1s in the unary representation
            while (bytesTab2[pos] == '1') {
                //print bytestap in pos
                unary++;
                pos++;
            }

            //add unary to number list
            if (unary > 0) {
                numbers.add(unary);
            }

            // Skip the 0 in the unary representation
            pos++;
        }

        // Return the list of decoded numbers
        return numbers;
    }

    public static void main(String[] args) throws IOException{
        // Variable-bytes code, an algorithm to compress integer to bytes
        // https://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html
        Test test = new Test();
        List<Integer> listDocId = new ArrayList<Integer>();
        String postingListDocId = "";
        //store the size of the posting list to know how many filler 0s to add in the end
        int sizePostingListGamma = 0;
        int sizePostingListUnary = 0;
        Boolean last = false;

        //add random numbers between 0 and 1000000
        for (int i = 0; i < 5; i++){
            listDocId.add((int)(Math.random() * 10000000));
        }

        List<Integer> listFreq = new ArrayList<Integer>();

        //add random numbers between 0 and 1000000
        for (int i = 0; i < 5; i++){
            listFreq.add((int)(Math.random() * 200));
        }

        //Go through every element in the posting list
        for (int i = 0; i < listDocId.size(); i++) {
            //check if last element
            if (i + 1 >= listDocId.size()) {
                last = true;
            }

            sizePostingListGamma = gammaEncode(listDocId.get(i), sizePostingListGamma, last);

            sizePostingListUnary = unaryEncode(listFreq.get(i),"0", sizePostingListUnary, last);

        }

        invIndexDocidBuffer.close();
        invIndexFreqBuffer.close();

        // read the file
        byte[] bytesTab = Files.readAllBytes(Paths.get("InvertedIndexDocid.txt"));
        // decode the file
        List<Integer> decodedList = gammaDecodeList(bytesTab);
        System.out.println("DocId List: " + decodedList);

        // read the file
        byte[] bytesTab2 = Files.readAllBytes(Paths.get("InvertedIndexFreq.txt"));
        // decode the file
        List<Integer> decodedList2 = unaryDecodeList(bytesTab2);
        System.out.println("Freq List: " + decodedList2);

    }

//    public static void main(String[] args) throws IOException{
//        // Variable-bytes code, an algorithm to compress integer to bytes
//        // https://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html
//        Test test = new Test();
//        List<Integer> listDocId = new ArrayList<Integer>();
//        //add random numbers between 0 and 1000000
//        for (int i = 0; i < 1000000; i++){
//            listDocId.add((int)(Math.random() * 1000000));
//        }
//        // listDocId.add(6964361);
//
//        List<Integer> listFreq = new ArrayList<Integer>();
//        //add random numbers between 0 and 1000000
//        for (int i = 0; i < 1000000; i++){
//            listFreq.add((int)(Math.random() * 200));
//        }
//        //listFreq.add(7);
//
//        for (int i = 0; i < listDocId.size(); i++){
//            List<Integer> encodedDocId = new ArrayList<Integer>();
//            List<Integer> encodedFreq = new ArrayList<Integer>();
//
//            if (listDocId.get(i) > listFreq.get(i)){ // if the docid is greater than the frequency, adapt the frequency
//                encodedDocId = VBEncodeNumber(listDocId.get(i));
//                VBEncode(encodedDocId, true);
//                int lenDocID = encodedDocId.size();
//                encodedFreq = VBEncodeNumber(listFreq.get(i));
//                while (encodedFreq.size() < lenDocID){
//                    encodedFreq.add(0);
//                }
//                VBEncode(encodedFreq, false);
//            }
//            else{ // if the frequency is greater than the docid, adapt the docid
//                encodedFreq = VBEncodeNumber(listFreq.get(i));
//                VBEncode(encodedFreq, false);
//                int lenFreq = encodedFreq.size();
//                encodedDocId = VBEncodeNumber(listDocId.get(i));
//                while (encodedDocId.size() < lenFreq){
//                    encodedDocId.add(0);
//                }
//                VBEncode(encodedDocId, true);
//            }
//        }
//
//        invIndexDocidBuffer.close();
//        invIndexFreqBuffer.close();
//
//        // read the file
//        byte[] bytesTab = Files.readAllBytes(Paths.get("InvertedIndexDocid.txt"));
//        List<Integer> decodedList = VBDecode(bytesTab);
//        //System.out.println(decodedList);
//
//        // read the file
//        byte[] bytesTab2 = Files.readAllBytes(Paths.get("InvertedIndexFreq.txt"));
//        List<Integer> decodedList2 = VBDecode(bytesTab2);
//        //System.out.println(decodedList2);
//
//
//    }
}