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

package deepimagej.gui.consumers;

import java.io.File;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import deepimagej.tools.ImPlusRaiManager;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.CompositeConverter;
import io.bioimage.modelrunner.gui.custom.CellposePluginUI;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;
import ij.ImagePlus;
import ij.plugin.frame.Recorder;

public class CellposeAdapter extends SmallPluginAdapter implements AutoCloseable {
    private JComboBox<String> cytoCbox;
    private JComboBox<String> nucleiCbox;
    private boolean selectionUpdatesEnabled = true;
    private volatile boolean closed;

    public void bindChannels(JComboBox<String> cyto, JComboBox<String> nuclei) {
        cytoCbox = cyto;
        nucleiCbox = nuclei;
        updateGUI();
    }

    public void setSelectionUpdatesEnabled(boolean enabled) { selectionUpdatesEnabled = enabled; }

    @SuppressWarnings("unchecked")
    @Override public void setComponents(List<JComponent> components) {
        componentsGui = components;
        bindLegacyComponents();
    }

    @Override public void setVarNames(List<String> names) {
        varNames = names;
        bindLegacyComponents();
    }

    @SuppressWarnings("unchecked")
    private void bindLegacyComponents() {
        if (componentsGui == null || varNames == null) return;
        int cyto = varNames.indexOf("Cytoplasm Color:");
        int nuclei = varNames.indexOf("Nuclei Color:");
        if (cyto >= 0 && nuclei >= 0)
            bindChannels((JComboBox<String>) componentsGui.get(cyto), (JComboBox<String>) componentsGui.get(nuclei));
    }

    @Override protected void changeOnFocusGained(ImagePlus image) {
        if (closed) return;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> changeOnFocusGained(image));
            return;
        }
        if (!selectionUpdatesEnabled || cytoCbox == null || nucleiCbox == null) return;
        boolean rgb = image != null && (image.getType() == ImagePlus.COLOR_RGB || image.getNChannels() == 3);
        int count = image == null ? 0 : rgb ? 3 : image.getNChannels();
        updateChoices(cytoCbox, count, rgb, false);
        updateChoices(nucleiCbox, count, rgb, true);
    }

    private static void updateChoices(JComboBox<String> box, int count, boolean rgb, boolean nuclei) {
        List<String> choices = new ArrayList<>();
        if (count == 1) choices.add("Gray");
        else {
            if (nuclei) choices.add("None");
            if (rgb) java.util.Collections.addAll(choices, "red", "green", "blue");
            else for (int i = 1; i <= count; i++) choices.add("Channel " + i);
        }
        // Do not replace a combo-box model merely because the image regained focus.
        boolean same = box.getItemCount() == choices.size();
        for (int i = 0; same && i < choices.size(); i++) same = Objects.equals(box.getItemAt(i), choices.get(i));
        if (same) return;
        String selected = (String) box.getSelectedItem();
        Integer previous = !nuclei && count != 1 && "Gray".equalsIgnoreCase(selected)
                ? null : selectedIndex(selected, nuclei);
        box.setModel(new DefaultComboBoxModel<>(choices.toArray(new String[0])));
        if (rgb && !nuclei) box.setSelectedItem("green");
        for (String choice : choices) {
            if (Objects.equals(previous, selectedIndex(choice, nuclei))) {
                box.setSelectedItem(choice);
                break;
            }
        }
    }

    private static Integer selectedIndex(String label, boolean nuclei) {
        if (label == null) return null;
        try {
            Channels selection = nuclei ? Channels.resolve("1", label, Integer.MAX_VALUE, true)
                    : Channels.resolve(label, "none", label.equalsIgnoreCase("gray") ? 1 : Integer.MAX_VALUE, true);
            return nuclei ? selection.nuclei() : selection.cytoplasm();
        } catch (IllegalArgumentException ex) { return null; }
    }

    @Override public void notifyParams(LinkedHashMap<String, String> args) {
        if (!Recorder.record) return;
        StringBuilder macro = new StringBuilder("run(\"DeepImageJ Cellpose\", \"");
        for (Entry<String, String> entry : args.entrySet())
            macro.append(entry.getKey()).append("=[").append(entry.getValue()).append("] ");
        if (!args.isEmpty()) macro.setLength(macro.length() - 1);
        Recorder.recordString(macro.append("\");\n").toString());
    }

    @Override public void imageOpened(ImagePlus image) {
        if (!closed && image.getWindow() != null) super.imageOpened(image);
    }

    @Override public void close() {
        closed = true;
        selectionUpdatesEnabled = false;
        ImagePlus.removeImageListener(this);
        cytoCbox = null;
        nucleiCbox = null;
    }

    public static void validateCellpose(String model, Float diameter) {
        if (model == null || model.trim().isEmpty()) throw new IllegalArgumentException("Select a Cellpose model.");
        if (new File(model).isAbsolute() && !new File(model).isFile())
            throw new IllegalArgumentException("Cellpose weights file not found: " + model);
        if (diameter != null && (!Float.isFinite(diameter) || diameter < 0))
            throw new IllegalArgumentException("Diameter must be a finite, non-negative number of pixels.");
    }

    public static void displayOutputs(Map<String, ? extends RandomAccessibleInterval<?>> outputs, String title, boolean all) {
        String[] names = all ? new String[] {"labels", "flows_0", "flows_1", "flows_2", "image_dn"}
                : new String[] {"labels"};
        String[] axes = all ? new String[] {"xyb", "xycb", "cxyb", "xyb", "xycb"} : new String[] {"xyb"};
        String base = title.lastIndexOf('.') < 0 ? title : title.substring(0, title.lastIndexOf('.'));
        for (int i = 0; i < names.length; i++) {
            if (outputs.get(names[i]) == null) continue;
            ImagePlus image = outputImage(Cast.unchecked(outputs.get(names[i])), axes[i]);
            String outputTitle = base + "_" + names[i] + ".tif";
            for (int n = 1; WindowManager.getWindow(outputTitle) != null; n++)
                outputTitle = base + "_" + names[i] + "-" + n + ".tif";
            image.setTitle(outputTitle);
            image.getProcessor().resetMinAndMax();
            SwingUtilities.invokeLater(image::show);
        }
    }

    private static <T extends RealType<T> & NativeType<T>> ImagePlus outputImage(
            RandomAccessibleInterval<T> data, String axes) {
        return ImPlusRaiManager.convert(data, axes);
    }

    /** Connects the packaged dialog to the existing ImageJ plugin's execution methods. */
    public interface Job extends AutoCloseable {
        Map<String, RandomAccessibleInterval<?>> runCellpose(String name, String directory,
                Input input, Float diameter, Consumer<String> log) throws Exception;
        void install(String name, String directory, Consumer<String> log) throws Exception;
        @Override void close();
    }

    /** One-based source channel indices; zero means no supporting nuclei channel. */
    public static final class Channels {
        private static final Pattern CHANNEL = Pattern.compile("(?:channel\\s*)?([1-9][0-9]*)(?:\\s*\\(.*\\))?");
        private final int cytoplasm;
        private final int nuclei;

        private Channels(int cytoplasm, int nuclei) {
            this.cytoplasm = cytoplasm;
            this.nuclei = nuclei;
        }

        public int cytoplasm() { return cytoplasm; }
        public int nuclei() { return nuclei; }
        public int[] packedChannels() { return new int[] {1, nuclei == 0 ? 0 : 2}; }

        public static Channels resolve(String cytoplasm, String nuclei, int count, boolean rgb) {
            if (count < 1) throw new IllegalArgumentException("The image has no channels.");
            return new Channels(resolveOne(cytoplasm, count, rgb, false),
                    resolveOne(nuclei == null ? "none" : nuclei, count, rgb, true));
        }

        public static Channels fromMacro(String options, int count, boolean rgb) {
            int cyto = resolveOption(options, "cyto_channel", "cyto_color", count, rgb, false);
            int nuclei = resolveOption(options, "nuclei_channel", "nuclei_color", count, rgb, true);
            return new Channels(cyto, nuclei);
        }

        private static int resolveOption(String options, String key, String alias, int count, boolean rgb, boolean nuclei) {
            String value = Macro.getValue(options, key, null);
            String oldValue = Macro.getValue(options, alias, null);
            if (value == null && oldValue == null) {
                if (nuclei) return 0;
                throw new IllegalArgumentException("DeepImageJ Cellpose requires '" + key + "' (or '" + alias + "').");
            }
            int resolved = resolveOne(value == null ? oldValue : value, count, rgb, nuclei);
            if (value != null && oldValue != null && resolved != resolveOne(oldValue, count, rgb, nuclei))
                throw new IllegalArgumentException("Conflicting '" + key + "' and '" + alias + "' selections.");
            return resolved;
        }

        private static int resolveOne(String value, int count, boolean rgb, boolean nuclei) {
            if (value == null) throw new IllegalArgumentException("Select a segmentation channel.");
            String token = value.trim().toLowerCase(Locale.ROOT);
            if (nuclei && (token.equals("none") || token.equals("0") || token.equals("gray"))) return 0;
            if (token.equals("gray")) {
                if (count != 1) throw new IllegalArgumentException("'gray' requires a single-channel image; select a channel number.");
                return 1;
            }
            int index;
            if (token.equals("red") || token.equals("green") || token.equals("blue")) {
                if (!rgb) throw new IllegalArgumentException("Colour names require an RGB or three-channel image; use a channel number for other multichannel images.");
                index = token.equals("red") ? 1 : token.equals("green") ? 2 : 3;
            } else {
                Matcher matcher = CHANNEL.matcher(token);
                if (!matcher.matches()) throw new IllegalArgumentException("Invalid " + (nuclei ? "nuclei" : "segmentation")
                        + " channel '" + value + "'. Use a channel number" + (nuclei ? " or 'none'." : "."));
                try { index = Integer.parseInt(matcher.group(1)); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("Channel number is too large: " + value, ex); }
            }
            if (index > count) throw new IllegalArgumentException("Channel " + index + " requested, but this image has " + count + " channels.");
            return index;
        }
    }

    /** A copy of the selected pixels, made before installation or model loading. */
    public static final class Input {
        private final RandomAccessibleInterval<FloatType> pixels;
        private final Channels channels;
        private final String title;
        private final int slices;
        private final int frames;

        private Input(RandomAccessibleInterval<FloatType> pixels, Channels channels,
                String title, int slices, int frames) {
            this.pixels = pixels;
            this.channels = channels;
            this.title = title;
            this.slices = slices;
            this.frames = frames;
        }

        public RandomAccessibleInterval<FloatType> pixels() { return pixels; }
        public Channels channels() { return channels; }
        public String title() { return title; }
        public int slices() { return slices; }
        public int frames() { return frames; }

        public static Input capture(ImagePlus image, String cyto, String nuclei) {
            requireImage(image);
            boolean rgb = image.getType() == ImagePlus.COLOR_RGB || image.getNChannels() == 3;
            Channels selection = Channels.resolve(cyto, nuclei, rgb ? 3 : image.getNChannels(), rgb);
            return capture(image, selection);
        }

        public static Input captureMacro(ImagePlus image, String options) {
            requireImage(image);
            boolean rgb = image.getType() == ImagePlus.COLOR_RGB || image.getNChannels() == 3;
            return capture(image, Channels.fromMacro(options, rgb ? 3 : image.getNChannels(), rgb));
        }

        private static void requireImage(ImagePlus image) {
            if (image == null) throw new IllegalArgumentException("Please open an image before running Cellpose.");
        }

        private static <T extends RealType<T> & NativeType<T>> Input capture(ImagePlus image, Channels selection) {
            ImagePlus converted = image.getType() == ImagePlus.COLOR_RGB ? CompositeConverter.makeComposite(image) : image;
            RandomAccessibleInterval<T> data = ImPlusRaiManager.convert(converted, "xyczt");
            return pack(data, selection, image.getTitle());
        }

        /** Legacy Java API: XY, XYC, XYCT or XYCZT; gray XYZ stacks remain supported. */
        public static <T extends RealType<T> & NativeType<T>> Input fromRai(
                RandomAccessibleInterval<T> source, String cyto, String nuclei) {
            RandomAccessibleInterval<T> data = source;
            int dimensions = data.numDimensions();
            if (dimensions < 2 || dimensions > 5) throw new IllegalArgumentException("Expected XY, XYC, XYCT or XYCZT input.");
            if (dimensions == 2) data = Views.addDimension(data, 0, 0);
            else if (dimensions == 3 && "gray".equalsIgnoreCase(cyto) && data.dimension(2) > 1)
                data = Views.permute(Views.addDimension(data, 0, 0), 2, 3);
            if (data.numDimensions() == 3) data = Views.addDimension(data, 0, 0);
            if (data.numDimensions() == 4) data = Views.permute(Views.addDimension(data, 0, 0), 3, 4);
            int count = Math.toIntExact(data.dimension(2));
            return pack(data, Channels.resolve(cyto, nuclei, count, count == 3), "input");
        }

        private static <T extends RealType<T> & NativeType<T>> Input pack(
                RandomAccessibleInterval<T> source, Channels selection, String title) {
            RandomAccessibleInterval<T> data = Views.zeroMin(source);
            int slices = Math.toIntExact(data.dimension(3));
            int frames = Math.toIntExact(data.dimension(4));
            long planes = Math.multiplyExact((long) slices, frames);
            RandomAccessibleInterval<FloatType> packed = ArrayImgs.floats(data.dimension(0), data.dimension(1), 3, planes);
            for (int t = 0; t < frames; t++) {
                for (int z = 0; z < slices; z++) {
                    RandomAccessibleInterval<T> plane = Views.hyperSlice(Views.hyperSlice(data, 4, t), 3, z);
                    RandomAccessibleInterval<FloatType> target = Views.hyperSlice(packed, 3, (long) t * slices + z);
                    copyChannel(plane, target, selection.cytoplasm() - 1, 0);
                    if (selection.nuclei() != 0) copyChannel(plane, target, selection.nuclei() - 1, 1);
                }
            }
            return new Input(packed, selection, title, slices, frames);
        }

        private static <T extends RealType<T> & NativeType<T>> void copyChannel(
                RandomAccessibleInterval<T> source, RandomAccessibleInterval<FloatType> target, int from, int to) {
            LoopBuilder.setImages(Views.hyperSlice(source, 2, from), Views.hyperSlice(target, 2, to))
                    .forEachPixel((s, d) -> d.setReal(s.getRealDouble()));
        }
    }

    /** ImageJ-specific controls use the same channel resolver and runner as macros. */
    public static class Dialog extends CellposePluginUI implements AutoCloseable {
        private static final long serialVersionUID = 1L;
        private final CellposeAdapter adapter;
        private final Supplier<? extends Job> runners;
        private SwingWorker<?, ?> worker;
        private Job runner;
        private boolean closed;

        public Dialog(CellposeAdapter adapter, Supplier<? extends Job> runners) {
            super(adapter);
            this.adapter = adapter;
            this.runners = runners;
            cytoplasmLabel.setText("Cytoplasm channel:");
            nucleiLabel.setText("Nuclei channel:");
            cytoCbox.removeAllItems();
            nucleiCbox.removeAllItems();
            nucleiCbox.addItem("None");
            adapter.bindChannels(cytoCbox, nucleiCbox);
            modelComboBox.addActionListener(event -> updateCustomControls());
            updateCustomControls();
        }

        @Override public void actionPerformed(ActionEvent event) {
            if (event.getSource() == footer.getButtons().getRunButton()) startRun();
            else if (event.getSource() == footer.getButtons().getInstallButton()) startInstall();
            else super.actionPerformed(event);
        }

        @Override public void setCancelCallback(Runnable callback) {
            super.setCancelCallback(() -> { close(); callback.run(); });
        }

        @Override protected void startModelInstallation(boolean starting) {
            // This dialog's SwingWorker owns the busy state until installation and inference finish.
        }

        private void updateCustomControls() {
            boolean custom = CUSTOM_STR.equals(modelComboBox.getSelectedItem());
            customLabel.setEnabled(custom);
            customModelPathField.setEnabled(custom);
            browseButton.setEnabled(custom);
        }

        private String selectedModel() {
            return CUSTOM_STR.equals(modelComboBox.getSelectedItem()) ? customModelPathField.getText().trim()
                    : (String) modelComboBox.getSelectedItem();
        }

        private void startRun() {
            if (closed || worker != null) return;
            try {
                final String model = selectedModel();
                String text = diameterField.getText().trim();
                final Float diameter = text.isEmpty() ? null : Float.valueOf(text);
                validateCellpose(model, diameter);
                final boolean all = check.isSelected();
                // Capture controls and selected pixels on the EDT, before starting any background work.
                final String cyto = (String) cytoCbox.getSelectedItem();
                final String nuclei = (String) nucleiCbox.getSelectedItem();
                final Input input = Input.capture(WindowManager.getCurrentImage(), cyto, nuclei);
                LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
                parameters.put("model", model);
                parameters.put("cyto_channel", cyto);
                parameters.put("nuclei_channel", nuclei);
                if (diameter != null) parameters.put("diameter", diameter.toString());
                parameters.put("display_all", Boolean.toString(all));
                adapter.notifyParams(parameters);
                runner = runners.get();
                final Job job = runner;
                setBusy(true);
                worker = new SwingWorker<Map<String, RandomAccessibleInterval<?>>, Void>() {
                    @Override protected Map<String, RandomAccessibleInterval<?>> doInBackground() throws Exception {
                        // Reuse JDLL's agreement dialog, installer window and weights progress.
                        Dialog.super.installCellpose();
                        if (closed || isCancelled() || !cellposeInstallationReady()) return null;
                        return job.runCellpose(model, adapter.getModelsDir(), input, diameter, Dialog.this::status);
                    }
                    @Override protected void done() {
                        boolean failed = false;
                        try {
                            if (!closed && !isCancelled()) {
                                Map<String, RandomAccessibleInterval<?>> outputs = get();
                                if (outputs != null) displayOutputs(outputs, input.title(), all);
                            }
                        } catch (Exception ex) { failed = true; showError(ex); }
                        finally {
                            job.close(); runner = null; worker = null;
                            if (!closed) {
                                setBusy(false);
                                if (failed) footer.getBar().setString("Error running the model");
                            }
                        }
                    }
                };
                worker.execute();
            } catch (Exception ex) { showError(ex); }
        }

        private void startInstall() {
            if (closed || worker != null) return;
            final String model = selectedModel();
            try { validateCellpose(model, null); }
            catch (IllegalArgumentException ex) { showError(ex); return; }
            setBusy(true);
            worker = new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    Dialog.super.installCellpose();
                    return null;
                }
                @Override protected void done() {
                    try { if (!isCancelled()) get(); }
                    catch (Exception ex) { showError(ex); }
                    finally { worker = null; if (!closed) setBusy(false); }
                }
            };
            worker.execute();
        }

        private void setBusy(boolean busy) {
            adapter.setSelectionUpdatesEnabled(!busy);
            footer.getButtons().getRunButton().setEnabled(!busy);
            footer.getButtons().getInstallButton().setEnabled(!busy);
            modelComboBox.setEnabled(!busy);
            diameterField.setEnabled(!busy);
            cytoCbox.setEnabled(!busy);
            nucleiCbox.setEnabled(!busy);
            check.setEnabled(!busy);
            customModelPathField.setEnabled(!busy && CUSTOM_STR.equals(modelComboBox.getSelectedItem()));
            browseButton.setEnabled(customModelPathField.isEnabled());
            footer.getBar().setIndeterminate(busy);
            if (!busy) footer.getBar().setValue(0);
            footer.getBar().setString(busy ? "Checking cellpose installed..." : "");
            if (!busy) adapter.updateGUI();
        }

        private void status(String message) {
            System.out.println(message);
            final String text;
            if (message.equals("Loading Cellpose model")) text = "Loading model";
            else if (message.startsWith("Running Cellpose plane "))
                text = "Running the model " + message.substring("Running Cellpose plane ".length());
            else return;
            SwingUtilities.invokeLater(() -> {
                if (!closed) { footer.getBar().setIndeterminate(true); footer.getBar().setString(text); }
            });
        }

        private void showError(Exception ex) {
            Throwable cause = ex instanceof ExecutionException ? ex.getCause() : ex;
            if (closed || cause instanceof CancellationException || cause instanceof InterruptedException) return;
            cause.printStackTrace();
            String message = cause.getMessage() == null ? cause.toString() : cause.getMessage();
            JOptionPane.showMessageDialog(this, message, "Cellpose", JOptionPane.ERROR_MESSAGE);
        }

        @Override public void close() {
            closed = true;
            if (worker != null) worker.cancel(true);
            if (runner != null) runner.close();
            super.close();
            adapter.close();
        }
    }
}
