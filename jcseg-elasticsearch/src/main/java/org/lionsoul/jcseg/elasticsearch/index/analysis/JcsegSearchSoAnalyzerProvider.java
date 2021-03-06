package org.lionsoul.jcseg.elasticsearch.index.analysis;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.lionsoul.jcseg.tokenizer.core.JcsegTaskConfig;

/**
 * Jcseg simple Analyzer Provider
 * 
 * @author xwz
 */
public class JcsegSearchSoAnalyzerProvider extends JcsegAnalyzerProvider
{
    public JcsegSearchSoAnalyzerProvider(IndexSettings indexSettings,
            Environment env, String name, Settings settings)
    {
        super(indexSettings, env, name, settings);
    }

    @Override
    protected int getSegMode()
    {
        return JcsegTaskConfig.SEARCHSO_MODE;
    }
    
}
