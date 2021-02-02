package deepimagej.tools;

public class Borrar {
/**********************************************************************************
 * 					Mask R-CNN CONFIG
 * DEFINITION OF THE MASK R-CNN PARAMETERS
 * 
 * In order to use the Java pre- and post-processing provided by the DeepImageJ team
 * it is necessary to fill the following parameters with the corresponding values relative 
 * to the model training.
 * 
 * DeepImageJ provides an adaptation to Java of the pre- and post-processing
 * implemented in Python of https://github.com/matterport/Mask_RCNN
 * The parameters below and their definition are a direct copy from their work.
 * In order to find out more information about the implementation of the Mask R-CNN
 * and its training, please refer to https://github.com/matterport/Mask_RCNN 
 * 
 * IMPORTANT: Change only the parameter values. DO NOT change the parameter names
 * or the comments
 * 
 * *****************************************************************************
 * *****************************************************************************
 * 
 * Name the configurations. For example, 'COCO', 'Experiment 3', ...etc.
 * Useful if your code needs to do things differently depending on which
 * experiment is running.
 *  
 * PARAMETER: NAME = None
 *  
 *  
 * Only useful if you supply a callable to BACKBONE. Should compute
 * the shape of each layer of the FPN Pyramid.
 * PARAMETER: BACKBONE = resnet101
 * PARAMETER: COMPUTE_BACKBONE_SHAPE = None
 * 
 * 
 * The strides of each layer of the FPN Pyramid. These values
 * are based on a Resnet101 backbone.
 * 
 * PARAMETER: BACKBONE_STRIDES = [4, 8, 16, 32, 64]
 * 
 * 
 * Size of the fully-connected layers in the classification graph
 * 
 * PARAMETER: FPN_CLASSIF_FC_LAYERS_SIZE = 1024
 * 
 * 
 * Size of the top-down layers used to build the feature pyramid
 * 
 * PARAMETER: TOP_DOWN_PYRAMID_SIZE = 256
 *  
 * 
 * Number of classification classes (including background)
 * 
 * PARAMETER: NUM_CLASSES = 81
 * 
 * 
 * Length of square anchor side in pixels
 * PARAMETER: RPN_ANCHOR_SCALES = (32, 64, 128, 256, 512)
 * 
 * 
 * Ratios of anchors at each cell (width/height)
 * A value of 1 represents a square anchor, and 0.5 is a wide anchor
 * PARAMETER: RPN_ANCHOR_RATIOS = [0.5, 1, 2]
 * 
 * 
 * Anchor stride
 * If 1 then anchors are created for each cell in the backbone feature map.
 * If 2, then anchors are created for every other cell, and so on.
 * PARAMETER: RPN_ANCHOR_STRIDE = 1
 * 
 * 
 * Non-max suppression threshold to filter RPN proposals.
 * You can increase this during training to generate more propsals.
 * PARAMETER: RPN_NMS_THRESHOLD = 0.7
 * 
 * 
 * How many anchors per image to use for RPN training
 * PARAMETER: RPN_TRAIN_ANCHORS_PER_IMAGE = 256
 * 
 * 
 * ROIs kept after tf.nn.top_k and before non-maximum suppression
 * PARAMETER: PRE_NMS_LIMIT = 6000
 * 
 * 
 * ROIs kept after non-maximum suppression (inference)
 * PARAMETER: POST_NMS_ROIS_INFERENCE = 1000
 * 
 * 
 * If enabled, resizes instance masks to a smaller size to reduce
 * memory load. Recommended when using high-resolution images.
 * PARAMETER: USE_MINI_MASK = true
 * Size of the mini mask (height, width)
 * PARAMETER: MINI_MASK_SHAPE = (56, 56)  
 * 
 * 
 * Input image resizing
 * Generally, use the "square" resizing mode for training and predicting
 * and it should work well in most cases. In this mode, images are scaled
 * up such that the small side is = IMAGE_MIN_DIM, but ensuring that the
 * scaling doesn't make the long side > IMAGE_MAX_DIM. Then the image is
 * padded with zeros to make it a square so multiple images can be put
 * in one batch.
 * Available resizing modes:
 * none:   No resizing or padding. Return the image unchanged.
 * square: Resize and pad with zeros to get a square image
 *         of size [max_dim, max_dim].
 * pad64:  Pads width and height with zeros to make them multiples of 64.
 *         If IMAGE_MIN_DIM or IMAGE_MIN_SCALE are not None, then it scales
 *         up before padding. IMAGE_MAX_DIM is ignored in this mode.
 *         The multiple of 64 is needed to ensure smooth scaling of feature
 *         maps up and down the 6 levels of the FPN pyramid (2**6=64).
 * crop:   Picks random crops from the image. First, scales the image based
 *         on IMAGE_MIN_DIM and IMAGE_MIN_SCALE, then picks a random crop of
 *         size IMAGE_MIN_DIM x IMAGE_MIN_DIM. Can be used in training only.
 *         IMAGE_MAX_DIM is not used in this mode.
 * PARAMETER: IMAGE_RESIZE_MODE = "square"
 * PARAMETER: IMAGE_MIN_DIM = 800
 * PARAMETER: IMAGE_MAX_DIM = 1024
 * PARAMETER: IMAGE_SHAPE = [1024, 1024, 3]
 * 
 * 
 * Minimum scaling ratio. Checked after MIN_IMAGE_DIM and can force further
 * up scaling. For example, if set to 2 then images are scaled up to double
 * the width and height, or more, even if MIN_IMAGE_DIM doesn't require it.
 * However, in 'square' mode, it can be overruled by IMAGE_MAX_DIM.
 * PARAMETER: IMAGE_MIN_SCALE = 0
 * 
 * 
 * Number of color channels per image. RGB = 3, grayscale = 1, RGB-D = 4
 * Changing this requires other changes in the code. See the WIKI for more
 * details: https://github.com/matterport/Mask_RCNN/wiki
 * PARAMETER: IMAGE_CHANNEL_COUNT = 3
 * 
 * 
 * Image mean (RGB)
 * PARAMETER: MEAN_PIXEL = [123.7, 116.8, 103.9]
 * 
 * 
 * Number of ROIs per image to feed to classifier/mask heads
 * The Mask RCNN paper uses 512 but often the RPN doesn't generate
 * enough positive proposals to fill this and keep a positive:negative
 * ratio of 1:3. You can increase the number of proposals by adjusting
 * the RPN NMS threshold.
 * PARAMETER: TRAIN_ROIS_PER_IMAGE = 200
 * 
 * 
 * Percent of positive ROIs used to train classifier/mask heads
 * PARAMETER: ROI_POSITIVE_RATIO = 0.33
 * 
 * 
 * Pooled ROIs
 * PARAMETER: POOL_SIZE = 7
 * PARAMETER: MASK_POOL_SIZE = 14
 * 
 * 
 * Shape of the output mask
 * To change this you also need to change the neural network mask branch
 * PARAMETER: MASK_SHAPE = [28, 28]
 * 
 * 
 * Maximum number of ground truth instances to use in one image
 * PARAMETER: MAX_GT_INSTANCES = 100
 * 
 * 
 * Bounding box refinement standard deviation for RPN and final detections.
 * PARAMETER: RPN_BBOX_STD_DEV = [0.1, 0.1, 0.2, 0.2]
 * PARAMETER: BBOX_STD_DEV = [0.1, 0.1, 0.2, 0.2]
 * 
 * 
 * Max number of final detections
 * PARAMETER: DETECTION_MAX_INSTANCES = 100
 * 
 * 
 * Minimum probability value to accept a detected instance
 * PARAMETER: DETECTION_MIN_CONFIDENCE = 0.7
 * 
 * 
 * Non-maximum suppression threshold for detection
 * PARAMETER: DETECTION_NMS_THRESHOLD = 0.3
 * 
 * 
 * Learning rate and momentum
 * The Mask RCNN paper uses lr=0.02, but on TensorFlow it causes
 * weights to explode. Likely due to differences in optimizer
 * implementation.
 * PARAMETER: LEARNING_RATE = 0.001
 * PARAMETER: LEARNING_MOMENTUM = 0.9
 * 
 * 
 * Weight decay regularization
 * PARAMETER: WEIGHT_DECAY = 0.0001
 * 
 * 
 * Gradient norm clipping
 * PARAMETER: GRADIENT_CLIP_NORM = 5.0
 * 
 * 
 * 
 * 
 * PARAMETER: IMAGE_META_SIZE = 93
 * PARAMETER: USE_RPN_ROIS = true
 * 
** ---- PARAMETERS_MODIFIED_AT_RUNTIME ------
 * The following parameters are modified during the processing. Leave as it is
 *
 *
 * RUNTIME_PARAMETER: WINDOW_SIZE = [0.0, 0.0, 660.0, 716.0]
 * RUNTIME_PARAMETER: ORIGINAL_IMAGE_SIZE = null
 * RUNTIME_PARAMETER: PROCESSING_IMAGE_SIZE = null
***/
}
