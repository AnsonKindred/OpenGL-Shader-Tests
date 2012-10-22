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
	private static final int NUM_THINGS = 10000000;
	private static final float MAX_SPEED = .1f;
	
	private static final int bytesPerFloat = Float.SIZE / Byte.SIZE;

	private int batchSize = 10;
	
	public float viewWidth, viewHeight;
	
	private long startTime;
	private long numDrawIterations = 0;
	
	private FPSAnimator animator;
	
	private boolean didInit = false;
	
	JFrame the_frame;
	DirtGeometry geometry;
	
	float[] velocities = new float[NUM_THINGS*2];
	
	// Shader attributes
	private int shaderProgram, projectionAttribute, vertexAttribute, positionAttribute;
	private int batchSizeAttribute, batchIndexAttribute;
	
	public static void main(String[] args) 
    {
		new GLRenderer();
    }
	
	public GLRenderer()
	{
		// setup OpenGL Version 2
		super(new GLCapabilities(GLProfile.get(GLProfile.GL2)));
		
		long heapFreeSize = Runtime.getRuntime().freeMemory();
		// Just to be safe, we won't use more than half of the available heap
		batchSize = (int) (Math.min((heapFreeSize/2)/(Float.SIZE*9), NUM_THINGS));
		System.out.println("Dynamic batch size: " + batchSize);
		
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
		gl.glUseProgram(shaderProgram);
		
        _getShaderAttributes(gl);
		
		// Calculate batch of vertex data from dirt geometry
		geometry = DirtGeometry.getInstance(.1f);
		geometry.buildGeometry(viewWidth, viewHeight);
	    
		_loadVertexBuffer(gl, geometry);
		_preparePositionBuffer(gl, geometry);
		
		gl.glClearColor(0f, 0f, 0f, 1f);
	}
	
	private void _preparePositionBuffer(GL2 gl, Geometry geometry)
	{
		geometry.positionBufferID = _generateBufferID(gl);
		gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, geometry.positionBufferID);

	    // Make sure the position sampler is bound to TEXTURE0
	    gl.glUniform1f(positionAttribute, 0);
	    gl.glActiveTexture(GL2.GL_TEXTURE0);
	    
	    // initialize buffer object
	    int size = NUM_THINGS * 2 * bytesPerFloat;
	    gl.glBufferData(GL2.GL_TEXTURE_BUFFER, size, null, GL2.GL_STATIC_DRAW);
	    
	    // The magic, point the active texture at the position buffer
	    gl.glTexBuffer(GL2.GL_TEXTURE_BUFFER, GL2.GL_RGBA32F, geometry.positionBufferID);
	    
	    gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, 0);
	}
	
	private void _loadVertexBuffer(GL2 gl, Geometry geometry)
	{
	    geometry.vertexBufferID = _generateBufferID(gl);
	    int numBytes = geometry.getNumPoints() * 3 * bytesPerFloat * batchSize;
	    
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, geometry.vertexBufferID);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, numBytes, null, GL2.GL_STATIC_DRAW);
	    gl.glEnableVertexAttribArray(vertexAttribute);
	    gl.glVertexAttribPointer(vertexAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
	    
	    ByteBuffer vertexBuffer = gl.glMapBuffer(GL2.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
	    FloatBuffer vertexFloatBuffer = vertexBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
	    
	    for(int i = 0; i < batchSize; i++)
		{
			for(int v = 0; v < geometry.vertices.length/2; v++)
			{
				int vertex_index = v * 2;
				vertexFloatBuffer.put(geometry.vertices[vertex_index]);
				vertexFloatBuffer.put(geometry.vertices[vertex_index+1]);
				vertexFloatBuffer.put(i);
			}
		}
	    
	    gl.glUnmapBuffer(GL2.GL_ARRAY_BUFFER);
	    gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
	}
	
	private void _getShaderAttributes(GL2 gl)
	{
        vertexAttribute = gl.glGetAttribLocation(shaderProgram, "vertex");
        projectionAttribute = gl.glGetUniformLocation(shaderProgram, "projection");
        positionAttribute = gl.glGetUniformLocation(shaderProgram, "positionSampler");
        batchSizeAttribute = gl.glGetUniformLocation(shaderProgram, "batchSize");
        batchIndexAttribute = gl.glGetUniformLocation(shaderProgram, "batchIndex");
	}
	
	// Called by me on the first resize call, useful for things that can't be initialized until the screen size is known
	public void viewInit(GL2 gl)
	{	
		gl.glUniformMatrix3fv(projectionAttribute, 1, false, Matrix.projection3f, 0);
		gl.glUniform1i(batchSizeAttribute, batchSize);
		
		gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, geometry.positionBufferID);
	    ByteBuffer textureBuffer = gl.glMapBuffer(GL2.GL_TEXTURE_BUFFER, GL2.GL_WRITE_ONLY);
	    FloatBuffer textureFloatBuffer = textureBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
	    
	    for(int i = 0; i < NUM_THINGS; i++)
	    {
	    	textureFloatBuffer.put((float) (Math.random()*viewWidth));
	    	textureFloatBuffer.put((float) (Math.random()*viewHeight));
	    	
	    	velocities[i*2]   = (float) Math.random()*MAX_SPEED*2 - MAX_SPEED;
	    	velocities[i*2+1] = (float) Math.random()*MAX_SPEED*2 - MAX_SPEED;
	    }
	    
	    gl.glUnmapBuffer(GL2.GL_TEXTURE_BUFFER);
	    gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, 0);
	}
	
	public void display(GLAutoDrawable d)
	{
		final GL2 gl = d.getGL().getGL2();
		
		if (!didInit || geometry.vertexBufferID == 0)
		{
			return;
		}
		
		if(startTime == 0)
		{
			startTime = System.currentTimeMillis();
		}
       
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		_updatePosition(gl);
		
		int i;
	    for(i = 0; i < NUM_THINGS/batchSize; i++)
		{
			gl.glUniform1i(batchIndexAttribute, i);
			gl.glDrawArrays(geometry.drawMode, 0, batchSize*3);
		}
		// Get the remainder that didn't fit perfectly into a batch
		gl.glUniform1i(batchIndexAttribute, i);
		gl.glDrawArrays(geometry.drawMode, 0, (NUM_THINGS - i*batchSize)*3);
		
		numDrawIterations++;
		if(numDrawIterations > 1)
		{
			long totalDrawTime = System.currentTimeMillis() - startTime;
			System.out.println(totalDrawTime / numDrawIterations);
			startTime = 0;
			numDrawIterations = 0;
		}
	}
	
	public void _updatePosition(GL2 gl)
	{
		gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, geometry.positionBufferID);
	    ByteBuffer textureBuffer = gl.glMapBuffer(GL2.GL_TEXTURE_BUFFER, GL2.GL_READ_WRITE);
	    FloatBuffer textureFloatBuffer = textureBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();
	    int i;
	    for(i = 0; i < velocities.length; i++)
	    {
	    	textureFloatBuffer.put(i, textureFloatBuffer.get(i)+velocities[i]);
	    }
	    gl.glUnmapBuffer(GL2.GL_TEXTURE_BUFFER);
	    //gl.glBindBuffer(GL2.GL_TEXTURE_BUFFER, 0);
	}
	
	public void reshape(GLAutoDrawable d, int x, int y, int width, int height)
	{
		final GL2 gl = d.getGL().getGL2();
		gl.glViewport(0, 0, width, height);
		float ratio = (float) height / width;
		
		viewWidth = 100;
		viewHeight = viewWidth * ratio;
		
		Matrix.ortho3f(0, viewWidth, 0, viewHeight);
		
		if(!didInit)
		{
			viewInit(gl);
			didInit = true;
		}
	}
	
	public float getViewWidth()
	{
		return viewWidth;
	}
	
	public float getViewHeight()
	{
		return viewHeight;
	}
	
	private int _generateBufferID(GL2 gl)
	{
		IntBuffer bufferIDBuffer = IntBuffer.allocate(1);
		gl.glGenBuffers(1, bufferIDBuffer);
		
		return bufferIDBuffer.get(0);
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