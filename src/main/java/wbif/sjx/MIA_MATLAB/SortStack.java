package wbif.sjx.MIA_MATLAB;

import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;

import MIA_MATLAB_Core.StackSorter;
import ij.ImagePlus;
import ij.ImageStack;
import wbif.sjx.MIA.MIA;
import wbif.sjx.MIA.Module.Categories;
import wbif.sjx.MIA.Module.Category;
import wbif.sjx.MIA.Module.ModuleCollection;
import wbif.sjx.MIA.Module.ImageProcessing.Stack.ExtractSubstack;
import wbif.sjx.MIA.Object.Image;
import wbif.sjx.MIA.Object.Status;
import wbif.sjx.MIA.Object.Workspace;
import wbif.sjx.MIA.Object.Parameters.BooleanP;
import wbif.sjx.MIA.Object.Parameters.ChoiceP;
import wbif.sjx.MIA.Object.Parameters.InputImageP;
import wbif.sjx.MIA.Object.Parameters.OutputImageP;
import wbif.sjx.MIA.Object.Parameters.ParameterCollection;
import wbif.sjx.MIA.Object.Parameters.SeparatorP;
import wbif.sjx.MIA.Object.Parameters.Text.IntegerP;
import wbif.sjx.MIA.Object.References.Collections.ImageMeasurementRefCollection;
import wbif.sjx.MIA.Object.References.Collections.MetadataRefCollection;
import wbif.sjx.MIA.Object.References.Collections.ObjMeasurementRefCollection;
import wbif.sjx.MIA.Object.References.Collections.ParentChildRefCollection;
import wbif.sjx.MIA.Object.References.Collections.PartnerRefCollection;
import wbif.sjx.common.MathFunc.Indexer;

/**
 * Created by sc13967 on 30/06/2017.
 */
public class SortStack extends CoreMATLABModule {
    public static final String INPUT_SEPARATOR = "Image input/output";
    public static final String INPUT_IMAGE = "Input image";
    public static final String APPLY_TO_INPUT = "Apply to input image";
    public static final String OUTPUT_IMAGE = "Output image";

    public static final String SORT_SEPARATOR = "Sorting controls";
    public static final String SORT_AXIS = "Sort axis";
    public static final String OTHER_AXIS_MODE = "Other axis mode";
    public static final String CALCULATION_SOURCE = "Calculation source";
    public static final String EXTERNAL_SOURCE = "External source";
    public static final String CALCULATION_CHANNEL = "Calculation channel";

    public interface SortAxes {
        String TIME = "Time";
        String Z = "Z";

        String[] ALL = new String[] { TIME, Z };

    }

    public interface OtherAxisModes {
        String INDEPENDENT = "Independent";
        String LINKED = "Linked";

        String[] ALL = new String[] { INDEPENDENT, LINKED };

    }

    public interface CalculationSources {
        String INTERNAL = "Internal";
        String EXTERNAL = "External";

        String[] ALL = new String[] { INTERNAL, EXTERNAL };

    }

    public SortStack(ModuleCollection modules) {
        super("Sort stack", modules);
    }

    @Override
    public Category getCategory() {
        return Categories.IMAGE_PROCESSING_STACK;
    }

    @Override
    public String getDescription() {
        return "";
    }

    public void processIndependent(Image inputImage, Image referenceImage, String sortAxis, int calculationChannel) {
        // Verifying reference stack has the correct number of dimensions. If it isn't
        // valid, skip sorting, but allow remaining modules to continue.
        if (testReferenceValidity(referenceImage, sortAxis, inputImage, OtherAxisModes.INDEPENDENT)) {
            // Getting non-sorting axis length
            int nOther = getNonSortAxisLength(inputImage, sortAxis);

            // Iterating over all non-sorting axis indices, applying the sorting
            for (int i = 0; i < nOther; i++) {
                // Convert reference image to MWArray
                MWNumericArray referenceArray = getReferenceArrayIndependent(referenceImage, calculationChannel,
                        sortAxis, i);

                // Getting optimal stack order (this is different for the "other" axis)
                int[] order = getStackOrder(referenceArray, isVerbose());
                if (order == null) {
                    MIA.log.writeWarning("Input stack has not been sorted");
                    return;
                }
                reorderStack(inputImage, order, sortAxis, i);

            }

        } else {
            MIA.log.writeWarning("Input stack has not been sorted");
        }
    }

    public void processLinked(Image inputImage, Image referenceImage, String sortAxis, int calculationChannel) {
        // Verifying reference stack has the correct number of dimensions. If it isn't
        // valid, skip sorting, but allow remaining modules to continue.
        if (testReferenceValidity(referenceImage, sortAxis, inputImage, OtherAxisModes.LINKED)) {
            // Getting non-sorting axis length
            int nOther = getNonSortAxisLength(inputImage, sortAxis);

            // Convert reference image to MWArray
            MWNumericArray referenceArray = getReferenceArrayLinked(referenceImage, calculationChannel);

            // Getting optimal stack order (this is the same for the "other" axis)
            int[] order = getStackOrder(referenceArray, isVerbose());
            if (order == null) {
                MIA.log.writeWarning("Input stack has not been sorted");
                return;
            }

            // Iterating over all non-sorting axis indices, applying the sorting
            for (int i = 0; i < nOther; i++)
                reorderStack(inputImage, order, sortAxis, i);

        } else {
            MIA.log.writeWarning("Input stack has not been sorted");
        }
    }

    int getNonSortAxisLength(Image inputImage, String sortAxis) {
        switch (sortAxis) {
            case SortAxes.TIME:
                return inputImage.getImagePlus().getNSlices();
            case SortAxes.Z:
                return inputImage.getImagePlus().getNFrames();
        }

        return 0;

    }

    boolean testReferenceValidity(Image referenceImage, String sortAxis, Image inputImage, String otherAxisMode) {
        ImagePlus refIpl = referenceImage.getImagePlus();
        ImagePlus inputIpl = inputImage.getImagePlus();

        // Reference should have equal number of slices/frames as input in sorting
        // dimension and, if using the same alignment for all other frames/slices
        // (respectively), be single valued in other dimension
        switch (sortAxis) {
            case SortAxes.TIME:
                if (otherAxisMode.equals(OtherAxisModes.LINKED) && refIpl.getNSlices() > 1) {
                    MIA.log.writeWarning("Reference stack has too many slices (" + refIpl.getNSlices()
                            + ") when sorting along time axis.  Reference stack should only have 1 slice.");
                    return false;
                }

                if (refIpl.getNFrames() != inputIpl.getNFrames()) {
                    MIA.log.writeWarning(
                            "Reference stack has different number of frames to input image.  Reference has "
                                    + refIpl.getNFrames() + " frames, input has " + inputIpl.getNFrames() + " frames.");
                    return false;
                }

                break;

            case SortAxes.Z:
                if (otherAxisMode.equals(OtherAxisModes.LINKED) && refIpl.getNFrames() > 1) {
                    MIA.log.writeWarning("Reference stack has too many frames (" + refIpl.getNFrames()
                            + ") when sorting along Z axis.  Reference stack should only have 1 frame.");
                    return false;
                }

                if (refIpl.getNSlices() != inputIpl.getNSlices()) {
                    MIA.log.writeWarning(
                            "Reference stack has different number of slices to input image.  Reference has "
                                    + refIpl.getNSlices() + " slices, input has " + inputIpl.getNSlices() + " slices.");
                    return false;
                }

                break;
        }

        // Reference stack is valid
        return true;

    }

    MWNumericArray getReferenceArrayLinked(Image referenceImage, int calculationChannel) {
        Image referenceChannel = ExtractSubstack.extractSubstack(referenceImage, "Reference",
                String.valueOf(calculationChannel + 1), "1-end", "1-end");

        return imageStackToMW(referenceChannel.getImagePlus().getImageStack());

    }

    MWNumericArray getReferenceArrayIndependent(Image referenceImage, int calculationChannel, String sortAxis,
            int otherAxisIdx) {
        Image referenceChannel = null;
        switch (sortAxis) {
            case SortAxes.TIME:
                referenceChannel = ExtractSubstack.extractSubstack(referenceImage, "Reference",
                        String.valueOf(calculationChannel + 1), String.valueOf(otherAxisIdx + 1), "1-end");
                break;

            case SortAxes.Z:
                referenceChannel = ExtractSubstack.extractSubstack(referenceImage, "Reference",
                        String.valueOf(calculationChannel + 1), "1-end", String.valueOf(otherAxisIdx + 1));
                break;
        }

        if (referenceChannel == null)
            return null;

        return imageStackToMW(referenceChannel.getImagePlus().getImageStack());

    }

    int[] getStackOrder(MWNumericArray referenceArray, boolean verbose) {
        try {
            // Getting optimised stack order
            StackSorter stackSorter = new StackSorter();
            Object[] output = stackSorter.getOptimisedOrder(1, referenceArray, verbose);
            stackSorter.dispose();
            referenceArray.dispose();

            // Extracting slice order
            MWNumericArray orderArray = (MWNumericArray) output[0];
            int nPoints = orderArray.getDimensions()[0];
            Indexer indexer = new Indexer(nPoints, 2);
            int[] data = orderArray.getIntData();
            int[] order = new int[nPoints];

            for (int i = 0; i < nPoints; i++)
                order[i] = data[indexer.getIndex(new int[] { i, 0 })];

            return order;

        } catch (MWException e) {
            e.printStackTrace();
            return null;
        }
    }

    void reorderStack(Image image, int[] order, String sortAxis, int nonSortIdx) {
        switch (sortAxis) {
            case SortAxes.TIME:
                reorderStackTime(image, order, nonSortIdx);
                break;

            case SortAxes.Z:
                reorderStackZ(image, order, nonSortIdx);
                break;
        }
    }

    void reorderStackTime(Image image, int[] order, int z) {
        // Iterating over all channels
        ImagePlus sourceIpl = image.getImagePlus().duplicate();
        ImagePlus targetIpl = image.getImagePlus();

        for (int c = 0; c < sourceIpl.getNChannels(); c++) {
            for (int t = 0; t < sourceIpl.getNFrames(); t++) {
                if (sourceIpl.isHyperStack())
                    sourceIpl.setPosition(c + 1, z + 1, order[t]);
                else
                    sourceIpl.setPosition(order[t]);

                if (targetIpl.isHyperStack())
                    targetIpl.setPosition(c + 1, z + 1, t + 1);
                else
                    targetIpl.setPosition(t + 1);

                targetIpl.setProcessor(sourceIpl.getProcessor());

            }
        }
    }

    void reorderStackZ(Image image, int[] order, int t) {
        // Iterating over all channels
        ImagePlus sourceIpl = image.getImagePlus();
        ImageStack sourceIst = sourceIpl.getStack().duplicate();
        ImagePlus targetIpl = image.getImagePlus();
        ImageStack targetIst = targetIpl.getStack();

        for (int c = 0; c < sourceIpl.getNChannels(); c++) {
            for (int z = 0; z < sourceIpl.getNSlices(); z++) {
                int sourceIdx = sourceIpl.getStackIndex(c + 1, order[z], t + 1);
                int targetIdx = targetIpl.getStackIndex(c + 1, z + 1, t + 1);

                targetIst.setProcessor(sourceIst.getProcessor(sourceIdx), targetIdx);

            }
        }
    }

    @Override
    public Status process(Workspace workspace) {
        // Getting input image
        String inputImageName = parameters.getValue(INPUT_IMAGE);
        boolean applyToInput = parameters.getValue(APPLY_TO_INPUT);
        String outputImageName = parameters.getValue(OUTPUT_IMAGE);
        String sortAxis = parameters.getValue(SORT_AXIS);
        String otherAxisMode = parameters.getValue(OTHER_AXIS_MODE);
        String calculationSource = parameters.getValue(CALCULATION_SOURCE);
        String externalSourceName = parameters.getValue(EXTERNAL_SOURCE);
        int calculationChannel = parameters.getValue(CALCULATION_CHANNEL);

        // Calculation channel is specified on the GUI with numbering starting at 1
        calculationChannel--;

        Image inputImage = workspace.getImage(inputImageName);

        if (applyToInput)
            outputImageName = inputImageName;
        else
            inputImage = new Image(outputImageName, inputImage.getImagePlus().duplicate());

        // Getting reference image
        Image referenceImage = calculationSource.equals(CalculationSources.EXTERNAL)
                ? workspace.getImage(externalSourceName)
                : inputImage;

        // Process stacks depending on whether they will all have the same alignment in
        // additional dimensions
        switch (otherAxisMode) {
            case OtherAxisModes.INDEPENDENT:
                processIndependent(inputImage, referenceImage, sortAxis, calculationChannel);
                break;

            case OtherAxisModes.LINKED:
                processLinked(inputImage, referenceImage, sortAxis, calculationChannel);
                break;
        }

        if (!applyToInput)
            workspace.addImage(inputImage);

        if (showOutput)
            inputImage.showImage();

        return Status.PASS;

    }

    @Override
    protected void initialiseParameters() {
        parameters.add(new SeparatorP(INPUT_SEPARATOR, this));
        parameters.add(new InputImageP(INPUT_IMAGE, this));
        parameters.add(new BooleanP(APPLY_TO_INPUT, this, true));
        parameters.add(new OutputImageP(OUTPUT_IMAGE, this));

        parameters.add(new SeparatorP(SORT_SEPARATOR, this));
        parameters.add(new ChoiceP(SORT_AXIS, this, SortAxes.TIME, SortAxes.ALL));
        parameters.add(new ChoiceP(OTHER_AXIS_MODE, this, OtherAxisModes.INDEPENDENT, OtherAxisModes.ALL));
        parameters.add(new ChoiceP(CALCULATION_SOURCE, this, CalculationSources.INTERNAL, CalculationSources.ALL));
        parameters.add(new InputImageP(EXTERNAL_SOURCE, this));
        parameters.add(new IntegerP(CALCULATION_CHANNEL, this, 1));

        addParameterDescriptions();

    }

    @Override
    public ParameterCollection updateAndGetParameters() {
        ParameterCollection returnedParameters = new ParameterCollection();

        returnedParameters.add(parameters.getParameter(INPUT_SEPARATOR));
        returnedParameters.add(parameters.getParameter(INPUT_IMAGE));
        returnedParameters.add(parameters.getParameter(APPLY_TO_INPUT));
        if (!(boolean) parameters.getValue(APPLY_TO_INPUT)) {
            returnedParameters.add(parameters.getParameter(OUTPUT_IMAGE));
        }

        returnedParameters.add(parameters.getParameter(SORT_SEPARATOR));
        returnedParameters.add(parameters.getParameter(SORT_AXIS));
        returnedParameters.add(parameters.getParameter(OTHER_AXIS_MODE));
        returnedParameters.add(parameters.getParameter(CALCULATION_SOURCE));
        switch ((String) parameters.getValue(CALCULATION_SOURCE)) {
            case CalculationSources.EXTERNAL:
                returnedParameters.add(parameters.getParameter(EXTERNAL_SOURCE));
                break;
        }

        returnedParameters.add(parameters.getParameter(CALCULATION_CHANNEL));

        return returnedParameters;

    }

    @Override
    public ImageMeasurementRefCollection updateAndGetImageMeasurementRefs() {
        return null;
    }

    @Override
    public ObjMeasurementRefCollection updateAndGetObjectMeasurementRefs() {
        return null;
    }

    @Override
    public MetadataRefCollection updateAndGetMetadataReferences() {
        return null;
    }

    @Override
    public ParentChildRefCollection updateAndGetParentChildRefs() {
        return null;
    }

    @Override
    public PartnerRefCollection updateAndGetPartnerRefs() {
        return null;
    }

    @Override
    public boolean verify() {
        return true;
    }

    void addParameterDescriptions() {

    }
}
