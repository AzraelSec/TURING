package it.azraelsec.Documents;

import it.azraelsec.Server.User;

import java.io.*;
import java.util.ArrayList;
import java.util.Vector;

/**
 * The {@code Document} class represents a document managed by TURING system.
 * Each document is made up by a number of {@code Section}s and is stored on the {@code Server} as a directory
 * which contains a number of different files.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class Document implements Serializable {
    private static final long serialVersionUID = 1L;
    private String documentName;
    private ArrayList<Section> sections;
    private User owner;
    private ArrayList<User> modifiers;

    /**
     * Creates a new {@code Document} storing its {@code Section}s' references, its name and the owner {@code User}'s
     * reference.
     * <p>
     * This constructor is not used by the outside. In fact, the {@code Document#createDocument} method should
     * be used instead.
     *
     * @param sections  sections' list
     * @param name  document's name
     * @param owner owner user
     */
    private Document(ArrayList<Section> sections, String name, User owner){
        documentName = name;
        this.sections = sections;
        this.owner = owner;
        modifiers = new ArrayList<>();
    }

    /**
     * Gets the document's name.
     *
     * @return  document's name
     */
    public String getName() {
        return documentName;
    }

    /**
     * Creates a new {@code Document}.
     *
     * @param directory output directory path
     * @param sectionsNumber    number of document's sections
     * @param name  document's name
     * @param owner owner user
     * @return  the new request document
     * @throws IOException  if file I/O error occurs
     */
    static Document createDocument(String directory, int sectionsNumber, String name, User owner) throws IOException {
        String documentLocation = directory + "/" + name;
        File document = new File(documentLocation);
        if(!document.exists() || !document.isDirectory())
            if(!document.mkdir()) throw new IOException("Document: Impossible to create a document");
        ArrayList<Section> sections = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        for(int i = 0; i < sectionsNumber; i++) {
            Section sec = new Section(documentLocation, String.valueOf(timestamp + i));
            sections.add(sec);
            File sectionFile = new File(sec.getFilePath());
            sectionFile.createNewFile();
        }
        return new Document(sections, name, owner);
    }

    /**
     * Gets the requested {@code Section}.
     *
     * @param index the section's number
     * @return  the requested section
     */
    public Section getSection(int index) {
        try {
            return sections.get(index);
        }
        catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    /**
     * Verifies if the requesting {@code User} has permissions to access this {@code Document}.
     *
     * @param user  requesting user
     * @return  true if user can access the this file, false otherwise
     */
    public boolean canAccess(User user) {
        synchronized (modifiers) {
            return modifiers.contains(user) || owner.equals(user);
        }
    }

    /**
     * Add input {@code User} to the modifiers' list.
     *
     * @param user  user to add to the allowed list
     */
    public void addModifier(User user) {
        synchronized (modifiers) {
            if (!modifiers.contains(user)) modifiers.add(user);
        }
    }

    /**
     * Checks if the input {@code User} is the {@code Document} owner.
     *
     * @param user  user reference
     * @return  true if {@code user} is this document owner
     */
    public boolean isCreator(User user) {
        return owner.equals(user);
    }

    /**
     * Gets an {@code InputStream} object that streams the entire {@code Document} content.
     *
     * @return  {@code InputStream} object
     * @throws IOException  if an I/O error occurs
     */
    public InputStream getDocumentInputStream() throws IOException {
        Vector<InputStream> secISVector = new Vector<>();
        for(Section sec : sections)
            secISVector.add(sec.getFileInputStream());
        SequenceInputStream docIS = new SequenceInputStream(secISVector.elements());
        return docIS;
    }

    /**
     * Gets the on editing {@code Section}s' list.
     *
     * @return  array of on editing sections' name
     */
    public String[] getOnEditingSections() {
        ArrayList<String> onEditingSections = new ArrayList<>();
        for(int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            if(section.getUserOnEditing() != null)
                onEditingSections.add(String.valueOf(i));
        }
        return onEditingSections.toArray(new String[0]);
    }
}
