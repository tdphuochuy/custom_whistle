package com.github.dmtk;

public class test2{
	public static void main(String [] args)
	{
		SequenceGetter getter = new SequenceGetter();
		int text = getter.getSequence("144076370", "105355", "");
		System.out.println(text);
		System.out.println(getter.getSequenceMap());
		getter.updateSequence("105355", 20, text);
		System.out.println(getter.getSequenceMap());
	
	}
}