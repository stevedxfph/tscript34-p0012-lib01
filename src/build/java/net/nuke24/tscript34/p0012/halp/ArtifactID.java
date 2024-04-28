package net.nuke24.tscript34.p0012.halp;

public class ArtifactID {
	public final String groupId;
	public final String artifactId;
	public final String version;
	public ArtifactID(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}
	@Override
	public String toString() {
		return groupId+"."+artifactId+":"+version;
	}
}
