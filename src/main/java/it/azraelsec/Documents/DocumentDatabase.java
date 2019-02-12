package it.azraelsec.Documents;

import it.azraelsec.Server.User;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

public class DocumentDatabase implements Serializable {
    private static final long serialVersionUID = 1L;
    // docName, username
    private ArrayList<Document> documents;

    public DocumentDatabase() {
        documents = new ArrayList<>();
    }

    public void createNewDocument(String path, int sectionsNumber, String name, User creator) throws IOException {
        Document document;
        if(alreadyExists(name)) throw new IOException("Document's name already existing");
        else document = Document.createDocument(path, sectionsNumber, name, creator);
        documents.add(document);
    }

    private boolean alreadyExists(String name) {
        return getDocumentByName(name) != null;
    }

    public Document getDocumentByName(String documentName) {
        for(Document d : documents)
            if(d.getName().compareTo(documentName) == 0) return d;
        return null;
    }
}
