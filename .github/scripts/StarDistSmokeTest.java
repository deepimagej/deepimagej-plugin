import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.bioimage.modelrunner.model.special.stardist.Stardist2D;
import io.bioimage.modelrunner.model.special.stardist.StardistAbstract;

import net.imglib2.img.array.ArrayImgs;

public class StarDistSmokeTest {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0], "deepimagej-stardist").toAbsolutePath();
        Path models = Files.createDirectories(root.resolve("models"));

        // Use the same dependency installer and model loader as Stardist_DeepImageJ.
        System.out.println("Installing StarDist requirements in " + StardistAbstract.getInstallationDir());
        StardistAbstract.installRequirements(System.out::println);
        System.out.println("Downloading StarDist Fluorescence Nuclei Segmentation");
        String modelPath = Stardist2D.downloadPretrained(
                "StarDist Fluorescence Nuclei Segmentation", models.toString());
        StardistAbstract model = StardistAbstract.init(modelPath);
        try {
            model.loadModel();
            System.out.println("SUCCESS: StarDist model loaded from " +   modelPath);
            model.run(ArrayImgs.floats(new long[] {512, 512, 1}));
            System.out.println("SUCCESS: Run StarDist");
        } catch (Exception ex) {
        	ex.printStackTrace();
        	throw ex;
    	} finally {
            model.close();
        }
    }
}
