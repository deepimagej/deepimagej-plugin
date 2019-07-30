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

import deepimagej.DeepPlugin;
import deepimagej.Parameters;;

public class XmlUtils {


	public static void writeXml(DeepPlugin dp) {

		try {
			Parameters params = dp.params;
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

			// Create Document
			Document document = documentBuilder.newDocument();

			// root element-->Model.
			Element root = document.createElement("Model");
			document.appendChild(root);

			// root element-->ModelCharacteritics. This info is used to run the model
			Element model_characteristics = document.createElement("ModelCharacteritics");
			root.appendChild(model_characteristics);

			// Minimum multiple of patch (child of "ModelCharacteritics")
			Element min_multiple = document.createElement("MinimumMultipleOfPatches");
			min_multiple.appendChild(document.createTextNode(params.minimum_patch_size));
			model_characteristics.appendChild(min_multiple);

			// Model tag (child of "ModelCharacteritics"). noramlly it will
			// be "serve"
			Element model_tag = document.createElement("ModelTag");
			model_tag.appendChild(document.createTextNode(params.tag));
			model_characteristics.appendChild(model_tag);

			// Signature Definition of the model (child of "ModelCharacteritics")
			// Normally "serving_default"
			Element sig_def = document.createElement("SignatureDefinition");
			sig_def.appendChild(document.createTextNode(params.graph));
			model_characteristics.appendChild(sig_def);

			// Dimensions of the input tensor (child of "ModelCharacteritics")
			// Normally "serving_default"
			String tensor_dim = tensorDims2String(params.in_dimensions);
			Element in_tensor_dims = document.createElement("InputTensorDimensions");
			in_tensor_dims.appendChild(document.createTextNode(tensor_dim));
			model_characteristics.appendChild(in_tensor_dims);

			// Name and form and dimensions of each of the inputs
			// (child of "ModelCharacteritics")
			params.n_inputs = params.inputs.length;
			Element n_inputs = document.createElement("NumberOfInputs");
			n_inputs.appendChild(document.createTextNode(String.valueOf(params.n_inputs)));
			model_characteristics.appendChild(n_inputs);
			for (int i = 0; i < params.n_inputs; i++) {

				String in_name = "InputNames" + String.valueOf(i);
				Element input_name = document.createElement(in_name);
				input_name.appendChild(document.createTextNode(params.inputs[i]));
				model_characteristics.appendChild(input_name);
				// Arrangement of the input dimensions
				// (child of "ModelCharacteritics")
				String in_dims = "InputOrganization" + String.valueOf(i);
				Element input_dims = document.createElement(in_dims);
				input_dims.appendChild(document.createTextNode(params.input_form[i]));
				model_characteristics.appendChild(input_dims);
			}
			// Name and form and dimensions of each of the outputs
			// (child of "ModelCharacteritics")
			params.n_outputs = params.outputs.length;
			Element n_outputs = document.createElement("NumberOfOutputs");
			n_outputs.appendChild(document.createTextNode(String.valueOf(params.n_inputs)));
			model_characteristics.appendChild(n_outputs);
			for (int i = 0; i < params.n_outputs; i++) {

				String out_name = "OutputNames" + String.valueOf(i);
				Element output_name = document.createElement(out_name);
				output_name.appendChild(document.createTextNode(params.outputs[i]));
				model_characteristics.appendChild(output_name);

				// Arrangement of the output dimensions
				// (child of "ModelCharacteritics")
				String out_dims = "OutputOrganization" + String.valueOf(i);
				Element output_dims = document.createElement(out_dims);
				output_dims.appendChild(document.createTextNode(params.output_form[i]));
				model_characteristics.appendChild(output_dims);
			}

			// Pixel size of the images with which the image was trained
			// (child of "ModelCharacteritics")
			Element n_channels = document.createElement("Channels");
			n_channels.appendChild(document.createTextNode(params.channels));
			model_characteristics.appendChild(n_channels);

			// Patch size to run the model optimally
			Element fixed = document.createElement("FixedPatch");
			fixed.appendChild(document.createTextNode(Boolean.toString(params.fixedPatch)));
			model_characteristics.appendChild(fixed);

			// Patch size to run the model optimally
			Element patch = document.createElement("PatchSize");
			patch.appendChild(document.createTextNode(Integer.toString(params.patch)));
			model_characteristics.appendChild(patch);

			// Pixel size of the images with which the image was trained
			// (child of "ModelCharacteritics")
			Element n_slices = document.createElement("slices");
			n_slices.appendChild(document.createTextNode("1"));
			model_characteristics.appendChild(n_slices);
			params.slices = 1;

			// Pixel size of the images with which the image was trained
			// (child of "ModelCharacteritics")
			Element overlap = document.createElement("FalseInformationBecauseCorners");
			overlap.appendChild(document.createTextNode(Integer.toString(params.overlap = 0)));
			model_characteristics.appendChild(overlap);

			// create the xml file
			// transform the DOM Object to an XML File
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			DOMSource domSource = new DOMSource(document);
			String xml = dp.getPath() + File.separator + dp.dirname + File.separator + "config.xml";
			StreamResult streamResult = new StreamResult(new File(xml));

			// If you use
			// StreamResult result = new StreamResult(System.out);
			// the output will be pushed to the standard output ...
			// You can use that for debugging

			transformer.transform(domSource, streamResult);

			System.out.println("Done creating XML File");
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
		Map model_info = new HashMap();
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
					NodeList characteristics_list = characteristic.getChildNodes();
					for (int childs = 0; childs < characteristics_list.getLength(); childs++) {
						Node n2 = characteristics_list.item(childs);
						if (n2.getNodeType() == Node.ELEMENT_NODE) {
							Element characteristic2 = (Element) n2;
							String text = characteristic2.getTagName();
							String text2 = characteristic2.getTextContent();
							model_info.put(text, text2);
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

		return model_info;
	}
	
	private static String tensorDims2String(int[] tensor_dims) {
		// method that transforms an int[] into a String
		String result = ",";
		for (int i = 0; i < tensor_dims.length; i++) {
			result = result + String.valueOf(tensor_dims[i]) + ",";
		}
		return result;
	}

}
