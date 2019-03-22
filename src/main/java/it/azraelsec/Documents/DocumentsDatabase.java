package it.azraelsec.Documents;

import it.azraelsec.Server.User;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DocumentsDatabase implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ArrayList<Document> documents;

    public DocumentsDatabase() {
        documents = new ArrayList<>();
    }

    public void createNewDocument(String path, int sectionsNumber, String name, User creator) throws IOException {
        Document document;
        if(alreadyExists(name)) throw new IOException("Document's name already existing");
        else document = Document.createDocument(path, sectionsNumber, name, creator);
        synchronized (documents) {
            documents.add(document);
        }
    }

    private boolean alreadyExists(String name) {
        return getDocumentByName(name) != null;
    }

    public Document getDocumentByName(String documentName) {
        synchronized (documents) {
            for (Document d : documents)
                if (d.getName().compareTo(documentName) == 0) return d;
            return null;
        }
    }

    public String[] getAllDocumentsNames(User user) {
        List<String> nameList = new ArrayList<>();
        synchronized (documents) {
            for(Document d : documents)
                if(d.canAccess(user))
                    nameList.add(d.getName());
        }
        return nameList.toArray(new String[0]);
    }
}
