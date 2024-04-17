package net.nuke24.tscript34.p0012.halp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
	
	static char hexDigit(int value) {
		if( value < 10 ) return (char)('0'+value);
		return (char)('a' - 10 + value);
	}
	
	static String hexEncode(byte[] data) {
		char[] hex = new char[data.length*2];
		for( int i=0; i<data.length; ++i ) {
			hex[i*2+0] = hexDigit(0xF & (data[i] >> 4));
			hex[i*2+1] = hexDigit(0xF & (data[i] >> 0));
		}
		return new String(hex);
	}
	
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
	
	interface Action {
		// Not necjknt the best design idk
		public int run(OutputStream out) throws IOException;
	}
	
	static class HashifyAction implements Action {
		final List<String> roots;
		public HashifyAction(List<String> roots) {
			this.roots = roots;
		}
		
		public void hashify(String name, File f, OutputStream out) throws IOException {
			if( f.isDirectory() ) {
				for( File s : f.listFiles() ) {
					hashify(name+"/"+s.getName(), s, out);
				}
			} else {
				MessageDigest sha1;
				MessageDigest md5;
				try {
					sha1 = MessageDigest.getInstance("SHA1");
					md5 = MessageDigest.getInstance("MD5");
				} catch (NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}
				FileInputStream fis = new FileInputStream(f);
				try {
					byte[] buffer = new byte[65536];
					int z;
					while( (z = fis.read(buffer)) > 0 ) {
						sha1.update(buffer, 0, z);
						md5.update(buffer, 0, z);
					}
					out.write((name+".sha1\tdata:,"+hexEncode(sha1.digest())+"\n").getBytes(UTF8));
					out.write((name+".md5\tdata:,"+hexEncode(md5.digest())+"\n").getBytes(UTF8));
				} finally {
					fis.close();
				}
			}
		}
		
		@Override public int run(OutputStream out) throws IOException {
			for( String root : roots ) {
				hashify(root, new File(root), out);
			}
			return 0;
		}
		
		public static HashifyAction parse(String[] argv, int i) {
			List<String> roots = new ArrayList<>();
			for( ; i<argv.length; ++i ) {
				if( argv[i].startsWith("-") ) {
					throw new RuntimeException("hashify: Unrecognized argument: "+argv[i]);
				} else {
					roots.add(argv[i]);
				}
			}
			return new HashifyAction(roots);
		}
	}
	
	public static void main(String[] args) throws IOException {
		Resolver<Blob> blobResolver = new MiltiResolver<Blob>(Arrays.asList(
			new DataUriResolver(),
			new FileBlobResolver(new File("."))
		));
		
		Action action = null;
		int i=0;
		for( ; i<args.length; ++i ) {
			if( "hashify".equals(args[i]) ) {
				action = HashifyAction.parse(args, i+1);
				break;
			} else {
				System.err.println("Unrecognized argument: "+args[i]);
				System.exit(1);
			}
		}
		
		if( action == null ) {
			System.err.println("No action specified");
			System.exit(1);
		}
		
		System.exit(action.run(System.out));
		
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
	}
}
