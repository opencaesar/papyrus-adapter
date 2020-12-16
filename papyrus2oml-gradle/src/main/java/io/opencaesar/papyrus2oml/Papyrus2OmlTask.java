package io.opencaesar.papyrus2oml;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

public class Papyrus2OmlTask extends DefaultTask {
	
	public String inputFolderPath = null;

	public String inputModelPath = null;
	
	public String outputCatalogPath = null;
	
	public boolean debug;

    @TaskAction
    public void run() {
        List<String> args = new ArrayList<String>();
        if (inputFolderPath != null) {
		    args.add("-i");
		    args.add(inputFolderPath);
        }
        if (inputModelPath != null) {
		    args.add("-m");
		    args.add(inputModelPath);
        }
        if (outputCatalogPath != null) {
		    args.add("-o");
		    args.add(outputCatalogPath);
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