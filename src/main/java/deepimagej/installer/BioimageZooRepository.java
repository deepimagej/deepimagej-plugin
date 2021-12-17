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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BioimageZooRepository {


	public String url = "https://bioimage.io/";
	public String location = "https://raw.githubusercontent.com/deepimagej/models/gh-pages/manifest.bioimage.io.json";
	public String title = "BioImage Model Zoo";
	public String name = "BioImage Model Zoo";

	public HashMap<String, Model> models = new HashMap<String, Model>();
	public ArrayList<String> logs = new ArrayList<String>();
	
	
	public BioimageZooRepository() {
		connect();
	}

	public ArrayList<String> connect() {
		logs.add("Time: " + new Date().toString());
		JsonParser parser2 = new JsonParser();
		try {
			String text = getJSONFromUrl(location);
			JsonObject json2 = (JsonObject) parser2.parse(text);
			logs.add("Name: " + getString(json2, "name", "n.a"));
			name = getString(json2, "name", "n.a");
			title = getString(json2, "splash_title", "n.a");
			JsonArray resources2 = (JsonArray) json2.get("resources");
			if (models != null) {
				for (Object resource : resources2) {
					JsonObject jm2 = (JsonObject) resource;
					Model model2 = parseModel(jm2);
					if (model2 != null && model2.deepImageJ) {
						models.put(model2.getFacename(), model2);
					}
				}
			}
			
		} catch (Exception ex) {
			logs.add("Error: " + ex);
			ex.printStackTrace();
			return logs;
		}
		logs.add("Connected: ");
		return logs;
	}
	
	public HashMap<String, Model> getModels() {
		return models;
	}

	private Model parseModel(JsonObject json) {
		String type = getString(json, "type", "n.a.");
		if (!type.equalsIgnoreCase("model"))
			return null;
		String id = getString(json, "id", "n.a");
		if (id.equalsIgnoreCase("n.a."))
			return null;
		String root_url = getString(json, "root_url", "n.a");
		String name = getString(json, "name", "n.a");
		String desc = getString(json, "description", "n.a");
		String doc = getString(json, "documentation", "n.a");
		String source = getString(json, "source", "n.a");
		String download = getString(json, "download_url", "n.a");
		ArrayList<String> covers = getArray(json, "covers");
		ArrayList<String> tags = getArray(json, "tags");

		String authors = getCSV(json, "authors");

		Model model = new Model(name, id, root_url, desc, authors, doc, source, covers, download);
		for (String tag : tags)
			if (tag.toLowerCase().equals("deepimagej")) {
				model.deepImageJ = true;
			}
	
		return model;
	}
	
	private String getString(JsonObject json, String tag, String defaultValue) {
		try {
			String o = json.get(tag).getAsString();
			return o;
		} catch (Exception ex) {
			return defaultValue;
		}
	}

	private ArrayList<String> getArray(JsonObject json, String tag) {
		ArrayList<String> array = new ArrayList<String>();
		if (json.get(tag).isJsonArray()) {
			JsonArray objects = json.get(tag).getAsJsonArray();
			for (JsonElement o : objects) 
				array.add(o.getAsString());
		}
		return array;
	}

	private String getCSV(JsonObject json, String tag) {
		String csv = "";
		int count = 0;
		if (json.get(tag).isJsonArray()) {
			JsonArray objects = json.get(tag).getAsJsonArray();
			for (JsonElement o : objects) {
				try {
				csv += (count == 0  ? "" : ", ") + o.getAsString();
				count++;
				} catch (Exception ex) {
				}
			}
		}
		return csv;
	}

	private String getJSONFromUrl(String url) {

		HttpsURLConnection con = null;
		try {
			URL u = new URL(url);
			con = (HttpsURLConnection) u.openConnection();
			con.connect();
			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null)
				sb.append(line + "\n");
			br.close();
			return sb.toString();
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



}

