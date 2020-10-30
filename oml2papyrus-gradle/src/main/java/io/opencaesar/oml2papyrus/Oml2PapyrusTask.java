package io.opencaesar.oml2papyrus;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

public class Oml2PapyrusTask extends DefaultTask {
	
	public String inputCatalogPath = null;

	public String inputOntologyPath = null;

	public String outputFolderPath = null;
	
	public boolean debug;

    @TaskAction
    public void run() {
        List<String> args = new ArrayList<String>();
        if (inputCatalogPath != null) {
		    args.add("-i");
		    args.add(inputCatalogPath);
        }
        if (inputOntologyPath != null) {
		    args.add("-r");
		    args.add(inputOntologyPath);
        }
        if (outputFolderPath != null) {
		    args.add("-o");
		    args.add(outputFolderPath);
        }
	    if (debug) {
		    args.add("-d");
	    }
	    try {
	    	Oml2PapyrusApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
   	}
    
}