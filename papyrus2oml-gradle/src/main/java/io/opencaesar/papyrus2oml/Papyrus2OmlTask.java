package io.opencaesar.papyrus2oml;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

public class Papyrus2OmlTask extends DefaultTask {
	
	public String inputModelPath;
	
	public String outputCatalogPath;
	
	public List<String> ignoredIriPrefixes;

	public String conversionType;
	
	public boolean debug;

    @TaskAction
    public void run() {
        List<String> args = new ArrayList<String>();
        if (inputModelPath != null) {
		    args.add("-i");
		    args.add(inputModelPath);
        }
        if (outputCatalogPath != null) {
		    args.add("-o");
		    args.add(outputCatalogPath);
        }
        if (ignoredIriPrefixes != null) {
	        for (String p : ignoredIriPrefixes) {
			    args.add("-p");
			    args.add(p);
	        }
        }
        if (conversionType != null) {
		    args.add("-c");
		    args.add(conversionType);
        }
	    if (debug) {
		    args.add("-d");
	    }
	    try {
	    	Papyrus2OmlApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
   	}
    
}