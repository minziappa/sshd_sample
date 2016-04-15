package io.sample.sshd.utilities;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:springConfig.xml"})
public class SftpTest {

	private static EmbeddedSftpServer sftpServer = new EmbeddedSftpServer();
	
	@Autowired
	private Sftp sftp; 

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		if(!sftpServer.isRunning()) {
			sftpServer.setPort(8091);
			sftpServer.afterPropertiesSet();
			sftpServer.start();
		}

	}

	@Test
	public void testInsertSample() throws Exception {
		JSch jsch = new JSch();
		Session session = jsch.getSession("user", "localhost", 8091);
        session.setPassword("user");
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();
        channel.disconnect();
        
        session.disconnect();

		System.out.println("sftp");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
		if(sftpServer != null) {
			sftpServer.stop();
		}

	}
}
