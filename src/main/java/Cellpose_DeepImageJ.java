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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import deepimagej.gui.consumers.CellposeAdapter.Input;
import deepimagej.gui.consumers.CellposeAdapter;
import ij.IJ;
import ij.ImageJ;
import ij.Macro;
import ij.Menus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import io.bioimage.modelrunner.model.special.cellpose.Cellpose;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;

public class Cellpose_DeepImageJ implements PlugIn, CellposeAdapter.Job {
    final static String MACRO_RECORD_COMMENT = "\n// Cellpose is recorded when Run is clicked.\n"
            + "// " + DeepImageJ_Run.MACRO_INFO + "\n";

    public static void main(String[] args) {
        new ImageJ();
        // Eclipse launches do not load the installed plugin's plugins.config.
        Menus.getCommands().putIfAbsent("DeepImageJ Cellpose", Cellpose_DeepImageJ.class.getName());
        new Cellpose_DeepImageJ().run("");
    }

    @Override
    public void run(String arg) {
        if (!IJ.isMacro()) runGUI();
        else if (Macro.getOptions() != null) runMacro();
    }

    private void runGUI() {
        if (Recorder.record) Recorder.recordString(MACRO_RECORD_COMMENT);
        SwingUtilities.invokeLater(() -> {
            CellposeAdapter adapter = new CellposeAdapter();
            ij.plugin.frame.PlugInFrame frame = new ij.plugin.frame.PlugInFrame("deepImageJ Cellpose");
            CellposeAdapter.Dialog gui = new CellposeAdapter.Dialog(adapter, Cellpose_DeepImageJ::new);
            frame.add(gui);
            frame.pack();
            frame.setSize(520, 320);
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent e) { gui.close(); }
            });
            gui.setCancelCallback(frame::dispose);
            frame.setVisible(true);
        });
    }

    private void runMacro() {
        String options = Macro.getOptions();
        String model = Macro.getValue(options, "model", null);
        String diameterText = Macro.getValue(options, "diameter", null);
        Float diameter = diameterText == null || diameterText.trim().isEmpty() ? null : Float.valueOf(diameterText);
        validateCellpose(model, diameter);
        String display = Macro.getValue(options, "display_all", "false");
        if (!display.equalsIgnoreCase("true") && !display.equalsIgnoreCase("false"))
            throw new IllegalArgumentException("display_all must be true or false.");
        Input input = Input.captureMacro(WindowManager.getCurrentImage(), options);
        try (Cellpose_DeepImageJ job = new Cellpose_DeepImageJ()) {
            Map<String, RandomAccessibleInterval<?>> outputs = job.runCellpose(
                    model, modelsDirectory(), input, diameter, System.out::println);
            CellposeAdapter.displayOutputs(outputs, input.title(), Boolean.parseBoolean(display));
        } catch (Exception ex) {
            throw new RuntimeException("Error running Cellpose: " + ex.getMessage(), ex);
        }
    }

    private static String modelsDirectory() {
        return new java.io.File(deepimagej.Constants.FIJI_FOLDER, "models").getAbsolutePath();
    }

    public static <T extends RealType<T> & NativeType<T>> Map<String, RandomAccessibleInterval<T>> runCellpose(
            String model, RandomAccessibleInterval<T> image, String cyto, String nuclei) {
        return runCellpose(model, image, cyto, nuclei, null);
    }

    public static <T extends RealType<T> & NativeType<T>> Map<String, RandomAccessibleInterval<T>> runCellpose(
            String model, RandomAccessibleInterval<T> image, String cyto, String nuclei, Float diameter) {
        validateCellpose(model, diameter);
        Input input = Input.fromRai(image, cyto, nuclei);
        try (Cellpose_DeepImageJ job = new Cellpose_DeepImageJ()) {
            return Cast.unchecked(job.runCellpose(model, modelsDirectory(), input, diameter, System.out::println));
        } catch (Exception ex) {
            throw new RuntimeException("Error running Cellpose: " + ex.getMessage(), ex);
        }
    }

    // Flattened CellposeDenoiseModel outputs: masks, three flows, styles, restored image.
    private static final String[] OUTPUT_NAMES = {"labels", "flows_0", "flows_1", "flows_2", "styles", "image_dn"};
    private static final Object INSTALL_LOCK = new Object();
    private static boolean INSTALLED_ENV;
    private volatile Cellpose model;
    private volatile boolean cancelled;

    public static void validateCellpose(String model, Float diameter) {
        CellposeAdapter.validateCellpose(model, diameter);
    }

    @Override public void install(String name, String directory, Consumer<String> log) throws Exception {
        installCellpose(name, directory, log);
    }

    public static void installCellpose(String name, String directory, Consumer<String> log) throws Exception {
        validateCellpose(name, null);
        synchronized (INSTALL_LOCK) {
            if (!INSTALLED_ENV) {
                log.accept("Installing Cellpose requirements");
                Cellpose.installRequirements(log);
                INSTALLED_ENV = true;
            }
        }
        weights(name, directory, log);
    }

    private static String weights(String name, String directory, Consumer<String> log) throws Exception {
        if (new File(name).isFile()) return name;
        if (new File(name).isAbsolute()) throw new IllegalArgumentException("Cellpose weights file not found: " + name);
        String path = Cellpose.fileIsCellpose(name, directory);
        if (path != null) return path;
        log.accept("Downloading Cellpose model " + name);
        return Cellpose.donwloadPretrained(name, directory, progress ->
                log.accept(String.format("Downloading %s: %.1f%%", name, progress * 100)));
    }

    public Map<String, RandomAccessibleInterval<?>> runCellpose(
            String name, String directory, Input input, Float diameter, Consumer<String> log) throws Exception {
        validateCellpose(name, diameter);
        checkCancelled();
        installCellpose(name, directory, log);
        checkCancelled();
        model = Cellpose.init(weights(name, directory, log));
        try {
            checkCancelled();
            log.accept("Loading Cellpose model");
            model.loadModel();
            checkCancelled();
            model.setChannels(input.channels().packedChannels());
            return Cast.unchecked(runCellposeOnFramesStack(input, diameter, log));
        } finally {
            model.close();
            model = null;
        }
    }

    protected void checkCancelled() {
        if (cancelled || Thread.currentThread().isInterrupted()) throw new CancellationException("Cellpose cancelled.");
    }

    protected <T extends RealType<T> & NativeType<T>> List<RandomAccessibleInterval<T>> predict(
            RandomAccessibleInterval<FloatType> image, Float diameter) throws Exception {
        if (diameter != null) model.setDiameter(diameter);
        // Direct inference keeps the actual dtype and shape, including restored images with one channel.
        return model.inference(Collections.singletonList(image));
    }

    protected <T extends RealType<T> & NativeType<T>> Map<String, RandomAccessibleInterval<T>> runCellposeOnFramesStack(
            Input input, Float diameter, Consumer<String> log) throws Exception {
        Map<String, RandomAccessibleInterval<T>> outputs = new LinkedHashMap<>();
        long planes = input.pixels().dimension(3);
        for (long p = 0; p < planes; p++) {
            checkCancelled();
            log.accept("Running Cellpose plane " + (p + 1) + "/" + planes);
            List<RandomAccessibleInterval<T>> result = predict(Views.hyperSlice(input.pixels(), 3, p), diameter);
            if (result.size() != OUTPUT_NAMES.length)
                throw new IllegalStateException("Expected six Cellpose outputs, received " + result.size());
            for (int i = 0; i < result.size(); i++) {
                String name = OUTPUT_NAMES[i];
                RandomAccessibleInterval<T> plane = result.get(i);
                if (!outputs.containsKey(name)) {
                    long[] dimensions = Arrays.copyOf(plane.dimensionsAsLongArray(), plane.numDimensions() + 1);
                    dimensions[dimensions.length - 1] = planes;
                    outputs.put(name, new ArrayImgFactory<>(plane.getType()).create(dimensions));
                }
                RandomAccessibleInterval<T> target = outputs.get(name);
                RandomAccessibleInterval<T> slice = Views.hyperSlice(target, target.numDimensions() - 1, p);
                if (!Arrays.equals(slice.dimensionsAsLongArray(), plane.dimensionsAsLongArray()))
                    throw new IllegalStateException("Cellpose output shape changed between planes: " + name);
                LoopBuilder.setImages(plane, slice).forEachPixel((s, d) -> d.set(s));
            }
        }
        return outputs;
    }

    @Override
    public void close() {
        cancelled = true;
        Cellpose active = model;
        if (active != null) active.close();
    }
}
