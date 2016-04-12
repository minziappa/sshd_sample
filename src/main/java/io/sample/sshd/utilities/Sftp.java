package io.sample.sshd.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Component
public class Sftp {

	final Logger logger = LoggerFactory.getLogger(Sftp.class);

	private JSch jsch = null;
    private Session session = null;
    private Channel channel = null;
    private ChannelSftp channelSftp = null;
    private String userName;
    private String password;
    private String host;
    private int port;

    private static Sftp instance = null;

    private Sftp(String host, String userName, String password, int port) {
        this.userName = userName;
        this.password = password;
        this.host = host;
        this.port = port;
        this.jsch = new JSch();
    }

    public static Sftp getInstance(String host, String userName, String password, int port) throws JSchException {
        if (instance == null) {
            instance = new Sftp(host, userName, password, port);
        }
        instance.connect();
        return instance;
    }

    /**
     * Connect with SFTP
     * 
     * @throws SftpException
     */
    public void connect() throws JSchException {
    	if(this.userName == null || this.password == null 
    			|| this.host == null || this.port < 1) {
    		throw new JSchException("You need to set configuration.");
    	}

        try {
            session = jsch.getSession(this.userName, this.host, this.port);
            session.setPassword(this.password);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            channel = session.openChannel("sftp");
            channel.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }

        channelSftp = (ChannelSftp) channel; 
    }

    /**
     * Pushes local file to remote location
     * 
     * @param localFile String representation of filePath + filename
     * @param remoteFile String representation of filePath + filename
     * @throws SftpException, IOException
     */
    public void upload(String localFile, String remoteFile) 
    		throws SftpException, IOException {

        FileInputStream in = null;
        try {
            in = new FileInputStream(localFile);
            channelSftp.put(in, remoteFile);
        } catch (SftpException e) {
            logger.error("SftpException", e);
            throw e;
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException", e);
            throw e;
        } finally {
        	if(in != null) {
                in.close();
        	}
        }
    }

    /**
     * Get files from remote location.
     * 
     * @param localFile String representation of filePath + filename
     * @param remoteFile String representation of filePath + filename
     * @throws SftpException, IOException
     */
    public void download(String remoteFile, String localFile) 
    		throws SftpException, IOException {

        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = channelSftp.get(remoteFile);
        } catch (SftpException e) {
            logger.error("SftpException", e);
            throw e;
        }

        try {
            out = new FileOutputStream(new File(localFile));
            int len;
            while ((len = in.read()) != -1) {
                out.write(len);
            }
        } catch (IOException e) {
            logger.error("IOException", e);
            throw e;
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Closes connection.
     */
    public void closeConnection() {
    	if(channelSftp != null) {
            channelSftp.exit();
    	}
    	if(channel != null) {
    		channel.disconnect();
    	}

    }
}
