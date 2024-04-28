package net.nuke24.tscript34.p0012.halp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.nuke24.tscript34.p0012.halp.Zippoganda.ZConsumer;

public class Packager {
	public static void main(String[] args) throws IOException, QuitException {
		// Push or pull?
		// PUSH OR PULL???
		final Resolver<OutputStreamable> resolver = Zippoganda.makeStandardResolver(new File("."));
		
		InputStream pomStream = new FileInputStream("pom.xml");
		ArtifactID artifid = POMParser.parseArtifactIdFromPomXml(pomStream);
		
		System.out.println("# artifact ID = "+artifid);
		
		final ZConsumer<Entry<OutputStreamable>> zipContentCollector = new ZConsumer<Entry<OutputStreamable>>() {
			final List<Entry<OutputStreamable>> zipRes = new ArrayList<Entry<OutputStreamable>>();
			@Override public void accept(Entry<OutputStreamable> item) throws IOException, QuitException {
				zipRes.add(item);
			}
			@Override public void end() throws IOException, QuitException {
				File outputFile = new File("target/the-package.zip");
				outputFile.delete();
				OutputStream os = new FileOutputStream(outputFile);
				try {
					new ZipBlob(zipRes).writeTo(os);
				} finally {
					os.close();
				}
				System.out.println("# Wrote "+outputFile.getPath());
			}
		};
		
		final ZConsumer<Entry<String>> zresolver = new Zippoganda.ZTransformer<Entry<String>,Entry<OutputStreamable>,IOException>(
			new ZFunction<Entry<String>,Entry<OutputStreamable>,IOException>() {
				@Override
				public Entry<OutputStreamable> apply(Entry<String> input) throws IOException {
					return new Entry<OutputStreamable>(input.name, resolver.get(input.target));
				}
			},
			zipContentCollector
		) {
		};
		
		ZConsumer<Entry<String>> hashifier = new Zippoganda.Hashifier(resolver, zresolver);
		String srcDir = "target";
		String targetDir = artifid.groupId.replace(".", "/") + "/" + artifid.artifactId + "/" + artifid.version;
		String fnBase = artifid.artifactId+"-"+artifid.version;
		String[] exts = new String[] { ".pom", ".jar", "-sources.jar", "-javadoc.jar" };
		String[] moreExts = new String[] { "", ".asc" };
		for( String ext : exts ) {
			for( String ext2 : moreExts ) {
				String fn = fnBase + ext + ext2;
				hashifier.accept(new Entry<String>(targetDir + "/" + fn, URIUtil.fileUri(srcDir+"/"+fn)));
			}
		}
		hashifier.end();
	}
}
