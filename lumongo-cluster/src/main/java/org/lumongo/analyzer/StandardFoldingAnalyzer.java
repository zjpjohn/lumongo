package org.lumongo.analyzer;

/*
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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.util.WordlistLoader;

import java.io.IOException;
import java.io.Reader;

public final class StandardFoldingAnalyzer extends StopwordAnalyzerBase {
	
	/** Default maximum allowed token length */
	public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;
	
	private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;
	
	public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
	
	/** Builds an analyzer with the given stop words.
	 * @param stopWords stop words */
	public StandardFoldingAnalyzer(CharArraySet stopWords) {
		super(stopWords);
	}
	
	/** Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
	 */
	public StandardFoldingAnalyzer() {
		this(STOP_WORDS_SET);
	}
	
	/** Builds an analyzer with the stop words from the given reader.
	 * @see WordlistLoader#getWordSet(Reader)
	 * @param stopwords Reader to read stop words from */
	public StandardFoldingAnalyzer(Reader stopwords) throws IOException {
		this(loadStopwordSet(stopwords));
	}
	
	/**
	 * Set maximum allowed token length.  If a token is seen
	 * that exceeds this length then it is discarded.  This
	 * setting only takes effect the next time tokenStream or
	 * tokenStream is called.
	 */
	public void setMaxTokenLength(int length) {
		maxTokenLength = length;
	}
	
	/**
	 * @see #setMaxTokenLength
	 */
	public int getMaxTokenLength() {
		return maxTokenLength;
	}
	
	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		final StandardTokenizer src = new StandardTokenizer();
		src.setMaxTokenLength(maxTokenLength);
		TokenStream tok = new StandardFilter(src);
		tok = new LowerCaseFilter(tok);
		tok = new StopFilter(tok, stopwords);
		tok = new ASCIIFoldingFilter(tok);
		return new TokenStreamComponents(src, tok) {
			@Override
			protected void setReader(final Reader reader) throws IOException {
				src.setMaxTokenLength(StandardFoldingAnalyzer.this.maxTokenLength);
				super.setReader(reader);
			}
		};
	}
}
