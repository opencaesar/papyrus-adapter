/**
 * 
 * Copyright 2021 Modelware Solutions and CAE-LIST.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.opencaesar.oml2papyrus;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

public class Oml2PapyrusTask extends DefaultTask {
	
	public String inputOntologyPath = null;

	public String inputProfilePath = null;

	public String outputFolderPath = null;
	
	public boolean forceReifiedLinks = false;
	
	public boolean debug;

    @TaskAction
    public void run() {
        List<String> args = new ArrayList<String>();
        if (inputOntologyPath != null) {
		    args.add("-i");
		    args.add(inputOntologyPath);
        }
        if (inputProfilePath != null) {
		    args.add("-p");
		    args.add(inputProfilePath);
        }
        if (outputFolderPath != null) {
		    args.add("-o");
		    args.add(outputFolderPath);
        }
	    if (debug) {
		    args.add("-d");
	    }
	    if (forceReifiedLinks) {
	    	args.add("-f");
	    }
	    try {
	    	Oml2PapyrusApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
   	}
    
}