package de.k3b.android.fotoviewer.directory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import de.k3b.io.Directory;


public class DirectoryDemoData extends Directory {

	public ArrayList<DirectoryDemoData> children;
	public ArrayList<String> selection;
	
	
	public String name;
	
	public DirectoryDemoData() {
		super(null, null, 0);
		children = new ArrayList<DirectoryDemoData>();
		selection = new ArrayList<String>();
	}
	
	public DirectoryDemoData(String name) {
		this();
		this.name = name;
	}

	public String toString() {
		return this.name;
	}
}
