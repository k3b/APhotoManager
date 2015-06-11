package de.k3b.android.fotoviewer.directory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;


public class Directory {

	public ArrayList<Directory> children;
	public ArrayList<String> selection;
	
	
	public String name;
	
	public Directory() {
		children = new ArrayList<Directory>();
		selection = new ArrayList<String>();
	}
	
	public Directory(String name) {
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
			Directory cat = new Directory("Child "+i);
			this.children.add(cat);
		}
	}
	
	public static ArrayList<Directory> getCategories() {
		ArrayList<Directory> categories = new ArrayList<Directory>();
		for(int i = 0; i < 10 ; i++) {
			Directory cat = new Directory("Category "+i);
			cat.generateChildren();
			categories.add(cat);
		}
		return categories;
	}
	
	public static Directory get(String name)
	{
		ArrayList<Directory> collection = Directory.getCategories();
		for (Iterator<Directory> iterator = collection.iterator(); iterator.hasNext();) {
			Directory cat = (Directory) iterator.next();
			if(cat.name.equals(name)) {
				return cat;
			}
			
		}
		return null;
	}
}
