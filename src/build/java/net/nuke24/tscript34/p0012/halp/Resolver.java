package net.nuke24.tscript34.p0012.halp;

interface Resolver<T> {
	/** May return null to indicate 'idk' */
	T get(String name);
}