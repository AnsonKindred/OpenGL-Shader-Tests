

import java.nio.FloatBuffer;
import java.util.Stack;

public class Matrix
{
    public static float[] model_view = new float[16];
    public static float[] projection = new float[16];
    
    private static Stack<float[]> modelViewMatrixStack = new Stack<float[]>();
    private static Stack<float[]> projectionMatrixStack = new Stack<float[]>();

    public static final float[] identity = {
	        1.0f, 0.0f, 0.0f, 0.0f,
	        0.0f, 1.0f, 0.0f, 0.0f,
	        0.0f, 0.0f, 1.0f, 0.0f,
	        0.0f, 0.0f, 0.0f, 1.0f
	    };
    
    public static void loadIdentityMV()
    {
    	model_view = identity.clone();
    }

    public static void loadIdentityProjection()
    {
    	projection = identity.clone();
    }
    
	public static void translate(float x, float y, float z) 
	{
		float[] t = { 
				1.0f, 0.0f, 0.0f, 0.0f, 
				0.0f, 1.0f, 0.0f, 0.0f, 
				0.0f, 0.0f, 1.0f, 0.0f, 
				x   , y   , z   , 1.0f 
			};
		
		model_view = multiply(model_view, t);
	}

	public static void rotate(float a, float x, float y, float z) 
	{
		float s, c;
		
		s = (float) Math.sin(Math.toRadians(a));
		c = (float) Math.cos(Math.toRadians(a));
		
		float[] r = { 
				x * x * (1.0f - c) + c, y * x * (1.0f - c) + z * s,
				x * z * (1.0f - c) - y * s, 0.0f, x * y * (1.0f - c) - z * s,
				y * y * (1.0f - c) + c, y * z * (1.0f - c) + x * s, 0.0f,
				x * z * (1.0f - c) + y * s, y * z * (1.0f - c) - x * s,
				z * z * (1.0f - c) + c, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f 
			};
		
		model_view = multiply(model_view, r);
	}
	
	/*
	 * Introducing projection matrix helper functions
	 * 
	 * OpenGL ES 2 vertex projection transformations gets applied inside the
	 * vertex shader, all you have to do are to calculate and supply a
	 * projection matrix.
	 * 
	 * Its recommended to use the com/jogamp/opengl/util/PMVMatrix.java import
	 * com.jogamp.opengl.util.PMVMatrix; To simplify all your projection model
	 * view matrix creation needs.
	 * 
	 * These helpers here are based on PMVMatrix code and common linear algebra
	 * for matrix multiplication, translate and rotations.
	 */
	public static void glMultMatrixf(FloatBuffer a, FloatBuffer b, FloatBuffer d) 
	{
		final int aP = a.position();
		final int bP = b.position();
		final int dP = d.position();
		for (int i = 0; i < 4; i++) 
		{
			final float ai0 = a.get(aP + i + 0 * 4), ai1 = a.get(aP + i + 1 * 4), ai2 = a.get(aP + i + 2 * 4), ai3 = a.get(aP + i + 3 * 4);
			d.put(dP + i + 0 * 4, ai0 * b.get(bP + 0 + 0 * 4) + ai1 * b.get(bP + 1 + 0 * 4) + ai2 * b.get(bP + 2 + 0 * 4) + ai3 * b.get(bP + 3 + 0 * 4));
			d.put(dP + i + 1 * 4, ai0 * b.get(bP + 0 + 1 * 4) + ai1 * b.get(bP + 1 + 1 * 4) + ai2 * b.get(bP + 2 + 1 * 4) + ai3 * b.get(bP + 3 + 1 * 4));
			d.put(dP + i + 2 * 4, ai0 * b.get(bP + 0 + 2 * 4) + ai1 * b.get(bP + 1 + 2 * 4) + ai2 * b.get(bP + 2 + 2 * 4) + ai3 * b.get(bP + 3 + 2 * 4));
			d.put(dP + i + 3 * 4, ai0 * b.get(bP + 0 + 3 * 4) + ai1 * b.get(bP + 1 + 3 * 4) + ai2 * b.get(bP + 2 + 3 * 4) + ai3 * b.get(bP + 3 + 3 * 4));
		}
	}
	
	// I wrote this myself and didn't follow the instructions in the comment above because I'm an ass
	public static void ortho(float left, float right, float bottom, float top)
	{
		float far = 1;
		float near = -1;
		// This is simplified to assume near of -1 and far of 1...I hope
		float tx = -((right + left) / (right - left));
		float ty = -((top + bottom) / (top - bottom));
		float tz = -((far + near) / (far - near));
		
		projection[0] = 2 / (right - left);
		projection[1] = 0.0f;
		projection[2] = 0.0f;
		projection[3] = 0.0f;
		projection[4] = 0.0f;
		projection[5] = 2 / (top - bottom);
		projection[6] = 0.0f;
		projection[7] = 0.0f;
		projection[8] = 0.0f;
		projection[9] = 0.0f;
		projection[10] = 2 / (far - near);
		projection[11] = 0.0f;
		projection[12] = tx;
		projection[13] = ty;
		projection[14] = tz;
		projection[15] = 1.0f;
	}

	public static float[] multiply(float[] a, float[] b) 
	{
		float[] tmp = new float[16];
		
		glMultMatrixf(FloatBuffer.wrap(a), FloatBuffer.wrap(b), FloatBuffer.wrap(tmp));
		
		return tmp;
	}
	
	public static void pushMV()
	{
		modelViewMatrixStack.push(model_view.clone());
	}
	
	public static void popMV()
	{
		model_view = modelViewMatrixStack.pop();
	}
	
	public static void pushProjection()
	{
		projectionMatrixStack.push(model_view.clone());
	}
	
	public static void popProjection()
	{
		projection = projectionMatrixStack.pop();
	}
}
