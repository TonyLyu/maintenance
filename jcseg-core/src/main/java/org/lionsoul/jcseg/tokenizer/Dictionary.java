package org.lionsoul.jcseg.tokenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.lionsoul.jcseg.tokenizer.core.ADictionary;
import org.lionsoul.jcseg.tokenizer.core.AutoLoadFile;
import org.lionsoul.jcseg.tokenizer.core.ILexicon;
import org.lionsoul.jcseg.tokenizer.core.IWord;
import org.lionsoul.jcseg.tokenizer.core.JcsegTaskConfig;

/**
 * Dictionary class
 * 
 * @author    chenxin<chenxin619315@gmail.com>
 */
public class Dictionary extends ADictionary
{
    
    /**hash table for the words*/
    private Map<String, IWord>[] dics = null;
    
    /*auto load thread*/
    private Thread autoloadThread = null;
    /*需要新增的词的文件后缀*/
    public static final String ADDWORDS_FILE_SUFFIX = "_add.lex";
    /*需要删除词的文件后缀*/
    public static final String DELETEWORDS_FILE_SUFFIX = "_delete.lex";
    /*需要线程监控的文件后缀*/
    public static final String TODO_FILE_SUFFIX = ".todo";
    
    @SuppressWarnings("unchecked")
    public Dictionary( JcsegTaskConfig config, Boolean sync )
    {
        super(config, sync);
        
        dics = new Map[ILexicon.T_LEN];
        if ( this.sync ) {
            for ( int j = 0; j < ILexicon.T_LEN; j++ ) {
                dics[j] = new ConcurrentHashMap<String, IWord>(16, 0.80F);
            }
        } else {
            for ( int j = 0; j < ILexicon.T_LEN; j++ ) {
                dics[j] = new HashMap<String, IWord>(16, 0.80F);
            }
        }
    }
    
    /**
     * @see ADictionary#match(int, String)
     */
    @Override
    public boolean match(int t, String key)
    {
        if ( t >= 0 && t < ILexicon.T_LEN ) {
            return dics[t].containsKey(key);
        }
        return false;
    }
     
    /**
     * @see ADictionary#add(int, IWord) 
    */
    @Override
    public IWord add(int t, IWord word)
    {
        if ( t >= 0 && t < ILexicon.T_LEN ) {
            if ( dics[t].containsKey(word.getValue()) ) {
                return dics[t].get(word.getValue());
            }
            
            dics[t].put(word.getValue(), word);
            return word;
        }
        
        return null;
    }

    /**
     * @see ADictionary#add(int, String, int, int, String) 
    */
    @Override
    public IWord add(int t, String key, int fre, int type, String entity)
    {
        if ( t >= 0 && t < ILexicon.T_LEN ) {
            if ( dics[t].containsKey(key) ) {
                return dics[t].get(key);
            }
            
            IWord word = new Word(key, fre, type, entity);
            dics[t].put(key, word);
            return word;
        }
        
        return null;
    }
    
    /**
     * @see ADictionary#add(int, String, int) 
    */
    @Override
    public IWord add(int t, String key, int type)
    {
        return add(t, key, 0, type, null);
    }

    /**
     * @see ADictionary#add(int, String, int, int) 
    */
    @Override
    public IWord add(int t, String key, int fre, int type)
    {
        return add(t, key, fre, type, null);
    }

    /**
     * @see ADictionary#add(int, String, int, String) 
    */
    @Override
    public IWord add(int t, String key, int type, String entity)
    {
        return add(t, key, 0, type, entity);
    }

    /**
     * @see ADictionary#get(int, String) 
    */
    @Override
    public IWord get(int t, String key)
    {
        if ( t >= 0 && t < ILexicon.T_LEN ) {
            return dics[t].get(key);
        }
        return null;
    }

    /**
     * @see ADictionary#remove(int, String) 
    */
    @Override
    public void remove(int t, String key)
    {
        if ( t >= 0 && t < ILexicon.T_LEN ) {
            dics[t].remove(key);
        }
    }
    
    /**
     * @see ADictionary#size(int) 
    */
    @Override
    public int size(int t)
    {
        if ( t >= 0 && t < ILexicon.T_LEN ) {
            return dics[t].size();
        }
        return 0;
    }
    @Override
   	public void startAutoload()
   	{
   		if(autoloadThread != null || config.getLexiconPath() == null)
   		{
   			return;
   		}
   		//create and start the lexion auto load thread
   		autoloadThread = new Thread(new Runnable() {
   			
   			@Override
   			public void run() {
   				String[] paths = config.getLexiconPath();
   				AutoLoadFile autofile = null;
   				List<AutoLoadFile> autoLoadFiles = new ArrayList<AutoLoadFile>();
   				Set<String> todoFileNames = new HashSet<String>();
   				/*遍历所有配置的词典目录，从配置的词典目录中找到所有以todo结尾的文件，并对这些文件进行监听*/
   				for(int i = 0; i < paths.length; i++)
   				{
   					File dir = new File(paths[i]);
   					if(dir.isDirectory()){
   						File files[] = dir.listFiles();
   						for(File file : files){
   							if(file.getAbsolutePath().endsWith(TODO_FILE_SUFFIX)){
   								autofile = new AutoLoadFile(file.getAbsolutePath());
   								autofile.setLastUpdateTime(file.lastModified());
   								autoLoadFiles.add(autofile);
   								todoFileNames.add(file.getAbsolutePath());
   							}
   						}
   					}
   				}
   				while(true){
   					//检查是否有新增的todo文件
   					for( int i=0; i < paths.length; i++){
   						File dir = new File(paths[i]);
   						if(dir.isDirectory()){
   							File files[] = dir.listFiles();
   							for(File file : files){
   								if(file.getAbsolutePath().endsWith(TODO_FILE_SUFFIX) 
   										&& !todoFileNames.contains(file.getAbsolutePath())){
   									autofile = new AutoLoadFile(file.getAbsolutePath());
   									autofile.setLastUpdateTime(file.lastModified());
   									autoLoadFiles.add(autofile);
   									todoFileNames.add(file.getAbsolutePath());
   								}
   							}
   						}
   					}
   					//sleep for some time (seconds)
   					try{
   						Thread.sleep(config.getPollTime() * 1000);
   					} catch (InterruptedException e) {break;};
   					
   					//check the update of all the reload todo files
   					File f = null;
   					for(AutoLoadFile af:autoLoadFiles){
   						f = af.getFile();
   						if(!f.exists()) continue;
   						if( f.lastModified() <= af.getLastUpdateTime()){
   							continue;
   						}
   						
   						
   						//load words from the lexion files
   						try{
   							BufferedReader reader = new BufferedReader(new FileReader(f));
   							String line = null;
   							while((line = reader.readLine()) != null){
   								line = line.trim();
   								if(line.indexOf('#') != -1) continue;
   								if("".equals(line)) continue;
   								//替换todo后缀为删除词文件的后缀，读取文件中的删除新词条，并删除
   								delete(f.getAbsolutePath().replaceAll(TODO_FILE_SUFFIX, DELETEWORDS_FILE_SUFFIX));
   								//替换todo后缀为新增词文件的后缀，读取文件中的新增词条，并新增
   								load(f.getAbsolutePath().replaceAll(TODO_FILE_SUFFIX, ADDWORDS_FILE_SUFFIX));
   								
   							}
   							
   							//读取词典成功后，生成标志文件，记录es哪个节点的jcseg实例动态读取词典成功
   							SimpleDateFormat formater = new SimpleDateFormat("YYYYMMddHHmmss");
   							String ymd = formater.format(f.lastModified());
   							String replacement = "result/" + f.getName().substring(0, f.getName().length()-TODO_FILE_SUFFIX.length()) + "_" + config.getEsNodeName() + "_" + ymd + ".completed";
   							String resultFilePath = f.getAbsolutePath().replaceAll(f.getName(), replacement);
   							System.out.println("结果文件路径: " + resultFilePath);
   							File fresult = new File(resultFilePath);
   							if(!fresult.exists()){
   								fresult.createNewFile();
   								fresult.setReadable(true);
   								fresult.setWritable(true);
   						    }
   						} catch (Exception e) {
   							System.out.println("发生异常，异常原因为: " + e.getMessage());
   							SimpleDateFormat formater = new SimpleDateFormat("YYYYMMddHHmmss");
   							String ymd = formater.format(f.lastModified());
   							String errorReplacement = "result/" + f.getName().substring(0, f.getName().length()-TODO_FILE_SUFFIX.length()) + "_" + config.getEsNodeName() + "_" + ymd + ".error";
   							String errorFilePath = f.getAbsolutePath().replaceAll(f.getName(), errorReplacement);
   							File ferror = new File(errorFilePath);
   							if(!ferror.exists()) {
   								try {
   									ferror.createNewFile();
   								} catch (IOException e1){
   									System.out.println("jcseg make ferror failed: reason is : " + e1.getMessage());
   								}
   							}
   						}
   						/**
   						 * update the last update time
   						 * @Note: some file system may close the in-time last update time update
   						 *       in that case, this won't work normally.
   						 * but, it will still work!!!!!!!
   						 */
   						
   						     af.setLastUpdateTime(f.lastModified());
   					}
   						
   				}
   				
   				
   			}
   		});
   		
   		autoloadThread.setDaemon(true);//创建其为守护线程
   		autoloadThread.start();
   	}
       
       
       /**
        * 将指定路径下的文件中的词从字典中删除
        * @param file 删除词所在的文件路径
        * @throws FileNotFoundException
        * @throws IOException
        */
       public void delete(String file) throws IOException {
       	File testfile = new File(file);
       	if(testfile.exists()){
       		deleteWords(config,this,file);
       	}
       }
       /**
        * 删除字典目录中指定文件中的词语
        * @param config  jcseg配置文件
        * @param dic  单例字典
        * @param file 记录删除词的文件
        * @throws FileNotFoundException
        * @throws IOException
        */
       public void deleteWords(JcsegTaskConfig config, ADictionary dic, String file) throws FileNotFoundException, IOException {
       	boolean isFirstLine = true;
       	int t = -1;
       	String line = null;
       	BufferedReader buffReader = new BufferedReader(new FileReader(file));
       	while((line = buffReader.readLine()) != null ) {
       		line = line.trim();
       		if("".equals(line)) continue;
       		if(line.charAt(0) == '#' && line.length() > 1){
       			continue;
       		}
       		
       		//the first line for the lexicon file
       		if(isFirstLine == true) {
       			t = ADictionary.getIndex(line);
       			isFirstLine = false;
       			if(t >= 0){
       				continue;
       			}
       		}
       		
       		//删除某一类词，t表示类别
       		dic.remove(t, line);
       	}
       	buffReader.close();
       	buffReader = null;
       }
}
