import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

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
	
	private static final int BATCH_SIZE = 1024;
	
	public float viewWidth, viewHeight;
	public float screenWidth, screenHeight;
	
	private FPSAnimator animator;
	
	private boolean didInit = false;
	private long totalDrawTime = 0;
	private long numDrawIterations = 0;
	
	IntBuffer counts;
	PointerBuffer offsets;
	
	private int elementBufferID;;
	
	JFrame the_frame;
	DirtGeometry geometry;
	
	private static final int NUM_THINGS = 100000;
	
	float[] position = new float[NUM_THINGS*2];
	
	// Shader attributes
	private int shaderProgram, projectionAttribute, vertexAttribute, colorAttribute, positionAttribute;
	
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
		
		shaderProgram = ShaderLoader.compileProgram(gl, "default");
        
        gl.glLinkProgram(shaderProgram);
        
        vertexAttribute = gl.glGetAttribLocation(shaderProgram, "vertex");
        projectionAttribute = gl.glGetUniformLocation(shaderProgram, "projection");
        positionAttribute = gl.glGetUniformLocation(shaderProgram, "position");
        
		gl.glClearColor(0f, 0f, 0f, 1f);
		
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL2.GL_BLEND);
		
		boolean VBOsupported = gl.isFunctionAvailable("glGenBuffersARB") && gl.isFunctionAvailable("glBindBufferARB")
				&& gl.isFunctionAvailable("glBufferDataARB") && gl.isFunctionAvailable("glDeleteBuffersARB");
		
		System.out.println("VBO Supported: " + VBOsupported);
		
		// Calculate batch of vertex data
		geometry = DirtGeometry.getInstance(.1f);
		geometry.buildGeometry(viewWidth, viewHeight);
		geometry.finalizeGeometry(BATCH_SIZE);
		
		int bytesPerFloat = Float.SIZE / Byte.SIZE;
	    int numBytes = geometry.getNumPoints() * 3 * bytesPerFloat * BATCH_SIZE;
	    
	    // Generate vertex buffer ID
		IntBuffer vertexBufferID = IntBuffer.allocate(1);
		gl.glGenBuffers(1, vertexBufferID);
		geometry.vertexBufferID = vertexBufferID.get(0);
		
		// Load in the vertex data
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, geometry.vertexBufferID);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, numBytes, geometry.vertexBuffer, GL2.GL_STATIC_DRAW);
	    gl.glEnableVertexAttribArray(vertexAttribute);
	    gl.glVertexAttribPointer(vertexAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
	    
	    // Create the indices
	    ByteBuffer vbb = ByteBuffer.allocateDirect(BATCH_SIZE * geometry.getNumPoints() * Short.SIZE);
		vbb.order(ByteOrder.nativeOrder());
		ShortBuffer indices = vbb.asShortBuffer();
	    for(int i = 0; i < BATCH_SIZE * geometry.getNumPoints(); i++)
	    {
	    	indices.put((short) (i));
	    }
	    indices.rewind();
	    
	    // Create the element buffer for the indices
	    IntBuffer elementBufferIDBuffer = IntBuffer.allocate(1);
	    gl.glGenBuffers(1, elementBufferIDBuffer);
	    elementBufferID = elementBufferIDBuffer.get(0);
	    
	    // Load the index data into the element buffer
	    gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, elementBufferID);
	    gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, Short.SIZE*BATCH_SIZE*geometry.getNumPoints(), indices, GL2.GL_STATIC_DRAW);
	    
	    // Create the counts
	    vbb = ByteBuffer.allocateDirect(BATCH_SIZE * Integer.SIZE);
		vbb.order(ByteOrder.nativeOrder());
		counts = vbb.asIntBuffer();
	    for(int i = 0; i < BATCH_SIZE; i++)
	    {
	    	counts.put(geometry.getNumPoints());
	    }
	    counts.rewind();
	    
	    offsets = PointerBuffer.allocateDirect(BATCH_SIZE);
	    for(int i = 0; i < BATCH_SIZE; i++)
	    {
	    	offsets.put(geometry.getNumPoints()*i*2);
	    }
	    offsets.rewind();
	    
		geometry.needsCompile = false;
	}
	
	// Called by me on the first resize call, useful for things that can't be initialized until the screen size is known
	public void viewInit(GL2 gl)
	{
		for(int i = 0; i < NUM_THINGS; i++)
		{
			position[i*2] = (float) (Math.random()*viewWidth);
			position[i*2+1] = (float) (Math.random()*viewHeight);
		}
	}
	
	public void display(GLAutoDrawable d)
	{

		if (!didInit || geometry.vertexBufferID == 0)
		{
			return;
		}
		
		long startDrawTime = System.currentTimeMillis();
		final GL2 gl = d.getGL().getGL2();
		
		gl.glUseProgram(shaderProgram);
		
		gl.glUniformMatrix3fv(projectionAttribute, 1, false, Matrix.projection3f, 0);
       
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		Matrix.loadIdentityMV3f();
		
		//gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, geometry.vertexBufferID);
		//gl.glVertexAttribPointer(vertexAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
	    int i = 0;
		for(; i < NUM_THINGS/BATCH_SIZE; i++)
		{
			_renderBatch(gl, geometry, i*BATCH_SIZE, BATCH_SIZE);
		}
		// Get the remainder that didn't git perfectly into a batch
		_renderBatch(gl, geometry, i*BATCH_SIZE, NUM_THINGS - i*BATCH_SIZE);
		
		totalDrawTime += System.currentTimeMillis() - startDrawTime;
		numDrawIterations ++;
		if(numDrawIterations > 10)
		{
			System.out.println(totalDrawTime / numDrawIterations);
			totalDrawTime = 0;
			numDrawIterations = 0;
		}
	}
	
	public void _render(GL2 gl, Geometry geometry, float x, float y)
	{
		if (geometry.vertexBufferID == 0)
		{
			return;
		}
		
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, geometry.vertexBufferID);
		gl.glVertexAttribPointer(vertexAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
	    
		//gl.glUniform2fv(positionBlockAttribute, 2, new float[]{x, y}, 0);
        
		gl.glDrawArrays(geometry.drawMode, 0, geometry.getNumPoints());
	}
	
	public void _renderBatch(GL2 gl, Geometry geometry, int offset, int count)
	{
		gl.glUniform1fv(positionAttribute, count*2, position, offset*2);
		//gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, elementBufferID);
		gl.glMultiDrawElements(geometry.drawMode, counts, GL2.GL_UNSIGNED_SHORT, offsets, BATCH_SIZE);
	}
	
	public void reshape(GLAutoDrawable d, int x, int y, int width, int height)
	{
		final GL2 gl = d.getGL().getGL2();
		gl.glViewport(0, 0, width, height);
		float ratio = (float) height / width;
		
		screenWidth = width;
		screenHeight = height;
		viewWidth = 100;
		viewHeight = viewWidth * ratio;
		
		Matrix.ortho3f(0, viewWidth, 0, viewHeight);
		
		if (!didInit)
		{
			viewInit(gl);
			didInit = true;
		} 
		else
		{
			// respond to view size changing
		}
	}
	
	public void screenToViewCoords(float[] xy)
	{
		float viewX = (xy[0] / screenWidth) * viewWidth;
		float viewY = viewHeight - (xy[1] / screenHeight) * viewHeight;
		xy[0] = viewX;
		xy[1] = viewY;
	}
	
	@Override
	public void dispose(GLAutoDrawable drawable){}
	
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
}