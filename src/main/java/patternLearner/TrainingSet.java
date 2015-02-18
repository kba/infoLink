package patternLearner;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import patternLearner.Util;

/**
 * Class for representing training sets in weka's arff file format. 
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 * 
 */
public class TrainingSet
{

	private File examples;
	private ExampleReader exReader;
	
	/**
	 * Class constructor specifying the file containing the training examples to be processed.
	 * 
	 * @param examples	a file containing training examples
	 */
	TrainingSet(File examples)
	{
		this.examples = examples;	
		this.exReader = new ExampleReader(examples);
	}
	
	/**
	 * Class constructor.
	 */
	TrainingSet()
	{
		;
	}
	
	/**
	 * Returns a set of all document filenames occurring in this examples.
	 * 
	 * @return	set of all document filenames occurring in this examples
	 */
	public HashSet<String> getDocuments()
	{
		return this.exReader.getDocuments();
	}
	
	/**
	 * Returns a set of all contexts occurring in this examples.
	 * 
	 * @return	set of all contexts occurring in this examples
	 */
	public HashSet<String[]> getContexts()
	{
		return this.exReader.getContexts();
	}
	
	/**
	 * Creates an ArffFile representation of this training set.
	 */
	public void createArff()
	{
		HashSet<String[]> contextSet = this.getContexts();
		new ArffFile(contextSet);
	}
	
	/**
	 * Class for representing training instances in weka's arff-file format. 
	 * Each training instance is assumed to consist of 10 string attributes representing the surrounding 
	 * words of a term + one class value for the binary relation <emph>isStudyReference</emph>. Class values  
	 * may either be <emph>"True"</emph> (positive training examples) or <emph>"False"</emph> 
	 * (negative training examples).
	 * 
	 * @author katarina.boland@gesis.org
	 * @version 2014-01-27
	 *
	 */
	class ArffFile 
	{
		Instances data;
	  
		/**
		 * Class constructor specifying the Instances to represent.
		 * 
		 * @param data	the Instances to represent
		 */
		ArffFile (Instances data)
		{
			this.data = data;
		}
		
		/**
		 * Class constructor specifying a set of values to construct the training instances to represent. 
		 * 
		 * @param instance	values for constructing the Instances to represent. 
		 * Each instance is assumed to consist of 10 string attributes representing the surrounding 
		 * words of a term + one class value for the binary relation <emph>isStudyReference</emph>. 
		 * Class values may either be <emph>"True"</emph> (positive training examples) or 
		 * <emph>"False"</emph> (negative training examples).
		 */
		ArffFile (HashSet<String[]> instances) 
		{
		    FastVector      atts;
		    FastVector      attVals;
		    Instances       data;
		    double[]        vals;
	
		    // 1. set up attributes
		    atts = new FastVector();
	
		    // - string
		    atts.addElement(new Attribute("l5", (FastVector) null));
		    atts.addElement(new Attribute("l4", (FastVector) null));
		    atts.addElement(new Attribute("l3", (FastVector) null));
		    atts.addElement(new Attribute("l2", (FastVector) null));
		    atts.addElement(new Attribute("l1", (FastVector) null));
		    atts.addElement(new Attribute("r1", (FastVector) null));
		    atts.addElement(new Attribute("r2", (FastVector) null));
		    atts.addElement(new Attribute("r3", (FastVector) null));
		    atts.addElement(new Attribute("r4", (FastVector) null));
		    atts.addElement(new Attribute("r5", (FastVector) null));
		    
		    // - nominal class attribute
		    attVals = new FastVector();
		    attVals.addElement("True");
		    attVals.addElement("False");
		    Attribute classAttr = new Attribute("class", attVals);
		    atts.addElement(classAttr);
	
		    // 2. create Instances object
		    data = new Instances("IsStudyReference", atts, 0);
		    
		    data.setClass(classAttr);
	
		    // 3. fill with data
		    for (String[] neighboringWords: instances)
		    {
			    vals = new double[data.numAttributes()];
			    // - string
			    vals[0] = data.attribute(0).addStringValue(neighboringWords[0]);
			    vals[1] = data.attribute(1).addStringValue(neighboringWords[1]);
			    vals[2] = data.attribute(2).addStringValue(neighboringWords[2]);
			    vals[3] = data.attribute(3).addStringValue(neighboringWords[3]);
			    vals[4] = data.attribute(4).addStringValue(neighboringWords[4]);
			    vals[5] = data.attribute(5).addStringValue(neighboringWords[5]);
			    vals[6] = data.attribute(6).addStringValue(neighboringWords[6]);
			    vals[7] = data.attribute(7).addStringValue(neighboringWords[7]);
			    vals[8] = data.attribute(8).addStringValue(neighboringWords[8]);
			    vals[9] = data.attribute(9).addStringValue(neighboringWords[9]);
			    
			    // add class value (nominal)
			    vals[10] = attVals.indexOf(neighboringWords[10]);
			    
			    Instance newInstance = new Instance(1.0, vals);
		
			    // add instance
			    newInstance.setDataset(data);
			    data.add(newInstance);	       
		    }
		    this.data = data;
		}
		
		/**
		 * Calls the saveToFile method to write this Instance data to file, additionally prints a summary
		 * of the data and does the error handling.
		 * 
		 * @param filename	name of the output file
		 */
		public void write(String filename)
		{   
		    try
		    {
		    	saveToFile(filename);
		    	System.out.println(this.data.toSummaryString());
		    	System.out.println("Wrote " + filename);
		    }
		    catch (IOException e)
		    {
		    	e.printStackTrace();
		    	System.out.println(data);
		    }
		}
		
		/**
		 * Writes this Instance data to file.
		 * 
		 * @param filename	name of the output file
		 * @throws IOException
		 */
		public void saveToFile(String filename) throws IOException
		{
			ArffSaver saver = new ArffSaver();
			saver.setInstances(this.data);
			saver.setFile(new File(filename));
			saver.writeBatch();
		}
	}
	
	/**
	 * Creates an ArffFile representation of all items in this examples (InfoLink XML output file 
	 * containing extracted dataset references), assigns the specified class value classVal and writes the 
	 * training examples to an arff file </emph>filename</emph>.
	 * Uses an <emph>ExampleReader</emph> instance to parse this examples and creates an ArffFile instance 
	 * using the derived context set.
	 * 
	 * @param classVal	the class value to be set for all instances in </emph>this examples</emph> (either "True" or "False")
	 * @param filename	name of the arff output file
	 * @return			an ArffFile instance representing the input example set
	 */
	public ArffFile createTrainingSet(String classVal, String filename)
	{
		ExampleReader exReader = new ExampleReader(this.examples);
		HashSet<String[]> contextSet = exReader.getContexts();
		HashSet<String[]> contextSetMerged = new HashSet<String[]>();
		for (String[] leftNrightContext: contextSet)
		{
			String leftContext = leftNrightContext[0];
			String rightContext = leftNrightContext[1];
			String[] mergedContext = new String[11];
			String[] _leftContext = leftContext.trim().split( "\\s+" );
			String[] _rightContext = rightContext.trim().split( "\\s+" );
				
			if ( _leftContext.length < 5 )
			{
				System.out.println( "Warning: ignoring context: " + leftContext );
				continue;
			}
			if ( _rightContext.length < 5 )
			{
				System.out.println( "Warning: ignoring context: " + rightContext );
				continue;
			}

			for ( int i = 0; i<5; i++ )
			{
				mergedContext[i] = Util.normalizeRegex( Util.unescapeXML( _leftContext[i] ));
				mergedContext[i+5] = Util.normalizeRegex( Util.unescapeXML( _rightContext[i] ));
			}
			mergedContext[10] = classVal; 
			contextSetMerged.add(mergedContext);
		}
		ArffFile test = new ArffFile( contextSetMerged );
		test.write( filename );
		return test;
	}

	/**
	 * Calls the <emph>createTrainingSet</emph> method with the specified parameters 
	 * 
	 * @param args args[0]: path of the file containing the training examples in InfoLink output XML format; args[1]: path of the output file
	 */
	public static void main(String[] args) 
	{ 
		String filename_examples = args[0];
		String filename_output = args[1];
		TrainingSet newSet = new TrainingSet(new File(filename_examples));
		newSet.createTrainingSet("True", filename_output);
	 }
}