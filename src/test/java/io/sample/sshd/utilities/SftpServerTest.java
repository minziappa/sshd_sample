package io.sample.sshd.utilities;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Collections;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.util.Base64;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StreamUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:springConfig.xml"})
public class SftpServerTest {

	private static SshServer sshd = SshServer.setUpDefaultServer();

	private static PublicKey decodePublicKey() throws Exception {
		InputStream stream = new ClassPathResource("keys/sftp_rsa.pub").getInputStream();
		byte[] decodeBuffer = Base64.decodeBase64(StreamUtils.copyToByteArray(stream));
		ByteBuffer bb = ByteBuffer.wrap(decodeBuffer);
		int len = bb.getInt();
		byte[] type = new byte[len];
		bb.get(type);
		if ("ssh-rsa".equals(new String(type))) {
			BigInteger e = decodeBigInt(bb);
			BigInteger m = decodeBigInt(bb);
			RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
			return KeyFactory.getInstance("RSA").generatePublic(spec);
		}
		else {
			throw new IllegalArgumentException("Only supports RSA");
		}
	}

	private static BigInteger decodeBigInt(ByteBuffer bb) {
		int len = bb.getInt();
		byte[] bytes = new byte[len];
		bb.get(bytes);
		return new BigInteger(bytes);
	}

	@BeforeClass
	public static void beforeClass() throws Exception {		
		final PublicKey allowedKey = decodePublicKey();

		sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
			@Override
			public boolean authenticate(String username, PublicKey key, ServerSession session) {
				return key.equals(allowedKey);
			}
		});
		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String username, String password, ServerSession session) {
                return username != null && username.equals(password);
            }
        });
		sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            public boolean authenticate(String username, PublicKey key, ServerSession session) {
                //File f = new File("/Users/" + username + "/.ssh/authorized_keys");
                return true;
            }
        });

		sshd.setPort(8091);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("src/test/resources/keys/hostkey.ser"));
		sshd.setSubsystemFactories(Collections.<NamedFactory<Command>>singletonList(new SftpSubsystem.Factory()));
		final String virtualDir = new FileSystemResource("src/test/resources/remote/").getFile().getAbsolutePath();
		sshd.setFileSystemFactory(new VirtualFileSystemFactory(virtualDir));
		sshd.start();

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
	public static void afterClass() throws Exception {
		
		if(sshd != null) {
			sshd.stop();
		}

	}
}
