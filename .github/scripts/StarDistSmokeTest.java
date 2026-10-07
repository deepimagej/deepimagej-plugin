import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.bioimage.modelrunner.model.special.stardist.Stardist2D;
import io.bioimage.modelrunner.model.special.stardist.StardistAbstract;

public class StarDistSmokeTest {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0], "deepimagej-stardist").toAbsolutePath();
        Path models = Files.createDirectories(root.resolve("models"));
        StardistAbstract.setInstallationDir(root.resolve("micromamba").toString());

        // Use the same dependency installer and model loader as Stardist_DeepImageJ.
        System.out.println("Installing StarDist requirements");
        StardistAbstract.installRequirements(System.out::println);
        System.out.println("Downloading StarDist Fluorescence Nuclei Segmentation");
        String modelPath = Stardist2D.downloadPretrained(
                "StarDist Fluorescence Nuclei Segmentation", models.toString());
        StardistAbstract model = StardistAbstract.init(modelPath);
        try {
            model.loadModel();
            System.out.println("SUCCESS: StarDist model loaded from " + modelPath);
        } finally {
            model.close();
        }
    }
}
