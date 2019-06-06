package org.lionsoul.jcseg.tokenizer;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.lionsoul.jcseg.tokenizer.core.ADictionary;
import org.lionsoul.jcseg.tokenizer.core.IChunk;
import org.lionsoul.jcseg.tokenizer.core.ILexicon;
import org.lionsoul.jcseg.tokenizer.core.IWord;
import org.lionsoul.jcseg.tokenizer.core.JcsegTaskConfig;

/**
 * search mode implementation all the possible combination will be returned, 
 * and build it for information retrieval of course.
 * 
 * @author  xwz
*/
public class SearchupPinSeg extends ASegment
{
    
    public SearchupPinSeg(JcsegTaskConfig config, ADictionary dic) throws IOException
    {
        super(config, dic);
    }
    
    public SearchupPinSeg(Reader input, JcsegTaskConfig config, ADictionary dic) throws IOException
    {
        super(input, config, dic);
    }

    /**
     * get the next CJK word from the current position of the input stream
     * and this function is the core part the most segmentation implements
     * 
     * @see ASegment#getNextCJKWord(int, int)
     * @throws IOException 
    */
    @Override 
    protected IWord getNextCJKWord(int c, int pos) throws IOException
    {
    	String key = null;
        char[] chars = nextCJKSentence(c);
        int cjkidx = 0, ignidx = 0, mnum = 0;
        IWord word = null;
        ArrayList<IWord> mList = new ArrayList<IWord>(8);
        
        while ( cjkidx < chars.length ) {

            mnum = 0;
            isb.clear().append(chars[cjkidx]);
            //System.out.println("ignore idx: " + ignidx);
            for ( int j = 1; j < config.MAX_LENGTH 
                    && (cjkidx+j) < chars.length; j++ ) {
                isb.append(chars[cjkidx+j]);
                key = isb.toString();
                if ( dic.match(ILexicon.CJK_WORD, key) ) {
                    mnum   = 1;
                    ignidx = Math.max(ignidx, cjkidx + j);
                    word = dic.get(ILexicon.CJK_WORD, key).clone();
                    word.setPosition(pos+cjkidx);
                    mList.add(word);
                }
            }
            
            if (cjkidx > ignidx) {//xwz
            	/// @Note added at 2017/04/29
                /// check and append the single char word
                String sstr = String.valueOf(chars[cjkidx]);//xwz 2.1.1版本中本没有单字，是2.2.0中新加的特性，但是根据需求要提前添加，且会产生重复拆除bug，下面也做了提前修复
                if ( dic.match(ILexicon.CJK_WORD, sstr) ) {
                    IWord sWord = dic.get(ILexicon.CJK_WORD, sstr).clone();
                    sWord.setPosition(pos+cjkidx);
                    mList.add(sWord);
                }
            }
            
            
            /*
             * no matches here:
             * should the current character chars[cjkidx] be a single word ?
             * lets do the current check 
            */
          /*  if ( mnum == 0 && (cjkidx == 0 || cjkidx > ignidx) ) {
                String temp = String.valueOf(chars[cjkidx]);
                if ( dic.match(ILexicon.CJK_WORD, temp) == false ) {
                    word = new Word(temp, ILexicon.UNMATCH_CJK_WORD);
                    word.setPosition(pos+cjkidx);
                    mList.add(new Word(temp, ILexicon.UNMATCH_CJK_WORD));
                } else {
                    word = dic.get(ILexicon.CJK_WORD, temp).clone();
                    word.setPosition(pos+cjkidx);
                    mList.add(word);
                    appendWordFeatures(word);
                }
            }*/
            if ( mnum == 0 && (cjkidx == 0 || cjkidx > ignidx) ) {//这是2.3.0修复的bug，在这里提前修复
                String temp = String.valueOf(chars[cjkidx]);
                if ( ! dic.match(ILexicon.CJK_WORD, temp) ) {
                    word = new Word(temp, ILexicon.UNMATCH_CJK_WORD);
                    word.setPosition(pos+cjkidx);
                    mList.add(word);
                } else {//xwz
                	if ( cjkidx == 0 ) {
                		word = new Word(temp, ILexicon.UNMATCH_CJK_WORD);
                        word.setPosition(pos+cjkidx);
                        mList.add(word);
                	}
                }
            }
            
            cjkidx++;
        }
        
        /*
         * do all the words analysis
         * 1, clear the stop words
         * 1, check and append the pinyin or synonyms words
        */
        for ( IWord w : mList ) {
            key = w.getValue();
            if ( config.CLEAR_STOPWORD 
                    && dic.match(ILexicon.STOP_WORD, key) ) {
                continue;
            }
            
            wordPool.add(w);
            appendWordFeatures(w);
        }
        
        //let gc do its work
        mList.clear();
        mList = null;
        
        return wordPool.size()==0 ? null : wordPool.remove();
    }

    /**
     * here we don't have to do anything
     * 
     * @see ASegment#getBestCJKChunk(char[], int)
    */
    @Override
    protected IChunk getBestCJKChunk(char[] chars, int index) throws IOException
    {
        return null;
    }
    
}
