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

package deepimagej.tools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.TensorFlowModel;;

public class XmlUtils {

	public static void writeXml(String filename, DeepImageJ dp) {

		try {
			Parameters params = dp.params;
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

			// Create Document
			Document document = documentBuilder.newDocument();

			// root element-->Model.
			Element root = document.createElement("Model");
			document.appendChild(root);

			////////////////////////////////////////////////////////////////////
			// Information about the model

			// root element-->ModelInformation. This info is used to run the model
			Element modelInformation = document.createElement("ModelInformation");
			root.appendChild(modelInformation);

			// Name of the model (child of "ModelInformation")
			Element name = document.createElement("Name");
			name.appendChild(document.createTextNode(params.name));
			modelInformation.appendChild(name);

			// Author of the model (child of "ModelInformation")
			Element author = document.createElement("Author");
			author.appendChild(document.createTextNode(params.author));
			modelInformation.appendChild(author);

			// URL where the model can be found (child of "ModelInformation")
			Element url = document.createElement("URL");
			url.appendChild(document.createTextNode(params.url));
			modelInformation.appendChild(url);

			// Credit for the model (child of "ModelInformation")
			Element credit = document.createElement("Credit");
			credit.appendChild(document.createTextNode(params.credit));
			modelInformation.appendChild(credit);

			// Version of the model (child of "ModelInformation")
			Element version = document.createElement("Version");
			version.appendChild(document.createTextNode(params.version));
			modelInformation.appendChild(version);

			// Date of the model (child of "ModelInformation")
			Element date = document.createElement("Date");
			date.appendChild(document.createTextNode(params.date));
			modelInformation.appendChild(date);

			// Reference for the model (child of "ModelInformation")
			Element reference = document.createElement("Reference");
			reference.appendChild(document.createTextNode(params.reference));
			modelInformation.appendChild(reference);

			/////////////////////////////////////////////////////////////////////////////////////////////
			// Parameters used when the model was prepared //

			// root element-->ModelCharacteristics. This info is used to run the model
			Element modelTest = document.createElement("ModelTest");
			root.appendChild(modelTest);

			// Size of the input image used for testing (child of "ModelTest")
			Element inputSize = document.createElement("InputSize");
			inputSize.appendChild(document.createTextNode(params.inputSize));
			modelTest.appendChild(inputSize);

			// Size of the input image used for testing (child of "ModelTest")
			Element outputSize = document.createElement("OutputSize");
			outputSize.appendChild(document.createTextNode(params.outputSize));
			modelTest.appendChild(outputSize);

			// Memory used during the model run (child of "ModelTest")
			Element memoryPeak = document.createElement("MemoryPeak");
			memoryPeak.appendChild(document.createTextNode(params.memoryPeak));
			modelTest.appendChild(memoryPeak);

			// Time the model run lasted (child of "ModelTest")
			Element runtime = document.createElement("Runtime");
			runtime.appendChild(document.createTextNode(params.runtime));
			modelTest.appendChild(runtime);
			
			// Time the model run lasted (child of "ModelTest")
			double pixDepth = Math.round(params.testImageBackup.getCalibration().pixelDepth * 100) / 100;
			double pixWidth = Math.round(params.testImageBackup.getCalibration().pixelWidth * 100) / 100;
			double pixHeight = Math.round(params.testImageBackup.getCalibration().pixelHeight * 100) / 100;
			String units = params.testImageBackup.getCalibration().getUnits();
			String pixSize = Double.toString(pixWidth) + units + "x" +
							Double.toString(pixHeight) + units;
			Element pixelSize = document.createElement("PixelSize");
			pixelSize.appendChild(document.createTextNode(pixSize));
			modelTest.appendChild(pixelSize);
			//////////////////////////////////////////////////////////////////
			// root element-->ModelCharacteristics. This info is used to run the model
			Element modelCharacteristics = document.createElement("ModelCharacteristics");
			root.appendChild(modelCharacteristics);

			// Model tag (child of "ModelCharacteritics"). Normally it will
			// be "serve"
			Element modelTag = document.createElement("ModelTag");
			modelTag.appendChild(document.createTextNode(TensorFlowModel.returnTfTag(params.tag)));
			modelCharacteristics.appendChild(modelTag);

			// Signature Definition of the model (child of "ModelCharacteritics")
			// Normally "serving_default"
			Element sigDef = document.createElement("SignatureDefinition");
			sigDef.appendChild(document.createTextNode(TensorFlowModel.returnTfSig(params.graph)));
			modelCharacteristics.appendChild(sigDef);

			// Dimensions of the input tensor (child of "ModelCharacteritics")
			// Normally "serving_default"
			String tensorDim = tensorDims2String(params.inDimensions);
			Element inTensorDims = document.createElement("InputTensorDimensions");
			inTensorDims.appendChild(document.createTextNode(tensorDim));
			modelCharacteristics.appendChild(inTensorDims);

			// Name and form and dimensions of each of the inputs
			// (child of "ModelCharacteritics")
			params.nInputs = params.inputs.length;
			Element nInputs = document.createElement("NumberOfInputs");
			nInputs.appendChild(document.createTextNode(String.valueOf(params.nInputs)));
			modelCharacteristics.appendChild(nInputs);
			for (int i = 0; i < params.nInputs; i++) {
				String inName = "InputNames" + String.valueOf(i);
				Element inputName = document.createElement(inName);
				inputName.appendChild(document.createTextNode(params.inputs[i]));
				modelCharacteristics.appendChild(inputName);
				// Arrangement of the input dimensions
				// (child of "ModelCharacteritics")
				String inDims = "InputOrganization" + String.valueOf(i);
				Element inputDims = document.createElement(inDims);
				inputDims.appendChild(document.createTextNode(params.inputForm[i]));
				modelCharacteristics.appendChild(inputDims);
			}
			// Name and form and dimensions of each of the outputs
			// (child of "ModelCharacteritics")
			params.nOutputs = params.outputs.length;
			Element nOutputs = document.createElement("NumberOfOutputs");
			nOutputs.appendChild(document.createTextNode(String.valueOf(params.nInputs)));
			modelCharacteristics.appendChild(nOutputs);
			for (int i = 0; i < params.nOutputs; i++) {
				String outName = "OutputNames" + String.valueOf(i);
				Element outputName = document.createElement(outName);
				outputName.appendChild(document.createTextNode(params.outputs[i]));
				modelCharacteristics.appendChild(outputName);
				// Arrangement of the output dimensions
				// (child of "ModelCharacteritics")
				String outDims = "OutputOrganization" + String.valueOf(i);
				Element outputDims = document.createElement(outDims);
				outputDims.appendChild(document.createTextNode(params.outputForm[i]));
				modelCharacteristics.appendChild(outputDims);
			}

			// Pixel size of the images with which the image was trained
			// (child of "ModelCharacteritics")
			Element nChannels = document.createElement("Channels");
			nChannels.appendChild(document.createTextNode(params.channels));
			modelCharacteristics.appendChild(nChannels);

			// Patch size to run the model optimally
			Element fixed = document.createElement("FixedPatch");
			fixed.appendChild(document.createTextNode(Boolean.toString(params.fixedPatch)));
			modelCharacteristics.appendChild(fixed);

			// Minimum multiple of patch (child of "ModelCharacteritics")
			Element minimumSize = document.createElement("MinimumSize");
			minimumSize.appendChild(document.createTextNode(params.minimumSize));
			modelCharacteristics.appendChild(minimumSize);

			// Patch size to run the model optimally
			Element patch = document.createElement("PatchSize");
			patch.appendChild(document.createTextNode(Integer.toString(params.patch)));
			modelCharacteristics.appendChild(patch);

			// Pixel size of the images with which the image was trained
			// (child of "ModelCharacteritics")
			Element fixedPadding = document.createElement("FixedPadding");
			fixedPadding.appendChild(document.createTextNode(Boolean.toString(params.fixedPadding)));
			modelCharacteristics.appendChild(fixedPadding);
			
			// Pixel size of the images with which the image was trained
			// (child of "ModelCharacteritics")
			Element padding = document.createElement("Padding");
			padding.appendChild(document.createTextNode(Integer.toString(params.padding)));
			modelCharacteristics.appendChild(padding);

			// NAme of the preprocessing file
			// (child of "ModelCharacteritics")
			Element preprocessingFile = document.createElement("PreprocessingFile");
			preprocessingFile.appendChild(document.createTextNode(params.preprocessingFile));
			modelCharacteristics.appendChild(preprocessingFile);
			
			// Name of the postprocessing file
			// (child of "ModelCharacteritics")
			Element postprocessingFile = document.createElement("PostprocessingFile");
			postprocessingFile.appendChild(document.createTextNode(params.postprocessingFile));
			modelCharacteristics.appendChild(postprocessingFile);
						
			// Pixel size of the images with which the image was trained
			// (child of "ModelCharacteritics")
			Element nSlices = document.createElement("slices");
			nSlices.appendChild(document.createTextNode(params.slices));
			modelCharacteristics.appendChild(nSlices);


			// create the xml file
			// transform the DOM Object to an XML File
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			DOMSource domSource = new DOMSource(document);
			StreamResult streamResult = new StreamResult(new File(filename));
			transformer.transform(domSource, streamResult);
		}
		catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		}
		catch (TransformerException tfe) {
			tfe.printStackTrace();
		}
	}

	public static Map<?, ?> readXML(String xml) {
		Document dom;
		Map modelInfo = new HashMap();
		// Make an instance of the DocumentBuilderFactory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			// use the factory to take an instance of the document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
			// parse using the builder to get the DOM mapping of the
			// XML file
			dom = db.parse(xml);

			Element root = dom.getDocumentElement();
			NodeList nList = root.getChildNodes();

			int length = nList.getLength();
			for (int node = 1; node < length; node = node + 2) {
				Node n = nList.item(node);
				String name = n.getNodeName();
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element characteristic = (Element) n;
					NodeList characteristicsList = characteristic.getChildNodes();
					for (int childs = 0; childs < characteristicsList.getLength(); childs++) {
						Node n2 = characteristicsList.item(childs);
						if (n2.getNodeType() == Node.ELEMENT_NODE) {
							Element characteristic2 = (Element) n2;
							String text = characteristic2.getTagName();
							String text2 = characteristic2.getTextContent();
							modelInfo.put(text, text2);
						}
					}
				}
			}
		}
		catch (ParserConfigurationException pce) {
			System.out.println(pce.getMessage());
		}
		catch (SAXException se) {
			System.out.println(se.getMessage());
		}
		catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}

		return modelInfo;
	}

	private static String tensorDims2String(int[] tensorDims) {
		// method that transforms an int[] into a String
		String result = ",";
		for (int i = 0; i < tensorDims.length; i++) {
			result = result + String.valueOf(tensorDims[i]) + ",";
		}
		return result;
	}

}
