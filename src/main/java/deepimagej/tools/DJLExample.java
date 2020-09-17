package deepimagej.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.Classifications.Classification;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.Image.Flag;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.CenterCrop;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import deepimagej.tools.ImageClassificationTranslator;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.nn.Parameter;
import ai.djl.pytorch.zoo.PtModelZoo;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.DownloadUtils;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Pipeline;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.util.PairList;
import ai.djl.modality.cv.output.DetectedObjects;

import java.awt.image.*;

public class DJLExample {
	
	public static void main(String args[]) throws IOException, TranslateException, ModelNotFoundException, MalformedModelException {
		PtModelZoo.SSD.loadModel(new ProgressBar());
		Criteria<Image, Classification> criteria =
		            Criteria.builder()
		                    .setTypes(Image.class, Classification.class)
		    		        .optProgress(new ProgressBar())
		                    .build();
		ZooModel<Image, Classification> ssd = ModelZoo.loadModel(criteria);
		DownloadUtils.download("https://djl-ai.s3.amazonaws.com/ml"
				+ "repo/model/cv/image_classification/ai/djl/pytorch/resnet/0.0.1/"
				+ "traced_resnet18.pt.gz", "C:\\Users\\Carlos(tfg)\\Documents\\DJL\\resnet18\\resnet18.pt",
				new ProgressBar());
		DownloadUtils.download("https://djl-ai.s3.amazonaws.com/mlrepo/model/cv/image_cl"
				+ "assification/ai/djl/pytorch/synset.txt", "C:\\Users\\Carlos(tfg)\\Documents\\DJL\\resne"
						+ "t18\\synset.txt", new ProgressBar());


		Pipeline pipeline = new Pipeline();
		pipeline.add(new Resize(256))
		        .add(new CenterCrop(224, 224))
		        .add(new ToTensor())
		        .add(new Normalize(
		            new float[] {0.485f, 0.456f, 0.406f},
		            new float[] {0.229f, 0.224f, 0.225f}));
		// What translator does is to add pre and post processing. In this case, it states that the 
		// input is going to be an image and the output a "classification". Pipeline is like "transforms"
		// 
		Translator<NDList, NDList> translator = new ImageClassificationTranslator();
		
		// Search for models in the build/pytorch_models folder
		//System.setProperty("ai.djl.repository.zoo.location", "C:\\Users\\Carlos(tfg)\\Documents\\DJL\\example");


		URL url = new File("C:\\Users\\Carlos(tfg)\\Documents\\DJL\\example").toURI().toURL();


		
		Criteria<NDList, NDList> criteria2 = Criteria.builder()
		        .setTypes(NDList.class, NDList.class)
		         // only search the model in local directory
		         // "ai.djl.localmodelzoo:{name of the model}"
		        .optModelUrls(url.toString()) // search models in specified path
		        //.optArtifactId("ai.djl.localmodelzoo:resnet_18") // defines which model to load
		        .optModelName("resnet_18")
		        //.optTranslator(translator)
		        .optProgress(new ProgressBar()).build();

		ZooModel model = ModelZoo.loadModel(criteria2);
		Block block = model.getBlock();
		

		Image img = ImageFactory.getInstance().fromUrl("https://github.com/pytorch/hub/raw/master/dog.jpg");
		img.getWrappedImage();
		try(NDManager manager = NDManager.newBaseManager()) {
			NDArray nd = img.toNDArray(manager, Flag.COLOR);
			NDList aa = new NDList(nd);
			int[] ex = nd.toUint8Array();
			NDArray aaa = nd.get(new long[]{0,0,0});
			Predictor<NDList, NDList> predictor = model.newPredictor();
			NDList classifications = predictor.predict(aa);
			System.out.print("done");
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		


	}

}
