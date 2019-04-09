package it.azraelsec.Documents;

import it.azraelsec.Server.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@code Section} class represents a {@code Document} section and contains a portion of the
 * document's itself.
 * <p>
 * Each {@code Section} is stored individually on a different file.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class Section implements Serializable {
    private static final long serialVersionUID = 1L;
    private User userOnEditing;
    private ReentrantLock lock;
    private String filePath;

    /**
     * Initializes the {@code Section} object based on the rootpath ({@code Document}) and its filename.
     *
     * @param rootPath  root file path
     * @param filename  section filename
     */
    Section(String rootPath, String filename)  {
        userOnEditing = null;
        filePath = rootPath + "/" +  filename + ".section";
        lock = new ReentrantLock();
    }

    /**
     * Gets the {@code Section}'s file path.
     *
     * @return  file path
     */
    String getFilePath() {
        return filePath;
    }

    /**
     * Gets the {@code User} that is editing the {@code Section}.
     *
     * @return  the {@code User} reference or null if no one is editing this {@code Section}
     */
    public User getUserOnEditing() {
        lock.lock();
        User onEditing = this.userOnEditing;
        lock.unlock();
        return onEditing;
    }

    /**
     * Tries to set the on editing {@code User} reference.
     *
     * @param user  user reference
     * @return  true if has been possible to set the on editing status, false otherwise
     */
    public boolean tryToSetEditing(User user) {
        if(lock.tryLock()) {
            if(userOnEditing != null) {
                if(user == null) userOnEditing = null;
                else {
                    lock.unlock();
                    return false;
                }
            }
            else userOnEditing = user;
            lock.unlock();
            return true;
        }
        return false;
    }

    /**
     * Gets the {@code InputStream} to read the {@code Section} content.
     *
     * @return  the input {@code InputStream}
     * @throws IOException  if I/O error occurs
     */
    public InputStream getFileInputStream() throws IOException{
        FileChannel fileChannel = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ);
        return Channels.newInputStream(fileChannel);
    }

    /**
     * Gets the {@code OutputStream} to fill the {@code Section} with a new content.
     *
     * @return  the output {@code OutputStream}
     * @throws IOException  if I/O error occurs
     */
    public OutputStream getWriteStream() throws IOException {
        FileChannel fileChannel = FileChannel.open(Paths.get(filePath), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        return Channels.newOutputStream(fileChannel);
    }
}
