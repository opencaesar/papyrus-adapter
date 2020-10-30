package io.opencaesar.oml2papyrus;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import io.opencaesar.oml2papyrus.Oml2PapyrusApp;

public class Oml2PapyrusTask extends DefaultTask {
	
	public String inputFolderPath = null;

	public String inputOntologyPath = null;

	public String outputCatalogPath = null;
	
	public boolean debug;

    @TaskAction
    public void run() {
        List<String> args = new ArrayList<String>();
        if (inputFolderPath != null) {
		    args.add("-i");
		    args.add(inputFolderPath);
        }
        if (inputOntologyPath != null) {
		    args.add("-r");
		    args.add(inputOntologyPath);
        }
        if (outputCatalogPath != null) {
		    args.add("-o");
		    args.add(outputCatalogPath);
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