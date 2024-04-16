package net.nuke24.tscript34.p0012.halp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zippoganda {
	static final Charset UTF8 = Charset.forName("UTF-8");
	
	public static void main(String[] args) throws IOException {
		// Okay so we gotta get dumb old Maven to generate three JAR files:
		// the regular one, one with sources, and one with 'javadoc',
		// which is a bunch of shittily-generated HTML.
		// Then you need to run some plugins
		// that call PGP to generate some signature files (.asc).
		// That part is already done if you added the right bullshit
		// to your pom.xml.
		// This part might be easy enough to do without involving
		// Maven once you have the dumb JAR files.
		// After that, you need to generate .sha1 and .md5 sidecar
		// files for every other file, including the .ascs.
		// Then all the files--the jars, the pom, the sigs, the
		// hashes--all need to get put inside the right directory
		// inside a zip file, and then you can go to some web page,
		// maybe near https://central.sonatype.com/publishing, and upload
		// the zip file, and maybe it will accept it.
		
		try( ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("target/zippogandized.zip")) ) {
			zos.putNextEntry(new ZipEntry("HELLO.txt"));
			zos.write("Hello, world!".getBytes(UTF8));
			
			ZipEntry readme = new ZipEntry("README.txt");
			readme.setLastModifiedTime(FileTime.from(1713290883, TimeUnit.SECONDS));
			zos.putNextEntry(readme);
			zos.write("Proof-of-concept zip file, made from a Java program using ZipOutputStream\n".getBytes(UTF8));
			
			zos.putNextEntry(new ZipEntry("subdir/.wat"));
			zos.write("The zip may\ncontain sub-\ndirectories\n".getBytes(UTF8));
		}
	}
}
