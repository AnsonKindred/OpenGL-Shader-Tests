import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GL2;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import com.jogamp.opengl.util.FPSAnimator;

public class GLRenderer extends GLCanvas implements GLEventListener, WindowListener
{
	static final int NUM_THINGS = 1000000;
	static final float SIZE = .1f;
	static final float MAX_SPEED = .2f;
	
	static final int FLOAT_BYTES = Float.SIZE / Byte.SIZE;
	static final int INT_BYTES = Integer.SIZE / Byte.SIZE;
	
	float viewWidth, viewHeight;
	
	FPSAnimator animator;
	
	boolean didInit = false;
	long startDrawTime = 0;
	long numDrawIterations = 0;
	
	IntBuffer idBuffer = IntBuffer.allocate(1);
	
	// The base geometry, just a triangle
	float[] vertices = { 
			-SIZE, -SIZE,
			-SIZE,  SIZE,
			 SIZE,  SIZE
		};
	
	// Interlaced x,y positions of each thing
	float[] velocities = new float[NUM_THINGS*2];
	
	// Always use VBOs when possible
	int vertexBufferID  = 0;
	int positionBufferID = 0;
	
	// Shader attributes
	int shaderProgram;
	int projectionAttribute, vertexAttribute, positionAttribute;
	
	public static void main(String[] args) 
    {
		GLCapabilities cap = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		new GLRenderer(cap);
    }
	
	public GLRenderer(GLCapabilities cap)
	{
		// Standard setup stuff
		super(cap);
		
		addGLEventListener(this);
		setSize(1800, 1000);

		JFrame the_frame = new JFrame("Hello World");
	    the_frame.getContentPane().add(this);
	    the_frame.setSize(the_frame.getContentPane().getPreferredSize());
	    the_frame.setVisible(true);
	    the_frame.addWindowListener(this);
		
		animator = new FPSAnimator(this, 60);
		animator.start();
	}
	
	/**
	 * Called when the GL context is first made available
	 * 
	 * Sets up the shaders and buffers
	 */
	public void init(GLAutoDrawable d)
	{		
		final GL2 gl = d.getGL().getGL2();
        
		gl.glClearColor(0f, 0f, 0f, 1f);
	    gl.glEnableVertexAttribArray(vertexAttribute);
		
		shaderProgram = ShaderLoader.compileProgram(gl, "default");
        gl.glLinkProgram(shaderProgram);
        
        // Grab references to the shader attributes
        projectionAttribute = gl.glGetUniformLocation(shaderProgram, "projection");
        vertexAttribute     = gl.glGetAttribLocation(shaderProgram, "vertex");
        positionAttribute   = gl.glGetUniformLocation(shaderProgram, "positionSampler");
		
	    _loadVertexData(gl);
	    _preparePositionBuffer(gl);
	    
		gl.glUseProgram(shaderProgram);
	}
	
	/**
	 * Loads NUM_THINGS instances of the vertex data into a buffer on the graphics card.
	 * Each vertex's z component is the index of the instance and can be used to
	 * look up the instance's position using the positionSampler in the vertex shader.
	 * 
	 * OpenGL prefers directly allocated buffers.
	 * They have improved performance over FloatBuffer.allocate and buffer.putFloat methods
	 * 
	 * @param gl
	 */
	private void _loadVertexData(GL2 gl)
	{
        // Buffer lengths are in bytes
	    int numBytes = 3*vertices.length*FLOAT_BYTES*NUM_THINGS/2;
	    
		// Bind a buffer on the graphics card and load our vertexBuffer into it
        vertexBufferID = _generateBufferID(gl);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBufferID);
		
		// Allocate some space
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, numBytes, null, GL2.GL_STATIC_DRAW);
		
		// Tell OpenGL to use our vertexAttribute as _the_ vertex attribute in the shader and to use
		// the currently bound buffer as the data source
		gl.glVertexAttribPointer(vertexAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
		
		// Map the buffer so that we can insert some data
		ByteBuffer vertexBuffer = gl.glMapBuffer(GL2.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
		FloatBuffer vertexFloatBuffer = vertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		
		// Add the vertices to the FloatBuffer
		// The z-component is used to store an index that the shader will use to 
		// look up the position for each vertex
        for(int i = 0; i < NUM_THINGS; i++)
        {
        	for(int v = 0; v < vertices.length; v+=2)
            {
        		vertexFloatBuffer.put(vertices[v]);
        		vertexFloatBuffer.put(vertices[v+1]);
        		vertexFloatBuffer.put(i); // the index
            }
        }
        
        gl.glUnmapBuffer(GL2.GL_ARRAY_BUFFER);
	    gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
	}

	/**
	 * Set up a texture buffer to hold the position data and tell TEXTURE0 to use it.
	 * Also makes sure that the positionSampler is hooked up to TEXTURE0.
	 * 
	 * @param gl
	 */
	private void _preparePositionBuffer(GL2 gl)
	{
	    // Make sure the position sampler is bound to TEXTURE0 and TEXTURE0 is active
	    gl.glUniform1f(positionAttribute, 0); // 0 means TEXTURE0
	    gl.glActiveTexture(GL2.GL_TEXTURE0);
	    
		// Bind a texture buffer
		positionBufferID = _generateBufferID(gl);
		gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, positionBufferID);
	    
	    // Give it a size
	    int size = NUM_THINGS * 2 * FLOAT_BYTES;
	    gl.glBufferData(GL2.GL_TEXTURE_BUFFER, size, null, GL2.GL_STREAM_DRAW);
	    
	    // Unbind
	    gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, 0);

	    // The magic: Point the active texture (TEXTURE0) at the position texture buffer
	    // Right now the buffer is empty, but once we fill it, the positionSampler in
	    // the vertex shader will be able to access the data using texelFetch
	    gl.glTexBuffer(GL2.GL_TEXTURE_BUFFER, GL2.GL_RGBA32F, positionBufferID);
	}
	
	/**
	 * Called by me the first time 'reshape' is called.
	 * Useful for things that can't be initialized until the screen size is known.
	 * 
	 * @param gl
	 */
	public void viewInit(GL2 gl)
	{
		// Bind and fill the position buffer
		gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, positionBufferID);
	    ByteBuffer textureBuffer = gl.glMapBuffer(GL2.GL_TEXTURE_BUFFER, GL2.GL_WRITE_ONLY);
	    FloatBuffer textureFloatBuffer = textureBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

		for(int i = 0; i < NUM_THINGS; i++)
		{
			// Give each thing a random starting position within the bounds of the view
			textureFloatBuffer.put((float) (Math.random()*viewWidth));
			textureFloatBuffer.put((float) (Math.random()*viewHeight));
			
			// and random starting velocity
			velocities[i*2] = (float) Math.random()*MAX_SPEED - MAX_SPEED/2;
			velocities[i*2+1] = (float) Math.random()*MAX_SPEED - MAX_SPEED/2;
		}
	    
	    gl.glUnmapBuffer(GL2.GL_TEXTURE_BUFFER);
	    gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, 0);
	}
	
	/**
	 * Draw the things
	 */
	public void display(GLAutoDrawable d)
	{
		if(numDrawIterations == 0)
		{
			startDrawTime = System.currentTimeMillis();
		}
		
		final GL2 gl = d.getGL().getGL2();
       
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		_updatePositions(gl);

		// Render all of the things
		gl.glDrawArrays(GL2.GL_TRIANGLES, 0, NUM_THINGS*3);
		
		numDrawIterations++;
		if(numDrawIterations > 100)
		{
			// Make sure opengl is done before we calculate the time
			gl.glFinish();
			
			long totalDrawTime = System.currentTimeMillis() - startDrawTime;
			System.out.println(totalDrawTime / numDrawIterations);
			numDrawIterations = 0;
		}
	}
	
	/**
	 * Update the positions on the graphics card
	 * 
	 * @param gl
	 */
	public void _updatePositions(GL2 gl)
	{
		gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, positionBufferID);
		ByteBuffer textureBuffer = gl.glMapBuffer(GL2.GL_TEXTURE_BUFFER, GL2.GL_READ_WRITE);
		
	    FloatBuffer textureFloatBuffer = textureBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
		for(int i = 0; i < NUM_THINGS*2; i++)
		{
			textureFloatBuffer.put(i, textureFloatBuffer.get(i) + velocities[i]);
		}
		
	    gl.glUnmapBuffer(GL2.GL_TEXTURE_BUFFER);
	    gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, 0);
	}
	
	/**
	 * Resizes the viewport and sets the transformation matrix
	 */
	public void reshape(GLAutoDrawable d, int x, int y, int width, int height)
	{
		final GL2 gl = d.getGL().getGL2();
		gl.glViewport(0, 0, width, height);
		float ratio = (float) height / width;
		
		// width is fixed at 100
		viewWidth = 100;
		// height is whatever it needs to be so that the aspect ratio is the same as the viewport
		viewHeight = viewWidth * ratio;
		
		Matrix3x3.ortho(0, viewWidth, 0, viewHeight);
	    
		// Send the projection matrix to the shader, only needs to be sent once per resize
        gl.glUniformMatrix3fv(projectionAttribute, 1, false, Matrix3x3.getMatrix());
		
		if (!didInit)
		{
			viewInit(gl);
			didInit = true;
		}
	}
	
	/**
	 * Generate an unused id for a buffer on the graphics card
	 * 
	 * @return the id
	 */
	private int _generateBufferID(GL2 gl)
	{
		gl.glGenBuffers(1, idBuffer);
		return idBuffer.get(0);
	}

	@Override
	public void windowClosing(WindowEvent arg0)
	{
		animator.stop();
		System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent arg0){}
	@Override
	public void windowDeiconified(WindowEvent arg0){}
	@Override
	public void windowIconified(WindowEvent arg0){}
	@Override
	public void windowOpened(WindowEvent arg0){}
	@Override
	public void windowActivated(WindowEvent arg0){}
	@Override
	public void windowClosed(WindowEvent arg0){}
	@Override
	public void dispose(GLAutoDrawable drawable){}
}