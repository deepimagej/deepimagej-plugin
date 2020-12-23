/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we expect you to include adequate citations and acknowledgments whenever you 
 * present or publish results that are based on it.
 * 
 * Reference: DeepImageJ: A user-friendly plugin to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, L. Donati, M. Unser, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 *
 * Corresponding authors: mamunozb@ing.uc3m.es, daniel.sage@epfl.ch
 *
 */

/*
 * Copyright 2019. Universidad Carlos III, Madrid, Spain and EPFL, Lausanne, Switzerland.
 * 
 * This file is part of DeepImageJ.
 * 
 * DeepImageJ is free software: you can redistribute it and/or modify it under the terms of 
 * the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with DeepImageJ. 
 * If not, see <http://www.gnu.org/licenses/>.
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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class BioimageZooRepository {


	public String url = "https://bioimage.io/";
	//public String location = "https://raw.githubusercontent.com/bioimage-io/bioimage-io-models/master/manifest.bioimage.io.json";
	public String location = "https://raw.githubusercontent.com/bioimage-io/bioimage-io-models/gh-pages/manifest.bioimage.io.json";
	public String title = "Bioimage Zoo";
	public String name = "Bioimage Zoo";
	
	public HashMap<String, Model> models = new HashMap<String, Model>();
	public ArrayList<String> logs = new ArrayList<String>();
	
	
	public BioimageZooRepository() {
		connect();
	}

	public ArrayList<String> connect() {
		logs.add("Time: " + new Date().toString());
		JSONParser parser = new JSONParser();
		try {
			String text = getJSONFromUrl(location);
			JSONObject json = (JSONObject) parser.parse(text);
			logs.add("Name: " + getString(json, "name", "n.a"));
			name = getString(json, "name", "n.a");
			title = getString(json, "splash_title", "n.a");
			JSONArray resources = (JSONArray) json.get("resources");
			if (models != null) {
				for (Object resource : resources) {
					JSONObject jm = (JSONObject) resource;
					if (jm != null) {
						Model model = parseModel(jm);
						if (model != null) {
							models.put(model.getFacename(), model);
						}
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

	private Model parseModel(JSONObject json) {
		String type = getString(json, "type", "n.a.");
		if (!type.equalsIgnoreCase("model"))
			return null;
		String id = getString(json, "id", "n.a");
		if (id.equalsIgnoreCase("n.a."))
			return null;
		String root_url = getString(json, "root_url", "n.a");
		String desc = getString(json, "description", "n.a");
		String doc = getString(json, "documentation", "n.a");
		String source = getString(json, "source", "n.a");
		String download = getString(json, "download_url", "n.a");
		ArrayList<String> covers = getArray(json, "covers");
		ArrayList<String> tags = getArray(json, "tags");

		String authors = getCSV(json, "authors");

		Model model = new Model(id, root_url, desc, authors, doc, source, covers, download);
		for (String tag : tags)
			if (tag.toLowerCase().equals("deepimagej")) {
				model.deepImageJ = true;
			}
	
		/*

			*/
		return model;
	}
	
	private String getString(JSONObject json, String tag, String defaultValue) {
		Object o = json.get(tag);
		if (o instanceof String)
			return (String) o;
		else
			return defaultValue;
	}

	private ArrayList<String> getArray(JSONObject json, String tag) {
		ArrayList<String> array = new ArrayList<String>();
		Object objects = json.get(tag);
		if (objects instanceof JSONArray) {
			for (Object o : (JSONArray) objects)
				if (o instanceof String)
					array.add((String) o);
		}
		return array;
	}

	private String getCSV(JSONObject json, String tag) {
		String csv = "";
		int count = 0;
		Object objects = json.get(tag);
		if (objects instanceof JSONArray) {
			for (Object o : (JSONArray) objects)
				if (o instanceof String) {
					csv += (count == 0  ? "" : ", ") + (String) o;
					count++;
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

