import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Don't use directly. Subclasses should always be singleton.
 */
public class Geometry
{
	public float[] vertices = null;
	
	IntBuffer indexBuffer;
	FloatBuffer vertexBuffer;
	
	public int vertexBufferID = 0;
	public int indexBufferID = 0;
	public int positionBufferID = 0;
	public int positionTextureID = 0;
	
	public int drawMode;

	protected float width = 0;
	protected float height = 0;
	
	protected static Geometry instance = null;
	
	public static Geometry getInstance()
	{
		if(instance == null)
		{
			instance = new Geometry();
		}
		
		return instance;
	}
	
	// subclasses are expected to override this method and put something in vertices 
	public void buildGeometry(float viewWidth, float viewHeight) {}
	
	public void finalizeGeometry()
	{
		
		if(vertices == null) return;
        
        // initialize vertex Buffer (# of coordinate values * 4 bytes per float)  
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * Float.SIZE);
		vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(vertices);
        vertexBuffer.rewind();
	}
	
	public void finalizeGeometry(int numInstances)
	{
		if(vertices == null) return;

		int num_vertices = this.getNumPoints();
        int total_num_vertices = numInstances * num_vertices;
        
        // initialize vertex Buffer (# of coordinate values * 4 bytes per float)  
		ByteBuffer vbb = ByteBuffer.allocateDirect(total_num_vertices * 3 * Float.SIZE);
		vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();

		for(int i = 0; i < numInstances; i++)
		{
			for(int v = 0; v < num_vertices; v++)
			{
				int vertex_index = v * 2;
				vertexBuffer.put(vertices[vertex_index]);
				vertexBuffer.put(vertices[vertex_index+1]);
				vertexBuffer.put(i);
			}
		}
        vertexBuffer.rewind();
	    
	    // Create the indices
	    vbb = ByteBuffer.allocateDirect(total_num_vertices * Integer.SIZE);
		vbb.order(ByteOrder.nativeOrder());
		indexBuffer = vbb.asIntBuffer();
		
	    for(int i = 0; i < total_num_vertices; i++)
	    {
	    	indexBuffer.put(i);
	    }
	    indexBuffer.rewind();
	}

	public int getNumPoints() 
	{
		return vertices.length/2;
	}
}
