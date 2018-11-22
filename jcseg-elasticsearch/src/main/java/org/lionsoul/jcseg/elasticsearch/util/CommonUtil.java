package org.lionsoul.jcseg.elasticsearch.util;

import org.elasticsearch.common.settings.Settings;
import org.lionsoul.jcseg.tokenizer.core.JcsegTaskConfig;

public class CommonUtil 
{
    /**
     * The default jcseg configuration file 
    */
//    public static final String JcsegConfigFile = "plugins/jcseg/jcseg.properties";
	public static final String JcsegConfigFile = "jcseg/jcseg.properties";//xwz
    
    /**
     * get the sementation mode and default to COMPLEX_MODE
     * 
     * @param   settings
     * @return  int
    */
    public static int getSegMode(Settings settings)
    {
        return getSegMode(settings, JcsegTaskConfig.COMPLEX_MODE);
    }
    
    /**
     * get the segmentation mode
     * 
     * @param   settings
     * @param   int
     * @return  int
    */
    public static int getSegMode(Settings settings, int default_mode)
    {
        String seg_mode = settings.get("seg_mode");
        if ( seg_mode == null ) {
            return default_mode;
        }
        
        int mode = default_mode;
        if( "complex".equals(seg_mode) ) {
            mode = JcsegTaskConfig.COMPLEX_MODE;
        } else if ( "simple".equals(seg_mode) ) {
            mode = JcsegTaskConfig.SIMPLE_MODE;
        } else if ( "detect".equals(seg_mode) ) {
            mode = JcsegTaskConfig.DETECT_MODE;
        } else if ( "search".equals(seg_mode) ) {
            mode = JcsegTaskConfig.SEARCH_MODE;
        } else if ( "nlp".equals(seg_mode) ){
            mode = JcsegTaskConfig.NLP_MODE;
        } else if ( "searchso".equals(seg_mode) ){//xwz
            mode = JcsegTaskConfig.SEARCHSO_MODE;
        } else if ( "complexso".equals(seg_mode) ){//xwz
            mode = JcsegTaskConfig.COMPLEXSO_MODE;
        } else if ( "delimiter".equals(seg_mode) ) {
            mode = JcsegTaskConfig.DELIMITER_MODE;
        } else if ( "searchup".equals(seg_mode) ) {
            mode = JcsegTaskConfig.SEARCHUP_MODE;
        } else if ( "searchupso".equals(seg_mode) ) {
            mode = JcsegTaskConfig.SEARCHUPSO_MODE;
        }
        
        return mode;
    }
    
}
