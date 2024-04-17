package net.nuke24.tscript34.p0012.halp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class Entry<C> {
	final String name;
	final C target;
	public Entry(String name, C target) {
		this.name = name;
		this.target = target;
	}
}

class Buncho<C> {
	final List<Entry<C>> entries;
	Buncho(List<Entry<C>> entries) {
		this.entries = entries;
	}
}

interface Blob {
	public void writeTo(OutputStream os) throws IOException;
}

class ByteBlob implements Blob {
	final byte[] data;
	public ByteBlob(byte[] data) {
		this.data = data;
	}
	
	@Override
	public void writeTo(OutputStream os) throws IOException {
		os.write(this.data);
	}
}

interface InputStreamSource {
	public InputStream getInputStream() throws IOException;
}

class FileInputStreamSource implements InputStreamSource {
	protected File file;
	FileInputStreamSource(File file) {
		this.file = file;
	}
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(this.file);
	}
}

class InputStreamSourceBlob implements Blob {
	final InputStreamSource source;
	public InputStreamSourceBlob(InputStreamSource source) {
		this.source = source;
	}
	
	@Override public void writeTo(OutputStream os) throws IOException {
		InputStream is = this.source.getInputStream();
		try {
			byte[] buffer = new byte[65536];
			int z;
			while( (z = is.read(buffer)) > 0 ) {
				os.write(buffer, 0, z);
			}
		} finally {
			is.close();
		}
	}
}

interface Resolver<T> {
	/** May return null to indicate 'idk' */
	T get(String name);
}

class ZipBlob implements Blob {
	final Buncho<Blob> content;
	public ZipBlob(Buncho<Blob> content) {
		this.content = content;
	}
	
	@Override
	public void writeTo(OutputStream os) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(os);
		
		for( Entry<Blob> e : content.entries ) {
			zos.putNextEntry(new ZipEntry(e.name));
			e.target.writeTo(zos);
		}
	}
}

public class Zippoganda {
	static final Charset UTF8 = Charset.forName("UTF-8");
	
	static class FileBlobResolver implements Resolver<Blob> {
		final private File root;
		public FileBlobResolver(File root) {
			this.root = root;
		}
		
		static final Pattern FILE_URI_REGEX = Pattern.compile("file:(.*)");
		
		@Override
		public Blob get(String name) {
			Matcher m;
			if( (m = FILE_URI_REGEX.matcher(name)).matches() ) {
				String path = URLDecoder.decode(m.group(1), UTF8);
				File f = new File(root, path);
				if( f.exists() ) return new InputStreamSourceBlob(new FileInputStreamSource(f));
			}
			return null;
		}
	}
	
	static class DataUriResolver implements Resolver<Blob> {
		// Why do I keep reinventing this stuff lol
		// Put it in TScript34.1 or whatever.
		static final Pattern DATA_URI_REGEX = Pattern.compile("data:,(.*)");
		
		@Override
		public Blob get(String name) {
			Matcher m;
			if( (m = DATA_URI_REGEX.matcher(name)).matches() ) {
				byte[] data = URLDecoder.decode(m.group(1), UTF8).getBytes(UTF8);
				return new ByteBlob(data);
			}
			return null;
		}
	}
	
	static class MiltiResolver<C> implements Resolver<C> {
		final List<Resolver<C>> resolvers;
		public MiltiResolver(List<Resolver<C>> resolvers) {
			this.resolvers = resolvers;
		}
		@Override
		public C get(String name) {
			for( Resolver<C> r : resolvers ) {
				C item = r.get(name);
				if( item != null ) return item;
			}
			return null;
		}
	}
	
	public static void main(String[] args) throws IOException {
		Resolver<Blob> blobResolver = new MiltiResolver<Blob>(Arrays.asList(
			new DataUriResolver(),
			new FileBlobResolver(new File("."))
		));
		
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
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while( (line = br.readLine()) != null ) {
			line = line.trim();
			if( line.startsWith("#") || line.isEmpty() ) continue;
			
			String[] tokens = line.split("\\s+");
			for( String t : tokens ) {
				Blob b = blobResolver.get(t);
				if( b == null ) System.err.println("Failed to resolve "+t);
				else b.writeTo(System.out);
			}
		}
	}
}
