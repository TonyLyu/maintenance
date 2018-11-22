package org.lionsoul.jcseg.tokenizer.core;

import java.io.Reader;
import java.lang.reflect.Constructor;

import org.lionsoul.jcseg.tokenizer.ComplexSeg;
import org.lionsoul.jcseg.tokenizer.ComplexSoSeg;
import org.lionsoul.jcseg.tokenizer.DelimiterSeg;
import org.lionsoul.jcseg.tokenizer.DetectSeg;
import org.lionsoul.jcseg.tokenizer.NLPSeg;
import org.lionsoul.jcseg.tokenizer.SearchSeg;
import org.lionsoul.jcseg.tokenizer.SearchSoSeg;
import org.lionsoul.jcseg.tokenizer.SearchupSeg;
import org.lionsoul.jcseg.tokenizer.SearchupSoSeg;
import org.lionsoul.jcseg.tokenizer.SimpleSeg;

/**
 * <p>
 * Segment factory to create singleton ISegment object
 * a path of the class that has implemented the ISegment interface must be given first
 * </p>
 * 
 * @author    chenxin<chenxin619315@gmail.com>
 */
public class SegmentFactory 
{
    //current Jcseg version.
    public static final String version = "2.1.1";
    
    /**
     * load the ISegment class with the given path
     * 
     * @param     _class
     * @return ISegment
     */
    public static ISegment createSegment( Class<? extends ISegment> _class,
                Class<?> paramtypes[], Object args[] ) 
    {
        ISegment seg = null;
        try {
            Constructor<?> cons = _class.getConstructor(paramtypes);
            seg = (ISegment) cons.newInstance(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("can't load the ISegment implements class " +
                    "with path ["+_class.getName()+"] ");
        }
        
        return seg;
    }
    
    /**
     * create the specified mode Jcseg instance
     * 
     * @param    mode
     * @return    ISegment
     * @throws JcsegException 
     */
    public static ISegment createJcseg( int mode, Object...args ) throws JcsegException 
    {
        Class<? extends ISegment> _clsname;
        switch ( mode ) {
        case JcsegTaskConfig.SIMPLE_MODE:
            _clsname = SimpleSeg.class;
            break;
        case JcsegTaskConfig.COMPLEX_MODE:
            _clsname = ComplexSeg.class;
            break;
        case JcsegTaskConfig.COMPLEXSO_MODE://xwz
            _clsname = ComplexSoSeg.class;
            break;
        case JcsegTaskConfig.DETECT_MODE:
            _clsname = DetectSeg.class;
            break;
        case JcsegTaskConfig.SEARCH_MODE:
            _clsname = SearchSeg.class;
            break;
        case JcsegTaskConfig.SEARCHSO_MODE://xwz
            _clsname = SearchSoSeg.class;
            break;
        case JcsegTaskConfig.DELIMITER_MODE:
            _clsname = DelimiterSeg.class;
            break;
        case JcsegTaskConfig.NLP_MODE:
            _clsname = NLPSeg.class;
            break;
        case JcsegTaskConfig.SEARCHUP_MODE:
            _clsname = SearchupSeg.class;
            break;
        case JcsegTaskConfig.SEARCHUPSO_MODE:
            _clsname = SearchupSoSeg.class;
            break;
        default:
            throw new JcsegException("No Such Algorithm Excpetion");
        }   
        
        Class<?>[] _paramtype = null;
        if ( args.length == 2 ) {
            _paramtype = new Class[]{JcsegTaskConfig.class, ADictionary.class};
        } else if ( args.length == 3 ) {
            _paramtype = new Class[]{Reader.class, JcsegTaskConfig.class, ADictionary.class};
        } else {
            throw new JcsegException("length of the arguments should be 2 or 3");
        }
        
        return createSegment(_clsname, _paramtype, args);
    }
}
