package org.lionsoul.jcseg.tokenizer;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

//import java.util.Iterator;



import org.lionsoul.jcseg.tokenizer.core.ADictionary;
import org.lionsoul.jcseg.tokenizer.core.IChunk;
import org.lionsoul.jcseg.tokenizer.core.ILexicon;
import org.lionsoul.jcseg.tokenizer.core.ISegment;
import org.lionsoul.jcseg.tokenizer.core.IWord;
import org.lionsoul.jcseg.tokenizer.core.JcsegTaskConfig;
import org.lionsoul.jcseg.tokenizer.core.MMSegFilter;
import org.lionsoul.jcseg.util.NumericUtil;


/**
 * <p>
 * Jcseg complex segmentation implements extended from the ASegment class 
 * this will need the filter works of the four MMSeg rules: 
 * </p>
 * 
 * <ul>
 * <li>1.maximum match chunk.</li>
 * <li>2.largest average word length.</li>
 * <li>3.smallest variance of words length.</li>
 * <li>4.largest sum of degree of morphemic freedom of one-character words.</li>
 * </ul>
 * 
 * @author  chenxin<chenxin619315@gmail.com>
*/
public class ComplexSoSeg extends ASegment
{
    
    public ComplexSoSeg( JcsegTaskConfig config, ADictionary dic ) throws IOException 
    {
        super(config, dic);
    }
    
    public ComplexSoSeg( Reader input, JcsegTaskConfig config, ADictionary dic ) throws IOException 
    {
        super(input, config, dic);
    }

    /**
     * get the next CJK word from the current position of the input stream
     * 
     * @param   c
     * @param   pos
     * @return  IWord could be null and that mean we reached a stop word
     * @throws  IOException 
    */
    protected IWord getNextCJKWord(int c, int pos) throws IOException //xwz--由于拼音检索改造将方法getNextCJKWord重写
    {
        char[] chars = nextCJKSentence(c);
        int cjkidx = 0;
        IWord w = null;
        while ( cjkidx < chars.length ) {
            /*
             * find the next CJK word.
             * the process will be different with the different algorithm
             * @see getBestCJKChunk() from SimpleSeg or ComplexSeg. 
             */
            w = null;
            
            
            /*
             * check if there is Chinese numeric. 
             * make sure chars[cjkidx] is a Chinese numeric
             * and it is not the last word.
            */
            if ( cjkidx + 1 < chars.length 
                    && NumericUtil.isCNNumeric(chars[cjkidx]) > -1 ) {
                //get the Chinese numeric chars
                String num = nextCNNumeric( chars, cjkidx );
                int NUMLEN = num.length();
                
                /*
                 * check the Chinese fraction.
                 * old logic: {{{
                 * cjkidx + 3 < chars.length && chars[cjkidx+1] == '分' 
                 *         && chars[cjkidx+2] == '之' 
                 * && CNNMFilter.isCNNumeric(chars[cjkidx+3]) > -1.
                 * }}}
                 * 
                 * checkCF will be reset to be 'TRUE' it num is a Chinese fraction.
                 * @added 2013-12-14.
                 * */
                if ( (ctrlMask & ISegment.CHECK_CF_MASK) != 0  ) {
                    w = new Word(num, IWord.T_CN_NUMERIC);
                    w.setPosition(pos+cjkidx);
                    w.setPartSpeech(IWord.NUMERIC_POSPEECH);
                    wordPool.add(w);
                    
                    /* 
                     * Here: 
                     * Convert the Chinese fraction to Arabic fraction,
                     * if the Config.CNFRA_TO_ARABIC is true.
                     */
                    if ( config.CNFRA_TO_ARABIC ) {
                        String[] split = num.split("分之");
                        IWord wd = new Word(
                            NumericUtil.cnNumericToArabic(split[1], true)+
                            "/"+NumericUtil.cnNumericToArabic(split[0], true),
                            IWord.T_CN_NUMERIC
                        );
                        wd.setPosition(w.getPosition());
                        wd.setPartSpeech(IWord.NUMERIC_POSPEECH);
                        wordPool.add(wd);
                    }
                }
                /*
                 * check the Chinese numeric and single units.
                 * type to find Chinese and unit composed word.
                */
                else if ( NumericUtil.isCNNumeric(chars[cjkidx+1]) > -1
                        || dic.match(ILexicon.CJK_UNIT, chars[cjkidx+1]+"") ) {
                    StringBuilder sb = new StringBuilder();
                    String temp      = null;
                    String ONUM      = num;    //backup the old numeric
                    sb.append(num);
                    boolean matched = false;
                    int j;
                    
                    /*
                     * find the word that made up with the numeric
                     * like: "五四运动"
                    */
                    for ( j = num.length();
                            (cjkidx + j) < chars.length 
                                && j < config.MAX_LENGTH; j++ ) {
                        sb.append(chars[cjkidx + j]);
                        temp = sb.toString();
                        if ( dic.match(ILexicon.CJK_WORD, temp) ) {
                            w = dic.get(ILexicon.CJK_WORD, temp);
                            num = temp;
                            matched = true;
                        }
                    }
                    
                    /*
                     * @Note: when matched is true, num maybe a word like '五月',
                     * yat, this will make it skip the Chinese numeric to Arabic logic
                     * so find the matched word that it maybe a single Chinese units word
                     * 
                     * @added: 2014-06-06
                     */
                    if ( matched == true && num.length() - NUMLEN == 1 
                            && dic.match(ILexicon.CJK_UNIT, num.substring(NUMLEN)) ) {
                        num     = ONUM;
                        matched = false;    //reset the matched
                    }
                    
                    IWord wd = null;
                    if ( matched == false && config.CNNUM_TO_ARABIC ) {
                        String arabic = NumericUtil.cnNumericToArabic(num, true)+"";
                        if ( (cjkidx + num.length()) < chars.length
                                && dic.match(ILexicon.CJK_UNIT, chars[cjkidx + num.length()]+"") ) {
                            char units = chars[ cjkidx + num.length() ];
                            num += units; arabic += units;
                        }
                        
                        wd = new Word( arabic, IWord.T_CN_NUMERIC);
                        wd.setPartSpeech(IWord.NUMERIC_POSPEECH);
                        wd.setPosition(pos+cjkidx);
                    }
                    
                    //clear the stop words as need
                    if ( config.CLEAR_STOPWORD 
                            && dic.match(ILexicon.STOP_WORD, num) ) {
                        cjkidx += num.length();
                        continue;
                    }
                    
                    /*
                     * @Note: added at 2016/07/19
                     * we cannot share the position with the original word item in the
                     * global dictionary accessed with this.dic
                     * 
                     * cuz at the concurrency that will lead to the position error
                     * so, we clone it if the word is directly get from the dictionary
                    */
                    if ( w == null ) {
                        w = new Word(num, IWord.T_CN_NUMERIC);
                        w.setPartSpeech(IWord.NUMERIC_POSPEECH);
                    } else {
                        w = w.clone();
                    }
                    
                    w.setPosition(pos + cjkidx);
                    wordPool.add(w);
                    if ( wd != null ) {
                        wordPool.add(wd);
                    }
                }
                
                if ( w != null ) {
                    cjkidx += w.getLength();
//                    appendWordFeatures(w);//xwz
                    continue;
                }
            }
            
            
            IChunk chunk = getBestCJKChunk(chars, cjkidx);
            w = chunk.getWords()[0];
            String wps = w.getPartSpeech()==null ? null : w.getPartSpeech()[0];
            
            /* 
             * check and try to find a Chinese name.
             * 
             * @Note at 2017/05/19
             * add the origin part of speech check, if the
             * w is a Chinese name already and just let it go
            */
            int T = -1;
            if ( config.I_CN_NAME && (!"nr".equals(wps))
                    && w.getLength() <= 2 && chunk.getWords().length > 1  ) {
                StringBuilder sb = new StringBuilder();
                sb.append(w.getValue());
                String str = null;

                //the w is a Chinese last name.
                if ( dic.match(ILexicon.CN_LNAME, w.getValue())
                        && (str = findCHName(chars, 0, chunk)) != null) {
                    T = IWord.T_CN_NAME;
                    sb.append(str);
                }
                //the w is Chinese last name adorn
                else if ( dic.match(ILexicon.CN_LNAME_ADORN, w.getValue())
                        && chunk.getWords()[1].getLength() <= 2
                        && dic.match(ILexicon.CN_LNAME, 
                                chunk.getWords()[1].getValue())) {
                    T = IWord.T_CN_NICKNAME;
                    sb.append(chunk.getWords()[1].getValue());
                }
                /*
                 * the length of the w is 2:
                 * the last name and the first char make up a word
                 * for the double name. 
                 */
                /*else if ( w.getLength() > 1
                        && findCHName( w, chunk ))  {
                    T = IWord.T_CN_NAME;
                    sb.append(chunk.getWords()[1].getValue().charAt(0));
                }*/
                
                if ( T != -1 ) {
                    w = new Word(sb.toString(), T);
                    w.setPartSpeech(IWord.NAME_POSPEECH);
                }
            }
            
            //check and clear the stop words
            if ( config.CLEAR_STOPWORD 
                    && dic.match(ILexicon.STOP_WORD, w.getValue()) ) {
                cjkidx += w.getLength();
                continue;
            }
            
            
            /*
             * reach the end of the chars - the last word.
             * check the existence of the Chinese and English mixed word
            */
            IWord ce = null;
            if ( (ctrlMask & ISegment.CHECK_CE_MASk) != 0 
                    && (chars.length - cjkidx) <= dic.mixPrefixLength ) {
                ce = getNextMixedWord(chars, cjkidx);
                if ( ce != null ) {
                    T = -1;
                }
            }
            
            /*
             * @Note: added at 2016/07/19
             * if the ce word is null and if the T is -1
             * the w should be a word that clone from itself
             */
            if ( ce == null ) {
                if ( T == -1 ) w = w.clone();
            } else {
                w = ce.clone();
            }
            
            w.setPosition(pos+cjkidx);
            wordPool.add(w);
            cjkidx += w.getLength();
            
            /*
             * check and append the Pinyin and the synonyms words.
            */
            if ( T == -1 ) {
//                appendWordFeatures(w);//xwz
            }
        }
        
        if ( wordPool.size() == 0 ) {
            return null;
        }
        
        return wordPool.remove();
    }
    
    /**
     * @see ASegment#getBestCJKChunk(char[], int) 
     */
    @Override
    public IChunk getBestCJKChunk(char chars[], int index) throws IOException
    {
        IWord[] mwords = getNextMatch(chars, index), mword2, mword3;
        if ( mwords.length == 1 
                && mwords[0].getType() == ILexicon.UNMATCH_CJK_WORD ) {
            return new Chunk(new IWord[]{mwords[0]});
        }
        
        int idx_2, idx_3;
        ArrayList<IChunk> chunkArr = new ArrayList<IChunk>();
        for ( int x = 0; x < mwords.length; x++ ) {
            //the second layer
            idx_2 = index + mwords[x].getLength();
            if ( idx_2 < chars.length ) {
                mword2 = getNextMatch(chars, idx_2);
                /*
                 * the first try for the second layer
                 * returned a UNMATCH_CJK_WORD
                 * here, just return the largest length word in
                 * the first layer. 
                 */
                if ( mword2.length == 1
                        && mword2[0].getType() == ILexicon.UNMATCH_CJK_WORD) {
                    return new Chunk(new IWord[]{mwords[mwords.length - 1]});
                }
                
                for ( int y = 0; y < mword2.length; y++ ) {
                    //the third layer
                    idx_3 = idx_2 + mword2[y].getLength();
                    if ( idx_3 < chars.length ) {
                        mword3 = getNextMatch(chars, idx_3);
                        for ( int z = 0; z < mword3.length; z++ ) {
                            ArrayList<IWord> wArr = new ArrayList<IWord>(3);
                            wArr.add(mwords[x]);
                            wArr.add(mword2[y]);
                            if ( mword3[z].getType() != ILexicon.UNMATCH_CJK_WORD )
                                wArr.add(mword3[z]);
                            
                            IWord[] words = new IWord[wArr.size()];
                            wArr.toArray(words);
                            wArr.clear();
                            
                            chunkArr.add(new Chunk(words));
                        }
                    } else {
                        chunkArr.add(new Chunk(new IWord[]{mwords[x], mword2[y]}));
                    }
                }
            } else {
                chunkArr.add(new Chunk(new IWord[]{mwords[x]}));
            }
        }
        
        if ( chunkArr.size() == 1 ) {
            return chunkArr.get(0);
        }
        
/*        Iterator<IChunk> it = chunkArr.iterator();
        while ( it.hasNext() ) {
            System.out.println(it.next());
        }
        System.out.println("-+---------------------+-");*/
        
        IChunk[] chunks = new IChunk[chunkArr.size()];
        chunkArr.toArray(chunks);
        chunkArr.clear();
        
        mwords = null;
        mword2 = null;
        mword3 = null;
        
        
        //-------------------------MMSeg core invoke------------------------
        
        //filter the maximum match rule.
        IChunk[] afterChunks = MMSegFilter.getMaximumMatchChunks(chunks);
        if ( afterChunks.length == 1 ) {
            return afterChunks[0];
        }
        
        //filter the largest average rule.
        afterChunks = MMSegFilter.getLargestAverageWordLengthChunks(afterChunks);
        if ( afterChunks.length == 1 ) {
            return afterChunks[0];
        }
        
        //filter the smallest variance rule.
        afterChunks = MMSegFilter.getSmallestVarianceWordLengthChunks(afterChunks);
        if ( afterChunks.length == 1 ) {
            return afterChunks[0];
        }
        
        //filter the largest sum of degree of morphemic freedom rule.
        afterChunks = MMSegFilter.getLargestSingleMorphemicFreedomChunks(afterChunks);
        if ( afterChunks.length == 1 ) {
            return afterChunks[0];
        }
        
        //consider this as the final rule
        //Change it to return the last chunk at 2017/07/04
        //return afterChunks[0];
        return afterChunks[afterChunks.length - 1];
    }
    
}
