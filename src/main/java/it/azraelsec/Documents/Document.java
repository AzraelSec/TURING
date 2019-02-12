package it.azraelsec.Documents;

import it.azraelsec.Server.Server;
import it.azraelsec.Server.User;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Document implements Serializable {
    private static final long serialVersionUID = 1L;
    private String documentName;
    private String path;
    private ArrayList<Section> sections;
    private User owner;
    private ArrayList<User> modifiers;

    private Document(String directory, ArrayList<Section> sections, String name, User owner){
        this.path = directory + "/" + name;
        documentName = name;
        this.sections = sections;
        this.owner = owner;
        modifiers = new ArrayList<>();
    }

    public String getName() {
        return documentName;
    }

    public static Document createDocument(String directory, int sectionsNumber, String name, User owner) throws IOException {
        String documentLocation = directory + "/" + name;
        File document = new File(documentLocation);
        if(!document.exists() || !document.isDirectory())
            if(!document.mkdir()) throw new IOException("Document: Impossible to create a document");
        ArrayList<Section> sections = new ArrayList<>();
        for(int i = 0; i < sectionsNumber; i++) {
            Section sec = new Section(documentLocation);
            sections.add(sec);
            File sectionFile = new File(sec.getFilePath());
            sectionFile.createNewFile();
        }
        return new Document(directory, sections, name, owner);
    }

    public Section getSection(int index) {
        try {
            return sections.get(index);
        }
        catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    public Iterator<Section> getSectionsIterator() {
        return sections.iterator();
    }

    synchronized public boolean canAccess(User user) {
        return modifiers.contains(user) || owner.equals(user);
    }

    synchronized public void addModifier(User user) {
        if(!modifiers.contains(user)) modifiers.add(user);
    }
}
