package org.lionsoul.jcseg.server.controller;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.lionsoul.jcseg.extractor.impl.TextRankSummaryExtractor;
import org.lionsoul.jcseg.sentence.SentenceSeg;
import org.lionsoul.jcseg.server.JcsegController;
import org.lionsoul.jcseg.server.JcsegGlobalResource;
import org.lionsoul.jcseg.server.JcsegTokenizerEntry;
import org.lionsoul.jcseg.server.core.GlobalResource;
import org.lionsoul.jcseg.server.core.ServerConfig;
import org.lionsoul.jcseg.server.core.UriEntry;
import org.lionsoul.jcseg.tokenizer.core.ISegment;
import org.lionsoul.jcseg.tokenizer.core.JcsegException;
import org.lionsoul.jcseg.tokenizer.core.JcsegTaskConfig;
import org.lionsoul.jcseg.tokenizer.core.SegmentFactory;

/**
 * keywords extractor handler
 * 
 * @author  chenxin<chenxin619315@gmail.com>
*/
public class SentenceController extends JcsegController
{

    public SentenceController(
            ServerConfig config,
            GlobalResource globalResource, 
            UriEntry uriEntry,
            Request baseRequest, 
            HttpServletRequest request,
            HttpServletResponse response) throws IOException
    {
        super(config, globalResource, uriEntry, baseRequest, request, response);
    }

    @Override
    protected void run(String method) throws IOException
    {
        String text = getString("text");
        int number = getInt("number", 6);
        if ( text == null || "".equals(text) ) {
            response(STATUS_INVALID_ARGS, "Invalid Arguments");
            return;
        }
        
        JcsegGlobalResource resourcePool = (JcsegGlobalResource)globalResource;
        JcsegTokenizerEntry tokenizerEntry = resourcePool.getTokenizerEntry("extractor");
        if ( tokenizerEntry == null ) {
            response(STATUS_INVALID_ARGS, "can't find tokenizer instance \"extractor\"");
            return;
        }
        
        try {
            ISegment seg = SegmentFactory
                    .createJcseg(JcsegTaskConfig.COMPLEX_MODE, 
                            new Object[]{tokenizerEntry.getConfig(), tokenizerEntry.getDict()});
            
            TextRankSummaryExtractor extractor = new TextRankSummaryExtractor(seg, new SentenceSeg());
            extractor.setSentenceNum(number);
            
            long s_time = System.nanoTime();
            List<String> sentence = extractor.getKeySentenceFromString(text);
            double c_time = (System.nanoTime() - s_time)/1E9;
            
            Map<String, Object> map = new HashMap<String, Object>();
            DecimalFormat df = new DecimalFormat("0.00000"); 
            map.put("took", Float.valueOf(df.format(c_time)));
            map.put("sentence", sentence);
            
            //response the request
            response(STATUS_OK, map);
        } catch (JcsegException e) {
            response(STATUS_INTERNEL_ERROR, "Internal error...");
        }
    }

}
