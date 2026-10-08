import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.gson.GsonBuilder;
import deepimagej.gui.consumers.CellposeAdapter.Input;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import io.bioimage.modelrunner.model.special.cellpose.Cellpose;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/** CI-only harness: the actual plugin installer, macro channel parser and inference path. */
public class CellposeChannels {
    public static void main(String[] args) throws Exception {
        Path fixture = Paths.get(args[0]).toAbsolutePath();
        Path output = Files.createDirectories(Paths.get(args[1]).toAbsolutePath());
        Path models = Files.createDirectories(output.resolve("models"));
        BufferedImage rgb = ImageIO.read(fixture.toFile());
        int width = rgb.getWidth(), height = rgb.getHeight();
        byte[][] channels = new byte[3][width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = rgb.getRGB(x, y), p = y * width + x;
                for (int c = 0; c < 3; c++) channels[c][p] = (byte) (pixel >> (16 - 8 * c));
                if (channels[0][p] != 0) throw new AssertionError("Fixture's red channel must be empty");
            }
        }
        ImagePlus original = new ImagePlus("rgb", new ColorProcessor(rgb));
        ImagePlus two = multichannel("two", width, height, channels[1], channels[2]);
        ImagePlus swapped = multichannel("two-swapped", width, height, channels[2], channels[1]);
        ImagePlus gray = multichannel("gray", width, height, channels[1]);
        byte[] blank = new byte[width * height];
        ImagePlus four = multichannel("four", width, height, blank, channels[2], blank, channels[1]);
        String[][] cases = {
            {"rgb-cyto", "cyto", "green", "none"},
            {"rgb-cyto-nuclei", "cyto-nuclei", "green", "blue"},
            {"two-cyto", "cyto", "1", "none"},
            {"two-cyto-nuclei", "cyto-nuclei", "1", "2"},
            {"two-swapped-cyto", "cyto", "Channel 2", "none"},
            {"two-swapped-cyto-nuclei", "cyto-nuclei", "Channel 2", "Channel 1"},
            {"gray-cyto", "cyto", "gray", "gray"},
            {"four-cyto", "cyto", "4", "none"},
            {"four-cyto-nuclei", "cyto-nuclei", "4", "2"}
        };
        List<Map<String, Object>> results = new ArrayList<>();
        for (String[] c : cases) {
            ImagePlus image = c[0].startsWith("rgb") ? original : c[0].startsWith("two-swapped") ? swapped
                    : c[0].startsWith("two") ? two : c[0].startsWith("gray") ? gray : four;
            String options = "model=[cyto3] cyto_channel=[" + c[2] + "] nuclei_channel=[" + c[3]
                    + "] diameter=[30] display_all=[false]";
            System.out.println("CASE " + c[0] + ": " + options);
            FileSaver saver = new FileSaver(image);
            String inputPath = output.resolve(c[0] + "-input.tif").toString();
            boolean saved = image.getStackSize() == 1 ? saver.saveAsTiff(inputPath) : saver.saveAsTiffStack(inputPath);
            if (!saved) throw new AssertionError("Cannot save " + inputPath);
            Input input = Input.captureMacro(image, options);
            try (Cellpose_DeepImageJ plugin = new Cellpose_DeepImageJ()) {
                Map<String, RandomAccessibleInterval<?>> prediction = plugin.runCellpose(
                        "cyto3", models.toString(), input, 30f, System.out::println);
                saveMask(prediction.get("labels"), width, height, output.resolve(c[0] + "-mask.png"));
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", c[0]);
            result.put("group", c[1]);
            result.put("options", options);
            results.add(result);
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("width", width);
        manifest.put("height", height);
        manifest.put("diameter", 30);
        manifest.put("weights", Cellpose.fileIsCellpose("cyto3", models.toString()));
        manifest.put("os", System.getProperty("os.name"));
        manifest.put("arch", System.getProperty("os.arch"));
        manifest.put("cases", results);
        Files.writeString(output.resolve("manifest.json"), new GsonBuilder().setPrettyPrinting().create().toJson(manifest));

        // Execute the reference using the environment installed by DeepImageJ, including on Windows.
        Path environment = Paths.get(Cellpose.getInstallationDir(), "envs", "biapy");
        Path python = environment.resolve(System.getProperty("os.name").startsWith("Windows") ? "python.exe" : "bin/python");
        int exit = new ProcessBuilder(python.toString(), "-u", "-X", "faulthandler", args[2], "verify",
                output.toString(), fixture.toString()).inheritIO().start().waitFor();
        if (exit != 0) throw new AssertionError("Native Python reference / channel comparison failed: " + exit);
    }

    private static ImagePlus multichannel(String title, int width, int height, byte[]... channels) {
        ImageStack stack = new ImageStack(width, height);
        for (byte[] channel : channels) stack.addSlice(new ByteProcessor(width, height, channel.clone()));
        ImagePlus image = new ImagePlus(title, stack);
        image.setDimensions(channels.length, 1, 1);
        return image;
    }

    private static void saveMask(RandomAccessibleInterval<?> labels, int width, int height, Path path) throws Exception {
        if (labels == null || labels.dimension(0) != width || labels.dimension(1) != height)
            throw new AssertionError("Incorrect mask dimensions (possible X/Y transposition)");
        for (int d = 2; d < labels.numDimensions(); d++)
            if (labels.dimension(d) != 1) throw new AssertionError("Unexpected mask axis " + d);
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        RandomAccess<?> access = labels.randomAccess();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                access.setPosition(x, 0);
                access.setPosition(y, 1);
                double label = ((RealType<?>) access.get()).getRealDouble();
                if (!Double.isFinite(label) || label < 0 || label > 65535 || label != Math.rint(label))
                    throw new AssertionError("Invalid instance label: " + label);
                mask.getRaster().setSample(x, y, 0, (int) label);
            }
        }
        if (!ImageIO.write(mask, "png", path.toFile())) throw new AssertionError("Cannot save " + path);
    }
}
