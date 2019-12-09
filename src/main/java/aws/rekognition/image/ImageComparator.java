//AWS Rekognition Tutorial https://docs.aws.amazon.com/rekognition/latest/dg/faces-comparefaces.html
package aws.rekognition.image;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.util.IOUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class ImageComparator.
 */
public class ImageComparator {
	
	/** The Constant similarityThreshold. */
	public static final Float similarityThreshold = 70F;
	
	/** The target. */
	private Image source, target;
	
	/** The rekognition client. */
	private AmazonRekognition rekognitionClient;
	
	/**
	 * Instantiates a new image comparator.
	 */
	public ImageComparator() {
	    rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();
	}
	
	/**
	 * Compare stage source.
	 *
	 * @param sourceImage the source image
	 */
	public void compareStageSource(String sourceImage) {
		ByteBuffer sourceImageBytes=null;
		try (InputStream inputStream = new FileInputStream(new File(sourceImage))) {
	    	sourceImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
	    } catch(Exception e) {
			// TODO Auto-generated catch block
	        System.out.println("Failed to load source image " + sourceImage);
	    }
	    source = new Image().withBytes(sourceImageBytes);
	}
	
	/**
	 * Compare stage target.
	 *
	 * @param targetImage the target image
	 */
	public void compareStageTarget(String targetImage) {
	    ByteBuffer targetImageBytes=null;
	    try (InputStream inputStream = new FileInputStream(new File(targetImage))) {
	        targetImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
	    } catch(Exception e) {
			// TODO Auto-generated catch block
	        System.out.println("Failed to load target images: " + targetImage);
	    }
	    target=new Image().withBytes(targetImageBytes);
	}
	
	/**
	 * Gets the confidence level.
	 *
	 * @return the confidence level
	 */
	public Float getConfidenceLevel() {
		CompareFacesRequest request = new CompareFacesRequest()
	               .withSourceImage(source)
	               .withTargetImage(target)
	               .withSimilarityThreshold(similarityThreshold);
		CompareFacesResult compareFacesResult=rekognitionClient.compareFaces(request);
		List <CompareFacesMatch> faceDetails = compareFacesResult.getFaceMatches();
	    	for (CompareFacesMatch match: faceDetails) {
	    		ComparedFace face= match.getFace();
	    		BoundingBox position = face.getBoundingBox();
	    		/*System.out.println("Face at " + position.getLeft().toString()
	    				+ " " + position.getTop()
	    				+ " matches with " + face.getConfidence().toString()
	    				+ "% confidence.");*/
	    		return face.getConfidence();
	    	}
	    List<ComparedFace> uncompared = compareFacesResult.getUnmatchedFaces();
	    return new Float(-1.0F);
	}
}
