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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Class to interact with the Bioimage.io API. Used to get information
 * about models and to download them
 * @author Carlos Javier Garcia Lopez de Haro
 *
 */
public class BioimageZooRepository {
	/**
	 * URL to the file containing all the model zoo models
	 */
	public static String location = "https://raw.githubusercontent.com/bioimage-io/collection-bioimage-io/gh-pages/collection.json";
	/**
	 * Map containing the models with their name as key
	 */
	public HashMap<String, Model> models = new HashMap<String, Model>();
	/**
	 * JSon containing all the info about the Bioimage.io models
	 */
	private JsonArray collections;
	/**
	 * List of all the model IDs of the models existing in the BioImage.io
	 */
	private static List<String> modelIDs;
	public String title = "BioImage Model Zoo";
	public String name = "BioImage Model Zoo";

	public String url = "https://bioimage.io/";
	
	public static void main(String[] args) {
		connect().listAllModels();
	}
	
	public BioimageZooRepository() {
		setCollectionsRepo();
	}
	
	/**
	 * Create an instance of the models stored in the Bioimage.io repository reading the 
	 * collections rdf.yaml.
	 * @return an instance of the {@link ModelRepo}
	 */
	public static BioimageZooRepository connect() {
		return new BioimageZooRepository();
	}
	
	/**
	 * Method that connects to the BioImage.io API and retrieves the models available
	 * at the Bioimage.io model repository
	 * @return an object containing the zip location of the model as key and the {@link ModelDescriptor}
	 * 	with the yaml file information in the value
	 */
	public void listAllModels() {
		models = new HashMap<String, Model>();
		if (collections == null) {
			return;
		}
		for (Object resource : collections) {
			JsonObject jsonResource = (JsonObject) resource;
			try {
				if (jsonResource.get("type") == null || !jsonResource.get("type").getAsString().equals("model"))
					continue;
				if (jsonResource.get("links") == null) 
					continue;
				JsonElement links = jsonResource.get("links").getAsJsonArray();
				JsonElement tags = jsonResource.get("tags").getAsJsonArray();
				if (links == null || !links.toString().toLowerCase().contains("deepimagej/deepimagej"))
					continue;
				String stringRDF = getJSONFromUrl(jsonResource.get("rdf_source").getAsString());
				Model mm = Model.build(stringRDF);
				if (mm == null)
					continue;
				models.put(mm.name, mm);
			} catch (Exception ex) {
                ex.printStackTrace();
			}
		}
	}
	
	public HashMap<String, Model> getModels() {
		if (models.size() == 0)
			listAllModels();
		return models;
	}
	
	/**
	 * Method that stores all the model IDs for the models available in the BIoImage.io repo
	 */
	private void setCollectionsRepo() {
		modelIDs = new ArrayList<String>();
		String text = getJSONFromUrl(location);
		if (text == null) {
			return;
		}
		JsonObject json = new JsonParser().parse(text).getAsJsonObject();
		// Iterate over the array corresponding to the key: "resources"
		// which contains all the resources of the Bioimage.io
		collections = (JsonArray) json.get("collection");
		if (collections == null) {
			return;
		}
		for (Object resource : collections) {
			JsonObject jsonResource = (JsonObject) resource;
			if (jsonResource.get("type") == null || !jsonResource.get("type").getAsString().equals("model"))
				continue;
			String modelID = jsonResource.get("id").getAsString();
			modelIDs.add(modelID);
		}
	}

	/**
	 * Method used to read a yaml or json file from a server as a raw string
	 * @param url
	 * 	String url of the file
	 * @return a String representation of the file. It is null if the file was not accessed
	 */
	private static String getJSONFromUrl(String url) {

		HttpsURLConnection con = null;
		try {
			URL u = new URL(url);
			con = (HttpsURLConnection) u.openConnection();
			con.connect();
			InputStream inputStream = con.getInputStream();
			
			 ByteArrayOutputStream result = new ByteArrayOutputStream();
			 byte[] buffer = new byte[1024];
			 for (int length; (length = inputStream.read(buffer)) != -1; ) {
			     result.write(buffer, 0, length);
			 }
			 // StandardCharsets.UTF_8.name() > JDK 7
			 String txt = result.toString("UTF-8");
			 inputStream.close();
			 result.close();
			 return txt;
		} 
		catch (MalformedURLException ex) {
			ex.printStackTrace();
		} 
		catch (IOException ex) {
			ex.printStackTrace();
		} 
		finally {
			if (con != null) {
				try {
					con.disconnect();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return null;
	}
	
	/**
	 * Create {@link Path} from Url String. This method removes the http:// or https://
	 * at the begining because in windows machines it caused errors creating Paths
	 * @param downloadUrl
	 * 	String url of the model of interest
	 * @return the path to the String url
	 */
	public static Path createPathFromURLString(String downloadUrl) {
		Path path;
		try {
			if (downloadUrl.startsWith("https://")) {
				downloadUrl = downloadUrl.substring(("https://").length());
			} else if (downloadUrl.startsWith("http://")) {
				downloadUrl = downloadUrl.substring(("http://").length());
			}
			path = new File(downloadUrl).toPath();
		} catch (Exception ex) {
			int startName = downloadUrl.lastIndexOf("/");
			downloadUrl = downloadUrl.substring(startName + 1);
			path = new File(downloadUrl).toPath();
		}
		return path;
	}
	
	/**
	 * Return a list with all the model IDs for the models existing in the Bioimage.io repo
	 * @return list with the ids for each of the models in the repo
	 */
	public static List<String> getModelIDs(){
		return modelIDs;
	}
}

