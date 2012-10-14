 import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
	
	private static final long serialVersionUID = -8513201172428486833L;
	public float viewWidth, viewHeight;
	public float screenWidth, screenHeight;
	
	private int currentlyBoundBuffer = 0;
	
	private FPSAnimator animator;
	
	private boolean didInit = false;
	private long totalDrawTime = 0;
	private long numDrawIterations = 0;
	
	JFrame the_frame;
	DirtGeometry geometry;
	
	private static final int NUM_THINGS = 100000;
	
	float[] position = new float[NUM_THINGS*2];
	
	// Shader attributes
	private int shaderProgram, mvpAttribute, positionAttribute, colorAttribute;
	
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
        
        positionAttribute = gl.glGetAttribLocation(shaderProgram, "position");
        mvpAttribute = gl.glGetUniformLocation(shaderProgram, "mvp");
        
		gl.glClearColor(0f, 0f, 0f, 1f);
		
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL2.GL_BLEND);
		
		boolean VBOsupported = gl.isFunctionAvailable("glGenBuffersARB") && gl.isFunctionAvailable("glBindBufferARB")
				&& gl.isFunctionAvailable("glBufferDataARB") && gl.isFunctionAvailable("glDeleteBuffersARB");
		
		System.out.println("VBO Supported: " + VBOsupported);
		
		geometry = DirtGeometry.getInstance(.1f);
		geometry.buildGeometry(viewWidth, viewHeight);
		geometry.finalizeGeometry();
		
		int bytesPerFloat = Float.SIZE / Byte.SIZE;
	    int numBytes = geometry.vertices.length * bytesPerFloat;
	    
		IntBuffer vertexBufferID = IntBuffer.allocate(1);
		gl.glGenBuffers(1, vertexBufferID);
		geometry.vertexBufferID = vertexBufferID.get(0);
		
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, geometry.vertexBufferID);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, numBytes, geometry.vertexBuffer, GL2.GL_STATIC_DRAW);
		gl.glVertexAttribPointer(positionAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
	    gl.glEnableVertexAttribArray(positionAttribute);
	    
		currentlyBoundBuffer = geometry.vertexBufferID;
		geometry.needsCompile = false;
	}
	
	// Called by me on the first resize call, useful for things that can't be initialized until the screen size is known
	public void viewInit(GL2 gl)
	{
		for(int i = 0; i < NUM_THINGS; i++)
		{
			position[i*2] = (float) (Math.random()*viewWidth);
			position[i*2+1] = (float) (Math.random()*viewWidth);
		}
	}
	
	public void display(GLAutoDrawable d)
	{
		long startDrawTime = System.currentTimeMillis();
		final GL2 gl = d.getGL().getGL2();
		
		gl.glUseProgram(shaderProgram);
       
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		Matrix.loadIdentityMV();
		
		for(int i = 0; i < NUM_THINGS; i++)
		{
			_render(gl, geometry, position[i*2], position[i*2+1]);
		}
		totalDrawTime += System.currentTimeMillis() - startDrawTime;
		numDrawIterations ++;
		if(numDrawIterations > 10)
		{
			System.out.println(totalDrawTime / numDrawIterations);
			totalDrawTime = 0;
			numDrawIterations = 0;
		}
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
		
		Matrix.ortho(0, viewWidth, 0, viewHeight);
		
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
	
	public void _render(GL2 gl, Geometry geometry, float x, float y)
	{
		Matrix.pushMV();
		Matrix.translate(x, y, 0);
		
		if (geometry.vertexBufferID != currentlyBoundBuffer)
		{
			if (geometry.vertexBufferID == 0)
			{
				return;
			}
			
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, geometry.vertexBufferID);
			gl.glVertexAttribPointer(positionAttribute, 3, GL2.GL_FLOAT, false, 0, 0);
			currentlyBoundBuffer = geometry.vertexBufferID;
		}
	    
        gl.glUniformMatrix4fv(mvpAttribute, 1, false, Matrix.multiply(Matrix.projection, Matrix.model_view), 0);
        
		gl.glDrawArrays(geometry.drawMode, 0, geometry.getNumPoints());
		
		Matrix.popMV();
	}
	
	public void screenToViewCoords(float[] xy)
	{
		float viewX = (xy[0] / screenWidth) * viewWidth;
		float viewY = viewHeight - (xy[1] / screenHeight) * viewHeight;
		xy[0] = viewX;
		xy[1] = viewY;
	}
	
	@Override
	public void dispose(GLAutoDrawable drawable)
	{
		
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
	public void windowActivated(WindowEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent arg0)
	{
		animator.stop();
		System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}
}