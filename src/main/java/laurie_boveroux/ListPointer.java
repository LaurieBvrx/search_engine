package laurie_boveroux;

import java.io.*;
import java.util.*;

public class ListPointer{

    public String term;
    public int startIndex;
    public int endIndex;

    public ListPointer(String term,int startIndex,int endIndex) throws IOException {
        this.term = term;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
}