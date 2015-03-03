package luceneIndexing;


/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import java.io.File;
import org.apache.commons.io.FileUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;


/** 
 * Class for adding text files to a Lucene index. 
 * 
 * @author	katarina.boland@gesis.org
 * @version	2014-01-27
 */
public class Indexer 
{
	/**
	 * Class constructor.
	 */
	private Indexer() { }
	
	public static Analyzer getAnalyzer()
	{
		return new CaseSensitiveStandardAnalyzer();
		//return new StandardAnalyzer(Version.LUCENE_CURRENT, new HashSet<String>());
	}

	/**
	 * Converts all text files in a directory and all subdirectories to lucene documents and 
	 * adds them to a lucene index.
	 * 
	 * @param writer	lucene IndexWriter instance to add the document(s) to
	 * @param file		the location of the text document(s) to be added to the index
	 * @throws IOException
	 */
	public static void indexDocs(IndexWriter writer, File file) throws IOException 
	{
		// do not try to index files that cannot be read
		if (file.canRead()) 
		{
			//call indexDocs recursively to index all documents in all subdirectories
			if (file.isDirectory()) 
			{
				String[] files = file.list();
				// an IO error could occur
				if (files != null) 
				{
					for (int i = 0; i < files.length; i++) { indexDocs(writer, new File(file, files[i])); }
				}
			} 
			else 
			{
				System.out.println("adding " + file);
				try { writer.addDocument(FileDocument.Document(file)); }
				// at least on windows, some temporary files raise this exception with an "access denied" message
				// checking if the file can be read doesn't help
				catch (FileNotFoundException fnfe) { ;}
			}
		}
	}
	
	/**
	 * Initializes the Lucene IndexWriter instance, starts the indexing process and prints some status 
	 * information. 
	 * 
	 * @param fileMap	A map listing index output locations (keys) and input directories (values) containing the documents to index	
	 */
	public static void indexAllFiles(HashMap<File, File> fileMap, boolean forceOverwrite)
	{
		for (File indexDir : fileMap.keySet())
		{
			File docDir = fileMap.get(indexDir);

			System.out.println("start indexing");
			if (indexDir.exists()) 
			{
				if (forceOverwrite) {
					System.out.println("Removing dir " + indexDir);
					try {
						FileUtils.deleteDirectory(indexDir);
					} catch (IOException ioe) {
						System.out.println("ERROR: Could not delete directory " + indexDir);
					}
				}
				System.out.println("Cannot save index to '" + indexDir + "' directory, please delete it first or set --force");
				continue;
			}

			Date start = new Date();
			try 
			{
				IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), Indexer.getAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
				System.out.println("Indexing to directory '" + indexDir + "'...");
				indexDocs(writer, docDir);
				System.out.println("Optimizing...");
				writer.optimize();
				writer.close();
				Date end = new Date();
				System.out.println(end.getTime() - start.getTime() + " total milliseconds");
			} 
			catch (IOException e) 
			{
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
			System.out.println("finished indexing");
		}
	}
	/** 
	 * Selects either all subdirectories in the specified directory (recursive mode) or only the root 
	 * directory, specifies a path for the indexes to be created and starts the indexing process. 
	 * 
	 * @param	args	args[0]: path to the corpus root directory; args[1]: path to the index directory; args[2]: "r" to set recursive mode
	 */
	public static void main(String[] args) {
		new OptionHandler().doMain(args);
	}
}

/**
* Class for processing command line options using args4j.
*
* @author konstantin.baierer@bib.uni-mannheim.de
* @version 2014-03-03
*/
class OptionHandler {
		
	@Option(name="-c",usage="path of the corpus root directory", metaVar = "CORPUS_PATH", required = true)
    private String root_corpus_str;
		
	@Option(name="-o",usage="path to save converted documents", metaVar = "OUTPUT_PATH", required = true)
    private String root_index;
		
	@Option(name="-r",usage="Recursive indexing", metaVar = "RECURSIVE")
    private boolean recursive = false;
		
	@Option(name="-f",usage="Force overwriting existing directories", metaVar = "FORCE")
    private boolean forceOverwrite = false;

	/**
     * Parses all command line options and calls <emph>main</emph> method accordingly.
     * 
     * @param args			
     * @throws IOException
     */
	public void doMain(String[] args)
	{
		CmdLineParser parser = new CmdLineParser(this); 

        try { parser.parseArgument(args); } 
        catch(CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("Indexer [options...]");
            parser.printUsage(System.err);
            System.err.println();
            return;
        } catch (Exception e) {
        	e.printStackTrace();
        	return;
		}
		HashMap<File, File> toIndex = new HashMap<File, File>();
		File root_corpus = new File(this.root_corpus_str);
		String root_index = new File(Paths.get(this.root_index).normalize().toString()).getAbsolutePath();
		if (this.recursive) 
		{
			for (File file : root_corpus.listFiles()) 
			{
				if (file.isDirectory())
				{
					toIndex.put(new File(root_index + "_" + file.getName()), new File(root_corpus + File.separator + file.getName()));
				}
			}
		}
		Indexer.indexAllFiles(toIndex, this.forceOverwrite);
	}
}
