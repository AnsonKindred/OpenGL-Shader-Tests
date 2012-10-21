import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
	static final int NUM_THINGS = 100000;
	static final float SIZE = .05f;
	
	static final int FLOAT_BYTES = Float.SIZE / Byte.SIZE;
	
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
	float[] positions = new float[NUM_THINGS*2];
	
	// Always use VBOs
	int vertexBufferID  = 0;
	
	// Shader attributes
	int shaderProgram;
	int mvpAttribute, vertexAttribute;
	
	public static void main(String[] args) 
    {
		new GLRenderer();
    }
	
	public GLRenderer()
	{
		// Standard setup stuff
		super(new GLCapabilities(GLProfile.get(GLProfile.GL2)));
		
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
		
		shaderProgram = ShaderLoader.compileProgram(gl, "default");
        gl.glLinkProgram(shaderProgram);
        
        // Grab references to the shader attributes
        vertexAttribute = gl.glGetAttribLocation(shaderProgram, "vertex");
        mvpAttribute    = gl.glGetUniformLocation(shaderProgram, "mvp");
		
        // Buffer lengths are in bytes
	    int numBytes = vertices.length * FLOAT_BYTES;
	    
		// OpenGL prefers directly allocated buffers. 
	    // They have improved performance over FloatBuffer.allocate and buffer.putFloat methods
	    
	    // Allocate a buffer to hold the vertices so that we can send them to the graphics card
		ByteBuffer vbb = ByteBuffer.allocateDirect(numBytes).order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuffer = vbb.asFloatBuffer();
        
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(vertices);
        // If you forget to rewind your buffers, you're gonna have a bad time
        vertexBuffer.rewind();
        
        vertexBufferID = _generateBufferID(gl);
		
		// Bind the buffer on the graphics card and load our vertexBuffer into it
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBufferID);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, numBytes, vertexBuffer, GL2.GL_STATIC_DRAW);
		
		// Tell OpenGL to use our vertexAttribute as _the_ vertex attribute in the shader and to use
		// the currently bound buffer as the data source
		gl.glVertexAttribPointer(vertexAttribute, 2, GL2.GL_FLOAT, false, 0, 0);
	    gl.glEnableVertexAttribArray(vertexAttribute);
	    
		gl.glUseProgram(shaderProgram);
	}
	
	/**
	 * Called by me the first time 'reshape' is called.
	 * Useful for things that can't be initialized until the screen size is known.
	 * 
	 * @param gl
	 */
	public void viewInit(GL2 gl)
	{
		// Give each thing a random starting position within the bounds of the view
		for(int i = 0; i < NUM_THINGS; i++)
		{
			positions[i*2] = (float) (Math.random()*viewWidth);
			positions[i*2+1] = (float) (Math.random()*viewWidth);
		}
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
		
		// Render all of the things
		for(int i = 0; i < NUM_THINGS; i++)
		{
			_render(gl, positions[i*2], positions[i*2+1]);
		}
		
		numDrawIterations++;
		if(numDrawIterations > 1)
		{
			// Make sure opengl is done before we calculate the time
			gl.glFinish();
			
			long totalDrawTime = System.currentTimeMillis() - startDrawTime;
			System.out.println(totalDrawTime / numDrawIterations);
			numDrawIterations = 0;
		}
	}
	
	/**
	 * Renders a triangle at a given position
	 * 
	 * @param gl
	 * @param x
	 * @param y
	 */
	public void _render(GL2 gl, float x, float y)
	{
		Matrix3x3.push();
		
		Matrix3x3.translate(x, y);
	    
		// Send the MVP matrix to the shader
        gl.glUniformMatrix3fv(mvpAttribute, 1, false, Matrix3x3.getMatrix());
        
        // Draw the vertices pointed to by the glVertexAttribPointer
		gl.glDrawArrays(GL2.GL_TRIANGLES, 0, 3);
		
		Matrix3x3.pop();
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
	
	public float getViewWidth()
	{
		return viewWidth;
	}
	
	public float getViewHeight()
	{
		return viewHeight;
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