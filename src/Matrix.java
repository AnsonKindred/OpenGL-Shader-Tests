

import java.nio.FloatBuffer;
import java.util.Stack;

public class Matrix
{
    public static float[] model_view4f = new float[16];
    public static float[] projection4f = new float[16];
    
    private static Stack<float[]> modelViewMatrixStack4f = new Stack<float[]>();
    private static Stack<float[]> projectionMatrixStack4f = new Stack<float[]>();
    
    public static float[] model_view3f = new float[9];
    public static float[] projection3f = new float[9];
    
    private static Stack<float[]> modelViewMatrixStack3f = new Stack<float[]>();
    private static Stack<float[]> projectionMatrixStack3f = new Stack<float[]>();

    public static final float[] identity4f = {
	        1.0f, 0.0f, 0.0f, 0.0f,
	        0.0f, 1.0f, 0.0f, 0.0f,
	        0.0f, 0.0f, 1.0f, 0.0f,
	        0.0f, 0.0f, 0.0f, 1.0f
	    };
    
    public static final float[] identity3f = {
	        1.0f, 0.0f, 0.0f,
	        0.0f, 1.0f, 0.0f,
	        0.0f, 0.0f, 1.0f
	    };
    
    public static void loadIdentityMV4f()
    {
    	model_view4f = identity4f.clone();
    }

    public static void loadIdentityProjection4f()
    {
    	projection4f = identity4f.clone();
    }
    
    public static void loadIdentityMV3f()
    {
    	model_view3f = identity3f.clone();
    }

    public static void loadIdentityProjection3f()
    {
    	projection3f = identity3f.clone();
    }
    
	public static void translate3f(float x, float y, float z) 
	{
		float[] t = { 
				1.0f, 0.0f, 0.0f, 0.0f, 
				0.0f, 1.0f, 0.0f, 0.0f, 
				0.0f, 0.0f, 1.0f, 0.0f, 
				x   , y   , z   , 1.0f 
			};
		
		model_view4f = multiply4f(model_view4f, t);
	}
	
	public static void translate2f(float x, float y) 
	{
		float[] t = { 
				1.0f, 0.0f, 0.0f, 
				0.0f, 1.0f, 0.0f, 
				x   , y   , 1.0f
			};
		
		model_view3f = multiply3f(model_view3f, t);
	}

	public static void rotate4f(float a, float x, float y, float z) 
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
		
		model_view4f = multiply4f(model_view4f, r);
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
	public static void glMultMatrix4f(FloatBuffer a, FloatBuffer b, FloatBuffer d) 
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
	
	public static void glMultMatrix3f(FloatBuffer a, FloatBuffer b, FloatBuffer d) 
	{
		final int aP = a.position();
		final int bP = b.position();
		final int dP = d.position();
		for (int i = 0; i < 3; i++) 
		{
			final float ai0 = a.get(aP + i + 0 * 3);
			final float ai1 = a.get(aP + i + 1 * 3);
			final float ai2 = a.get(aP + i + 2 * 3);
			d.put(dP + i + 0 * 3, ai0 * b.get(bP + 0 + 0 * 3) + ai1 * b.get(bP + 1 + 0 * 3) + ai2 * b.get(bP + 2 + 0 * 3));
			d.put(dP + i + 1 * 3, ai0 * b.get(bP + 0 + 1 * 3) + ai1 * b.get(bP + 1 + 1 * 3) + ai2 * b.get(bP + 2 + 1 * 3));
			d.put(dP + i + 2 * 3, ai0 * b.get(bP + 0 + 2 * 3) + ai1 * b.get(bP + 1 + 2 * 3) + ai2 * b.get(bP + 2 + 2 * 3));
		}
	}
	
	// I wrote this myself and didn't follow the instructions in the comment above because I'm an ass
	public static void ortho3f(float left, float right, float bottom, float top)
	{
		// This is simplified to assume near of -1 and far of 1...I hope
		float tx = -((right + left) / (right - left));
		float ty = -((top + bottom) / (top - bottom));
		
		projection3f[0] = 2 / (right - left);
		projection3f[1] = 0.0f;
		projection3f[2] = 0.0f;
		
		projection3f[3] = 0.0f;
		projection3f[4] = 2 / (top - bottom);
		projection3f[5] = 0.0f;
		
		projection3f[6] = tx;
		projection3f[7] = ty;
		projection3f[8] = 1.0f;
	}
	
	// I wrote this myself and didn't follow the instructions in the comment above because I'm an ass
	public static void ortho4f(float left, float right, float bottom, float top)
	{
		float far = 1;
		float near = -1;
		// This is simplified to assume near of -1 and far of 1...I hope
		float tx = -((right + left) / (right - left));
		float ty = -((top + bottom) / (top - bottom));
		float tz = -((far + near) / (far - near));
		
		projection4f[0] = 2 / (right - left);
		projection4f[1] = 0.0f;
		projection4f[2] = 0.0f;
		projection4f[3] = 0.0f;
		projection4f[4] = 0.0f;
		projection4f[5] = 2 / (top - bottom);
		projection4f[6] = 0.0f;
		projection4f[7] = 0.0f;
		projection4f[8] = 0.0f;
		projection4f[9] = 0.0f;
		projection4f[10] = 2 / (far - near);
		projection4f[11] = 0.0f;
		projection4f[12] = tx;
		projection4f[13] = ty;
		projection4f[14] = tz;
		projection4f[15] = 1.0f;
	}

	public static float[] multiply4f(float[] a, float[] b) 
	{
		float[] tmp = new float[16];
		
		glMultMatrix4f(FloatBuffer.wrap(a), FloatBuffer.wrap(b), FloatBuffer.wrap(tmp));
		
		return tmp;
	}
	
	public static float[] multiply3f(float[] a, float[] b) 
	{
		float[] tmp = new float[9];
		
		glMultMatrix3f(FloatBuffer.wrap(a), FloatBuffer.wrap(b), FloatBuffer.wrap(tmp));
		
		return tmp;
	}
	
	public static void pushMV4f()
	{
		modelViewMatrixStack4f.push(model_view4f.clone());
	}
	
	public static void popMV4f()
	{
		model_view4f = modelViewMatrixStack4f.pop();
	}
	
	public static void pushProjection4f()
	{
		projectionMatrixStack4f.push(model_view4f.clone());
	}
	
	public static void popProjection34()
	{
		projection4f = projectionMatrixStack4f.pop();
	}
	
	public static void pushMV3f()
	{
		modelViewMatrixStack3f.push(model_view3f.clone());
	}
	
	public static void popMV3f()
	{
		model_view3f = modelViewMatrixStack3f.pop();
	}
	
	public static void pushProjection3f()
	{
		projectionMatrixStack3f.push(model_view3f.clone());
	}
	
	public static void popProjection3f()
	{
		projection3f = projectionMatrixStack3f.pop();
	}
}
