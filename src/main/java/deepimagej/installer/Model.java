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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class Model {

	public String name;
	public String desc;
	public String doc;
	public String source;
	public String root_url;
	public ArrayList<String> covers;
	public String authors;
	public boolean deepImageJ;
	
	public Model(String name, String root_url, String desc, String authors, String doc, String source, ArrayList<String> covers) {
		this.name = name;
		this.root_url = root_url;
		this.desc = desc;
		this.authors = authors;
		this.doc = doc;
		this.source = source;
		this.covers = covers;

	}
	
	public String getFacename() {
		return name + (deepImageJ ? " [DeepImageJ compatible]" : "");
	}
	

	public String getCoverHTML() {
		if (covers.size() >= 1)
			return "<img src=\"" + root_url + "/" + covers.get(0) +"\" >";
		else
			return "no cover";
	}

	/*
	public void setCover(String cover) {
		try {
			URL url = new URL(root_url);
			File file = new File(url.getPath( ));
			String parentPath = file.getParent( );
			URL parentUrl = new URL( url.getProtocol( ), url.getHost( ), url.getPort( ), parentPath );
			image = parentUrl.toString() ;
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	*/
	
	
}
