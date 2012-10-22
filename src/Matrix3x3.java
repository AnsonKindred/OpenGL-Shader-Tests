import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Maintains a matrix for transforming vertices in a scene.
 * Specialized for 2d transformations.
 * 
 * Seems to be a slight speed increase in using buffers instead of arrays
 * 
 * @author Zeb Long
 *
 */
public class Matrix3x3
{   
	private static final int STACK_SIZE = 10;
	// 3x3 matrix of floats
	private static final int MATRIX_BYTE_SIZE = 3*3*Float.SIZE/Byte.SIZE;
	
	// 3x3 identity matrix
    public static final FloatBuffer identity = (FloatBuffer) ByteBuffer.allocateDirect(MATRIX_BYTE_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer()
    		.put(1).put(0).put(0)
    		.put(0).put(1).put(0)
    		.put(0).put(0).put(1)
    		.position(0);
    
    // The entire matrix stack is a single directly allocated bytebuffer, we just keep track of an offset into it
    private static FloatBuffer matrixStack = ByteBuffer.allocateDirect(MATRIX_BYTE_SIZE*STACK_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static int currentIndex = 0;
    
    /**
     * Load the identity matrix into the current matrix
     */
    public static void loadIdentity()
    {
    	for(int i = 0; i < 9; i++)
		{
			matrixStack.put(currentIndex*9+i, identity.get(i));
		}
    	identity.position(0);
    }
	
    /**
     * Perform a translation operation on the current matrix
     */
	public static void translate(float x, float y) 
	{
		for (int i = currentIndex*9; i < currentIndex*9+3; i++) 
		{	
			float a0 = matrixStack.get(i);      
			float a1 = matrixStack.get(i + 3); 
			float a2 = matrixStack.get(i + 6); 
			matrixStack.put(i+6, a0*x + a1*y + a2);
		}
	}
	
	/**
	 * Set the current matrix to a 2d orthographic projection.
	 * This is equivalent to translating and scaling.
	 * 
	 * @param left
	 * @param right
	 * @param bottom
	 * @param top
	 */
	public static void ortho(float left, float right, float bottom, float top)
	{
		float tx = -((right + left) / (right - left));
		float ty = -((top + bottom) / (top - bottom));
		
		FloatBuffer m = (FloatBuffer) matrixStack.position(currentIndex*9);
		m.put(2 / (right - left)).put(0.0f).put(0.0f);
		m.put(0.0f).put(2 / (top - bottom)).put(0.0f);
		m.put(tx).put(ty).put(1.0f);
	}
	
	/**
	 * Gets the slice of the matrix stack that represents the current matrix.
	 * Modifying the buffer returned by this method will modify the actual matrix stack.
	 * 
	 * @return The current matrix
	 */
	public static FloatBuffer getMatrix()
	{
		int position = matrixStack.position();
		int limit = matrixStack.limit();
		
		matrixStack.position(currentIndex*9);
		matrixStack.limit((currentIndex+1)*9);
		
		FloatBuffer temp = matrixStack.slice();
		
		matrixStack.position(position);
		matrixStack.limit(limit);
		
		return temp;
	}
	
	/**
	 * Copies the current matrix into the next position in the stack which then
	 * becomes the new current matrix
	 */
	public static void push()
	{
		for(int from = currentIndex*9; from < (currentIndex+1)*9; from++)
		{
			int to = from+9;
			matrixStack.put(to, matrixStack.get(from));
		}
		currentIndex++;
	}
	
	/**
	 * Replaces the current matrix with the previous matrix in the stack
	 */
	public static void pop()
	{
		currentIndex--;
	}
}
