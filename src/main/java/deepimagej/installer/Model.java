/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 * Science for Life Laboratory, School of Engineering Sciences in Chemistry, Biotechnology and Health, KTH - Royal Institute of Technology, Sweden
 * 
 * Authors: Carlos Garcia-Lopez-de-Haro and Estibaliz Gomez-de-Mariscal
 *
 */

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019-2021, DeepImageJ
 * All rights reserved.
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package deepimagej.installer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bioimageanalysis.icy.deepicy.model.description.SampleImage;
import org.bioimageanalysis.icy.deepicy.model.description.weights.ModelWeight;
import org.bioimageanalysis.icy.deepicy.model.description.weights.WeightFormatInterface;
import org.yaml.snakeyaml.Yaml;

public class Model {

	public String id;
	public String name;
	public String description;
	public String doc;
	public String downloadUrl;
	public List<String> downloadLinks = new ArrayList<String>();
	public List<String> covers;
	public List<Author> authors;
	public boolean deepImageJ = true;
	public Map<String, Object> allModelInfo;
	public String rdf_source;
	
	private Model(Map<String, Object> map) throws Exception {
		allModelInfo = map;
		setName();
		setAuthors();
		setCovers();
		setDescription();
		setId();
		setRdfSource();
		setDownloadLinks();
		setDownloadUrl();

	}
	
	private void setDownloadLinks() {
		setDownloadUrl();
		if (this.downloadUrl != null && checkURL(downloadUrl)) {
			downloadLinks.add(downloadUrl);
			return;
		}
		addAttachments();
		addRDF();
		addSampleInputs();
		addSampleOutputs();
		addTestInputs();
		addTestOutputs();
		addWeights();
		
	}
	
	/**
	 * Add weight links to the downloadable links
	 */
	private void addWeights() {
		ModelWeight weights = descriptor.getWeights();
		int c = 0;
		for (String ww : weights.getEnginesListWithVersions()) {
			try {
				WeightFormatInterface w = weights.getWeightsByIdentifier(ww);
				if (w.getSource() != null && checkURL(w.getSource())) {
					downloadableLinks.put(weightsKey + "_" + c ++, w.getSource());
				}
			} catch (Exception ex) {
				// The exception is thrown whenever the weight format is not present.
				// This exception will not be thrown here because the weight formats are retrieved from the same object
			}
		}
	}
	
	/**
	 * Add the test inputs to the downloadable links
	 */
	private void addTestInputs() {
		List<String> sampleInps = descriptor.getTest_inputs();
		if (sampleInps == null)
			return;
		int c = 0;
		for (String ss : sampleInps) {
			if (ss != null && checkURL(ss)) {
				downloadableLinks.put(testInputsKey + "_" + c ++, ss);
			}
		}
	}
	
	/**
	 * Add the test outputs to the dowloadable links
	 */
	private void addTestOutputs() {
		List<String> sampleOuts = descriptor.getTest_outputs();
		if (sampleOuts == null)
			return;
		int c = 0;
		for (String ss : sampleOuts) {
			if (ss != null && checkURL(ss)) {
				downloadableLinks.put(testOutputsKey + "_" + c ++, ss);
			}
		}
	}
	
	/**
	 * Add the sample inputs to the dowloadable links
	 */
	private void addSampleInputs() {
		List<SampleImage> sampleInps = descriptor.getSample_inputs();
		if (sampleInps == null)
			return;
		int c = 0;
		for (SampleImage ss : sampleInps) {
			if (ss != null && ss.getUrl() != null) {
				downloadableLinks.put(sampleInputsKey + "_" + c ++, ss.getString());
			}
		}
	}
	
	/**
	 * Add the sample outputs to the dowloadable links
	 */
	private void addSampleOutputs() {
		List<SampleImage> sampleOuts = descriptor.getSample_outputs();
		if (sampleOuts == null)
			return;
		int c = 0;
		for (SampleImage ss : sampleOuts) {
			if (ss != null && ss.getUrl() != null) {
				downloadableLinks.put(sampleOutputsKey + "_" + c ++, ss.getString());
			}
		}
	}
	
	/**
	 * Add the rdf.yaml file to the downloadable links of the model
	 */
	private void addRDF() {
		String rdf = descriptor.getRDFSource();
		if (rdf != null && checkURL(rdf)) {
			downloadableLinks.put(rdfKey, rdf);
		}
	}
	
	/**
	 * Add the attachment files to the downloadable links of the model
	 */
	private void addAttachments() {
		Map<String, Object> attachments = descriptor.getAttachments();
		if (attachments == null)
			return;
		int c = 0;
		for (String kk : attachments.keySet()) {
			if (attachments.get(kk) instanceof String && checkURL((String) attachments.get(kk))) {
				downloadableLinks.put(attachmentsKey + "_" + c ++, (String) attachments.get(kk));
			} else if (attachments.get(kk) instanceof URL) {
				downloadableLinks.put(attachmentsKey + "_" + c ++, ((URL) attachments.get(kk)).toString());
			}
		}
	}

	private void setName() {
		if (allModelInfo.get("name") instanceof String)
			name = (String) allModelInfo.get("name");
		else
			throw new IllegalArgumentException("Rdf.yaml does not contain the compulsory field 'name'.");
	}
	
	private void setId() {
		if (allModelInfo.get("id") instanceof String)
			id = (String) allModelInfo.get("id");
		else
			id = "unknown";
	}
	
	// TODO implement a downloader that gets all the links
	private void setDownloadUrl() throws Exception {
		if (allModelInfo.get("download_url") instanceof String)
			downloadUrl = (String) allModelInfo.get("download_url");
		else
			throw new Exception();
	}
	
	private void setDescription() {
		if (allModelInfo.get("description") instanceof String)
			description = (String) allModelInfo.get("description");
		else
			description = "";
	}
	
	private void setCovers() {
		covers = new ArrayList<String>();
		if (allModelInfo.get("covers") instanceof String) {
			covers.add((String) allModelInfo.get("covers"));
		} else if (allModelInfo.get("covers") instanceof List<?>) {
			covers = (List<String>) allModelInfo.get("covers");
		}
	}
	
	private void setAuthors() {
		authors = new ArrayList<Author>();
		if (!(allModelInfo.get("authors") instanceof List<?>))
			return;
		List<Object> authElements = (List<Object>) allModelInfo.get("authors");
        for (Object elem : authElements)
        {
            if (!(elem instanceof Map<?, ?>))
            	continue;
            @SuppressWarnings("unchecked")
            Map<String, String> dict = (Map<String, String>) elem;
            authors.add(Author.build(dict));
        }
	}
	
	public static Model build(String yamlString) {
		Map<String, Object> map = loadFromString(yamlString);
		Model mm = null;
		try {
			mm = new Model(map);
		} catch (Exception ex) {
		}
		return mm;
	}
	
	private void setRdfSource() {
		if (allModelInfo.get("rdf_source") instanceof String)
			rdf_source = (String) allModelInfo.get("rdf_source");
		else
			rdf_source = null;
	}
	

	public String getCoverHTML() {
		if (covers.size() >= 1)
			return "<img src=\"" + covers.get(0) +"\" >";
		else
			return "no cover";
	}

    /**
     * Reads the provided yaml String and loads it into a map of string keys and object values.
     * 
     * @param yamlString
     *        The String yaml file.
     * @return The map loaded with the yaml elements.
     */
    public static Map<String, Object> loadFromString(String yamlString)
    {
    	Yaml yaml = new Yaml();
    	HashMap<String,Object> yamlElements = yaml.load(yamlString);
        return yamlElements;
    }
	
	/**
	 * Check if a String contains a valid URL
	 * @param str
	 * 	str that might contain an URL
	 * @return true if the String corresponds to an URL and false otherwise
	 */
	public static boolean checkURL(String str) {
		try {
			URL url = new URL(str);
		    return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}
	
	
}
