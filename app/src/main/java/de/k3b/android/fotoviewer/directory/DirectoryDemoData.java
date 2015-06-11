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
	
	// generate some random amount of child objects (1..10)
	private void generateChildren() {
		Random rand = new Random();
		for(int i=0; i < rand.nextInt(9)+1; i++) {
			DirectoryDemoData cat = new DirectoryDemoData("Child "+i);
			this.children.add(cat);
		}
	}
	
	public static DirectoryDemoRoot getCategories() {
		ArrayList<DirectoryDemoData> categories = new ArrayList<DirectoryDemoData>();
		for(int i = 0; i < 10 ; i++) {
			DirectoryDemoData cat = new DirectoryDemoData("Category "+i);
			cat.generateChildren();
			categories.add(cat);
		}
		DirectoryDemoRoot root = new DirectoryDemoRoot(categories);

		return root;
	}

	/*
	public static DirectoryDemoData getCategories(String name)
	{
		ArrayList<DirectoryDemoData> collection = DirectoryDemoRoot.get Categories();
		for (Iterator<DirectoryDemoData> iterator = collection.iterator(); iterator.hasNext();) {
			DirectoryDemoData cat = (DirectoryDemoData) iterator.next();
			if(cat.name.equals(name)) {
				return cat;
			}
			
		}
		return null;
	}
	*/
}
