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

import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opengl.util.FPSAnimator;

public class GLRenderer extends GLCanvas implements GLEventListener, WindowListener
{
	
	private static final long serialVersionUID = -8513201172428486833L;
	
	private static final int bytesPerFloat = Float.SIZE / Byte.SIZE;
	private static final int bytesPerInt   = Integer.SIZE / Byte.SIZE;
	
	public float viewWidth, viewHeight;
	
	private FPSAnimator animator;
	
	private boolean didInit = false;
	
	JFrame the_frame;
	
	// Thought power of 2 might be required, doesn't seem to make a difference
	private static final int NUM_THINGS = 100000;
	private static final float THING_SIZE = .1f;
	
	private int batchSize = 1;
	
	// Shader attributes
	private int shaderProgram, projectionAttribute, vertexAttribute, positionAttribute;
	
	// vertices is just a single instance. 4 vertexes with 2 components each
	float[] vertices = {
			-THING_SIZE/2, -THING_SIZE/2,
			-THING_SIZE/2, THING_SIZE/2,
			THING_SIZE/2, THING_SIZE/2,
			THING_SIZE/2, -THING_SIZE/2
		};
	
	// positions holds the current x,y position of every instance
	float[] positions = new float[NUM_THINGS*2];
	
	int vertexBufferID = 0;
	int indexBufferID = 0;
	int positionBufferID = 0;
	int positionTextureID = 0;
	
	IntBuffer countBuffer;
	PointerBuffer offsetBuffer;
	
	public static void main(String[] args) 
    {
		new GLRenderer();
    }
	
	public GLRenderer()
	{
		// setup OpenGL Version 2
		super(new GLCapabilities(GLProfile.get(GLProfile.GL2)));
		
		addGLEventListener(this);
		setSize(1800, 1000);
				
	    the_frame = new JFrame("Hello World");
	    the_frame.getContentPane().add(this);
	    the_frame.setSize(the_frame.getContentPane().getPreferredSize());
	    the_frame.setVisible(true);
	    the_frame.addWindowListener(this);
		
		animator = new FPSAnimator(this, 60);
		animator.start();
	}
	
	// Called by the drivers when the gl context is first made available
	public void init(GLAutoDrawable d)
	{
		final GL2 gl = d.getGL().getGL2();
		
		IntBuffer asd = IntBuffer.allocate(1);
		gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, asd);
		batchSize = asd.get(0)/Float.SIZE;
		System.out.println("Batch size: " + batchSize);

		shaderProgram = ShaderLoader.compileProgram(gl, "default");
        
		gl.glLinkProgram(shaderProgram);

        _getShaderAttributes(gl);
        
		gl.glUseProgram(shaderProgram);

		gl.glClearColor(0f, 0f, 0f, 1f);
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
	}
	
	public void loadBuffers(GL2 gl)
	{
        int total_num_vertices = NUM_THINGS * 4;
        
        // initialize vertex Buffer (# of coordinate values * 4 bytes per float)  
		ByteBuffer vbb = ByteBuffer.allocateDirect(total_num_vertices * 3 * Float.SIZE);
		vbb.order(ByteOrder.nativeOrder());
        FloatBuffer vertexBuffer = vbb.asFloatBuffer();
        
		for(int i = 0; i < NUM_THINGS; i++)
		{
			for(int v = 0; v < 4; v++)
			{
				int vertex_index = v * 2;
				vertexBuffer.put(vertices[vertex_index]);
				vertexBuffer.put(vertices[vertex_index+1]);
				vertexBuffer.put(i);
			}
		}
        vertexBuffer.rewind();
	    
		// Load vertex data
	    vertexBufferID = _generateBufferID(gl);
	    // 4 points per instance, 3 components per point (x, y, i)
	    int numBytes = 4 * 3 * bytesPerFloat * NUM_THINGS;
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertexBufferID);
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, numBytes, vertexBuffer, GL2.GL_STATIC_DRAW);
		    gl.glEnableVertexAttribArray(vertexAttribute);
		    gl.glVertexAttribPointer(vertexAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
	    //gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
	    
	    // Create the indices
	    vbb = ByteBuffer.allocateDirect(total_num_vertices * Integer.SIZE);
		vbb.order(ByteOrder.nativeOrder());
		IntBuffer indexBuffer = vbb.asIntBuffer();
	    for(int i = 0; i < total_num_vertices; i++)
	    {
	    	indexBuffer.put(i);
	    }
	    indexBuffer.rewind();
		
	    // Load the index buffer
		indexBufferID = _generateBufferID(gl);
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
	    	gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, bytesPerInt*NUM_THINGS*4, indexBuffer, GL2.GL_STATIC_DRAW);
	    //gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
	    	
    	// Create the count buffer
	    vbb = ByteBuffer.allocateDirect(NUM_THINGS * Integer.SIZE);
		vbb.order(ByteOrder.nativeOrder());
		countBuffer = vbb.asIntBuffer();
	    for(int i = 0; i < total_num_vertices; i++)
	    {
	    	countBuffer.put(4);
	    }
	    countBuffer.rewind();
	    
		// create the offset buffer
	    offsetBuffer = PointerBuffer.allocateDirect(NUM_THINGS);
	    for(int i = 0; i < NUM_THINGS; i++)
	    {
	    	offsetBuffer.put(i*4*bytesPerFloat);
	    }
	    offsetBuffer.rewind();

	    // Point the positionSampler in the vertex shader at TEXTURE0
	    gl.glUniform1i(positionAttribute, 0);
	    gl.glActiveTexture(GL2.GL_TEXTURE0);
	    
	    // Set up the position buffer
		positionBufferID = _generateBufferID(gl);
	    gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, positionBufferID);
		    // Load position data into a buffer
			vbb = ByteBuffer.allocateDirect(positions.length * Float.SIZE);
			vbb.order(ByteOrder.nativeOrder());
			FloatBuffer positionBuffer = vbb.asFloatBuffer();
			positionBuffer.put(positions);
			positionBuffer.rewind();
			
			// Load buffer into gl texture buffer
			gl.glBufferData(GL2.GL_TEXTURE_BUFFER, positions.length*bytesPerFloat, positionBuffer, GL2.GL_STATIC_DRAW);
			
			IntBuffer bla = IntBuffer.allocate(1);
		    gl.glGenTextures(1, bla);
		    positionTextureID = bla.get(0);
	
	        gl.glBindTexture(GL2.GL_TEXTURE_BUFFER, positionTextureID);
		    	gl.glTexBuffer(GL2.GL_TEXTURE_BUFFER, GL2.GL_RGBA32F, positionBufferID);
		    //gl.glBindTexture(GL2.GL_TEXTURE_BUFFER, 0);
	    //gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, 0);
	    
	}
	
	private int _generateBufferID(GL2 gl)
	{
		IntBuffer bufferIDBuffer = IntBuffer.allocate(1);
		gl.glGenBuffers(1, bufferIDBuffer);
		
		return bufferIDBuffer.get(0);
	}
	
	private void _getShaderAttributes(GL2 gl)
	{
        vertexAttribute = gl.glGetAttribLocation(shaderProgram, "vertex");
        projectionAttribute = gl.glGetUniformLocation(shaderProgram, "projection");
        positionAttribute = gl.glGetUniformLocation(shaderProgram, "positionSampler");
	}
	
	// Called by me on the first resize call, useful for things that can't be initialized until the screen size is known
	public void viewInit(GL2 gl)
	{
		for(int i = 0; i < NUM_THINGS; i++)
		{
			positions[i*2] = (float) (Math.random()*viewWidth);
			positions[i*2+1] = (float) (Math.random()*viewHeight);
		}
		
		loadBuffers(gl);
		
		gl.glUniformMatrix3fv(projectionAttribute, 1, false, Matrix.projection3f, 0);
	}
	
	public void display(GLAutoDrawable d)
	{
		if (!didInit || vertexBufferID == 0) return;
		
		final GL2 gl = d.getGL().getGL2();
       
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indexBufferID);
		gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, positionBufferID);
		gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, 0);
		
		// This works, but is very much not what I want
		for(int i = 0; i < NUM_THINGS; i++)
		{
			// If I comment in the two glBindTexture lines it works
			gl.glBindTexture(GL2.GL_TEXTURE_BUFFER, 0);
			gl.glBindTexture(GL2.GL_TEXTURE_BUFFER, positionTextureID);
			gl.glDrawElements(GL2.GL_POLYGON, 4, GL2.GL_UNSIGNED_INT, i*4*bytesPerInt);
		}
		
		// This also works, but obviously isn't what I want
		//gl.glDrawElements(GL2.GL_POINTS, NUM_THINGS*4, GL2.GL_UNSIGNED_INT, 0);
		
		// This does not work, and is what I want
		//gl.glBindTexture(GL2.GL_TEXTURE_BUFFER, positionTextureID);
		//gl.glMultiDrawElements(GL2.GL_LINE_LOOP, countBuffer, GL2.GL_UNSIGNED_INT, offsetBuffer, NUM_THINGS);
		//gl.glBindTexture(GL2.GL_TEXTURE_BUFFER, 0);
	}
	
	public void reshape(GLAutoDrawable d, int x, int y, int width, int height)
	{
		final GL2 gl = d.getGL().getGL2();
		gl.glViewport(0, 0, width, height);
		float ratio = (float) height / width;
		
		viewWidth = 100;
		viewHeight = viewWidth * ratio;
		
		Matrix.ortho3f(0, viewWidth, 0, viewHeight);
		
		if (!didInit)
		{
			viewInit(gl);
			didInit = true;
		}
	}
	
	@Override
	public void dispose(GLAutoDrawable d)
	{
		final GL2 gl = d.getGL().getGL2();
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
}