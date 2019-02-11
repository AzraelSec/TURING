package it.azraelsec;

import java.nio.file.Path;
import java.util.ArrayList;

public class Document {
    private String name;
    private ArrayList<Path> sections;

    public Document(int sectionNumber, String name){
        this.name = name;
        sections = new ArrayList<>(sectionNumber);
    }
}
