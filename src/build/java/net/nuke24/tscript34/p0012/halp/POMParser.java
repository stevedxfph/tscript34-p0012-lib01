package net.nuke24.tscript34.p0012.halp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class POMParser {
	static class Artifinfo {
		public final String groupId;
		public final String artifactId;
		public final String version;
		public Artifinfo(String groupId, String artifactId, String version) {
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
		}
		@Override
		public String toString() {
			return groupId+"."+artifactId+":"+version;
		}
	}
	
	static final Pattern GROUPID_PATTERN = Pattern.compile(".*<groupId>([^<]*)</groupId>.*");
	static final Pattern ARTIFACTID_PATTERN = Pattern.compile(".*<artifactId>([^<]*)</artifactId>.*");
	static final Pattern VERSION_PATTERN = Pattern.compile(".*<version>([^<]*)</version>.*");
	
	public static Artifinfo parseArtifinfoFromPomXml(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		String groupId = null;
		String artifactId = null;
		String version = null;
		Matcher m;
		while( (line = br.readLine()) != null ) {
			if( groupId == null && (m = GROUPID_PATTERN.matcher(line)).matches() ) {
				groupId = m.group(1);
			}
			if( artifactId == null && (m = ARTIFACTID_PATTERN.matcher(line)).matches() ) {
				artifactId = m.group(1);
			}
			if( version == null && (m = VERSION_PATTERN.matcher(line)).matches() ) {
				version = m.group(1);
			}
		}
		return new Artifinfo(groupId, artifactId, version);
	}
	
	public static void main(String[] args) throws IOException {
		String infilename = "-";
		for( String arg : args ) {
			infilename = arg;
		}
		InputStream is = "-".equals(infilename) ? System.in : new FileInputStream(infilename);
		try {
			Artifinfo artifinfo = parseArtifinfoFromPomXml(is);
			System.out.println(artifinfo);
		} finally {
			is.close();
		}
	}
}
