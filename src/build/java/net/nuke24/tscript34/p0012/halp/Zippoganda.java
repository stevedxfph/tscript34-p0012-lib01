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
import java.util.Collections;
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

interface OutputStreamable {
	public void writeTo(OutputStream os) throws IOException;
}

class ByteBlob implements OutputStreamable {
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

class InputStreamSourceBlob implements OutputStreamable {
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

/** A blob representing a zip file to be generated from given content */
class ZipBlob implements OutputStreamable {
	final Buncho<OutputStreamable> content;
	public ZipBlob(Buncho<OutputStreamable> content) {
		this.content = content;
	}
	
	@Override
	public void writeTo(OutputStream os) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(os);
		
		for( Entry<OutputStreamable> e : content.entries ) {
			zos.putNextEntry(new ZipEntry(e.name));
			e.target.writeTo(zos);
		}
	}
}

class QuitException extends Exception {
	private static final long serialVersionUID = 1L;
	
	final int code;
	public QuitException(int code) {
		this.code = code;
	}
	public final int getCode() {
		return code;
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
	
	static class FileBlobResolver implements Resolver<OutputStreamable> {
		final private File root;
		public FileBlobResolver(File root) {
			this.root = root;
		}
		
		static final Pattern FILE_URI_REGEX = Pattern.compile("file:(.*)");
		
		@Override
		public OutputStreamable get(String name) {
			Matcher m;
			if( (m = FILE_URI_REGEX.matcher(name)).matches() ) {
				String path = URLDecoder.decode(m.group(1), UTF8);
				File f = new File(root, path);
				if( f.exists() ) return new InputStreamSourceBlob(new FileInputStreamSource(f));
			}
			return null;
		}
	}
	
	static class DataUriResolver implements Resolver<OutputStreamable> {
		// Why do I keep reinventing this stuff lol
		// Put it in TScript34.1 or whatever.
		static final Pattern DATA_URI_REGEX = Pattern.compile("data:,(.*)");
		
		@Override
		public OutputStreamable get(String name) {
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
	
	interface ZConsumer<T> {
		public void accept(T item) throws IOException, QuitException;
		/**
		 * Indicate end of input, which a consumer may
		 * take to mean that should flush any internal buffers
		 * and finish its work, since it will never be called again
		 * @throws IOException
		 * @throws QuitException
		 */
		public void end() throws IOException, QuitException;
	}
	
	static class DigestingOutputStream extends OutputStream {
		final MessageDigest[] digestors;
		public DigestingOutputStream(MessageDigest[] digestors) {
			this.digestors = digestors;
		}
		@Override public void write(int b) {
			for( int i=0; i<digestors.length; ++i ) digestors[i].update((byte)b);
		}
		@Override public void write(byte[] data, int off, int len) {
			for( int i=0; i<digestors.length; ++i ) digestors[i].update(data, off, len);
		}
	}
	
	static class Hashifier implements ZConsumer<Entry<String>> {
		final ZConsumer<Entry<String>> dest;
		final Resolver<OutputStreamable> resolver;
		public Hashifier(Resolver<OutputStreamable> resolver, ZConsumer<Entry<String>> dest) {
			this.resolver = resolver;
			this.dest = dest;
		}
		@Override public void accept(Entry<String> inputEntry) throws IOException, QuitException {
			OutputStreamable blob = resolver.get(inputEntry.target);
			if( blob == null ) {
				System.err.println("Failed to resolve "+inputEntry.target+"; can't generate hash files");
				return;
			}
			
			MessageDigest sha1;
			MessageDigest md5;
			try {
				sha1 = MessageDigest.getInstance("SHA1");
				md5 = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			DigestingOutputStream dos = new DigestingOutputStream(new MessageDigest[] { sha1, md5 });
			blob.writeTo(dos);
			
			// Pass input entries through
			dest.accept(inputEntry);
			dest.accept(new Entry<String>(inputEntry.name+".sha1", "data:,"+hexEncode(sha1.digest())));
			dest.accept(new Entry<String>(inputEntry.name+".md5", "data:,"+hexEncode(md5.digest())));
		}
		@Override public void end() throws IOException {}
	}
	
	/** An abstract command that lacks context */
	interface ZCommand<O,R> {
		/**
		 * Given some real context, turn this into an action that can be run.
		 * 'Context' being some combination of I/O streams and
		 * system interfaces.
		 * */
		public R build(Resolver<OutputStreamable> blobResolver, O outputDest);
	}
	
	static class ZOutputter implements ZConsumer<byte[]> {
		static final int NOOP  = 0;
		static final int FLUSH = 1;
		static final int CLOSE = 2;
		final OutputStream os;
		final int onClose;
		public ZOutputter(OutputStream os, int onClose) {
			this.os = os;
			this.onClose = onClose;
		}
		@Override public void accept(byte[] item) throws IOException {
			this.os.write(item);
		}
		@Override public void end() throws IOException {
			if( (onClose & FLUSH) == FLUSH ) os.flush();
			if( (onClose & CLOSE) == CLOSE ) os.close();
		}
	}
	
	interface ZAction {
		public void run() throws IOException, QuitException;
	}
	
	/** Pre-loaded with stuff to push! */
	static class ZPrepender<T> implements ZConsumer<T>, ZAction {
		private List<T> queue;
		protected final ZConsumer<T> next;
		public ZPrepender(List<T> queue, ZConsumer<T> next) {
			this.queue = queue;
			this.next = next;
		}
		private synchronized List<T> take() throws IOException {
			List<T> queue = this.queue;
			this.queue = Collections.emptyList();
			return queue;
		}
		void flushQueue() throws IOException, QuitException {
			for( T item : take() ) next.accept(item);
		}
		@Override public void accept(T item) throws IOException, QuitException {
			flushQueue();
			next.accept(item);
		}
		@Override
		public void end() throws IOException, QuitException {
			flushQueue();
			next.end();
		}
		@Override
		public void run() throws IOException, QuitException {
			flushQueue();
		}
	}
	
	static <O1,O2> ZCommand<ZConsumer<O2>,ZAction> pipe(
		final ZCommand<ZConsumer<O1>,ZAction> cmd,
		final ZCommand<ZConsumer<O2>,ZConsumer<O1>> dest
	) {
		return new ZCommand<ZConsumer<O2>,ZAction>() {
			// And this is why maybe wiriting up output streams
			// should be a separate step from other configuration;
			// so that composition via piping doesn't need to know
			// about all the other stuff.
			@Override
			public ZAction build(Resolver<OutputStreamable> blobResolver, ZConsumer<O2> out) {
				ZConsumer<O1> destCons = dest.build(blobResolver, out);
				ZAction act = cmd.build(blobResolver, destCons);
				return act;
			}
		};
	}
	
	// This evolved from an earlier implementation
	// and should probably be renamed and/or refactored
	// to be 'just stages in the pipeline', as opposed
	// to 'wrapping' the hashifier
	static class HashifyAction implements ZConsumer<String> {
		final ZConsumer<Entry<String>> hashifier;
		public HashifyAction(Resolver<OutputStreamable> blobResolver, final ZConsumer<byte[]> out) {
			this.hashifier = new Hashifier(blobResolver, new ZConsumer<Entry<String>>() {
				@Override
				public void accept(Entry<String> item) throws IOException, QuitException {
					// TODO Auto-generated method stub
					out.accept((item.name+"\t"+item.target+"\n").getBytes(UTF8));
				}
				@Override public void end() throws IOException, QuitException {
					out.end();
				}
			});
		}
		
		public void hashify(String name, File f) throws IOException, QuitException {
			if( f.isDirectory() ) {
				for( File s : f.listFiles() ) {
					hashify(name+"/"+s.getName(), s);
				}
			} else {
				hashifier.accept(new Entry<String>(name, "file:"+f.getPath().replace("%", "%25")));
			}
		}
		
		@Override public void accept(String root) throws IOException, QuitException {
			hashify(root, new File(root));
		}
		@Override public void end() throws IOException, QuitException {
			hashifier.end();
		}
		
		public static ZCommand<OutputStream,ZAction> parse(String[] argv, int i) {
			final List<String> roots = new ArrayList<>();
			for( ; i<argv.length; ++i ) {
				if( argv[i].startsWith("-") ) {
					throw new RuntimeException("hashify: Unrecognized argument: "+argv[i]);
				} else {
					roots.add(argv[i]);
				}
			}
			return new ZCommand<OutputStream,ZAction>() {
				public ZAction build(Resolver<OutputStreamable> blobResolver, OutputStream out) {
					return new ZPrepender<String>(roots, new HashifyAction(blobResolver, new ZOutputter(out, ZOutputter.CLOSE)));
				}
			};
		}
	}
	
	public static void main(ZAction act) {
		try {
			act.run();
		} catch( QuitException quit ) {
			System.exit(quit.code);
		} catch( Exception e ) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
	
	public static void main(String[] args) throws IOException {
		Resolver<OutputStreamable> blobResolver = new MiltiResolver<OutputStreamable>(Arrays.asList(
			new DataUriResolver(),
			new FileBlobResolver(new File("."))
		));
		
		ZCommand<OutputStream,ZAction> command = null;
		int i=0;
		for( ; i<args.length; ++i ) {
			if( "hashify".equals(args[i]) ) {
				command = HashifyAction.parse(args, i+1);
				break;
			} else {
				System.err.println("Unrecognized argument: "+args[i]);
				System.exit(1);
			}
		}
		
		if( command == null ) {
			System.err.println("No command given");
			System.exit(1);
		}
		
		ZAction action = command.build(blobResolver, System.out);
		
		main(action);
		
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
		
		// Automation progress:
		// - [ ] Determine group ID, artifact ID, version (by parsing pom.xml?)
		// - [ ] Determine proper names of generated files as they must
		//   be in resulting zip file
		// - [ ] Determine location of generated files
		// - [X] Generate SHA1 and MD5 files
		//   - Hashifier will do this
		// - [ ] Generate final zip file
	}
}
