import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
	
	private static final long serialVersionUID = -8513201172428486833L;
	
	private static final int BATCH_SIZE = 1024;
	private static final int bytesPerFloat = Float.SIZE / Byte.SIZE;
	
	public float viewWidth, viewHeight;
	public float screenWidth, screenHeight;
	
	private FPSAnimator animator;
	
	private boolean didInit = false;
	private long totalDrawTime = 0;
	private long numDrawIterations = 0;
	
	JFrame the_frame;
	DirtGeometry geometry;
	
	private static final int NUM_THINGS = 100000;
	
	float[] position = new float[NUM_THINGS*2];
	
	// Shader attributes
	private int shaderProgram, projectionAttribute, vertexAttribute, positionAttribute;
	
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
        
        _getShaderAttributes(gl);
        
        _checkGLCapabilities(gl);
		_initGLSettings(gl);
		
		// Calculate batch of vertex data from dirt geometry
		geometry = DirtGeometry.getInstance(.1f);
		geometry.buildGeometry(viewWidth, viewHeight);
		geometry.finalizeGeometry(BATCH_SIZE);
	    
	    geometry.vertexBufferID = _generateBufferID(gl);
		_loadVertexBuffer(gl, geometry);
		
		geometry.indexBufferID = _generateBufferID(gl);
		_loadIndexBuffer(gl, geometry);
	}
	
	private void _initGLSettings(GL2 gl)
	{
		gl.glClearColor(0f, 0f, 0f, 1f);
	}
	
	private void _loadIndexBuffer(GL2 gl, Geometry geometry)
	{
	    gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, geometry.indexBufferID);
	    gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, Short.SIZE*BATCH_SIZE*geometry.getNumPoints(), geometry.indexBuffer, GL2.GL_STATIC_DRAW);
	}
	
	private void _loadVertexBuffer(GL2 gl, Geometry geometry)
	{
	    int numBytes = geometry.getNumPoints() * 3 * bytesPerFloat * BATCH_SIZE;
	    
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, geometry.vertexBufferID);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, numBytes, geometry.vertexBuffer, GL2.GL_STATIC_DRAW);
	    gl.glEnableVertexAttribArray(vertexAttribute);
	    gl.glVertexAttribPointer(vertexAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
	}
	
	private int _generateBufferID(GL2 gl)
	{
		IntBuffer bufferIDBuffer = IntBuffer.allocate(1);
		gl.glGenBuffers(1, bufferIDBuffer);
		
		return bufferIDBuffer.get(0);
	}
	
	private void _checkGLCapabilities(GL2 gl)
	{
		// TODO: Respond to this information in a meaningful way.
		boolean VBOsupported = gl.isFunctionAvailable("glGenBuffersARB") && gl.isFunctionAvailable("glBindBufferARB")
				&& gl.isFunctionAvailable("glBufferDataARB") && gl.isFunctionAvailable("glDeleteBuffersARB");
		
		System.out.println("VBO Supported: " + VBOsupported);
	}
	
	private void _getShaderAttributes(GL2 gl)
	{
        vertexAttribute = gl.glGetAttribLocation(shaderProgram, "vertex");
        projectionAttribute = gl.glGetUniformLocation(shaderProgram, "projection");
        positionAttribute = gl.glGetUniformLocation(shaderProgram, "position");
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
		
		// If we were drawing any other buffers here we'd need to set this every time
		// but instead we just leave them bound after initialization, saves a little render time
		//gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, geometry.vertexBufferID);
		//gl.glVertexAttribPointer(vertexAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
		//gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, geometry.indexBufferID);
		
	    int i = 0;
		for(; i < NUM_THINGS/BATCH_SIZE; i++)
		{
			_renderBatch(gl, geometry, i*BATCH_SIZE, BATCH_SIZE);
		}
		// Get the remainder that didn't fit perfectly into a batch
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
	
	public void _renderBatch(GL2 gl, Geometry geometry, int offset, int count)
	{
		gl.glUniform1fv(positionAttribute, count*2, position, offset*2);
		gl.glMultiDrawElements(geometry.drawMode, geometry.countBuffer, GL2.GL_UNSIGNED_SHORT, geometry.offsetBuffer, BATCH_SIZE);
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