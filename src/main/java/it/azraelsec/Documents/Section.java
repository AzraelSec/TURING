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

public class Section implements Serializable {
    private static final long serialVersionUID = 1L;
    private transient User userOnEditing;
    private transient ReentrantLock lock;
    private String filepath;

    public Section(String rootPath, String filename)  {
        userOnEditing = null;
        filepath = rootPath + "/" +  filename + ".section";
        lock = new ReentrantLock();
    }

    public String getFilePath() {
        return filepath;
    }

    public User getUserOnEditing() {
        lock.lock();
        User onEditing = this.userOnEditing;
        lock.unlock();
        return onEditing;
    }

    public boolean tryToSetEditing(User user) {
        if(lock.tryLock()) {
            if(userOnEditing != null) {
                if(user == null)
                    userOnEditing = user;
                else return false;
            }
            else userOnEditing = user;
            lock.unlock();
            return true;
        }
        return false;
    }

    public InputStream getFileInputStream() throws IOException{
        FileChannel fileChannel = FileChannel.open(Paths.get(filepath), StandardOpenOption.READ);
        return Channels.newInputStream(fileChannel);
    }

    public OutputStream getWriteStream() throws IOException {
        FileChannel fileChannel = FileChannel.open(Paths.get(filepath), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        return Channels.newOutputStream(fileChannel);
    }
}
