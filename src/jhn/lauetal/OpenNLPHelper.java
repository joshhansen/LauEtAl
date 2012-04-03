package jhn.lauetal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

class OpenNLPHelper {
	private final ChunkerME chunker;
	private final POSTaggerME tagger;
	
	public OpenNLPHelper(String taggerModelPath, String chunkerModelPath) {
		this.tagger = loadTagger(taggerModelPath);
		this.chunker = loadChunker(chunkerModelPath);
	}
	
	private static POSTaggerME loadTagger(String path) {
		InputStream modelIn = null;
		POSModel model = null;
		try {
			modelIn = new FileInputStream(path);
			model = new POSModel(modelIn);
		}
		catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new POSTaggerME(model);
	}
	
	private static ChunkerME loadChunker(String path) {
		InputStream modelIn = null;
		ChunkerModel model = null;
		try {
			modelIn = new FileInputStream(path);
			model = new ChunkerModel(modelIn);
		} catch (IOException e) {
			// Model loading failed, handle the error
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
				}
			}
		}
		return new ChunkerME(model);
	}

	public String[] posTag(String[] sentence) {
		return tagger.tag(sentence);
	}
	
	public String[] chunk(String[] sentence, String[] posTags) {
		return chunker.chunk(sentence, posTags);
	}
	
	public Set<String> chunks(String sentence) {
		String[] sArr = sentence.split(" ");
		String[] pos = posTag(sArr);
		String[] chunkTags = chunk(sArr, pos);
		
		
		return null;
	}
}